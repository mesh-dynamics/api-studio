package io.md.dao;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class RecordingOperationSetSP {
	public String id;// Solr id
    public String operationSetId;
    public String customer;
    public String app;
    public String service;
    public  String path;
    // list of operations
    @JsonProperty("operationSet")
    public List<ReqRespUpdateOperation> operationsList;

    // for jackson deserialization
    public RecordingOperationSetSP() {
        operationSetId = "";
        customer = "";
        app = "";
        service = "";
        path = "";
        operationsList = Collections.emptyList();
    }

    // constructor that takes in id
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

    // constructor that auto generates id
    public RecordingOperationSetSP(String operationSetId, String customer, String app, String service, String path,
        List<ReqRespUpdateOperation> operationsList) {
        this.operationSetId = operationSetId;
        this.customer = customer;
        this.app = app;
        this.service = service;
        this.path = path;
        generateId();
        this.operationsList = operationsList;
    }

    public void setOperationsList(List<ReqRespUpdateOperation> operationsList) {
        this.operationsList = operationsList;
    }

    // unique id for (operationSetId, service, path)
    public String generateId() {
        this.id =  "RecordingOperationSetSP-" + Objects.hash(operationSetId, service, path);
        return id;
    }
}
