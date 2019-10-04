/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import static com.cube.dao.RRBase.RR.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.dao.RRBase.RR;
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


    public Event(String customerId, String app, String service, String instanceId, String collection, String traceId,
                 RR rrType, Instant timestamp, String reqId, String apiPath, EventType eventType, byte[] rawPayloadBinary,
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
        this.rrType = Record;
        this.timestamp = null;
        this.reqId = null;
        this.apiPath = null;
        this.eventType = null;
        this.rawPayloadBinary = null;
        this.rawPayloadString = null;
        this.payload = null;
        this.payloadKey = 0;

    }


    public static Event fromRequest(Request request, RequestComparator comparator, Config config)
        throws JsonProcessingException, EventBuilder.InvalidEventException {

        HTTPRequestPayload payload = new HTTPRequestPayload(request.hdrs, request.qparams, request.fparams,
            request.method, request.body);
        String payloadStr;
        payloadStr = config.jsonmapper.writeValueAsString(payload);

        EventBuilder eventBuilder = new EventBuilder(request.customerid.orElse("NA"), request.app.orElse("NA"),
            request.getService().orElse("NA"), request.getInstance().orElse("NA"), request.collection.orElse("NA"),
            request.getTraceId().orElse("NA"), request.rrtype.orElse(Record), request.timestamp.orElse(Instant.now()),
            request.reqid.orElse(
                "NA"),
            request.path, EventType.HTTPRequest);
        eventBuilder.setRawPayloadString(payloadStr);
        Event event = eventBuilder.createEvent();
        event.parseAndSetKey(config, comparator.getCompareTemplate());

        return event;
    }

    public static Event fromResponse(Response response, Config config)
        throws JsonProcessingException, EventBuilder.InvalidEventException {

        HTTPResponsePayload payload = new HTTPResponsePayload(response.hdrs, response.status, response.body);
        String payloadStr;
        payloadStr = config.jsonmapper.writeValueAsString(payload);

        EventBuilder eventBuilder = new EventBuilder(response.customerid.orElse("NA"), response.app.orElse("NA"),
            response.getService().orElse("NA"), response.getInstance().orElse("NA"), response.collection.orElse("NA"),
            response.getTraceId().orElse("NA"), response.rrtype.orElse(Record), response.timestamp.orElse(Instant.now()),
            response.reqid.orElse(
                "NA"),
            "NA", EventType.HTTPResponse);
        eventBuilder.setRawPayloadString(payloadStr);
        Event event = eventBuilder.createEvent();
        event.parsePayLoad(config);

        return event;
    }


    public String getCollection() {
        return collection;
    }

    public boolean validate() {

        if ((customerId == null) || (app == null) || (service == null) || (instanceId == null) || (collection == null)
            || (traceId == null) || (rrType == null) ||
            (timestamp == null) || (reqId == null) || (apiPath == null) || (eventType == null)
            || ((rawPayloadBinary == null) == (rawPayloadString == null))) {
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
        LOGGER.info("Generating event key from vals: " + keyVals.toString());
        payloadKey = Objects.hash(keyVals);
    }

    public void parsePayLoad(Config config) {
        payload = DataObjFactory.build(eventType, rawPayloadBinary, rawPayloadString, config);
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
    public final RR rrType;


    public void setCollection(String collection) {
        this.collection = collection;
    }

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

}
