/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-23
 */

/**
 * EventQuery is POJO to query generic events with matching criteria
 */
@JsonDeserialize(builder = EventQuery.Builder.class)
public class EventQuery {

    private static final Logger LOGGER = LogManager.getLogger(EventQuery.class);

    private final String customerId;
    private final String app;
    private final EventType eventType;

    private final Optional<String> service;
    private final Optional<String> instanceId;
    private final Optional<String> collection;

    private final Optional<String> traceId;
    private final Optional<RRBase.RR> rrType;
    private final Optional<String> spanId;
    private final Optional<String> parentSpanId;
    private final Optional<Instant> timestamp;

    private final Optional<List<String>> reqIds;
    private final Optional<List<String>> paths;
    private final Optional<Integer> payloadKey;
    private final Optional<Integer> offset;
    private final Optional<Integer> limit;
    private final Optional<Boolean> sortOrderAsc;

    public static class Builder {
        private final String customerId;
        private final String app;
        private final EventType eventType;

        private String service = null;
        private String instanceId = null;
        private String collection = null;
        private String traceId = null;
        private RRBase.RR rrType = null;
        private String spanId = null;
        private String parentSpanId = null;
        private Instant timestamp = null;
        private List<String> reqIds = Collections.emptyList();
        private List<String> paths = Collections.emptyList();
        private Integer payloadKey = null;
        private Integer offset = null;
        private Integer limit = null;
        private Boolean sortOrderAsc = null;

        @JsonCreator
        public Builder(@JsonProperty("customerId") String customerId,
                       @JsonProperty("app") String app,
                       @JsonProperty("eventType") EventType eventType) {
            this.customerId = customerId;
            this.app = app;
            this.eventType = eventType;
        }

        public Builder withService(String val) {
            service = val;
            return this;
        }

        public Builder withInstanceId(String val) {
            instanceId = val;
            return this;
        }

        public Builder withCollection(String val) {
            collection = val;
            return this;
        }

        public Builder withTraceId(String val) {
            traceId = val;
            return this;
        }

        public Builder withRRType(RRBase.RR val) {
            rrType = val;
            return this;
        }

        public Builder withSpanId(String val) {
            spanId = val;
            return this;
        }

        public Builder withParentSpanId(String val) {
            parentSpanId = val;
            return this;
        }

        public Builder withTimestamp(Instant val) {
            timestamp = val;
            return this;
        }

        public Builder withReqIds(List<String> val) {
            reqIds = val;
            return this;
        }

        public Builder withPaths(List<String> val) {
            paths = val;
            return this;
        }

        public Builder withPayloadKey(int val) {
            payloadKey = val;
            return this;
        }

        public Builder withOffset(int val) {
            offset = val;
            return this;
        }

        public Builder withLimit(int val) {
            limit = val;
            return this;
        }

        public Builder withSortOrderAsc(boolean val) {
            sortOrderAsc = val;
            return this;
        }

        public EventQuery build() {
            return new EventQuery(this);
        }

    }

    private EventQuery(Builder builder) {
        customerId = builder.customerId;
        app = builder.app;
        eventType = builder.eventType;
        service = Optional.ofNullable(builder.service);
        instanceId = Optional.ofNullable(builder.instanceId);
        collection = Optional.ofNullable(builder.collection);
        traceId = Optional.ofNullable(builder.traceId);
        rrType = Optional.ofNullable(builder.rrType);
        spanId = Optional.ofNullable(builder.spanId);
        parentSpanId = Optional.ofNullable(builder.parentSpanId);
        timestamp = Optional.ofNullable(builder.timestamp);
        reqIds = Optional.ofNullable(builder.reqIds);
        paths = Optional.ofNullable(builder.paths);
        payloadKey = Optional.ofNullable(builder.payloadKey);
        offset = Optional.ofNullable(builder.offset);
        limit = Optional.ofNullable(builder.limit);
        sortOrderAsc = Optional.ofNullable(builder.sortOrderAsc);
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getApp() {
        return app;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Optional<String> getCollection() {
        return collection;
    }

    public Optional<String> getService() {
        return service;
    }

    public Optional<String> getInstanceId() {
        return instanceId;
    }

    public Optional<String> getTraceId() {
        return traceId;
    }

    public Optional<RRBase.RR> getRRType() { return rrType; }

    public Optional<List<String>> getReqids() {
        return reqIds;
    }

    public Optional<List<String>> getPaths() {
        return paths;
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

}
