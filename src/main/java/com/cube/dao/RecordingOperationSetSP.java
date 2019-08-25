package com.cube.dao;

import com.cube.golden.ReqRespUpdateOperation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RecordingOperationSetSP {
    public final String id; // Solr id

    @JsonProperty("operationSetId")
    public final String operationSetId; // OperationSet id

    @JsonProperty("customer")
    public final String customer;

    @JsonProperty("app")
    public final String app;

    @JsonProperty("service")
    public final String service;

    @JsonProperty("path")
    public final String path;

    // list of operations
    @JsonProperty("operationSet")
    public List<ReqRespUpdateOperation> operationsList;


    // for jackson deserialization
    public RecordingOperationSetSP() {
        id = generateId();
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
        this.id = generateId();
        this.operationSetId = operationSetId;
        this.customer = customer;
        this.app = app;
        this.service = service;
        this.path = path;
        this.operationsList = operationsList;
    }

    public void setOperationsList(List<ReqRespUpdateOperation> operationsList) {
        this.operationsList = operationsList;
    }

    // unique id for (operationSetId, service, path)
    private String generateId() {
        return "RecordingOperationSetSP-" + Objects.hash(operationSetId, service, path);
    }
}


