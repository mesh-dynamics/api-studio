package io.md.dao;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.constants.ReqRespUpdateOperationType;
import io.md.constants.ReqRespUpdateType;

public class ReqRespUpdateOperation {
	@JsonProperty("op")
    public ReqRespUpdateOperationType operationType;
    @JsonProperty("path")
    public String jsonpath;
    public Object value;
    public ReqRespUpdateType eventType;

    public ReqRespUpdateOperation(ReqRespUpdateOperationType operationType, String jsonpath, Object value, ReqRespUpdateType eventType) {
        this.operationType = operationType;
        this.jsonpath = jsonpath;
        this.value = value;
        this.eventType = eventType;
    }

    public ReqRespUpdateOperation() {
        this.operationType = ReqRespUpdateOperationType.REMOVE;
        this.jsonpath = "";
        this.value = "";
        this.eventType = ReqRespUpdateType.Request;
    }
}
