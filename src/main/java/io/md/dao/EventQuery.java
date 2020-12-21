package io.md.dao;

import java.time.Instant;
import java.util.*;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.constants.Constants;
import io.md.dao.Event.EventType;

/**
 * EventQuery is POJO to query generic events with matching criteria
 */
@JsonDeserialize(builder = EventQuery.Builder.class)
public class EventQuery {

    //Mocking Optional Query Weight related Constants
    public static final float COLLECTION_WEIGHT = 1.0f;
    public static final float TRACEID_WEIGHT = 1.5f;
    public static final float PAYLOAD_KEY_WEIGHT = 3.0f;

    public static float getEventMaxWeight(){
        return COLLECTION_WEIGHT + TRACEID_WEIGHT + PAYLOAD_KEY_WEIGHT;
    }


    private final String customerId;
    private final String app;
    private final List<EventType> eventTypes;

    private final List<String> services;
    private final Optional<String> instanceId;
    private final List<String> collections;

    private final List<String> traceIds;
    private final List<Event.RunType> runTypes;
    private final Optional<String> spanId;
    private final Optional<String> parentSpanId;
    private final Optional<Instant> startTimestamp;
    private final Optional<Instant> endTimestamp;

    private final Optional<String> startSeqId ;
    private final Optional<String> endSeqId;


    private final List<String> reqIds;
    private final List<String> paths;
    private final boolean excludePaths;
    private final Optional<Integer> payloadKey;
    private final Optional<Integer> offset;
    private final Optional<Integer> limit;

    private final LinkedHashMap<String , Boolean> sortingOrder;
    private final List<String> payloadFields;
    private final Map<String , Float> orQueryWeightage;
    private final Optional<JoinQuery> joinQuery;

    public static class Builder {
        private final String customerId;
        private final String app;
        private final List<EventType> eventTypes;

        private List<String> services = Collections.emptyList();
        private String instanceId = null;
        private List<String> collections = Collections.emptyList();
        private List<String> traceIds = Collections.emptyList();
        private List<Event.RunType> runTypes = Collections.EMPTY_LIST;
        private String spanId = null;
        private String parentSpanId = null;
        private Instant startTimestamp = null;
        private Instant endTimestamp = null;
        private String startSeqId = null;
        private String endSeqId   = null;
        private List<String> reqIds = Collections.emptyList();
        private List<String> paths = Collections.emptyList();
        private boolean excludePaths = false;
        private Integer payloadKey = null;
        private Integer offset = null;
        private Integer limit = null;

        private LinkedHashMap<String , Boolean> sortingOrder = new LinkedHashMap<>();
        private List<String> payloadFields = Collections.EMPTY_LIST;
        private Map<String , Float> orQueryWeightage = new HashMap<>();
        private Optional<JoinQuery> joinQuery = Optional.empty();

        {
            // By default score field is first priority with desc order
            sortingOrder.put(Constants.SCORE_FIELD , false);
        }

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

        @JsonSetter(nulls = Nulls.FAIL)
        public Builder withService(String val) {
            services = Arrays.asList(val);
            return this;
        }

        public Builder withService(String val , Float weight){
            services = Arrays.asList(val);
            orQueryWeightage.put(Constants.SERVICE_FIELD, weight);
            return this;
        }

        @JsonSetter(nulls = Nulls.FAIL , contentNulls = Nulls.FAIL)
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


        @JsonSetter(value = "collection", nulls = Nulls.FAIL)
        public Builder withCollection(String val) {
            collections = Arrays.asList(val);
            return this;
        }

        public Builder withCollection(String val , Float weight) {
            collections = Arrays.asList(val);
            orQueryWeightage.put(Constants.COLLECTION_FIELD , weight);
            return this;
        }

        @JsonSetter(nulls = Nulls.FAIL , contentNulls = Nulls.FAIL)
        public Builder withCollections(List<String> val) {
            collections = val;
            return this;
        }

        public Builder withCollections(List<String> val , Float weight) {
            collections = val;
            orQueryWeightage.put(Constants.COLLECTION_FIELD , weight);
            return this;
        }


        @JsonSetter(nulls = Nulls.FAIL)
        public Builder withTraceId(String val) {
            traceIds = Arrays.asList(val);
            return this;
        }

        public Builder withTraceId(String val , Float weight) {
            traceIds = Arrays.asList(val);
            orQueryWeightage.put(Constants.TRACE_ID_FIELD , weight);

            return this;
        }

        @JsonSetter(nulls = Nulls.FAIL , contentNulls = Nulls.FAIL)
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
            runTypes = Arrays.asList(val);
            return this;
        }

        public Builder withRunType(Event.RunType val , Float weight) {
            runTypes = Arrays.asList(val);
            orQueryWeightage.put(Constants.RUN_TYPE_FIELD ,weight);
            return this;
        }

        public Builder withRunTypes(List<Event.RunType> vals) {
            runTypes = vals;
            return this;
        }

        public Builder withRunTypes(List<Event.RunType> vals , Float weight) {
            runTypes = vals;
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

        public Builder withStartTimestamp(Instant val) {
            startTimestamp = val;
            return this;
        }

        public Builder withEndTimestamp(Instant val) {
            endTimestamp = val;
            return this;
        }

        public Builder withStartTimestamp(Instant val ,Float weight) {
            startTimestamp = val;
            orQueryWeightage.put(Constants.START_TIMESTAMP_FIELD , weight);
            return this;
        }

        public Builder withEndTimestamp(Instant val , Float weight) {
            endTimestamp = val;
            orQueryWeightage.put(Constants.END_TIMESTAMP_FIELD , weight);
            return this;
        }

        public Builder withStartSeqId(String seqId) {
            startSeqId = seqId;
            return this;
        }

        public Builder withEndSeqId(String seqId) {
            endSeqId = seqId;
            return this;
        }


        @JsonSetter(nulls = Nulls.FAIL)
        public Builder withReqId(String val) {
            reqIds = Arrays.asList(val);
            return this;
        }

        public Builder withReqId(String val , Float weight) {
            reqIds = Arrays.asList(val);
            orQueryWeightage.put(Constants.REQ_ID_FIELD , weight);
            return this;
        }

        @JsonSetter(nulls = Nulls.FAIL , contentNulls = Nulls.FAIL)
        public Builder withReqIds(List<String> val) {
            reqIds = val;
            return this;
        }

        public Builder withReqIds(List<String> val , Float weight) {
            reqIds = val;
            orQueryWeightage.put(Constants.REQ_ID_FIELD , weight);
            return this;
        }


        @JsonSetter(nulls = Nulls.FAIL)
        public Builder withPath(String val) {
            paths = Arrays.asList(val);
            return this;
        }

        public Builder withPath(String val , Float weight) {
            paths = Arrays.asList(val);
            orQueryWeightage.put(Constants.PATH_FIELD , weight);
            return this;
        }

        @JsonSetter(nulls = Nulls.FAIL , contentNulls = Nulls.FAIL)
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

        public Builder withSortingOrder(LinkedHashMap<String, Boolean> order) {
            sortingOrder = order;
            return this;
        }

        /*
        public Builder withIndexAsc(boolean asc){
            sortingOrder.clear();
            return withTimestampAsc(asc);
        }*/

        public Builder withTimestampAsc(boolean asc){
            sortingOrder.put(Constants.TIMESTAMP_FIELD , asc);
            return this;
        }
        public Builder withScoreAsc(boolean asc){
            sortingOrder.put(Constants.SCORE_FIELD , asc);
            return this;
        }

        // Remove the default scoring order
        public Builder withoutScoreOrder(){
            sortingOrder.remove(Constants.SCORE_FIELD);
            return this;
        }

        public Builder withSeqIdAsc(boolean asc){
            sortingOrder.put(Constants.SEQID_FIELD , asc);
            return this;
        }

        @JsonSetter(nulls = Nulls.FAIL , contentNulls = Nulls.FAIL)
        public Builder withPayloadFields(List<String> pyldFields){
            this.payloadFields = pyldFields;
            return this;
        }

        public Builder withJoinQuery(JoinQuery joinQuery){
            this.joinQuery = Optional.ofNullable(joinQuery);
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
        collections = builder.collections;
        traceIds = builder.traceIds;
        runTypes = builder.runTypes;
        spanId = Optional.ofNullable(builder.spanId);
        parentSpanId = Optional.ofNullable(builder.parentSpanId);
        startTimestamp = Optional.ofNullable(builder.startTimestamp);
        endTimestamp = Optional.ofNullable(builder.endTimestamp);
        reqIds = builder.reqIds;
        paths = builder.paths;
        excludePaths = builder.excludePaths;
        payloadKey = Optional.ofNullable(builder.payloadKey);
        offset = Optional.ofNullable(builder.offset);
        limit = Optional.ofNullable(builder.limit);
        sortingOrder = builder.sortingOrder;
        orQueryWeightage = builder.orQueryWeightage;
        payloadFields = builder.payloadFields;
        joinQuery = builder.joinQuery;
        startSeqId = Optional.ofNullable(builder.startSeqId);
        endSeqId = Optional.ofNullable(builder.endSeqId);
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

    public List<String> getCollections() {
        return collections;
    }
    @JsonIgnore
    public Optional<Float> getCollectionWeight() { return Optional.ofNullable(orQueryWeightage.get(Constants.COLLECTION_FIELD)) ; }

    public List<String> getServices() {
        return services;
    }
    @JsonIgnore
    public Optional<Float> getServicesWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.SERVICE_FIELD)); }

    public Optional<String> getInstanceId() {
        return instanceId;
    }
    @JsonIgnore
    public Optional<Float> getInstanceIdWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.INSTANCE_ID_FIELD)); }

    public List<String> getTraceIds() {
        return traceIds;
    }
    @JsonIgnore
    public Optional<Float> getTraceIdsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.TRACE_ID_FIELD)); }

    public List<Event.RunType> getRunTypes() { return runTypes; }
    @JsonIgnore
    public Optional<Float> getRunTypeWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.RUN_TYPE_FIELD)); }

    public List<String> getReqIds() {
        return reqIds;
    }
    @JsonIgnore
    public Optional<Float> getReqIdsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.REQ_ID_FIELD)); }

    public List<String> getPaths() {
        return paths;
    }
    @JsonIgnore
    public Optional<Float> getPathsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.PATH_FIELD)); }

    public boolean excludePaths() {
        return excludePaths;
    }
    @JsonIgnore
    public Optional<Float> excludePathsWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.EXCLUDE_PATH_FIELD)); }

    public Optional<Integer> getPayloadKey() {
        return payloadKey;
    }
    @JsonIgnore
    public Optional<Float> getPayloadKeyWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.PAYLOAD_KEY_FIELD)); }

    public Optional<Integer> getOffset() {
        return offset;
    }

    public Optional<Integer> getLimit() {
        return limit;
    }

    public LinkedHashMap<String, Boolean> getSortingOrder() {
        return sortingOrder;
    }

    public Optional<Instant> getStartTimestamp() { return startTimestamp; }

    public Optional<Instant> getEndTimestamp() {
        return endTimestamp;
    }

    public Optional<String> getStartSeqId() { return startSeqId; }

    public Optional<String> getEndSeqId() {
        return endSeqId;
    }

    @JsonIgnore
    public Optional<Float> getTimestampWeight() {return Optional.ofNullable(orQueryWeightage.get(Constants.TIMESTAMP_FIELD)); }

    public List<String> getPayloadFields() {
        return payloadFields;
    }

    public Optional<JoinQuery> getJoinQuery() {
        return joinQuery;
    }
}