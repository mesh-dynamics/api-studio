package io.md.dao;

import java.time.Instant;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.constants.Constants;
import io.md.dao.Event.EventType;

/**
 * EventQuery is POJO to query generic events with matching criteria
 */
@JsonDeserialize(builder = EventQuery.Builder.class)
public class EventQuery {

    private final String customerId;
    private final String app;
    private final List<EventType> eventTypes;

    private final List<String> services;
    private final Optional<String> instanceId;
    private final Optional<String> collection;

    private final List<String> traceIds;
    private final Optional<Event.RunType> runType;
    private final Optional<String> spanId;
    private final Optional<String> parentSpanId;
    private final Optional<Instant> timestamp;

    private final List<String> reqIds;
    private final List<String> paths;
    private final boolean excludePaths;
    private final Optional<Integer> payloadKey;
    private final Optional<Integer> offset;
    private final Optional<Integer> limit;
    private final Optional<Boolean> sortOrderAsc;
    private final Map<String , Float> orQueryWeightage;

    public static class Builder {
        private final String customerId;
        private final String app;
        private final List<EventType> eventTypes;

        private List<String> services = Collections.emptyList();
        private String instanceId = null;
        private String collection = null;
        private List<String> traceIds = Collections.emptyList();
        private Event.RunType runType = null;
        private String spanId = null;
        private String parentSpanId = null;
        private Instant timestamp = null;
        private List<String> reqIds = Collections.emptyList();
        private List<String> paths = Collections.emptyList();
        private boolean excludePaths = false;
        private Integer payloadKey = null;
        private Integer offset = null;
        private Integer limit = null;
        private Boolean sortOrderAsc = null;
        private Map<String , Float> orQueryWeightage = new HashMap<>();

        //@JsonCreator
        public Builder(String customerId,
            String app,
            EventType eventType) {
            this.customerId = customerId;
            this.app = app;
            this.eventTypes = Arrays.asList(eventType);
        }

        private void verifyWeight(Float w){
            if(w<=0 || w>=100) throw new RuntimeException("Invalid value for weight "+w);
        }

        @JsonCreator
        public Builder(@JsonProperty("customerId") String customerId,
            @JsonProperty("app") String app,
            @JsonProperty("eventTypes") List<EventType> eventTypes) {
            this.customerId = customerId;
            this.app = app;
            this.eventTypes = eventTypes;
        }

        public Builder withService(String val) {
            services = Arrays.asList(val);
            return this;
        }

        public Builder withService(String val , Float weight){
            services = Arrays.asList(val);
            orQueryWeightage.put(Constants.SERVICE_FIELD, weight);
            return this;
        }


        public Builder withServices(List<String> vals) {
            services = vals;
            return this;
        }
        public Builder withServices(List<String> vals , Float weight) {
            services = vals;
            orQueryWeightage.put(Constants.SERVICE_FIELD , weight);
            return this;
        }

        public Builder withInstanceId(String val) {
            instanceId = val;
            return this;
        }
        public Builder withInstanceId(String val , Float weight) {
            instanceId = val;
            orQueryWeightage.put(Constants.INSTANCE_ID_FIELD , weight);
            return this;
        }


        public Builder withCollection(String val) {
            collection = val;
            return this;
        }

        public Builder withCollection(String val , Float weight) {
            collection = val;
            orQueryWeightage.put(Constants.COLLECTION_FIELD , weight);
            return this;
        }


        public Builder withTraceId(String val) {
            traceIds = Arrays.asList(val);
            return this;
        }

        public Builder withTraceId(String val , Float weight) {
            traceIds = Arrays.asList(val);
            orQueryWeightage.put(Constants.TRACE_ID_FIELD , weight);

            return this;
        }

        public Builder withTraceIds(List<String> vals) {
            traceIds = vals;
            return this;
        }

        public Builder withTraceIds(List<String> vals , Float weight) {
            traceIds = vals;
            orQueryWeightage.put(Constants.TRACE_ID_FIELD , weight);
            return this;
        }


        public Builder withRunType(Event.RunType val) {
            runType = val;
            return this;
        }

        public Builder withRunType(Event.RunType val , Float weight) {
            runType = val;
            orQueryWeightage.put(Constants.RUN_TYPE_FIELD ,weight);
            return this;
        }

        public Builder withSpanId(String val) {
            spanId = val;
            return this;
        }

        public Builder withSpanId(String val , Float weight) {
            spanId = val;
            orQueryWeightage.put(Constants.SPAN_ID_FIELD ,weight);
            return this;
        }

        public Builder withParentSpanId(String val) {
            parentSpanId = val;
            return this;
        }

        public Builder withParentSpanId(String val , Float weight) {
            parentSpanId = val;
            orQueryWeightage.put(Constants.PARENT_SPAN_ID_FIELD , weight);
            return this;
        }

        public Builder withTimestamp(Instant val) {
            timestamp = val;
            return this;
        }

        public Builder withTimestamp(Instant val ,Float weight) {
            timestamp = val;
            orQueryWeightage.put(Constants.TIMESTAMP_FIELD , weight);
            return this;
        }

        public Builder withReqId(String val) {
            reqIds = Arrays.asList(val);
            return this;
        }

        public Builder withReqId(String val , Float weight) {
            reqIds = Arrays.asList(val);
            orQueryWeightage.put(Constants.REQ_ID_FIELD , weight);
            return this;
        }

        public Builder withReqIds(List<String> val) {
            reqIds = val;
            return this;
        }

        public Builder withReqIds(List<String> val , Float weight) {
            reqIds = val;
            orQueryWeightage.put(Constants.REQ_ID_FIELD , weight);
            return this;
        }


        public Builder withPath(String val) {
            paths = Arrays.asList(val);
            return this;
        }

        public Builder withPath(String val , Float weight) {
            paths = Arrays.asList(val);
            orQueryWeightage.put(Constants.PATH_FIELD , weight);
            return this;
        }

        public Builder withPaths(List<String> val) {
            paths = val;
            return this;
        }

        public Builder withPaths(List<String> val , Float weight) {
            paths = val;
            orQueryWeightage.put(Constants.PATH_FIELD , weight);
            return this;
        }

        public Builder withExcludePaths(boolean val) {
            excludePaths = val;
            return this;
        }

        public Builder withExcludePaths(boolean val , Float weight) {
            excludePaths = val;
            orQueryWeightage.put(Constants.EXCLUDE_PATH_FIELD , weight);
            return this;
        }


        // Need to use boxed type (Integer) here instead of int, so that null can be passed if needed
        // https://github.com/FasterXML/jackson-databind/issues/605
        public Builder withPayloadKey(Integer val) {
            payloadKey = val;
            return this;
        }

        public Builder withPayloadKey(Integer val , Float weight) {
            payloadKey = val;
            orQueryWeightage.put(Constants.PAYLOAD_KEY_FIELD , weight);
            return this;
        }

        public Builder withOffset(Integer val) {
            offset = val;
            return this;
        }

        public Builder withLimit(Integer val) {
            limit = val;
            return this;
        }

        public Builder withSortOrderAsc(Boolean val) {
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
        eventTypes = builder.eventTypes;
        services = builder.services;
        instanceId = Optional.ofNullable(builder.instanceId);
        collection = Optional.ofNullable(builder.collection);
        traceIds = builder.traceIds;
        runType = Optional.ofNullable(builder.runType);
        spanId = Optional.ofNullable(builder.spanId);
        parentSpanId = Optional.ofNullable(builder.parentSpanId);
        timestamp = Optional.ofNullable(builder.timestamp);
        reqIds = builder.reqIds;
        paths = builder.paths;
        excludePaths = builder.excludePaths;
        payloadKey = Optional.ofNullable(builder.payloadKey);
        offset = Optional.ofNullable(builder.offset);
        limit = Optional.ofNullable(builder.limit);
        sortOrderAsc = Optional.ofNullable(builder.sortOrderAsc);
        orQueryWeightage = builder.orQueryWeightage;
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
    public Optional<Float> getCollectionWeight() { return Optional.ofNullable(orQueryWeightage.get(Constants.COLLECTION_FIELD)) ; }

    public List<String> getServices() {
        return services;
    }
    public Optional<Float> getServicesWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.SERVICE_FIELD)); }

    public Optional<String> getInstanceId() {
        return instanceId;
    }
    public Optional<Float> getInstanceIdWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.INSTANCE_ID_FIELD)); }

    public List<String> getTraceIds() {
        return traceIds;
    }
    public Optional<Float> getTraceIdsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.TRACE_ID_FIELD)); }

    public Optional<Event.RunType> getRunType() { return runType; }
    public Optional<Float> getRunTypeWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.RUN_TYPE_FIELD)); }

    public List<String> getReqIds() {
        return reqIds;
    }
    public Optional<Float> getReqIdsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.REQ_ID_FIELD)); }

    public List<String> getPaths() {
        return paths;
    }
    public Optional<Float> getPathsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.PATH_FIELD)); }

    public boolean excludePaths() {
        return excludePaths;
    }
    public Optional<Float> excludePathsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.EXCLUDE_PATH_FIELD)); }

    public Optional<Integer> getPayloadKey() {
        return payloadKey;
    }
    public Optional<Float> getPayloadKeyWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.PAYLOAD_KEY_FIELD)); }

    public Optional<Integer> getOffset() {
        return offset;
    }

    public Optional<Integer> getLimit() {
        return limit;
    }

    public Optional<Boolean> isSortOrderAsc() {
        return sortOrderAsc;
    }

    public Optional<Instant> getTimestamp() {
        return timestamp;
    }
    public Optional<Float> getTimestampWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.TIMESTAMP_FIELD)); }

}