package com.cube.dao;

public class DefaultEvent {

    private final Event event;
    // Event will have the request payload. The following
    // fields will hold the response payload.
    private final byte[] rawRespPayloadBinary;
    private final String rawRespPayloadString;

    public DefaultEvent(Event event, byte[] rawRespPayloadBinary, String rawRespPayloadString) {
        this.event = event;
        this.rawRespPayloadBinary = rawRespPayloadBinary;
        this.rawRespPayloadString = rawRespPayloadString;
    }

    private DefaultEvent() {
        this.event = null;
        this.rawRespPayloadString = null;
        this.rawRespPayloadBinary = null;
    }

    public Event getEvent() {
        return event;
    }

    public byte[] getRawRespPayloadBinary() {
        return rawRespPayloadBinary;
    }

    public String getRawRespPayloadString() {
        return rawRespPayloadString;
    }
}
