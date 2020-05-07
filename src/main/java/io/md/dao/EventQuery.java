package io.md.dao;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;

/**
 * EventQuery is POJO to query generic events with matching criteria
 */
public class EventQuery {

    public  String customerId;
    public  String app;
    public  List<EventType> eventTypes;

    public  List<String> services;
    public  Optional<String> instanceId;
    public  Optional<String> collection;

    public  List<String> traceIds;
    public  Optional<RunType> runType;
    public  Optional<String> spanId;
    public  Optional<String> parentSpanId;
    public  Optional<Instant> timestamp;

    public  List<String> reqIds;
    public  List<String> paths;
    private boolean excludePaths;
    public  Optional<Integer> payloadKey;
    public  Optional<Integer> offset;
    public  Optional<Integer> limit;
    public  Optional<Boolean> sortOrderAsc;

    public EventQuery(String customerId, String app, EventType eventType) {
        this.customerId = customerId;
        this.app = app;
        this.eventTypes = List.of(eventType);
        this.services = Collections.emptyList();
        this.instanceId = Optional.empty();
        this.collection = Optional.empty();
        this.traceIds = Collections.emptyList();
        this.runType = Optional.empty();
        this.spanId = Optional.empty();
        this.parentSpanId = Optional.empty();
        this.timestamp = Optional.empty();
        this.reqIds = Collections.emptyList();
        this.paths = Collections.emptyList();
        this.excludePaths = false;
        this.payloadKey = Optional.empty();
        this.offset = Optional.empty();
        this.limit = Optional.empty();
        this.sortOrderAsc = Optional.empty();
    }

    public EventQuery(String customerId, String app, List<EventType> eventType) {
        this.customerId = customerId;
        this.app = app;
        this.eventTypes = eventType;
        this.services = Collections.emptyList();
        this.instanceId = Optional.empty();
        this.collection = Optional.empty();
        this.traceIds = Collections.emptyList();
        this.runType = Optional.empty();
        this.spanId = Optional.empty();
        this.parentSpanId = Optional.empty();
        this.timestamp = Optional.empty();
        this.reqIds = Collections.emptyList();
        this.paths = Collections.emptyList();
        this.excludePaths = false;
        this.payloadKey = Optional.empty();
        this.offset = Optional.empty();
        this.limit = Optional.empty();
        this.sortOrderAsc = Optional.empty();
    }
    public EventQuery() {
        this.customerId = "";
        this.app = "";
        this.eventTypes = Collections.emptyList();
        this.services = Collections.emptyList();
        this.instanceId = Optional.empty();
        this.collection = Optional.empty();
        this.traceIds = Collections.emptyList();
        this.runType = Optional.empty();
        this.spanId = Optional.empty();
        this.parentSpanId = Optional.empty();
        this.timestamp = Optional.empty();
        this.reqIds = Collections.emptyList();
        this.paths = Collections.emptyList();
        this.excludePaths = false;
        this.payloadKey = Optional.empty();
        this.offset = Optional.empty();
        this.limit = Optional.empty();
    }
    public EventQuery withService(String val) {
        services = List.of(val);
        return this;
    }


    public EventQuery withServices(List<String> vals) {
        services = vals;
        return this;
    }

    public EventQuery withInstanceId(String val) {
        instanceId = Optional.of(val);
        return this;
    }

    public EventQuery withCollection(String val) {
        collection = Optional.of(val);
        return this;
    }

    public EventQuery withTraceId(String val) {
        traceIds = List.of(val);
        return this;
    }

    public EventQuery withTraceIds(List<String> vals) {
        traceIds = vals;
        return this;
    }


    public EventQuery withRunType(RunType val) {
        runType = Optional.of(val);
        return this;
    }

    public EventQuery withSpanId(String val) {
        spanId = Optional.of(val);
        return this;
    }

    public EventQuery withParentSpanId(String val) {
        parentSpanId = Optional.of(val);
        return this;
    }

    public EventQuery withTimestamp(Instant val) {
        timestamp = Optional.of(val);
        return this;
    }

    public EventQuery withReqId(String val) {
        reqIds = List.of(val);
        return this;
    }

    public EventQuery withReqIds(List<String> val) {
        reqIds = val;
        return this;
    }

    public EventQuery withPath(String val) {
        paths = List.of(val);
        return this;
    }

    public EventQuery withPaths(List<String> val) {
        paths = val;
        return this;
    }

    public EventQuery withExcludePaths(boolean val) {
        excludePaths = val;
        return this;
    }

    public EventQuery withPayloadKey(int val) {
        payloadKey = Optional.of(val);
        return this;
    }

    public EventQuery withOffset(int val) {
        offset = Optional.of(val);
        return this;
    }

    public EventQuery withLimit(int val) {
        limit = Optional.of(val);
        return this;
    }

    public EventQuery withSortOrderAsc(boolean val) {
        sortOrderAsc = Optional.of(val);
        return this;
    }

    public EventQuery build() {
        return this;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getApp() {
        return app;
    }

    public List<EventType> getEventTypes() {
        return eventTypes;
    }

    public Optional<String> getCollection() {
        return collection;
    }

    public List<String> getServices() {
        return services;
    }

    public Optional<String> getInstanceId() {
        return instanceId;
    }

    public List<String> getTraceIds() {
        return traceIds;
    }

    public Optional<RunType> getRunType() { return runType; }

    public List<String> getReqIds() {
        return reqIds;
    }

    public List<String> getPaths() {
        return paths;
    }

    public boolean excludePaths() {
        return excludePaths;
    }

    public Optional<Integer> getPayloadKey() {
        return payloadKey;
    }

    public Optional<Integer> getOffset() {
        return offset;
    }

    public Optional<Integer> getLimit() {
        return limit;
    }

    public Optional<Boolean> isSortOrderAsc() {
        return sortOrderAsc;
    }
}
