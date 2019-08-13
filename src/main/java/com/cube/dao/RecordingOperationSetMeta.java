package com.cube.dao;

import java.util.Objects;
import java.util.UUID;

public class RecordingOperationSetMeta {
    public final String id;
    public final String customer;
    public final String app;

    // constructor that generates an id
    public RecordingOperationSetMeta(String customer, String app) {
        this.customer = customer;
        this.app = app;
        this.id = generateId();
    }

    // constructor that takes in an id
    public RecordingOperationSetMeta(String id, String customer, String app) {
        this.customer = customer;
        this.app = app;
        this.id = id;
    }

    // generate unique id for the entire operation set
    private String generateId() {
        return UUID.randomUUID().toString();
    }
}
