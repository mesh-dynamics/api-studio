package io.md.dao;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecordingOperationSetSP {
	public String id;
    public String operationSetId;
    public String customer;
    public String app;
    public String service;
    public  String path;
    @JsonProperty("operationSet")
    public List<ReqRespUpdateOperation> operationsList;

    public RecordingOperationSetSP() {
        operationSetId = "";
        customer = "";
        app = "";
        service = "";
        path = "";
        operationsList = Collections.emptyList();
    }

    public RecordingOperationSetSP(String id, String operationSetId, String customer, String app,
                                   String service, String path, List<ReqRespUpdateOperation> operationsList) {
        this.id = id;
        this.operationSetId = operationSetId;
        this.customer = customer;
        this.app = app;
        this.service = service;
        this.path = path;
        this.operationsList = operationsList;
    }
}
