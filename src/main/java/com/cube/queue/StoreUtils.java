package com.cube.queue;

import static io.md.utils.Utils.createHTTPRequestEvent;

import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonProcessingException;

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
import io.md.services.DataStore.TemplateNotFoundException;

import com.cube.core.Utils;
import com.cube.dao.CubeEventMetaInfo;
import com.cube.dao.ReqRespStore;
import com.cube.utils.Constants;
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

		Optional<Event.RunType> runType = Optional.of(recordOrReplay.get().getRunType());
		cubeEventMetaInfo.setRunType(runType.map(Enum::name));

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
				app.get(), service.get(), path, Type.RequestMatch);

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
					app, requestComparator);
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
				responseEvent = Utils
					.createHTTPResponseEvent(reqApiPath, rid, status, meta, hdrs, rr.body,
						collection, timestamp, runType, customerId, app, rrstore);

			} catch (JsonProcessingException | InvalidEventException | URISyntaxException e) {
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
	public static void processEvent(Event event, ReqRespStore rrstore) throws CubeStoreException {
		if (event == null) {
			throw new CubeStoreException(null, "Event is null", new CubeEventMetaInfo());
		}

		Optional<String> collection;

		event.setCollection("NA"); // so that validate doesn't fail

		if (!event.validate()) {
			throw new CubeStoreException(null, "some required field missing,"
				+ " or both binary and string payloads set", event);
		}

		Optional<RecordOrReplay> recordOrReplay =
			rrstore.getCurrentRecordOrReplay(Optional.of(event.customerId),
				Optional.of(event.app), Optional.of(event.instanceId), true);

		if (recordOrReplay.isEmpty()) {
			throw new CubeStoreException(null, "No current record/replay!", event);
		}

		event.setRunType(recordOrReplay.get().getRunType());

		collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);

		// check collection, validate, fetch template for request, set key and store. If error at any point stop
		if (collection.isEmpty()) {
			throw new CubeStoreException(null, "Collection is missing", event);
		}
		event.setCollection(collection.get());
		if (event.isRequestType()) {
			// if request type, need to extract keys from request and index it, so that it can be
			// used while mocking
			if (event.payload instanceof HTTPRequestPayload)  {
				HTTPRequestPayload payload = (HTTPRequestPayload) event.payload;
				payload.transformSubTree("/queryParams" , URLDecoder::decode);
			}

			try {
				Optional<URLClassLoader> classLoader = Optional.empty();
				if (event.eventType.equals(EventType.ThriftRequest)) {
					classLoader = recordOrReplay.flatMap(RecordOrReplay::getClassLoader);
				}

				event.parseAndSetKey(rrstore.getRequestMatchTemplate(event,
					recordOrReplay.get().getTemplateVersion()), classLoader);
			} catch (TemplateNotFoundException e) {
				throw new CubeStoreException(e, "Compare Template Not Found", event);
			}
		}

		boolean saveResult = rrstore.save(event);
		if (!saveResult) {
			throw new CubeStoreException(null, "Unable to store event in solr", event);
		}

	}

}
