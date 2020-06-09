package io.md.dao;

public class ConvertEventPayloadResponse {

  private boolean truncated;
  private String response;

  public ConvertEventPayloadResponse() {
    this.response = "";
    this.truncated = false;
  }


  public boolean isTruncated() {
    return truncated;
  }

  public void setTruncated(boolean truncated) {
    this.truncated = truncated;
  }

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }
}
