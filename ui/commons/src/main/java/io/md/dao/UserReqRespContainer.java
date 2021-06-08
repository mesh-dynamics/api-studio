package io.md.dao;

public class UserReqRespContainer {
  public Event request;
  public Event response;

  private UserReqRespContainer() {
    request = null;
    response = null;
  }

  public UserReqRespContainer(Event request, Event response) {
    this.request = request;
    this.response = response;
  }
}