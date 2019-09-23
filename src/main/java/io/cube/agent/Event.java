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
import java.util.Optional;

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


    private Event(String customerid, String app, String service, String instanceid, String collection, String traceid,
                  Instant timestamp, String reqid, String apiPath, EventType type, byte[] rawPayloadBinary,
                  String rawPayloadString, DataObj payload, int payloadKey) {
        this.customerid = customerid;
        this.app = app;
        this.service = service;
        this.instanceid = instanceid;
        this.collection = collection;
        this.traceid = traceid;
        this.timestamp = timestamp;
        this.reqid = reqid;
        this.apiPath = apiPath;
        this.type = type;
        this.rawPayloadBinary = rawPayloadBinary;
        this.rawPayloadString = rawPayloadString;
        this.payload = payload;
        this.payloadKey = payloadKey;
    }

    /**
     * For jackson
     */
    private Event() {
        this.customerid = null;
        this.app = null;
        this.service = null;
        this.instanceid = null;
        this.collection = null;
        this.traceid = null;
        this.timestamp = null;
        this.reqid = null;
        this.apiPath = null;
        this.type = null;
        this.rawPayloadBinary = null;
        this.rawPayloadString = null;
        this.payload = null;
        this.payloadKey = 0;

    }


    public static Optional<Event> createEvent(String docid, Optional<String> customerid, Optional<String> app,
                                              Optional<String> service,
                                              Optional<String> instanceid, Optional<String> collection, Optional<String> traceid,
                                              Optional<Instant> timestamp, Optional<String> reqid,
                                              Optional<String> apiPath, Optional<String> type,
                                              Optional<byte[]> rawPayloadBin, Optional<String> rawPayloadStr) {

        if (customerid.isPresent() && app.isPresent() && service.isPresent() && instanceid.isPresent() && collection.isPresent() &&
        traceid.isPresent() && timestamp.isPresent() && reqid.isPresent() && apiPath.isPresent() && type.isPresent()  &&
                (rawPayloadBin.isPresent() ^ rawPayloadStr.isPresent())) {
            return CommonUtils.valueOf(EventType.class, type.get()).map(eventType -> {
                byte [] payloadBin = rawPayloadBin.orElse(null);
                String payloadStr = rawPayloadStr.orElse(null);

                return new Event(customerid.get(), app.get(), service.get(), instanceid.get(),
                    collection.get(), traceid.get(), timestamp.get(), reqid.get(), apiPath.get(), eventType,
                    payloadBin, payloadStr, null, 0);
            }).or(() -> {
                LOGGER.error("Type field has invalid value in Event object with doc id: " + docid);
                return Optional.empty();
            });
        }
        LOGGER.error(String.format("Required field is missing in Event constructor. Cannot create event"));
        return Optional.empty();
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

    public final String customerid;
    public final String app;
    public final String service;
    public final String instanceid;
    private String collection;
    public final String traceid;

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public final Instant timestamp;
    public final String reqid; // for responses, this is the reqid of the corresponding request
    public final String apiPath; // apiPath for HTTP req, function signature for Java functions, etc
    public final EventType type;

    // Payload can be binary or string. Keeping both types, since otherwise we will have to encode string also
    // as base64. For debugging its easier if the string is readable.
    public final byte[] rawPayloadBinary;
    public final String rawPayloadString;

    @JsonIgnore
    DataObj payload;

    @JsonIgnore
    int payloadKey;

}
