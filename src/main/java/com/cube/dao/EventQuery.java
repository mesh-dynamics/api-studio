/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

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
@JsonDeserialize(builder = EventQuery.Builder.class)
public class EventQuery {

    private static final Logger LOGGER = LogManager.getLogger(EventQuery.class);

    private final String customerId;
    private final String app;
    private final String service;
    private final String instanceId;
    private final String collection;

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;

    private final Instant timestamp;
    private final EventType eventType;
    private final List<String> reqids;
    private final List<String> paths;
    @JsonIgnore
    private final int payloadKey;
    private final int offset;
    private final int limit;
    private final boolean sortOrderAsc;

    public static class Builder {
        private final EventType eventType;

        private String customerId = null;
        private String app = null;
        private String service = null;
        private String instanceId = null;
        private String collection = null;
        private String traceId = null;
        private String spanId = null;
        private String parentSpanId = null;
        private Instant timestamp = null;
        private List<String> reqIds = Collections.emptyList();
        private List<String> paths = Collections.emptyList();
        private int payloadKey = 0;
        private int offset = 0;
        private int limit = 0;
        private boolean sortOrderAsc = false;

        @JsonCreator
        public Builder(@JsonProperty("eventType") EventType eventType) {
            this.eventType = eventType;
        }

        public Builder withCustomerId(String val) {
            customerId = val;
            return this;
        }

        public Builder withApp(String val) {
            app = val;
            return this;
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
        eventType = builder.eventType;
        customerId = builder.customerId;
        app = builder.app;
        service = builder.service;
        instanceId = builder.instanceId;
        collection = builder.collection;
        traceId = builder.traceId;
        spanId = builder.spanId;
        parentSpanId = builder.parentSpanId;
        timestamp = builder.timestamp;
        reqids = builder.reqIds;
        paths = builder.paths;
        payloadKey = builder.payloadKey;
        offset = builder.offset;
        limit = builder.limit;
        sortOrderAsc = builder.sortOrderAsc;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getApp() {
        return app;
    }

    public String getCollection() {
        return collection;
    }

    public String getService() {
        return service;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getTraceId() {
        return traceId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public List<String> getReqids() {
        return reqids;
    }

    public List<String> getPaths() {
        return paths;
    }

    public int getPayloadKey() {
        return payloadKey;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isSortOrderAsc() {
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
