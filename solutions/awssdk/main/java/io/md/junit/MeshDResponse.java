package io.md.junit;

import com.fasterxml.jackson.databind.JsonNode;

public class MeshDResponse {
  private JsonNode body;

  private JsonNode headers;

  private int status;

  private String traceId;

  public MeshDResponse(JsonNode body, JsonNode headers, int status,
      String traceId) {
    this.body = body;
    this.headers = headers;
    this.status = status;
    this.traceId = traceId;
  }

  public JsonNode getBody() {
    return body;
  }

  public JsonNode getHeaders() {
    return headers;
  }

  public int getStatus() {
    return status;
  }

  public String getTraceId() {
    return traceId;
  }
}
