package io.md.dao;

/*
 *
 *    Copyright Cube I O
 *
 */

import io.md.dao.DataObj.PathNotFoundException;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.ReplayTypeEnum;
import io.md.dao.Event.EventBuilder.InvalidEventException;


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

	private static final Logger LOGGER = LoggerFactory.getLogger(Event.class);

	private Event(String customerId, String app, String service, String instanceId,
		String collection, String traceId, String spanId, String parentSpanId,
		RunType runType, Instant timestamp, String reqId, String apiPath, EventType eventType,
		Payload payload, int payloadKey) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instanceId;
		this.collection = collection;
		this.traceId = traceId;
		this.spanId = spanId;
		this.parentSpanId = parentSpanId;
		this.runType = runType;
		this.timestamp = timestamp;
		this.reqId = reqId;
		this.apiPath = CompareTemplate.normaliseAPIPath(apiPath);
		this.eventType = eventType;
		this.payload = payload;
		this.payloadKey = payloadKey;
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
		this.spanId = null;
		this.parentSpanId = null;
		this.runType = RunType.Record;
		this.timestamp = null;
		this.reqId = null;
		this.apiPath = null;
		this.eventType = null;
		this.payload = null;
		this.payloadKey = 0;
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
			|| (payload == null || payload.isRawPayloadEmpty())) {
			return false;
		}
		return true;
	}

	@JsonIgnore
	public boolean isRequestType() {
		return REQUEST_EVENT_TYPES.contains(eventType);
	}

	public String getReqId() {
		return reqId;
	}

	@JsonIgnore
	public String getPayloadAsJsonString() {
		return getPayloadAsJsonString(false);
	}

	@JsonIgnore
	public ConvertEventPayloadResponse checkAndConvertResponseToString(boolean wrapForDisplay, List<String> pathsToKeep, long size, String path) {
		ConvertEventPayloadResponse response = new ConvertEventPayloadResponse();
		if(this.payload != null) {
			this.updatePayloadBody();
			if(this.payload.size() > size) {
				this.payload.replaceContent(pathsToKeep, path, size);
				response.setTruncated(true);
			}
			response.setResponse(this.getPayloadAsJsonString(wrapForDisplay));
		}
		return response;
	}

	@JsonIgnore
	public void updatePayloadBody() {
		try {
			this.payload.updatePayloadBody();
		} catch (PathNotFoundException e) {
			LOGGER.error("Error while "
					+ "updating payload body", e);
		}
	}

	@JsonIgnore
	public String getPayloadAsJsonString(boolean wrapForDisplay) {
		if (this.payload != null && !this.payload.isRawPayloadEmpty()) {
			try {
				return this.payload.rawPayloadAsString(wrapForDisplay);
			} catch (Exception e) {
				LOGGER.error("Error while "
					+ "converting payload to json string", e);
			}
		}
		return "";
	}

	public enum EventType {
		HTTPRequest,
		HTTPResponse,
		JavaRequest,
		JavaResponse,
		ThriftRequest,
		ThriftResponse,
		ProtoBufRequest,
		ProtoBufResponse;

		public static EventType mapType(EventType sourceType, boolean requireResponseType) {
			switch (sourceType) {
				case JavaRequest:
				case JavaResponse:
					return requireResponseType ? JavaResponse : JavaRequest;
				case ThriftRequest:
				case ThriftResponse:
					return requireResponseType ? ThriftResponse : ThriftRequest;
				case ProtoBufRequest:
				case ProtoBufResponse:
					return requireResponseType ? ProtoBufResponse : ProtoBufRequest;
				case HTTPRequest:
				case HTTPResponse:
				default:
					return requireResponseType ? HTTPResponse : HTTPRequest;
			}
		}

		public static EventType getResponseType(EventType eventType) {
			return mapType(eventType, true);
		}

		public static EventType fromReplayType(ReplayTypeEnum replayType) {
			switch(replayType) {
				case THRIFT:
					return ThriftRequest;
				case GRPC:
					return ProtoBufRequest;
				default:
					return HTTPRequest;
			}
		}
	}

	public void parseAndSetKeyAndCollection( String collection,
		CompareTemplate template) {
		this.collection = collection;
		parseAndSetKey(template);
	}

	public void parseAndSetKey(CompareTemplate template) {
		parseAndSetKey(template,Optional.empty());
	}

	public void parseAndSetKey(CompareTemplate template, Optional<URLClassLoader> classLoader)  {
		List<String> keyVals = new ArrayList<>();
		payload.collectKeyVals(path -> template.getRule(path).getCompareType()
			== CompareTemplate.ComparisonType.Equal, keyVals);
		LOGGER.info("Generating event key from vals : ".concat(keyVals.toString()));
		payloadKey = Objects.hash(keyVals);
		// TODO revert it later
		/*if (!keyVals.isEmpty()) {
			payloadKey = Objects.hash(keyVals.get(0));
		}
		for (int i = 1 ; i < keyVals.size(); i++) {
			payloadKey ^= Objects.hash(keyVals.get(i));
		}*/
		// TODO deal with this later
		/*if (eventType == EventType.ThriftRequest) {
			this.traceId = ((ThriftDataObject) payload).traceId;
		}*/
		LOGGER.info("Event key generated : ".concat(String.valueOf(payloadKey)));
	}

	/**
	 * Create a new event with transformed payload. While transforming request events, need to send
	 * the comparator so that payloadKey can be calculated
	 *
	 * @param rhs
	 * @param operationList
	 * @param newCollection
	 * @param newReqId
	 * @return
	 * @throws InvalidEventException
	 */
	public Event applyTransform(Optional<Event> rhs, List<ReqRespUpdateOperation> operationList,
		String newCollection, String newReqId, Optional<Comparator> comparator)
		throws InvalidEventException {
		Optional<Payload> newPayload = rhs.map(rhsEvent ->
				payload.applyTransform(rhsEvent.payload, operationList));
		Event toReturn = new EventBuilder(customerId, app, service, instanceId, newCollection,
			new MDTraceInfo(this.traceId, this.spanId, this.parentSpanId),
			runType, Optional.of(timestamp), newReqId, apiPath, eventType)
			.setPayload(newPayload.orElse(payload))
			.createEvent();
		// set key for request events
		comparator.ifPresent(
			comparatorVal -> toReturn.parseAndSetKey(comparatorVal.getCompareTemplate()));
		return toReturn;
	}


	public static final List<EventType> REQUEST_EVENT_TYPES = Arrays.asList(
			EventType.HTTPRequest, EventType.JavaRequest,
			EventType.ThriftRequest, EventType.ProtoBufRequest);
	// currently JavaRequest stores the response as well
	// TODO: change JavaRequest to JavaResponse in list below once we separate the two
	public static final List<EventType> RESPONSE_EVENT_TYPES = Arrays.asList(
			EventType.HTTPResponse, EventType.JavaRequest,
			EventType.ThriftResponse, EventType.ProtoBufResponse);


	public final String customerId;
	public final String app;
	public final String service;
	public final String instanceId;
	private String collection;
	private String traceId;
	public final String spanId;
	public final String parentSpanId;
	private RunType runType;


	public void setCollection(String collection) {
		this.collection = collection;
	}

	public final Instant timestamp;
	public final String reqId; // for responses, this is the reqId of the corresponding request
	public final String apiPath; // apiPath for HTTP req, function signature for Java functions, etc
	public final EventType eventType;
	public final Payload payload;

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

	public RunType getRunType() {return this.runType;}

	public void setRunType(RunType runType) {this.runType = runType;}

	public static class EventBuilder {

		private static final Logger LOGGER = LoggerFactory.getLogger(EventBuilder.class);

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
		private Payload payload;
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
			this.parentSpanId = mdTraceInfo.getParentSpanId();
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
			this.parentSpanId = mdTraceInfo.getParentSpanId();
			this.runType = runType;
			this.apiPath = apiPath;
			this.eventType = eventType;
			this.timestamp = timestamp;
			this.reqId = reqId;
			this.collection = collection;
		}


		public EventBuilder setPayload(Payload payload) {
			this.payload = payload;
			return this;
		}

		public EventBuilder setPayloadKey(int payloadKey) {
			this.payloadKey = payloadKey;
			return this;
		}

		public Event createEvent() throws io.md.dao.Event.EventBuilder.InvalidEventException {
			Event event = new Event(customerId, app, service, instanceId, collection, traceId
				, spanId, parentSpanId, runType, timestamp.orElse(Instant.now()), reqId, apiPath,
				eventType , payload, payloadKey);
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
				LOGGER.error("Exception in creating an Event" , e);
			}
			return Optional.empty();
		}


		public static class InvalidEventException extends Exception {

		}
	}
}
