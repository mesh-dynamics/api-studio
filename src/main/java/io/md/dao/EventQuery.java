package io.md.dao;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.md.constants.EventType;
import io.md.constants.EventQueryRunType;

public class EventQuery {

    public  String customerId;
    public  String app;
    public  List<EventType> eventTypes;

    public  List<String> services;
    public  Optional<String> instanceId;
    public  Optional<String> collection;

    public  List<String> traceIds;
    public  Optional<EventQueryRunType> runType;
    public  Optional<String> spanId;
    public  Optional<String> parentSpanId;
    public  Optional<Instant> timestamp;

    public  List<String> reqIds;
    public  List<String> paths;
    public  Optional<Integer> payloadKey;
    public  Optional<Integer> offset;
    public  Optional<Integer> limit;
    public  Optional<Boolean> sortOrderAsc;

    public EventQuery() {
        this.customerId = "";
        this.app = "";
        this.eventTypes = Collections.emptyList();
        this.services = Collections.emptyList();
        this.instanceId = null;
        this.collection = null;
        this.traceIds = Collections.emptyList();
        this.runType = null;
        this.spanId = null;
        this.parentSpanId = null;
        this.timestamp = null;
        this.reqIds = Collections.emptyList();
        this.paths = Collections.emptyList();
        this.payloadKey = null;
        this.offset = null;
        this.limit = null;
        this.sortOrderAsc = null;

    }

    public EventQuery(String customerId, String app, List<EventType> eventTypes, List<String> services, Optional<String> instanceId,
                      Optional<String> collection, List<String> traceIds, Optional<EventQueryRunType> runType, Optional<String> spanId,
                      Optional<String> parentSpanId, Optional<Instant> timestamp, List<String> reqIds, List<String> paths,
                      Optional<Integer> payloadKey, Optional<Integer> offset, Optional<Integer> limit, Optional<Boolean> sortOrderAsc) {
        this.customerId = customerId;
        this.app = app;
        this.eventTypes = eventTypes;
        this.services = services;
        this.instanceId = instanceId;
        this.collection = collection;
        this.traceIds = traceIds;
        this.runType = runType;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.timestamp = timestamp;
        this.reqIds = reqIds;
        this.paths = paths;
        this.payloadKey = payloadKey;
        this.offset = offset;
        this.limit = limit;
        this.sortOrderAsc = sortOrderAsc;
    }
}
