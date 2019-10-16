/*
 *
 *    Copyright Cube I O
 *
 */
package io.cube.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;

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


    public Event(String customerId, String app, String service, String instanceId, String collection, String traceId,
                 RecordReplayType rrType, Instant timestamp, String reqId, String apiPath, EventType eventType, byte[] rawPayloadBinary,
                 String rawPayloadString, DataObj payload, int payloadKey) {
        this.customerId = customerId;
        this.app = app;
        this.service = service;
        this.instanceId = instanceId;
        this.collection = collection;
        this.traceId = traceId;
        this.rrType = rrType;
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
        this.rrType = RecordReplayType.Record;
        this.timestamp = null;
        this.reqId = null;
        this.apiPath = null;
        this.eventType = null;
        this.rawPayloadBinary = null;
        this.rawPayloadString = null;
        this.payload = null;
        this.payloadKey = 0;

    }

    public boolean validate() {

        //Timestamp can be null for a function during mocking.
        if ((customerId == null) || (app == null) || (service == null) || (instanceId == null) || (collection == null)
                || (traceId == null) || (rrType == null) || (reqId == null) || (apiPath == null) || (eventType == null)
                || ((rawPayloadBinary == null) == (rawPayloadString == null))) {
            return false;
        }
        return true;
    }

    @JsonIgnore
    public boolean isRequestType() {
        return eventType == EventType.HTTPRequest || eventType == EventType.JavaRequest
                || eventType == EventType.ThriftRequest || eventType == EventType.ProtoBufRequest;
    }

    public enum EventType {
        HTTPRequest,
        HTTPResponse,
        JavaRequest,
        JavaResponse,
        ThriftRequest,
        ThriftResponse,
        ProtoBufRequest,
        ProtoBufResponse
    }

    public final String customerId;
    public final String app;
    public final String service;
    public final String instanceId;
    private String collection;
    public final String traceId;
    public final RecordReplayType rrType;
    public final Instant timestamp;

    public final String reqId; // for responses, this is the reqid of the corresponding request
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
    public enum RecordReplayType {
        Record,
        Replay,
        Manual  // manually created e.g. default requests and responses
    }
}
