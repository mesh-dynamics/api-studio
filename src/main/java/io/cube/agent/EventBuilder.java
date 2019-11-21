/*
 *
 *    Copyright Cube I O
 *
 */

package io.cube.agent;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cube.agent.Event.EventType;

public class EventBuilder {

	private static final Logger LOGGER = LogManager.getLogger(Event.class);

	private final String customerId;
	private final String app;
	private final String service;
	private final String instanceId;
	private String collection;
	private final String traceId;
	private final String spanId;
	private final String parentSpanId;
	private final Event.RunType runType;
	private Instant timestamp;
	private String reqId;
	private final String apiPath;
	private final Event.EventType eventType;
	private byte[] rawPayloadBinary;
	private String rawPayloadString;
	private DataObj payload;
	private int payloadKey = 0;

    public EventBuilder(String customerId, String app, String service, String instanceId,
        String collection, String traceId, Event.RunType runType, Instant timestamp, String reqId,
        String apiPath, Event.EventType eventType) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instanceId;
		this.collection = collection;
		this.traceId = traceId;
		this.spanId = null;
		this.parentSpanId = null;
		this.runType = runType;
		this.timestamp = timestamp;
		this.reqId = reqId;
		this.apiPath = apiPath;
		this.eventType = eventType;
	}

	public EventBuilder(CubeMetaInfo cubeMetaInfo, CubeTraceInfo cubeTraceInfo,
		Event.RunType runType, String apiPath, EventType eventType) {
		this.customerId = cubeMetaInfo.customerId;
		this.app = cubeMetaInfo.appName;
		this.instanceId = cubeMetaInfo.instanceId;
		this.service = cubeMetaInfo.serviceName;

		this.traceId = cubeTraceInfo.traceId;
		this.spanId = cubeTraceInfo.spanId;
		this.parentSpanId = cubeTraceInfo.parentSpanId;

		this.runType = runType;
		this.apiPath = apiPath;
		this.eventType = eventType;
	}

	public EventBuilder withRawPayloadBinary(byte[] rawPayloadBinary) {
		this.rawPayloadBinary = rawPayloadBinary;
		return this;
	}

	public EventBuilder withRawPayloadString(String rawPayloadString) {
		this.rawPayloadString = rawPayloadString;
		return this;
	}

	public EventBuilder withPayload(DataObj payload) {
		this.payload = payload;
		return this;
	}

	public EventBuilder withPayloadKey(int payloadKey) {
		this.payloadKey = payloadKey;
		return this;
	}

	public EventBuilder withCollection(String collection) {
		this.collection = collection;
		return this;
	}

	public EventBuilder withTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public EventBuilder withReqId(String reqId) {
		this.reqId = reqId;
		return this;
	}

	public Event build() throws InvalidEventException {
		if (this.reqId == null) {
			this.reqId = UUID.randomUUID().toString();
		}
		if (this.timestamp == null) {
			this.timestamp = Instant.now();
		}
        Event event = new Event(customerId, app, service, instanceId, collection, traceId, runType,
            timestamp, reqId, apiPath, eventType, rawPayloadBinary, rawPayloadString, payload,
            payloadKey);
        if (event.validate()) {
            return event;
        } else {
            throw new InvalidEventException();
        }
	}

	public Optional<Event> createEventOpt() {
		try {
			return Optional.of(build());
		} catch (InvalidEventException e) {
			LOGGER.error("Exception in creating an Event", e.getMessage(),
				UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
		}
		return Optional.empty();
	}


	public static class InvalidEventException extends Exception {

	}
}