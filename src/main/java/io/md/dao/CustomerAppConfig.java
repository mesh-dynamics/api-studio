package io.md.dao;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = CustomerAppConfig.Builder.class)
public class CustomerAppConfig {

    @JsonProperty("customerId")
    public final String customerId;

    @JsonProperty("app")
    public final String app;

    @JsonProperty("tracer")
    public final Optional<String> tracer;

    @JsonProperty("apiGenericPaths")
    public final Optional<Map<String , String[]>> apiGenericPaths;

    private CustomerAppConfig(Builder builder){
        this.customerId = builder.customerId;
        this.app = builder.app;
        this.tracer = Optional.ofNullable(builder.tracer);
        this.apiGenericPaths = Optional.ofNullable(builder.apiGenericPaths);
    }

    public static class Builder{
        public  String customerId;
        public  String app;
        private String tracer;
        private Map<String , String[]> apiGenericPaths;

        @JsonCreator
        public Builder(@JsonProperty("customerId") String customerId , @JsonProperty("app") String app){
            this.customerId = customerId;
            this.app = app;
        }

        /*
        public Builder withCustomerId(String customerId){
            this.customerId = customerId;
            return this;
        }
        public Builder withApp(String app){
            this.app = app;
            return this;
        }*/

        public Builder withTracer(String tracer){
            this.tracer = tracer;
            return this;
        }

        public Builder withApiGenericPaths(Map<String , String[]> apiGenPaths){
            this.apiGenericPaths = apiGenPaths;
            return this;
        }

        public CustomerAppConfig build(){
            return new CustomerAppConfig(this);
        }
    }
}

