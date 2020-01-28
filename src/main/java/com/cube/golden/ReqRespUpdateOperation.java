package com.cube.golden;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReqRespUpdateOperation {
    @JsonProperty("op")
    OperationType operationType;
    @JsonProperty("path")
    final String jsonpath;
    @JsonProperty("value")
    Object value;
    @JsonProperty("eventType")
    Type eventType;

    public enum Type {
        Request,
        Response;
    }

    @JsonCreator
    public ReqRespUpdateOperation(@JsonProperty("op") OperationType operationType,
                                  @JsonProperty("path") String jsonpath) {
        this.operationType = operationType;
        this.jsonpath = jsonpath;
        this.value = null;
    }

    @Override
    public String toString() {
        return "ReqRespUpdateOperation{" +
            "operationType=" + operationType +
            ", jsonpath='" + jsonpath + '\'' +
            ", value=" + value +
            '}';
    }

    public int hash() {
        return Objects.hash(jsonpath , eventType);
    }

    public String hashString() {
        return String.valueOf(hash());
    }
}
