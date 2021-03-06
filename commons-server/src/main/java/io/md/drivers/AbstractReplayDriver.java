/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.drivers;

import io.md.injection.DynamicInjectionConfig;
import java.io.IOException;
import java.time.Instant;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import io.md.core.CollectionKey;
import io.md.dao.*;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.Recording.RecordingType;
import io.md.injection.DynamicInjectorFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.injection.DynamicInjector;
import io.md.constants.ReplayStatus;
import io.md.services.DataStore;
import io.md.utils.Constants.POLL_REQUEST_COMPARISON_OPERATOR;
import io.md.utils.CubeObjectMapperProvider;

import io.md.core.Utils;
import io.md.utils.Constants;
import io.md.utils.UtilException;

public abstract class AbstractReplayDriver {

	private final static Logger LOGGER = LogManager.getLogger(AbstractReplayDriver.class);
	protected final Replay replay;
	public final DataStore dataStore;
	protected ObjectMapper jsonMapper;
	Map<String, DataObj> extractionMap;
	protected DynamicInjector diMgr;
	protected DynamicInjector siMgr;
	//Todo : this needs to be passed or present as singleton somewhere
	DynamicInjectorFactory factory;



	static int UPDBATCHSIZE = 10; // replay metadata will be updated after each such batch
	static int BATCHSIZE = 40; // this controls the number of requests in a batch that
	// could be sent in async fashion

	protected AbstractReplayDriver(Replay replay, DataStore dataStore) {
		super();
		this.replay = replay;
		this.dataStore = dataStore;
		this.jsonMapper = CubeObjectMapperProvider.getInstance();
		this.extractionMap = new HashMap<>();
		this.factory = new DynamicInjectorFactory(dataStore , jsonMapper);
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

		populateStaticExtactionMap();

		this.diMgr = factory.getMgr(replay.customerId, replay.app, replay.dynamicInjectionConfigVersion , extractionMap);
		this.siMgr = factory.getMgr(replay.customerId, replay.app,
			replay.dynamicInjectionConfigVersion
				.map(version -> version + DynamicInjectionConfig.staticVersionSuffix),
			extractionMap);

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

	public interface IReplayClient {

		ResponsePayload send(Event requestEvent, Replay replay)
			throws Exception;

		CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay);

		boolean isSuccessStatusCode(String statusCode);

		String getErrorStatusCode();

		boolean tearDown();
	}

	public static abstract  class AbstractIReplayClient implements IReplayClient {

		abstract RequestPayload modifyRequest(Event reqEvent);

		abstract ResponsePayload formResponsePayload(MDResponse response);

		abstract boolean verifyPayload(Event reqEvent) throws IOException;

		abstract MDHttpClient getClient(RequestDetails details);

		@Override
		public ResponsePayload send(Event requestEvent, Replay replay) throws Exception {
			MDHttpClient client = buildClient(replay, requestEvent);
			//We are reading only the response here.
			// when we read the trailers also for this http2 call , pass the same
			MDResponse response = client.makeRequest();
			return formResponsePayload(response);
		}

		@Override
		public CompletableFuture<ResponsePayload> sendAsync(Event requestEvent, Replay replay){

			try{
				MDHttpClient client = buildClient(replay, requestEvent);
				CompletableFuture<MDResponse>  response = client.makeRequestAsync();
				return response.thenApply((mdResponse -> formResponsePayload(mdResponse)));
			}catch (Exception e){
				LOGGER.error("send async error " , e);
				throw new CompletionException(e);
			}
		}

		protected MDHttpClient buildClient(Replay replay, Event reqEvent)
			throws IOException {
			if (!verifyPayload(reqEvent)) {
				throw new IOException("Invalid Payload type");
			}
			RequestPayload httpRequest = modifyRequest(reqEvent);
			RequestDetails details = io.md.core.Utils.buildRequestDetails(replay, reqEvent, httpRequest);

			return getClient(details);
		}

		@Override
		public boolean isSuccessStatusCode(String responseCode) {
			Optional<Integer> intResponse = Utils.strToInt(responseCode);
			return intResponse.map(intCode -> {
				if (Response.Status.Family.familyOf(intCode)
					.equals(Response.Status.Family.SUCCESSFUL)) {
					return true;
				}
				return false;
			}).orElse(false);
		}

		@Override
		public String getErrorStatusCode() {
			return String.valueOf(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		}

		@Override
		public boolean tearDown() {
			//do nothing
			return true;
		}

	}

	// this is just a marker interface
	public interface IReplayRequest {

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

		// TODO: create CollectionKey

		// This is a dummy lookup, just to get the Replay running status into Redis, so that
		// deferred delete  can be applied when replay ends. This is needed for very small replays
		Optional<RecordOrReplay> recordOrReplay =
			dataStore.getCurrentRecordOrReplay(replay.customerId, replay.app, replay.instanceId);

		// using seed generated from replayId so that same requests get picked in replay and analyze
		long seed = replay.replayId.hashCode();
		Random random = new Random(seed);

		// TODO: add support for matrix params

		try {
			for (String currentCollection : replay.collection) {

				if (replay.collection.size() > 1) {
					ReplayContext replayContext = replay.replayContext.orElse(new ReplayContext());
					replayContext.setCurrentCollection(currentCollection);
					replay.replayContext = Optional.of(replayContext);
					dataStore.populateCache(replay.collectionKey, RecordOrReplay.createFromReplay(replay));
				}
				Pair<Stream<List<Event>>, Long> batchedResult = ReplayUpdate
					.getRequestBatchesUsingEvents(BATCHSIZE, dataStore, replay);
				replay.reqcnt += batchedResult.getRight().intValue();
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
							if (replay.sampleRate.map(sr -> random.nextDouble() > sr)
								.orElse(false)) {
								return; // drop this request
							}
							LOGGER.debug(
								new ObjectMessage(Map.of(Constants.MESSAGE, "Enqueuing request"
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

					Map<String, Instant> reqIdRespTsMap = Map.of();
					if (!replay.tracePropagation) {
						Stream<Event> respEventStream = ReplayUpdate
							.getResponseEvents(dataStore, replay,
								reqs.stream().map(Event::getReqId).collect(Collectors.toList()));
						reqIdRespTsMap = respEventStream.collect(Collectors.toMap(e -> e.reqId,
							e -> Instant.ofEpochSecond(e.timestamp.getEpochSecond(),
								e.timestamp.getNano())));
					}
					Map<String, Instant> finalReqIdRespTsMap = reqIdRespTsMap;
					List<Event> storeEvents = new ArrayList<>();
					List<String> respcodes =
						replay.async ? sendReqAsync(reqs.stream(), storeEvents) :
							sendReqSync(reqs.stream(), finalReqIdRespTsMap, replay.collectionKey,
								storeEvents);

					if (replay.storeToDatastore) {
						boolean success = dataStore.save(storeEvents.stream());
						if (!success) {
							LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
								"Error during saving storeToDatastore", Constants.REPLAY_ID_FIELD,
								replay.replayId)));
						}
					}
					// count number of errors
					replay.reqfailed += respcodes.stream()
						.filter(s -> (!client.isSuccessStatusCode(s))).count();
				});
			}

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


	private List<String> sendReqAsync(Stream<Event> replayRequests , List<Event> storeEvents) {
		// exceptions are converted to status code indicating error
		List<CompletableFuture<String>> respcodes = replayRequests.map(request -> {
			replay.reqsent++;
			logUpdate();
			diMgr.inject(request);
			siMgr.inject(request);
			Instant requestTime = Instant.now();
			CompletableFuture<ResponsePayload> responsePayloadCompletableFuture = client
				.sendAsync(request, replay);

			return responsePayloadCompletableFuture.thenApply(responsePayload -> {
				Instant respTime = Instant.now();
				diMgr.extract(request, responsePayload);
				if (replay.storeToDatastore) {
					try {
						storeEvents(storeEvents, request, requestTime, respTime, responsePayload);
					} catch (InvalidEventException e) {
						throw new CompletionException(e);
					}
				}
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

	private void storeEvents(List<Event> storeEvents , Event goldenReq , Instant reqTime , Instant respTime, ResponsePayload responsePayload )
		throws InvalidEventException {

		String reqid = io.md.utils.Utils.generateRequestId(goldenReq.service , goldenReq.getTraceId());
		MDTraceInfo traceInfo = new MDTraceInfo(goldenReq.getTraceId() , goldenReq.getSpanId() , goldenReq.getParentSpanId());
		EventBuilder reqBuilder = new EventBuilder(goldenReq.customerId , goldenReq.app , goldenReq.service , replay.instanceId , replay.replayId ,
			traceInfo , RunType.Replay , Optional.of(reqTime) , reqid ,
			goldenReq.apiPath , goldenReq.eventType , RecordingType.Replay);
		reqBuilder.setPayload(goldenReq.payload);
		reqBuilder.setPayloadKey(goldenReq.payloadKey);
		reqBuilder.withMetaData(goldenReq.metaData);
		reqBuilder.withRunId(replay.runId);
		reqBuilder.withPayloadFields(goldenReq.payloadFields);
		reqBuilder.withSeqId(goldenReq.getSeqId()); // both will be empty. in case they are populated in future

		storeEvents.add(reqBuilder.createEvent());

		EventBuilder respBuilder = new EventBuilder(goldenReq.customerId , goldenReq.app , goldenReq.service , replay.instanceId , replay.replayId ,
			traceInfo , RunType.Replay , Optional.of(respTime) , reqid ,
			goldenReq.apiPath , EventType.mapType(goldenReq.eventType , true)  , RecordingType.Replay);
		respBuilder.setPayload(responsePayload);
		respBuilder.withRunId(replay.runId);
		respBuilder.withSeqId(goldenReq.getSeqId());
		Event respEvent = respBuilder.createEvent();
		modifyResponse(respEvent);

		storeEvents.add(respEvent);
	}

	protected abstract void modifyResponse(Event respEvent);

	private List<String> sendReqSync(Stream<Event> requests, Map<String, Instant> reqIdRespTsMap,
	  CollectionKey replayCollKey , List<Event> storeEvents) {

		return requests.map(request -> {
			try {
				replay.reqsent++;
				logUpdate();
				diMgr.inject(request);
				siMgr.inject(request);
				/* when
				1. trace propagation is not possible through service , replay ctx (of current request) is propagated through replayCtx (redis)
				2. store to datastore is true which means replay is happening through our replay driver and devtool proxy. need to propagate request traceId and spanId
				*/
				if(!replay.tracePropagation || replay.storeToDatastore){
					Optional<Instant> respTs = Optional.ofNullable(reqIdRespTsMap.get(request.getReqId()));
					Optional<ReplayContext> replayCtx = respTs.map(ts->new ReplayContext(request , ts , replay.replayContext));
					replay.replayContext = replayCtx;
					dataStore.populateCache(replayCollKey, RecordOrReplay.createFromReplay(replay));
				}
				Instant requestTime = Instant.now();

				ResponsePayload responsePayload =
					request.getMetaFieldValue(Constants.IS_POLL_REQUEST_METADATA).map(
					UtilException.rethrowFunction(isPollRequest -> {
					if (isPollRequest.equalsIgnoreCase(Constants.POLL_REQUEST_TRUE)) {
						int waitTimeInMillis = Integer.parseInt(request.getMetaFieldValue(Constants
							.POLL_REQUEST_RETRY_INTERVAL_METADATA).orElse("1")) * 1000;
						int maxNumberOfRetries = Integer.parseInt(request.getMetaFieldValue(
							Constants.POLL_REQUEST_MAX_RETRIES_METADATA).orElse("10"));
						String jsonPath = request
							.getMetaFieldValue(Constants.POLL_REQUEST_RESP_JSON_PATH_METADATA)
							.orElseThrow(() -> new Exception("Json Path to Compare Not Specified "
								+ "in Poll Event Metadata"));
						POLL_REQUEST_COMPARISON_OPERATOR comparisonOperator = Utils.valueOf(
							POLL_REQUEST_COMPARISON_OPERATOR.class, request.getMetaFieldValue(
								Constants.POLL_REQUEST_RESP_COMPARATOR_METADATA)
								.orElse(POLL_REQUEST_COMPARISON_OPERATOR.equals.name()))
							.orElseThrow(() -> new Exception("Unable to extract comparison "
								+ "operator from event metadata")) ;
						String comparisonValue = request.getMetaFieldValue(Constants
							.POLL_REQUEST_RESP_COMPARISON_VALUE_METADATA).orElseThrow(() ->
							new Exception("Poll Request Response Comparision "
								+ "Value not Specified in Metadata"));

						int numberOfRetries = 0;
						ResponsePayload intermediatePayload = null;
						while (numberOfRetries++ < maxNumberOfRetries) {
							intermediatePayload = client.send(request, replay);

							try {
								String resultToCompare = intermediatePayload
									.getValAsString(jsonPath);
								if (comparisonOperator.compare(resultToCompare, comparisonValue)) {
									break;
								}
							} catch (Exception e) {
								LOGGER.error("Error while fetching json path/"
									+ "converting json path value to integer : "
									+ jsonPath + " in poll response");
							}

							Thread.sleep(waitTimeInMillis);
						}
						return intermediatePayload;
					} else {
						return client.send(request, replay);
					}
				})).orElse(client.send(request, replay));

				Instant respTime = Instant.now();
				// Extract variables in extractionMap
				diMgr.extract(request, responsePayload);
				String statusCode = responsePayload.getStatusCode();
				if(replay.storeToDatastore){
					storeEvents(storeEvents , request , requestTime, respTime, responsePayload);
				}

				// for debugging - can remove later
				if (!client.isSuccessStatusCode(statusCode)) {
					LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "Got Error Status while Replaying Request",
							Constants.REQUEST, request.toString(), "Return Status", statusCode
							, Constants.REPLAY_ID_FIELD, replay.replayId)));
				}
				return statusCode;
			} catch (Exception e) {
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

	public static Optional<Replay> getStatus(String replayId, DataStore dataStore) {
		return dataStore.getReplay(replayId);
	}

}
