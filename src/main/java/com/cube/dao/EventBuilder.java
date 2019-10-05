/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.dao;

import java.time.Instant;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cube.agent.UtilException;

public class EventBuilder {

    private static final Logger LOGGER = LogManager.getLogger(Event.class);

    private final String customerId;
    private final String app;
    private final String service;
    private final String instanceId;
    private final String collection;
    private final String traceId;
    private final Event.RecordReplayType rrType;
    private final Instant timestamp;
    private final String reqId;
    private final String apiPath;
    private final Event.EventType eventType;
    private byte[] rawPayloadBinary;
    private String rawPayloadString;
    private DataObj payload;
    private int payloadKey = 0;

    public EventBuilder(String customerId, String app, String service, String instanceId, String collection, String traceId,
                        Event.RecordReplayType rrType, Instant timestamp, String reqId, String apiPath, Event.EventType eventType) {
        this.customerId = customerId;
        this.app = app;
        this.service = service;
        this.instanceId = instanceId;
        this.collection = collection;
        this.traceId = traceId;
        this.rrType = rrType;
        this.timestamp = timestamp;
        this.reqId = reqId;
        this.apiPath = apiPath;
        this.eventType = eventType;
    }


    public EventBuilder setRawPayloadBinary(byte[] rawPayloadBinary) {
        this.rawPayloadBinary = rawPayloadBinary;
        return this;
    }

    public EventBuilder setRawPayloadString(String rawPayloadString) {
        this.rawPayloadString = rawPayloadString;
        return this;
    }

    public EventBuilder setPayload(DataObj payload) {
        this.payload = payload;
        return this;
    }

    public EventBuilder setPayloadKey(int payloadKey) {
        this.payloadKey = payloadKey;
        return this;
    }

    public Event createEvent() throws InvalidEventException {
        Event event = new Event(customerId, app, service, instanceId, collection, traceId, rrType, timestamp, reqId, apiPath,
        eventType,
        rawPayloadBinary, rawPayloadString, payload, payloadKey);
        if (event.validate()) {
            return event;
        } else {
            throw new InvalidEventException();
        }
    }

    public Optional<Event> createEventOpt() {
        try {
            return Optional.of(createEvent());
        } catch (InvalidEventException e) {
            LOGGER.error("Exception in creating an Event", e.getMessage(),
                UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
        }
        return Optional.empty();
    }


    public static class InvalidEventException extends Exception {

    }
}
