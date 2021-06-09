package io.md.junit;

import com.fasterxml.jackson.databind.JsonNode;

public class MeshDRequest {
  private JsonNode body;

  private JsonNode headers;

  private JsonNode queryPraams;

  private JsonNode formParams;

  private String httpMethod;

  private String path;

  private String traceId;

  public MeshDRequest(JsonNode body, JsonNode headers,
      JsonNode queryPraams, JsonNode formParams, String httpMethod, String path,
      String traceId) {
    this.body = body;
    this.headers = headers;
    this.queryPraams = queryPraams;
    this.formParams = formParams;
    this.httpMethod = httpMethod;
    this.path = path;
    this.traceId = traceId;
  }

  public JsonNode getBody() {
    return body;
  }

  public JsonNode getHeaders() {
    return headers;
  }

  public JsonNode getQueryPraams() {
    return queryPraams;
  }

  public JsonNode getFormParams() {
    return formParams;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public String getPath() {
    return path;
  }

  public String getTraceId() {
    return traceId;
  }
}
