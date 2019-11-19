/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import static com.cube.dao.Event.RunType.Record;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.cube.agent.UtilException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.cube.core.CompareTemplate;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */

/**
 * Event represents a generic event that is recorded. It can be a request or response captured from various protocols
 * such as REST over HTTP, Java function calls, GRPC, Thrift etc.
 * The common metadata for all such events in captured in the event fields. The payload represents the actual data
 * that can be encoded in different formats such as Json, ProtoBuf, Thrift etc depending on the event type
 */
public class Event {

    private static final Logger LOGGER = LogManager.getLogger(Event.class);


    private Event(String customerId, String app, String service, String instanceId, String collection, String traceId,
                 RunType runType, Instant timestamp, String reqId, String apiPath, EventType eventType, byte[] rawPayloadBinary,
                 String rawPayloadString, DataObj payload, int payloadKey) {
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
        this.runType = Record;
        this.timestamp = null;
        this.reqId = null;
        this.apiPath = null;
        this.eventType = null;
        this.rawPayloadBinary = null;
        this.rawPayloadString = null;
        this.payload = null;
        this.payloadKey = 0;

    }

    public static List<EventType> getRequestEventTypes() {
        return requestEventTypes;
    }

    public String getCollection() {
        return collection;
    }

    public boolean validate() {

        if ((customerId == null) || (app == null) || (service == null) || (instanceId == null) || (collection == null)
            || (traceId == null) || (runType == null) ||
            (timestamp == null) || (reqId == null) || (apiPath == null) || (eventType == null)
            || ((rawPayloadBinary == null) == (rawPayloadString == null || rawPayloadString.trim().isEmpty()))) {
            return false;
        }
        return true;
    }

    public void parseAndSetKeyAndCollection(Config config, String collection,
                                                        CompareTemplate template) {
        this.collection = collection;
        parseAndSetKey(config, template);
    }

    public void parseAndSetKey(Config config, CompareTemplate template) {

        parsePayLoad(config);
        List<String> keyVals = new ArrayList<>();
        payload.collectKeyVals(path -> template.getRule(path).getCompareType() == CompareTemplate.ComparisonType.Equal, keyVals);
        LOGGER.info(new ObjectMessage(Map.of("message", "Generating event key from vals", "vals", keyVals.toString())));
        payloadKey = Objects.hash(keyVals);
        LOGGER.info(new ObjectMessage(Map.of("message", "Event key generated", "key", payloadKey)));
    }

    public DataObj parsePayLoad(Config config) {
        // parse if not already parsed
        if (payload == null) {
            payload = DataObjFactory.build(eventType, rawPayloadBinary, rawPayloadString, config);
        }

        return payload;
    }

    @JsonIgnore
    public boolean isRequestType() {
        return requestEventTypes.contains(eventType);
    }

    public String getReqId() {
        return reqId;
    }

    public String getPayloadAsString(Config config) {
        parsePayLoad(config);
        return payload.toString();
    }

    public DataObj getPayload(Config config) {
        parsePayLoad(config);
        return payload;
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
    }

    public static List<EventType> requestEventTypes = List.of(EventType.HTTPRequest, EventType.JavaRequest,
        EventType.ThriftRequest, EventType.ProtoBufRequest);

    public final String customerId;
    public final String app;
    public final String service;
    public final String instanceId;
    private String collection;
    public final String traceId;
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
    public final byte[] rawPayloadBinary;
    public final String rawPayloadString;

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

    public static class EventBuilder {

        private static final Logger LOGGER = LogManager.getLogger(Event.class);

        private final String customerId;
        private final String app;
        private final String service;
        private final String instanceId;
        private final String collection;
        private final String traceId;
        private final Event.RunType runType;
        private final Instant timestamp;
        private final String reqId;
        private final String apiPath;
        private final Event.EventType eventType;
        private byte[] rawPayloadBinary;
        private String rawPayloadString;
        private DataObj payload;
        private int payloadKey = 0;

        public EventBuilder(String customerId, String app, String service, String instanceId, String collection, String traceId,
                            Event.RunType runType, Instant timestamp, String reqId, String apiPath, Event.EventType eventType) {
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

        public Event createEvent() throws com.cube.dao.Event.EventBuilder.InvalidEventException {
            Event event = new Event(customerId, app, service, instanceId, collection, traceId, runType, timestamp, reqId, apiPath,
                eventType,
                rawPayloadBinary, rawPayloadString, payload, payloadKey);
            if (event.validate()) {
                return event;
            } else {
                throw new com.cube.dao.Event.EventBuilder.InvalidEventException();
            }
        }

        public Optional<Event> createEventOpt() {
            try {
                return Optional.of(createEvent());
            } catch (com.cube.dao.Event.EventBuilder.InvalidEventException e) {
                LOGGER.error("Exception in creating an Event", e.getMessage(),
                    UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
            }
            return Optional.empty();
        }


        public static class InvalidEventException extends Exception {

        }
    }

}
