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

package com.cube.queue;

import static io.md.utils.Utils.createHTTPRequestEvent;

import io.md.cache.ProtoDescriptorCache;
import io.md.dao.GRPCPayload;
import io.md.dao.Recording.RecordingType;
import io.md.dao.Replay;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.RecordOrReplay;
import io.md.dao.ReplayContext;
import io.md.services.DataStore.TemplateNotFoundException;
import io.md.utils.Constants;
import io.md.core.Utils;
import io.md.utils.UtilException;

import com.cube.core.ServerUtils;
import com.cube.dao.CubeEventMetaInfo;
import com.cube.dao.ReqRespStore;
import com.cube.ws.CubeStore.CubeStoreException;

public class StoreUtils {

	private static final Logger LOGGER = LogManager.getLogger(StoreUtils.class);

	private static void logStoreInfo(String message, CubeEventMetaInfo e, Level level){
		Map<String, String> propertiesMap = e.getPropertiesMap();
		propertiesMap.put(Constants.MESSAGE, message);
		LOGGER.log(level, new ObjectMessage(propertiesMap));
	}

	public static void storeSingleReqResp(ReqRespStore.ReqResp rr, String path,
		MultivaluedMap<String, String> queryParams, ReqRespStore rrstore) throws CubeStoreException {

		path = CompareTemplate.normaliseAPIPath(path);

		MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
		rr.hdrs.forEach(kv -> {
			hdrs.add(kv.getKey(), kv.getValue());
		});

		MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>();
		rr.meta.forEach(kv -> {
			meta.add(kv.getKey(), kv.getValue());
		});

		CubeEventMetaInfo cubeEventMetaInfo = new CubeEventMetaInfo();

		Optional<String> customerId = Optional
			.ofNullable(meta.getFirst(Constants.CUSTOMER_ID_FIELD));
		cubeEventMetaInfo.setCustomer(customerId);
		Optional<String> app = Optional.ofNullable(meta.getFirst(Constants.APP_FIELD));
		cubeEventMetaInfo.setApp(app);
		Optional<String> instanceId = Optional
			.ofNullable(meta.getFirst(Constants.INSTANCE_ID_FIELD));
		cubeEventMetaInfo.setInstance(instanceId);
		Optional<String> service = Optional.ofNullable(meta.getFirst(Constants.SERVICE_FIELD));
		cubeEventMetaInfo.setService(service);
		Optional<String> rid = Optional.ofNullable(meta.getFirst("c-request-id"));
		cubeEventMetaInfo.setReqId(rid);
		Optional<String> type = Optional.ofNullable(meta.getFirst("type"));
		cubeEventMetaInfo.setEventType(type);
		// TODO: the following can pass replayid to cubestore but currently requests don't match in the mock
		// since we don't have the ability to ignore certain fields (in header and body)
		//if (inpcollection.isEmpty()) {
		//	inpcollection = Optional.ofNullable(hdrs.getFirst(Constants.CUBE_REPLAYID_HDRNAME));
		//}
		Instant timestamp = Optional.ofNullable(meta.getFirst("timestamp"))
			.flatMap(Utils::strToTimeStamp)
			.orElseGet(() -> {
				LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE,
					"Timestamp missing in event, using current time")));
				return Instant.now();
			});

		Optional<RecordOrReplay> recordOrReplay = rrstore
			.getCurrentRecordOrReplay(customerId, app, instanceId, true);

		if (recordOrReplay.isEmpty()) {
			throw new CubeStoreException(null, "Unable to find running record/replay"
				, cubeEventMetaInfo);
		}

		RecordingType recordingType = recordOrReplay.get().getRecordingType();
		Optional<Event.RunType> runType = Optional.of(recordOrReplay.get().getRunType());
		cubeEventMetaInfo.setRunType(runType.map(Enum::name));
		Optional<Replay> currentRunningReplay = recordOrReplay.flatMap(runningRecordOrReplay -> runningRecordOrReplay.replay);
		String runId = "";
		if(currentRunningReplay.isPresent()) {
			runId = currentRunningReplay.get().runId;
		}

		Optional<String> collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);
		cubeEventMetaInfo.setCollection(collection);
		if (collection.isEmpty()) {
			// Dropping if collection is empty, i.e. recording is not started
			throw new CubeStoreException(null, "Collection is empty", cubeEventMetaInfo);
		} else {
			logStoreInfo("Attempting store", cubeEventMetaInfo, Level.DEBUG);
		}

		MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();

		String typeStr = type.orElseThrow(() ->
			new CubeStoreException(null, "Type not specified", cubeEventMetaInfo));

		if (typeStr.equals(Constants.REQUEST)) {
			String method = Optional.ofNullable(meta.getFirst("method")).orElseThrow(() ->
				new CubeStoreException(null, "Method field missing", cubeEventMetaInfo));

			// create Event object from Request
			// fetch the template version, create template key and get a request comparator
			String templateVersion = recordOrReplay.get().getTemplateVersion();
			if (!(customerId.isPresent() && app.isPresent() && service.isPresent())) {
				throw new CubeStoreException(null, "customer id, app or service not present"
					, cubeEventMetaInfo);
			}

			TemplateKey tkey = new TemplateKey(templateVersion, customerId.get(),
				app.get(), service.get(), path, Type.RequestMatch, Optional.of(method), collection.get());

			Comparator requestComparator = null;
			try {
				requestComparator = rrstore
					.getComparator(tkey, Event.EventType.HTTPRequest);
			} catch (TemplateNotFoundException e) {
				throw new CubeStoreException(e, "Request Comparator Not Found"
					, cubeEventMetaInfo);
			}

			Event requestEvent = null;
			try {
				requestEvent = createHTTPRequestEvent(path, rid, queryParams, formParams, meta,
					hdrs, method, rr.body, collection, timestamp, runType, customerId,
					app, requestComparator, runId, recordingType);
			} catch (EventBuilder.InvalidEventException e) {
				throw new CubeStoreException(e, "Invalid Event"
					, cubeEventMetaInfo);
			}

			if (!rrstore.save(requestEvent)) {
				throw new CubeStoreException(null, "Unable to store request event in solr"
					, cubeEventMetaInfo);
			}
		} else if (typeStr.equals(Constants.RESPONSE)) {
			int status;
			try {
				status =
					Optional.ofNullable(meta.getFirst(Constants.STATUS))
						.map(Integer::valueOf).orElseThrow(() ->
						new CubeStoreException(null, "Status missing", cubeEventMetaInfo));
				// to catch number format exception
			} catch (Exception e) {
				throw new CubeStoreException(e, "Expecting Integer status"
					, cubeEventMetaInfo);
			}
			// pick apiPath from meta fields
			String reqApiPath = Optional
				.ofNullable(meta.getFirst(Constants.API_PATH_FIELD)).orElse("");

			Event responseEvent;
			try {
				if (!reqApiPath.isEmpty()) {
					URIBuilder uriBuilder = new URIBuilder(reqApiPath);
					reqApiPath = uriBuilder.getPath();
				}
				responseEvent = ServerUtils
					.createHTTPResponseEvent(reqApiPath, rid, status, meta, hdrs, rr.body,
						collection, timestamp, runType, customerId, app, rrstore, runId, recordingType);

			} catch (InvalidEventException | URISyntaxException e) {
				throw new CubeStoreException(e, "Invalid Event"
					, cubeEventMetaInfo);
			}
			if (!rrstore.save(responseEvent)) {
				throw new CubeStoreException(null, "Unable to store response event in solr"
					, cubeEventMetaInfo);
			}

		} else {
			throw new CubeStoreException(null, "Unknown type"
				, cubeEventMetaInfo);
		}

	}



	// process and store Event
	// return error string (Optional<String>)
	public static void processEvent(Event event, ReqRespStore rrstore, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) throws CubeStoreException {

		boolean saveResult = rrstore.save(checkAndTransformEvent(event, rrstore, protoDescriptorCacheOptional, Optional.empty()));
		if (!saveResult) {
			throw new CubeStoreException(null, "Unable to store event in solr", event);
		}

	}

    // process and store Event - stream version
    public static void processEvents(Stream<Event> events, ReqRespStore rrstore,
                                     Optional<ProtoDescriptorCache> protoDescriptorCacheOptional,
                                     Optional<RecordOrReplay> recordOrReplay) throws CubeStoreException {

        boolean saveResult = rrstore.save(events.map(UtilException.rethrowFunction(event -> checkAndTransformEvent(event, rrstore,
            protoDescriptorCacheOptional, recordOrReplay))));
        if (!saveResult) {
            throw new CubeStoreException(null, "Unable to store events in solr", new CubeEventMetaInfo());
        }

    }

    /*
     * If recordOrReplay is empty - fetch from current running recording or replay and update collection accordingly,
     *  else use given recordOrReplay and don't update collection
     * This will also set the payloadKey in either case
     */
    private static Event checkAndTransformEvent(Event event, ReqRespStore rrstore,
                               Optional<ProtoDescriptorCache> protoDescriptorCacheOptional, Optional<RecordOrReplay> recordOrReplay) throws CubeStoreException {
        if (event == null) {
            throw new CubeStoreException(null, "Event is null", new CubeEventMetaInfo());
        }

        if (recordOrReplay.isEmpty()) {
           event.setCollection("NA"); // so that validate doesn't fail
        }else{
        	Optional<ReplayContext>  replayContext = recordOrReplay.get().replay.flatMap(r->r.replayContext);
	        replayContext.ifPresent(ctx->{
	        	ctx.reqTraceId.ifPresent(traceId->{
	        		event.setTraceId(traceId);
	        		LOGGER.debug("setting traceId from replay context : "+traceId);
	        	});
	        	ctx.reqSpanId.ifPresent(spanId->{
	        		event.setParentSpanId(spanId);
			        LOGGER.debug("setting parentSpanId from replay context : "+spanId);
		        });
	        });
        }
        if (!event.validate()) {
            throw new CubeStoreException(null, "some required field missing,"
                + " or both binary and string payloads set", event);
        }

        Optional<String> collection = Optional.empty();
        if (recordOrReplay.isEmpty()) {
            recordOrReplay = rrstore.getCurrentRecordOrReplay(Optional.of(event.customerId), Optional.of(event.app),
                Optional.of(event.instanceId), false);
        }

        if (recordOrReplay.isEmpty()) {
            throw new CubeStoreException(null, "No current record/replay!", event);
        }
        event.setRecordingType(recordOrReplay.get().getRecordingType());
        recordOrReplay.flatMap(RecordOrReplay::getRunId).ifPresent(runId -> {
            event.setRunId(runId);
        });

        event.setRunType(recordOrReplay.get().getRunType());

        collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);

        // check collection, validate, fetch template for request, set key and store. If error at any point stop
        if (collection.isEmpty()) {
            throw new CubeStoreException(null, "Collection is missing", event);
        }
        event.setCollection(collection.get());

        if(event.payload instanceof GRPCPayload) {
            protoDescriptorCacheOptional.map(
                protoDescriptorCache -> {
                    io.md.utils.Utils.setProtoDescriptorGrpcEvent(event, protoDescriptorCache);
                    return protoDescriptorCache;
                }
            ).orElseThrow(() -> new CubeStoreException(null, "protoDescriptorCache is missing for GRPCPAyload", event));
        }

        if (event.isRequestType()) {
            // if request type, need to extract keys from request and index it, so that it can be
            // used while mocking
            Optional<String> method = Optional.empty();
            if (event.payload instanceof HTTPRequestPayload)  {
                HTTPRequestPayload payload = (HTTPRequestPayload) event.payload;
                payload.transformSubTree("/queryParams" , URLDecoder::decode);
                method = Optional.ofNullable(payload.getMethod());
            }

            try {
                Optional<URLClassLoader> classLoader = Optional.empty();
                if (event.eventType.equals(EventType.ThriftRequest)) {
                    classLoader = recordOrReplay.flatMap(RecordOrReplay::getClassLoader);
                }

                event.parseAndSetKey(rrstore.getTemplate(event.customerId, event.app, event.service, event.apiPath,
                    recordOrReplay.get().getTemplateVersion(), Type.RequestMatch, Optional.ofNullable(event.eventType),
                    method , collection.get()),
                    classLoader);
            } catch (TemplateNotFoundException e) {
                throw new CubeStoreException(e, "Compare Template Not Found", event);
            }
        }

        return event;
    }




}
