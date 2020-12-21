package io.md.dao;

/*
 *
 *    Copyright Cube I O
 *
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.*;

import io.md.dao.Recording.RecordingType;

import io.md.logger.LogMgr;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

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
public class Event implements MDStorable {

	private static final Logger LOGGER = LogMgr.getLogger(Event.class);

	@JsonCreator
	private Event(@JsonProperty("customerId") String customerId, @JsonProperty("app") String app,
			@JsonProperty("service") String service, @JsonProperty("instanceId") String instanceId,
			@JsonProperty("collection") String collection, @JsonProperty("traceId") String traceId,
			@JsonProperty("spanId") String spanId, @JsonProperty("parentSpanId") String parentSpanId,
			@JsonProperty("runType") RunType runType, @JsonProperty("timestamp") Instant timestamp,
			@JsonProperty("reqId") String reqId, @JsonProperty("apiPath") String apiPath,
			@JsonProperty("eventType") EventType eventType, @JsonProperty("payload") Payload payload,
			@JsonProperty("payloadKey") int payloadKey, @JsonProperty("recordingType") RecordingType recordingType,
			@JsonProperty("metaData") Map<String, String> metaData, @JsonProperty("runId") String runId,
			@JsonProperty("payloadFields") List<String> payloadFields, @JsonProperty("seqId") String seqId) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instanceId;
		this.collection = collection != null ? collection : "NA";
		this.traceId = traceId != null ?  traceId : "NA";
		this.spanId = spanId;
		this.parentSpanId = parentSpanId != null ? parentSpanId : "NA";
		this.runType = runType != null ? runType : RunType.Record;
		this.timestamp = timestamp;
		this.reqId = reqId;
		this.apiPath = apiPath != null ? CompareTemplate.normaliseAPIPath(apiPath) : apiPath;
		this.eventType = eventType;
		this.payload = payload;
		this.payloadKey = payloadKey;
		this.recordingType = recordingType != null ? recordingType : RecordingType.Golden;
		this.metaData = metaData;
		this.runId = runId != null ? runId : this.traceId;
		this.payloadFields = payloadFields!=null && !payloadFields.isEmpty() ? payloadFields : payload != null ? payload.getPayloadFields() : Collections.EMPTY_LIST;
		if(seqId!=null) this.seqId = seqId;
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
		try {
			validateEvent();
			return true;
		} catch (Exception exception) {
			return false;
		}
	}

	public void validateEvent() throws InvalidEventException{
		try {
			Validate.notNull(customerId);
			Validate.notNull(app);
			Validate.notNull(service);
			Validate.notNull(instanceId);
			Validate.notNull(collection);
			Validate.notNull(runType);
			Validate.notNull(timestamp);
			Validate.notNull(reqId);
			Validate.notNull(apiPath);
			Validate.notNull(eventType);
			if (eventType != EventType.ThriftResponse && eventType != EventType.ThriftRequest) {
				Validate.notNull(traceId);
			}
			Validate.notNull(payload);
			Validate.isTrue(!payload.isRawPayloadEmpty());
			Validate.notNull(payloadFields);
			Validate.notNull(seqId);
		} catch (Exception ex) {
			throw new InvalidEventException("Invalid Event Object " + ex.getMessage(), ex);
		}

	}

	@JsonIgnore
	public boolean isRequestType() {
		return REQUEST_EVENT_TYPES.contains(eventType);
	}

	public String getReqId() {
		return reqId;
	}

	public String getSpanId() {
		return spanId;
	}

	public String getParentSpanId() {
		return parentSpanId;
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
					// For java, the response is stored in the request event itself
					return requireResponseType ? JavaRequest : JavaRequest;
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
					// For now treat Grpc as http
					return HTTPRequest;
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
		Map<String, String> keyValMap = new HashMap<>();
		List<String> keyVals = new ArrayList<>();
		payload.collectKeyVals(path -> template.getRule(path).getCompareType()
			== CompareTemplate.ComparisonType.Equal, keyValMap);

		for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
			keyVals.add((entry.getKey() + "=" + entry.getValue()).toLowerCase());
		}

		LOGGER.info("Generating event key from vals : ".concat(keyVals.toString()));
		//Making parameter matching for mock, Case Insensitive

		if (!keyVals.isEmpty()) {
			payloadKey = Objects.hash(keyVals.get(0));
		}
		for (int i = 1 ; i < keyVals.size(); i++) {
			payloadKey ^= Objects.hash(keyVals.get(i));
		}
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
			runType, Optional.of(timestamp), newReqId, apiPath, eventType, recordingType).withRunId(runId)
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
	public String runId;


	public void setCollection(String collection) {
		this.collection = collection;
	}

	public final Instant timestamp;
	public final String reqId; // for responses, this is the reqId of the corresponding request
	public final String apiPath; // apiPath for HTTP req, function signature for Java functions, etc
	public final EventType eventType;
	public final Payload payload;
	public RecordingType recordingType;
	public final Map<String, String> metaData;
	public final List<String> payloadFields;
	private String seqId = "";

	@JsonIgnore
	public int payloadKey;

	/* when did the event get created - record, replay or manually added */
	public enum RunType {
		Record,
		Replay,
		Manual, // manually created e.g. default requests and responses
		DevTool,
		DevToolProxy,
		Mock
	}

	public String getTraceId() {
		return this.traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public void setRecordingType(RecordingType recordingType) {
		this.recordingType = recordingType;
	}

	public RunType getRunType() {return this.runType;}

	public void setRunType(RunType runType) {this.runType = runType;}
	public void setRunId(String runId) {this.runId = runId;}

	public Optional<String> getMetaFieldValue(String field){
		return Optional.ofNullable(metaData.get(field)) ;
	}

	public void setMetaFieldValue(String field , String val){
		metaData.put(field , val);
	}

	public String getSeqId() {
		return seqId;
	}

	public void setSeqId(String seqId) {
		this.seqId = seqId;
	}


	public static class EventBuilder {

		private static final Logger LOGGER = LogMgr.getLogger(EventBuilder.class);

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
		private final RecordingType recordingType;
		private Map<String, String> metaData = new HashMap<>(0);
		private String runId;
		private List<String> payloadFields = Collections.EMPTY_LIST;
		private String seqId = "";

		public EventBuilder(String customerId, String app, String service, String instanceId,
			String collection, MDTraceInfo mdTraceInfo,
			Event.RunType runType, Optional<Instant> timestamp, String reqId, String apiPath,
			Event.EventType eventType, RecordingType recordingType) {
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
			this.recordingType = recordingType;
		}

		public EventBuilder(CubeMetaInfo cubeMetaInfo, MDTraceInfo mdTraceInfo,
			Event.RunType runType, String apiPath, EventType eventType, Optional<Instant> timestamp,
			String reqId, String collection, RecordingType recordingType) {
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
			this.recordingType = recordingType;
		}


		public EventBuilder setPayload(Payload payload) {
			this.payload = payload;
			return this;
		}

		public EventBuilder setPayloadKey(int payloadKey) {
			this.payloadKey = payloadKey;
			return this;
		}

		public EventBuilder withMetaData(Map<String, String> metaData) {
			this.metaData = metaData;
			return this;
		}

		public EventBuilder withRunId(String runId) {
			this.runId = runId;
			return this;
		}
		public EventBuilder withPayloadFields(List<String> payloadFields) {
			this.payloadFields = payloadFields;
			return this;
		}

		public EventBuilder withSeqId(String nSeqId){
			this.seqId = nSeqId;
			return this;
		}


		public Event createEvent() throws io.md.dao.Event.EventBuilder.InvalidEventException {
			Event event = new Event(customerId, app, service, instanceId, collection, traceId
				, spanId, parentSpanId, runType, timestamp.orElse(Instant.now()), reqId, apiPath,
				eventType , payload, payloadKey, recordingType, metaData, runId , payloadFields , seqId);
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

			public InvalidEventException() {}
			public InvalidEventException(String message) {
				super(message);
			}
			public InvalidEventException(String message, Exception ex) {
				super(message, ex);
			}

		}
	}
}
