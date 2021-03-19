package io.md.dao;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Optional;

public class RecordingOperationSetSP {
	public String id;// Solr id
    public String operationSetId;
    public String customer;
    public String app;
    public String service;
    public String path;
    public Optional<String> method;
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
        method = Optional.empty();
        operationsList = Collections.emptyList();
    }

    // constructor that takes in id
    public RecordingOperationSetSP(String id, String operationSetId, String customer, String app,
        String service, String path, Optional<String> method ,  List<ReqRespUpdateOperation> operationsList) {
        this.id = id;
        this.operationSetId = operationSetId;
        this.customer = customer;
        this.app = app;
        this.service = service;
        this.path = path;
        this.method = method;
        this.operationsList = operationsList;
    }

    // constructor that auto generates id
    public RecordingOperationSetSP(String operationSetId, String customer, String app, String service, String path,
        Optional<String> method,
        List<ReqRespUpdateOperation> operationsList) {
        this.operationSetId = operationSetId;
        this.customer = customer;
        this.app = app;
        this.service = service;
        this.path = path;
        this.method = method;
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
