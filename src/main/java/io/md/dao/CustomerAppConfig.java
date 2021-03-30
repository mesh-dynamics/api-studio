package io.md.dao;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = CustomerAppConfig.Builder.class)
public class CustomerAppConfig {

    @JsonProperty("id")
    public final String id;

    @JsonProperty("customerId")
    public final String customerId;

    @JsonProperty("app")
    public final String app;

    @JsonProperty("tracer")
    public final Optional<String> tracer;

    @JsonProperty("apiGenericPaths")
    public final Optional<Map<String , String[]>> apiGenericPaths;

    @JsonProperty("stopWaitInterval")
    public final Integer stopWaitInterval;

    @JsonProperty("filterTransform")
    public  final List<FilterTransform> filterTransform;

    private CustomerAppConfig(Builder builder){
        this.customerId = builder.customerId;
        this.app = builder.app;
        this.id = Objects.requireNonNull(builder.id) ;
        this.tracer = Optional.ofNullable(builder.tracer);
        this.apiGenericPaths = Optional.ofNullable(builder.apiGenericPaths);
        this.stopWaitInterval = builder.stopWaitInterval;
        this.filterTransform = builder.filterTransform;
    }

    public static class Builder{
        public  String customerId;
        public  String app;
        private String tracer;
        private Map<String , String[]> apiGenericPaths;
        private String id;
        private Integer stopWaitInterval;
        private List<FilterTransform> filterTransform = Collections.EMPTY_LIST;

        @JsonCreator
        public Builder(@JsonProperty("customerId") String customerId , @JsonProperty("app") String app){
            this.customerId = customerId;
            this.app = app;
        }

        public Builder withTracer(String tracer){
            this.tracer = tracer;
            return this;
        }

        public Builder withApiGenericPaths(Map<String , String[]> apiGenPaths){
            this.apiGenericPaths = apiGenPaths;
            return this;
        }

        public Builder withFilterTransform(List<FilterTransform> filterTransform){
            this.filterTransform = filterTransform;
            return this;
        }

        public Builder withId(String id){
            this.id = id;
            return this;
        }

        public Builder withStopWaitInterval(Integer stopWaitInterval) {
            this.stopWaitInterval = stopWaitInterval;
            return this;
        }

        public CustomerAppConfig build(){
            if(id==null){
                id = recalculateId();
            }
            return new CustomerAppConfig(this);
        }

        private String recalculateId() {
            return "CustomerAppConfig-".concat(String.valueOf(Math.abs(Objects.hash(customerId, app))));
        }
    }
}

