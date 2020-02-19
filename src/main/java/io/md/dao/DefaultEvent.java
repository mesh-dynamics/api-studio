package io.md.dao;

public class DefaultEvent {
	public  Event event;
    public  byte[] rawRespPayloadBinary;
    public  String rawRespPayloadString;

    public DefaultEvent(Event event, byte[] rawRespPayloadBinary, String rawRespPayloadString) {
        this.event = event;
        this.rawRespPayloadBinary = rawRespPayloadBinary;
        this.rawRespPayloadString = rawRespPayloadString;
    }

    public DefaultEvent() {
        this.event = null;
        this.rawRespPayloadString = null;
        this.rawRespPayloadBinary = null;
    }
}
