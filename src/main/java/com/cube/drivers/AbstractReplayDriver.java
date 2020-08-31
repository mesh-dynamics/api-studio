package com.cube.drivers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.md.constants.ReplayStatus;
import io.md.dao.DataObj;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.JsonDataObj;
import io.md.dao.Payload;
import io.md.dao.RecordOrReplay;
import io.md.dao.Replay;
import io.md.dao.ResponsePayload;
import io.md.injection.DynamicInjectionConfig;
import io.md.services.Analyzer;
import io.md.services.DataStore;
import io.md.utils.CubeObjectMapperProvider;

import com.cube.core.Utils;
import com.cube.dao.ReplayUpdate;
import com.cube.utils.Constants;
import com.cube.utils.InjectionVarResolver;

public abstract class AbstractReplayDriver {

	private static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);
	protected final Replay replay;
	public final DataStore dataStore;
	protected ObjectMapper jsonMapper;
	protected Optional<DynamicInjectionConfig> dynamicInjectionConfig;
	Map<String, DataObj> extractionMap;


	static int UPDBATCHSIZE = 10; // replay metadata will be updated after each such batch
	static int BATCHSIZE = 40; // this controls the number of requests in a batch that
	// could be sent in async fashion

	AbstractReplayDriver(Replay replay, DataStore dataStore) {
		super();
		this.replay = replay;
		this.dataStore = dataStore;
		this.jsonMapper = CubeObjectMapperProvider.getInstance();
		this.dynamicInjectionConfig = Optional.empty();
		this.extractionMap = new HashMap<>();
	}

	public abstract IReplayClient initClient(Replay replay) throws Exception;

	public boolean start() {

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
			return dataStore.getDynamicInjectionConfig(replay.customerId, replay.app, DIConfVersion);
		}).orElse(Optional.empty());

		if (dynamicInjectionConfig.isPresent()) {
			populateStaticExtactionMap();
		}

		CompletableFuture.runAsync(() -> replay()).handle((ret, e) -> {
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

	protected void replay() {

		//List<Request> requests = getRequests();

		if (replay.status != ReplayStatus.Init) {
			return;
		}
		replay.status = ReplayStatus.Running;
		if (!dataStore.saveReplay(replay)) {
			return;
		}
		// This is a dummy lookup, just to get the Replay running status into Redis, so that
		// deferred delete  can be applied when replay ends. This is needed for very small replays
		Optional<RecordOrReplay> recordOrReplay =
			dataStore.getCurrentRecordOrReplay(replay.customerId, replay.app, replay.instanceId);

		// using seed generated from replayId so that same requests get picked in replay and analyze
		long seed = replay.replayId.hashCode();
		Random random = new Random(seed);

		// TODO: add support for matrix params

		try {
			Pair<Stream<List<Event>>, Long> batchedResult = ReplayUpdate
				.getRequestBatchesUsingEvents(BATCHSIZE, dataStore, replay);
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
							Constants.MESSAGE,
							"Skipping request. Exception in Creating Replay Request"
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
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Error during replay", Constants.REPLAY_ID_FIELD, replay.replayId)), e);
			replay.status = ReplayStatus.Error;
		}

		//rrstore.saveReplay(replay);
		dataStore.deferredDelete(replay);
		this.client.tearDown();
	}

	private void logUpdate() {
		if (replay.reqsent % UPDBATCHSIZE == 0) {
			LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Replay Update",
				Constants.REPLAY_ID_FIELD, replay.replayId, "sentRequests", replay.reqsent
				, "totalRequests", replay.reqcnt)));
			dataStore.saveReplay(replay);
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
				extract(request, responsePayload);
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
				extract(request, responsePayload);
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

	public Replay getReplay() {
		return replay;
	}

	private void populateStaticExtactionMap() {
		this.extractionMap = replay.staticInjectionMap.map(sim -> {
			try {
				Map<String, DataObj> extractionMapTmp = new HashMap<>();
				// Read tree will set correct type of json nodes while parsing the json
				JsonNode jsonTree = jsonMapper.readTree(sim);
				Iterator<Entry<String, JsonNode>> fields = jsonTree.fields();
				while (fields.hasNext()) {
					Entry<String, JsonNode> jsonField = fields.next();
					extractionMapTmp.put(jsonField.getKey(), new JsonDataObj(jsonField.getValue(), jsonMapper));
				}
				return extractionMapTmp;
			} catch (Exception ex) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Error in reading static injection map from Replay",
					Constants.REPLAY_ID_FIELD, replay.replayId)), ex);
				return new HashMap<String, DataObj>();
			}
		}).orElse(new HashMap<String, DataObj>());
	}

	public void extract(Event goldenRequestEvent, Payload testResponsePayload) {
		dynamicInjectionConfig.ifPresent(dic-> {

			dic.extractionMetas.forEach(extractionMeta -> {
				// Test request is same as golden for replay extraction.
				InjectionVarResolver varResolver = new InjectionVarResolver(goldenRequestEvent,
					testResponsePayload,
					goldenRequestEvent.payload, dataStore);
				StringSubstitutor sub = new StringSubstitutor(varResolver);
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
					if (value != null) {
						extractionMap
							.put(sub.replace(extractionMeta.name), value);
					}
				}
			});
		});
	}

	public void inject(Event request) {

		dynamicInjectionConfig.ifPresent(dic -> {
			dic.injectionMetas.forEach(injectionMeta -> {
				StringSubstitutor sub = new StringSubstitutor(
					new InjectionVarResolver(request, null, request.payload, dataStore));

				if (injectionMeta.injectAllPaths || injectionMeta.apiPaths
					.contains(request.apiPath)) {
					String key = sub.replace(injectionMeta.name);
					DataObj value = extractionMap.get(key);
					try {
						if (value != null) {
							request.payload.put(injectionMeta.jsonPath,
								injectionMeta.map(request.payload
									.getValAsString(injectionMeta.jsonPath), value, jsonMapper));
							LOGGER.info(new ObjectMessage(
								Map.of(Constants.MESSAGE,
									"Injecting value in request before replaying",
									"Key", key,
									"Value", value,
									Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
									Constants.REQ_ID_FIELD, request.reqId)));
						} else {
							LOGGER.info(new ObjectMessage(
								Map.of(Constants.MESSAGE,
									"Not injecting value as key not found in extraction map",
									"Key", key,
									Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
									Constants.REQ_ID_FIELD, request.reqId)));
						}
					} catch (PathNotFoundException e) {
						LOGGER.error(new ObjectMessage(
							Map.of(Constants.MESSAGE,
								"Couldn't inject value as path not found in request",
								"Key", key,
								Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
								Constants.REQ_ID_FIELD, request.reqId)), e);
					} catch (Exception e) {
						LOGGER.error(new ObjectMessage(
							Map.of(Constants.MESSAGE,
								"Exception occurred while injecting in request",
								"Key", key,
								Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
								Constants.REQ_ID_FIELD, request.reqId)), e);
					}

				}
			});
		});
	}


	public static Optional<Replay> getStatus(String replayId, DataStore dataStore) {
		return dataStore.getReplay(replayId);
	}


}
