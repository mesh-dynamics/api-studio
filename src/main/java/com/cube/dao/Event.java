/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.cube.core.CompareTemplate;
import com.cube.core.Utils;
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
                  Instant timestamp, String reqId, String apiPath, EventType eventType, byte[] rawPayloadBinary,
                  String rawPayloadString, DataObj payload, int payloadKey) {
        this.customerId = customerId;
        this.app = app;
        this.service = service;
        this.instanceId = instanceId;
        this.collection = collection;
        this.traceId = traceId;
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
        this.timestamp = null;
        this.reqId = null;
        this.apiPath = null;
        this.eventType = null;
        this.rawPayloadBinary = null;
        this.rawPayloadString = null;
        this.payload = null;
        this.payloadKey = 0;

    }


    public static Optional<Event> createEvent(String docId, Optional<String> customerId, Optional<String> app,
                                              Optional<String> service,
                                              Optional<String> instanceId, Optional<String> collection, Optional<String> traceId,
                                              Optional<Instant> timestamp, Optional<String> reqId,
                                              Optional<String> apiPath, Optional<String> eventTypeOpt,
                                              Optional<byte[]> rawPayloadBin, Optional<String> rawPayloadStr,
                                              Optional<Integer> payloadKey, Config config) {

        if (customerId.isPresent() && app.isPresent() && service.isPresent() && instanceId.isPresent() && collection.isPresent() &&
        traceId.isPresent() && timestamp.isPresent() && reqId.isPresent() && apiPath.isPresent() && eventTypeOpt.isPresent()  &&
        payloadKey.isPresent() && (rawPayloadBin.isPresent() ^ rawPayloadStr.isPresent())) {
            return Utils.valueOf(EventType.class, eventTypeOpt.get()).map(eventType -> {
                byte [] payloadBin = rawPayloadBin.orElse(null);
                String payloadStr = rawPayloadStr.orElse(null);
                DataObj payload = DataObjFactory.build(eventType, payloadBin, payloadStr, config);

                return new Event(customerId.get(), app.get(), service.get(), instanceId.get(),
                    collection.get(), traceId.get(), timestamp.get(), reqId.get(), apiPath.get(), eventType,
                    payloadBin, payloadStr, payload, payloadKey.get());
            }).or(() -> {
                LOGGER.error("Type field has invalid value in Event object with doc id: " + docId);
                return Optional.empty();
            });
        }
        LOGGER.error(String.format("Required field is missing in Event constructor. Cannot create event"));
        return Optional.empty();
    }

    public String getCollection() {
        return collection;
    }

    public boolean validate() {

        if ((customerId == null) || (app == null) || (service == null) || (instanceId == null) || (collection == null)
            || (traceId == null) || (timestamp == null) || (reqId == null) || (apiPath == null) || (eventType == null)
            || ((rawPayloadBinary == null) == (rawPayloadString == null))) {
            return false;
        }
        return true;
    }

    public void parseAndSetKeyAndCollection(Config config, String collection,
                                                        Optional<CompareTemplate> templateOpt) {
        this.collection = collection;

        payload = DataObjFactory.build(eventType, rawPayloadBinary, rawPayloadString, config);
        payloadKey = templateOpt.map(template -> {
            List<String> keyVals = new ArrayList<>();
            payload.collectKeyVals(path -> template.getRule(path).getCompareType() == CompareTemplate.ComparisonType.Equal, keyVals);
            return Objects.hash(keyVals);
        }).orElse(0);
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
    int payloadKey;

}
