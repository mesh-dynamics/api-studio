/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.time.Instant;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-09-03
 */

/**
 * Event represents a generic event that is recorded. It can be a request or response captured from various protocols
 * such as REST over HTTP, Java function calls, GRPC, Thrift etc.
 * The common metadata for all such events in captured in the event fields. The payload represents the actual data
 * that can be encoded in different formats such as Json, ProtoBuf, Thrift etc depending on the event type
 */
public class Event {


    public Event(String customerid, String app, String service, String traceid, Instant timestamp, EventType type, DataObj payload, int payloadKey) {
        this.customerid = customerid;
        this.app = app;
        this.service = service;
        this.traceid = traceid;
        this.timestamp = timestamp;
        this.type = type;
        this.payload = payload;
        this.payloadKey = payloadKey;
    }

    enum EventType {
        HTTPRequest,
        HTTPResponse,
        JavaRequest,
        JavaResponse,
        ThriftRequest,
        ThriftResponse,
        ProtoBufRequest,
        ProtoBufResponse
    }

    final String customerid;
    final String app;
    final String service;
    final String traceid;
    final Instant timestamp;
    final EventType type;
    final DataObj payload;
    final int payloadKey;

}
