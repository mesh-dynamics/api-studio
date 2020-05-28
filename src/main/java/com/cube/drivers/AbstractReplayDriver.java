package com.cube.drivers;

import com.cube.dao.CubeMetaInfo;
import com.cube.dao.ReplayUpdate;

import io.md.constants.ReplayStatus;
import io.md.dao.DataObj;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.JsonDataObj;
import io.md.dao.Payload;
import io.md.dao.Replay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.md.dao.Event;
import io.md.dao.ResponsePayload;
import io.md.dao.RecordOrReplay;
import io.md.services.DataStore;

import com.cube.core.Utils;
import com.cube.dao.ReqRespStore;
import com.cube.injection.DynamicInjectionConfig;
import com.cube.utils.Constants;
import com.cube.ws.Config;

public abstract class AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);
	protected final Replay replay;
	public final ReqRespStore rrstore;
	protected final Config config;
	protected ObjectMapper jsonMapper;
	protected Optional<DynamicInjectionConfig> dynamicInjectionConfig;
	Map<String, DataObj> extractionMap;


	static int UPDBATCHSIZE = 10; // replay metadata will be updated after each such batch
	static int BATCHSIZE = 40; // this controls the number of requests in a batch that
	// could be sent in async fashion

	AbstractReplayDriver(Replay replay, Config config) {
		super();
		this.replay = replay;
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
		this.dynamicInjectionConfig = Optional.empty();
		this.extractionMap = new HashMap<>();
	}

	public abstract IReplayClient initClient(Replay replay) throws Exception;

	public boolean start(boolean analyze) {

		if (replay.status != ReplayStatus.Init) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Replay already running or completed", Constants.REPLAY_ID_FIELD
				, replay.replayId)));
			return false;
		}
		try {
			this.client = initClient(replay);
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Unable to initialize replay client", Constants.REPLAY_ID_FIELD, replay.replayId)),
				e);
		}
		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Starting Replay",
			Constants.REPLAY_ID_FIELD, replay.replayId)));

		this.dynamicInjectionConfig = replay.dynamicInjectionConfigVersion.map(DIConfVersion -> {
			return rrstore.getDynamicInjectionConfig(
				new CubeMetaInfo(replay.customerId, replay.app, replay.instanceId), DIConfVersion);
		}).orElse(Optional.empty());

		CompletableFuture.runAsync(() -> replay(analyze)).handle((ret, e) -> {
			if (e != null) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Exception in replaying requests", Constants.REPLAY_ID_FIELD, replay.replayId)),
					e);
			}
			return ret;
		});
		return true;
	}

	interface IReplayClient {

		ResponsePayload send(Event requestEvent, Replay replay)
			throws IOException, InterruptedException;

		CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay);

		boolean isSuccessStatusCode(String statusCode);

		String getErrorStatusCode();

		boolean tearDown();

	}

	// this is just a marker interface
	interface IReplayRequest {

	}

	private IReplayClient client;

	protected void replay(boolean analyze) {

		//List<Request> requests = getRequests();

		if (replay.status != ReplayStatus.Init) {
			return;
		}
		replay.status = ReplayStatus.Running;
		if (!rrstore.saveReplay(replay)) {
			return;
		}
		// This is a dummy lookup, just to get the Replay running status into Redis, so that
		// deferred delete  can be applied when replay ends. This is needed for very small replays
		Optional<RecordOrReplay> recordOrReplay =
			rrstore.getCurrentRecordOrReplay(Optional.of(replay.customerId),
				Optional.of(replay.app), Optional.of(replay.instanceId));

		// using seed generated from replayId so that same requests get picked in replay and analyze
		long seed = replay.replayId.hashCode();
		Random random = new Random(seed);

		// TODO: add support for matrix params

		Pair<Stream<List<Event>>, Long> batchedResult = ReplayUpdate
			.getRequestBatchesUsingEvents(BATCHSIZE, rrstore, replay);
		replay.reqcnt = batchedResult.getRight().intValue();
		// NOTE: converting long to int, should be ok, since we
		// never replay so many requests

		batchedResult.getLeft().forEach(requests -> {

			// replay.reqcnt += requests.size();

			List<Event> reqs = new ArrayList<>();
			requests.forEach(eventReq -> {

				try {
					/*
                     TODO: currently sampling samples across all paths with same rate.
                      If we want to ensure that we have some minimum requests from each path
                      (particularly the rare ones), we need to add more logic
                    */
					if (replay.sampleRate.map(sr -> random.nextDouble() > sr).orElse(false)) {
						return; // drop this request
					}
					LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Enqueuing request"
							+ "for reply", Constants.REPLAY_ID_FIELD, replay.replayId
						, Constants.REQ_ID_FIELD, Optional.ofNullable(eventReq.reqId)
							.orElse(Constants.NOT_PRESENT), Constants.TRACE_ID_FIELD,
						eventReq.getTraceId())));
					reqs.add(eventReq);

				} catch (Exception e) {
					LOGGER.error(new ObjectMessage(Map.of(
						Constants.MESSAGE, "Skipping request. Exception in Creating Replay Request"
						, Constants.REQ_ID_FIELD,
						Optional.ofNullable(eventReq.reqId).orElse(Constants.NOT_PRESENT)
						, Constants.REPLAY_ID_FIELD, replay.replayId
					)), e);
				}
			});

			List<String> respcodes = replay.async ? sendReqAsync(reqs.stream()) :
				sendReqSync(reqs.stream());

			// count number of errors
			replay.reqfailed += respcodes.stream()
				.filter(s -> (!client.isSuccessStatusCode(s))).count();
		});

		LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Completed"
			, Constants.REPLAY_ID_FIELD, replay.replayId,
			"totalRequests", replay.reqcnt, "errorRequests", replay.reqfailed)));

		replay.status =
			(replay.reqfailed == 0) ? ReplayStatus.Completed : ReplayStatus.Error;

		rrstore.saveReplay(replay);
		if (analyze) {
			analyze();
		}
		this.client.tearDown();
	}

	private void logUpdate() {
		if (replay.reqsent % UPDBATCHSIZE == 0) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Update",
				Constants.REPLAY_ID_FIELD, replay.replayId, "sentRequests", replay.reqsent
				, "totalRequests", replay.reqcnt)));
			rrstore.saveReplay(replay);
		}
	}


	private List<String> sendReqAsync(Stream<Event> replayRequests) {
		// exceptions are converted to status code indicating error
		List<CompletableFuture<String>> respcodes = replayRequests.map(request -> {
			replay.reqsent++;
			logUpdate();
			inject(request);
			CompletableFuture<ResponsePayload> responsePayloadCompletableFuture = client
				.sendAsync(request, replay);

			return responsePayloadCompletableFuture.thenApply(responsePayload -> {
				extract(request, responsePayload, rrstore);
				return responsePayload.getStatusCode();
			}).handle((ret, e) -> {
				if (e != null) {
					LOGGER.error(
						new ObjectMessage(
							Map.of(Constants.MESSAGE, "Exception in replaying requests")),
						e);
					return client.getErrorStatusCode();
				}
				return ret;
			});
		}).collect(Collectors.toList());

		CompletableFuture<List<String>> rcodes = Utils.sequence(respcodes);

		try {
			return rcodes.get();
		} catch (InterruptedException | ExecutionException e) {
			LOGGER.error(
				new ObjectMessage(Map.of(Constants.MESSAGE, "Exception in replaying requests",
					Constants.REPLAY_ID_FIELD, replay.replayId)), e);
			return Collections
				.nCopies(respcodes.size(), client.getErrorStatusCode());
		}
	}

	private List<String> sendReqSync(Stream<Event> requests) {

		return requests.map(request -> {
			try {
				replay.reqsent++;
				logUpdate();
				inject(request);
				ResponsePayload responsePayload = client.send(request, replay);
				// Extract variables in extractionMap
				extract(request, responsePayload, rrstore);
				String statusCode = responsePayload.getStatusCode();

				// for debugging - can remove later
				if (!client.isSuccessStatusCode(statusCode)) {
					LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "Got Error Status while Replaying Request",
							Constants.REQUEST, request.toString(), "Return Status", statusCode
							, Constants.REPLAY_ID_FIELD, replay.replayId)));
				}
				return statusCode;
			} catch (IOException | InterruptedException e) {
				LOGGER.error(
					new ObjectMessage(Map.of(Constants.MESSAGE, "Exception in replaying requests"
						, Constants.REPLAY_ID_FIELD, replay.replayId)), e);
				return client.getErrorStatusCode();
			}
		}).collect(Collectors.toList());
	}

	public void analyze() {
		ReplayStatus status = ReplayStatus.Running;
		while (status == ReplayStatus.Running) {
			try {
				Thread.sleep(5000);
				Optional<Replay> currentRunningReplay = rrstore
					.getCurrentRecordOrReplay(Optional.of(replay.customerId),
						Optional.of(replay.app), Optional.of(replay.instanceId))
					.flatMap(runningRecordOrReplay -> runningRecordOrReplay.replay);
				status = currentRunningReplay.filter(runningReplay -> runningReplay.
					replayId.equals(replay.replayId)).map(r -> r.status).orElse(replay.status);
			} catch (InterruptedException e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Exception while sleeping  the thread", Constants.REPLAY_ID_FIELD
					, replay.replayId)));
			}
		}
		try {
			Analyzer.analyze(replay.replayId, "", config);
		} catch (DataStore.TemplateNotFoundException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Unable to analyze replay since template does not exist :",
				Constants.REPLAY_ID_FIELD,
				replay.replayId)), e);
		}
	}

	public Replay getReplay() {
		return replay;
	}

	public void extract(Event goldenRequestEvent, Payload testResponsePayload,
		ReqRespStore rrstore) {
		// Test request is same as golden for replay extraction.
		VarResolver varResolver = new VarResolver(goldenRequestEvent, testResponsePayload,
			goldenRequestEvent.payload, rrstore);
		StringSubstitutor sub = new StringSubstitutor(varResolver);
		dynamicInjectionConfig.get().extractionMetas.forEach(extractionMeta -> {
			DataObj value;
			if (extractionMeta.apiPath.equalsIgnoreCase(goldenRequestEvent.apiPath)) {
				//  TODO ADD checks for method type GET/POST & also on reset field
				String sourceString = extractionMeta.value;
				// Boolean placeholder to specify if the value to be extracted
				// is an Object and not a string.
				// NOTE - if this is true value should be a single source & jsonPath
				// (Only one placeholder of ${Source: JSONPath}
				if (extractionMeta.valueObject) {
					String lookupString = sourceString.trim()
						.substring(sourceString.indexOf("{") + 1, sourceString.indexOf("}"));
					value = varResolver.lookupObject(lookupString);
				} else {
					String valueString = sub.replace(sourceString);
					value = new JsonDataObj(new TextNode(valueString), jsonMapper);
				}
				extractionMap
					.put(sub.replace(extractionMeta.name), value);
			}
		});
	}


	public void inject(Event request) {
		StringSubstitutor sub = new StringSubstitutor(
			new VarResolver(request, null, request.payload, rrstore));

		dynamicInjectionConfig.get().injectionMetas.forEach(injectionMeta -> {
			if (injectionMeta.injectAllPaths || injectionMeta.apiPaths.contains(request.apiPath)) {
				String key = sub.replace(injectionMeta.name);
				DataObj value = extractionMap.get(key);
				try {
					if (value != null) {
						request.payload.put(injectionMeta.jsonPath, value);
						LOGGER.info(new ObjectMessage(
							Map.of(Constants.MESSAGE, "Injecting value in request before replaying",
								"Key", key,
								"Value", value,
								Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
								Constants.REQ_ID_FIELD, request.reqId)));
					}
				} catch (PathNotFoundException e) {
					LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "Not injecting value as key not found in request",
							"Key", key,
							Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
							Constants.REQ_ID_FIELD, request.reqId)), e);
				}

			}
		});

	}


	public static Optional<Replay> getStatus(String replayId, ReqRespStore rrstore) {
		return rrstore.getReplay(replayId);
	}


	static class VarResolver implements StringLookup {

		Event goldenRequestEvent;
		Payload testResponsePayload;
		Payload testRequestPayload;
		ReqRespStore rrstore;

		public VarResolver(Event goldenRequestEvent, Payload testResponsePayload,
			Payload testRequestPayload, ReqRespStore rrstore) {
			this.goldenRequestEvent = goldenRequestEvent;
			this.testResponsePayload = testResponsePayload;
			this.testRequestPayload = testRequestPayload;
			this.rrstore = rrstore;
		}

		public Pair<Payload, String> getSourcePayloadAndJsonPath(String lookupString) {
			String[] splitStrings = lookupString.split(":");
			if (splitStrings.length != 2) {
				LOGGER.error("Lookup String format mismatch");
				return null; // Null resorts to default variable in substitutor
			}
			String source = splitStrings[0].trim();
			String jsonPath = splitStrings[1].trim();
			Payload sourcePayload;
			switch (source) {
				case Constants.GOLDEN_REQUEST:
					sourcePayload = goldenRequestEvent.payload;
					break;
				case Constants.GOLDEN_RESPONSE:
					Optional<Event> goldenResponseOptional = rrstore
						.getResponseEvent(goldenRequestEvent.reqId);
					if (goldenResponseOptional.isEmpty()) {
						LOGGER.error("Cannot fetch golden response for golden request");
						return null; // Null resorts to default variable in substitutor
					}
					sourcePayload = goldenResponseOptional.get().payload;
					break;
				case Constants.TESTSET_RESPONSE:
					sourcePayload = testResponsePayload;
					break;
				case Constants.TESTSET_REQUEST:
					sourcePayload = testRequestPayload;
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + source);
			}
			return new ImmutablePair<>(sourcePayload, jsonPath);
		}

		public DataObj lookupObject(String lookupString) {
			Pair<Payload, String> pair = getSourcePayloadAndJsonPath(lookupString);
			Payload sourcePayload = pair.getLeft();
			String jsonPath = pair.getRight();
			DataObj value;
			value = sourcePayload.getVal(jsonPath);
			return value;
		}

		@Override
		/** Lookup String will always be in format of
		 "VariableSources: <JSONPath>
		 **/
		public String lookup(String lookupString) {
			String value = null;
			Pair<Payload, String> pair = getSourcePayloadAndJsonPath(lookupString);
			Payload sourcePayload = pair.getLeft();
			String jsonPath = pair.getRight();
			try {
				value = sourcePayload.getValAsString(jsonPath);
			} catch (PathNotFoundException e) {
				LOGGER.error("Cannot find JSONPath" + jsonPath + " in source", e);
				return null;
			}
			return value;
		}
	}

}
