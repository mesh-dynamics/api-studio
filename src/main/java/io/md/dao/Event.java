package io.md.dao;

/*
 *
 *    Copyright Cube I O
 *
 */


import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.constants.Constants;
import io.md.core.ReplayTypeEnum;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.utils.UtilException;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */

/**
 * Event represents a generic event that is recorded. It can be a request or response captured from
 * various protocols such as REST over HTTP, Java function calls, GRPC, Thrift etc. The common
 * metadata for all such events in captured in the event fields. The payload represents the actual
 * data that can be encoded in different formats such as Json, ProtoBuf, Thrift etc depending on the
 * event type
 */
public class Event {

	private static final Logger LOGGER = LogManager.getLogger(Event.class);


	private Event(String customerId, String app, String service, String instanceId,
		String collection, String traceId,
		RunType runType, Instant timestamp, String reqId, String apiPath, EventType eventType,
		byte[] rawPayloadBinary,
		String rawPayloadString, DataObj payload, int payloadKey, AbstractMDPayload rawPayloadObject) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instanceId;
		this.collection = collection;
		this.traceId = traceId;
		this.runType = runType;
		this.timestamp = timestamp;
		this.reqId = reqId;
		this.apiPath = apiPath;
		this.eventType = eventType;
		this.rawPayloadBinary = rawPayloadBinary;
		this.rawPayloadString = rawPayloadString;
		this.payload = payload;
		this.payloadKey = payloadKey;
		this.rawPayloadObject = rawPayloadObject;
	}

	/**
	 * For jackson
	 */
	private Event() {
		this.customerId = null;
		this.app = null;
		this.service = null;
		this.instanceId = null;
		this.collection = null;
		this.traceId = null;
		this.runType = RunType.Record;
		this.timestamp = null;
		this.reqId = null;
		this.apiPath = null;
		this.eventType = null;
		this.rawPayloadBinary = null;
		this.rawPayloadString = null;
		this.payload = null;
		this.payloadKey = 0;
		this.rawPayloadObject = null;
	}

	public static List<EventType> getRequestEventTypes() {
		return REQUEST_EVENT_TYPES;
	}

	public static boolean isReqType(EventType eventType) {
		return REQUEST_EVENT_TYPES.contains(eventType);
	}

	public String getCollection() {
		return collection;
	}

	public boolean validate() {

		if ((customerId == null) || (app == null) || (service == null) || (instanceId == null) || (
			collection == null)
			|| (traceId == null && eventType != EventType.ThriftResponse
			&& eventType != EventType.ThriftRequest) || (runType == null) ||
			(timestamp == null) || (reqId == null) || (apiPath == null) || (eventType == null)
			|| ((rawPayloadBinary == null) && (rawPayloadString == null || rawPayloadString.trim()
			.isEmpty()) && (rawPayloadObject == null))) {
			return false;
		}
		return true;
	}

	//TODO keep this logic in cube respository
/*	public void parseAndSetKeyAndCollection(Config config, String collection,
		CompareTemplate template)
		throws DataObjCreationException {
		this.collection = collection;
		parseAndSetKey(config, template);
	}

	public void parseAndSetKey(Config config, CompareTemplate template) {
		parseAndSetKey(config,template,Optional.empty());
	}

	public void parseAndSetKey(Config config, CompareTemplate template, Optional<URLClassLoader> classLoader)  {
		parsePayLoad(config, classLoader);
		List<String> keyVals = new ArrayList<>();
		payload.collectKeyVals(path -> template.getRule(path).getCompareType()
			== CompareTemplate.ComparisonType.Equal, keyVals);
		LOGGER.info(new ObjectMessage(
			Map.of("message", "Generating event key from vals", "vals", keyVals.toString())));
		payloadKey = Objects.hash(keyVals);
		if (eventType == EventType.ThriftRequest) {
			this.traceId = ((ThriftDataObject) payload).traceId;
		}
		LOGGER.info(
			new ObjectMessage(Map.of("message", "Event key generated", "key", payloadKey)));
	}*/

	public DataObj parsePayLoad(Map<String, Object> params) {
		// parse if not already parsed
		if (payload == null) {
			;
			// TODO commenting this out from here need to put this logic in md-thrift-commons
//			if ((Objects.equals(this.eventType, EventType.ThriftRequest) ||
//				Objects.equals(this.eventType, EventType.ThriftResponse)) && this.apiPath != null) {
//				Map<String, Object> newParams = Utils.extractThriftParams(this.apiPath);
//				params.putAll(newParams);
//				// TODO push the class loader from outside
//				/*  classLoader.ifPresent(urlClassLoader -> finalParams
//					.put(Constants.CLASS_LOADER, urlClassLoader));*/
//			}
			payload = DataObjFactory
				.build(eventType, rawPayloadBinary, rawPayloadString, params);
		}

		return payload;
	}

	@JsonIgnore
	public boolean isRequestType() {
		return REQUEST_EVENT_TYPES.contains(eventType);
	}

	public String getReqId() {
		return reqId;
	}

	public String getPayloadAsString(Map<String, Object> params) {
		parsePayLoad(params);
		return payload.toString();
	}

	public DataObj getPayload(Map<String, Object> params) {
		parsePayLoad(params);
		return payload;
	}

	public String getPayloadAsJsonString(Map<String, Object> params) {
		switch (this.eventType) {
			case HTTPRequest:
			case HTTPResponse:
				return rawPayloadString;
			case JavaRequest:
			case JavaResponse:
				try {
					return getPayload(params).getValAsString(Constants.FN_RESPONSE_PATH);
				} catch (PathNotFoundException e) {
					LOGGER.error(new ObjectMessage(
						Map.of(
							Constants.MESSAGE, "Response path not found in JSON"
						)));
				}
			case ThriftRequest:
			case ThriftResponse:
			case ProtoBufRequest:
			case ProtoBufResponse:
			default:
				throw new NotImplementedException("Thrift and Protobuf not implemented");
		}
	}

	// TODO keep this in cube repository

	/**
	 * Create a new event with transformed payload. While transforming request events, need to send
	 * the comparator so that payloadKey can be calculated
	 * <p>
	 * //@param rhs
	 * //@param operationList
	 * //@param config
	 * //@param newCollection
	 * //@param newReqId
	 * //@return
	 * //@throws EventBuilder.InvalidEventException
	 */
	/*public Event applyTransform(Optional<Event> rhs, List<ReqRespUpdateOperation> operationList,
		Config config,
		String newCollection, String newReqId, Optional<Comparator> comparator)
		throws EventBuilder.InvalidEventException {
		// parse if not already parsed
		parsePayLoad(config);
		Optional<RawPayload> newPayload = rhs.map(rhsEvent -> {
			DataObj transformedDataObj = payload
				.applyTransform(rhsEvent.getPayload(config), operationList);
			DataObjFactory.wrapIfNeeded(transformedDataObj, eventType);
			return transformedDataObj.toRawPayload();
		});

		// payload doesn't change if rhs is empty
		byte[] newRawPayloadBinary = newPayload.map(newPayloadVal -> newPayloadVal.rawPayloadBinary)
			.orElse(rawPayloadBinary);
		String newRawPayloadString = newPayload.map(newPayloadVal -> newPayloadVal.rawPayloadString)
			.orElse(rawPayloadString);

		Event toReturn = new EventBuilder(customerId, app, service, instanceId, newCollection,
			traceId,
			runType, timestamp, newReqId, apiPath, eventType)
			.setRawPayloadBinary(newRawPayloadBinary)
			.setRawPayloadString(newRawPayloadString)
			.createEvent();
		// set key for request events
		comparator.ifPresent(
			comparatorVal -> toReturn.parseAndSetKey(config, comparatorVal.getCompareTemplate()));
		return toReturn;
	}*/

	public enum EventType {
		HTTPRequest,
		HTTPResponse,
		JavaRequest,
		JavaResponse,
		ThriftRequest,
		ThriftResponse,
		ProtoBufRequest,
		ProtoBufResponse;

		public static EventType getResponseType(EventType eventType) {
			switch (eventType) {
				case HTTPRequest:
				case HTTPResponse:
					return HTTPResponse;
				case JavaRequest:
				case JavaResponse:
					return JavaRequest; // JavaRequest itself has response. Check if JavaResponse can be removed
				case ThriftRequest:
				case ThriftResponse:
					return ThriftResponse;
				case ProtoBufRequest:
				case ProtoBufResponse:
					return ProtoBufResponse;
				default:
					return HTTPResponse;
			}
		}

		public static EventType fromReplayType(ReplayTypeEnum replayType) {
			switch (replayType) {
				case THRIFT:
					return ThriftRequest;
				case GRPC:
					return ProtoBufRequest;
				default:
					return HTTPRequest;
			}
		}
	}

	public static final List<EventType> REQUEST_EVENT_TYPES = List
		.of(EventType.HTTPRequest, EventType.JavaRequest,
			EventType.ThriftRequest, EventType.ProtoBufRequest);
	// currently JavaRequest stores the response as well
	// TODO: change JavaRequest to JavaResponse in list below once we separate the two
	public static final List<EventType> RESPONSE_EVENT_TYPES = List
		.of(EventType.HTTPResponse, EventType.JavaRequest,
			EventType.ThriftResponse, EventType.ProtoBufResponse);


	public final String customerId;
	public final String app;
	public final String service;
	public final String instanceId;
	private String collection;
	private String traceId;
	public final RunType runType;


	public void setCollection(String collection) {
		this.collection = collection;
	}

	public final Instant timestamp;
	public final String reqId; // for responses, this is the reqId of the corresponding request
	public final String apiPath; // apiPath for HTTP req, function signature for Java functions, etc
	public final EventType eventType;

	// Payload can be binary or string. Keeping both types, since otherwise we will have to encode string also
	// as base64. For debugging its easier if the string is readable.
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	public final byte[] rawPayloadBinary;
	public final String rawPayloadString;

	public final AbstractMDPayload rawPayloadObject;

	@JsonIgnore
	DataObj payload;

	@JsonIgnore
	public int payloadKey;

	/* when did the event get created - record, replay or manually added */
	public enum RunType {
		Record,
		Replay,
		Manual  // manually created e.g. default requests and responses
	}

	public String getTraceId() {
		return this.traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public static class EventBuilder {

		private static final Logger LOGGER = LogManager.getLogger(Event.class);

		private final String customerId;
		private final String app;
		private final String service;
		private final String instanceId;
		private final String collection;
		private final String traceId;
		private final String spanId;
		private final String parentSpanId;
		private final Event.RunType runType;
		private final Optional<Instant> timestamp;
		private final String reqId;
		private final String apiPath;
		private final Event.EventType eventType;
		private byte[] rawPayloadBinary;
		private String rawPayloadString;
		private AbstractMDPayload rawPayloadObject;
		private DataObj payload;
		private int payloadKey = 0;

		public EventBuilder(String customerId, String app, String service, String instanceId,
			String collection, MDTraceInfo mdTraceInfo,
			Event.RunType runType, Optional<Instant> timestamp, String reqId, String apiPath,
			Event.EventType eventType) {
			this.customerId = customerId;
			this.app = app;
			this.service = service;
			this.instanceId = instanceId;
			this.collection = collection;

			this.traceId = mdTraceInfo.traceId;
			this.spanId = mdTraceInfo.spanId;
			this.parentSpanId = mdTraceInfo.parentSpanId;

			this.runType = runType;
			this.timestamp = timestamp;
			this.reqId = reqId;
			this.apiPath = apiPath;
			this.eventType = eventType;
		}

		public EventBuilder(CubeMetaInfo cubeMetaInfo, MDTraceInfo mdTraceInfo,
			Event.RunType runType, String apiPath, EventType eventType, Optional<Instant> timestamp,
			String reqId, String collection) {
			this.customerId = cubeMetaInfo.customerId;
			this.app = cubeMetaInfo.appName;
			this.instanceId = cubeMetaInfo.instance;
			this.service = cubeMetaInfo.serviceName;

			this.traceId = mdTraceInfo.traceId;
			this.spanId = mdTraceInfo.spanId;
			this.parentSpanId = mdTraceInfo.parentSpanId;

			this.runType = runType;
			this.apiPath = apiPath;
			this.eventType = eventType;
			this.timestamp = timestamp;
			this.reqId = reqId;
			this.collection = collection;
		}


		public EventBuilder setRawPayloadBinary(byte[] rawPayloadBinary) {
			this.rawPayloadBinary = rawPayloadBinary;
			return this;
		}

		public EventBuilder setRawPayloadString(String rawPayloadString) {
			this.rawPayloadString = rawPayloadString;
			return this;
		}

		public EventBuilder setPayload(DataObj payload) {
			this.payload = payload;
			return this;
		}

		public EventBuilder setPayloadKey(int payloadKey) {
			this.payloadKey = payloadKey;
			return this;
		}

		public EventBuilder setRawPayload(AbstractMDPayload rawPayload) {
			this.rawPayloadObject = rawPayload;
			return this;
		}

		public Event createEvent() throws io.md.dao.Event.EventBuilder.InvalidEventException {
			if (timestamp.isEmpty()) {
				LOGGER.info(new ObjectMessage(
					Map.of(Constants.MESSAGE, "Timestamp empty, using current instant")));
			}
			Event event = new Event(customerId, app, service, instanceId, collection, traceId,
				runType, timestamp.orElse(Instant.now()), reqId, apiPath,
				eventType,
				rawPayloadBinary, rawPayloadString, payload, payloadKey,rawPayloadObject);
			if (event.validate()) {
				return event;
			} else {
				throw new io.md.dao.Event.EventBuilder.InvalidEventException();
			}
		}

		public Optional<Event> createEventOpt() {
			try {
				return Optional.of(createEvent());
			} catch (io.md.dao.Event.EventBuilder.InvalidEventException e) {
				LOGGER.error("Exception in creating an Event", e.getMessage(),
					UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
			}
			return Optional.empty();
		}


		public static class InvalidEventException extends Exception {

		}
	}

	public static class RawPayload {

		public final byte[] rawPayloadBinary;
		public final String rawPayloadString;

		public RawPayload(byte[] rawPayloadBinary, String rawPayloadString) {
			this.rawPayloadBinary = rawPayloadBinary;
			this.rawPayloadString = rawPayloadString;
		}
	}

}
