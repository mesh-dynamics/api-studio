package io.md.dao;

public class DefaultEvent {
  private final Event event;
  // Event will have the request payload. The following
  // fields will hold the response payload.
  private final Payload respPayload;

  public DefaultEvent(Event event, Payload respPayload) {
    this.event = event;
    this.respPayload = respPayload;
  }

  private DefaultEvent() {
    this.event = null;
    respPayload = null;
  }

  public Event getEvent() {
    return event;
  }

  public Payload getRespPayload() {
    return this.respPayload;
  }
}
