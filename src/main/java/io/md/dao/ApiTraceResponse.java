package io.md.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;

public class ApiTraceResponse {
  public String traceId;
  public String collection;
  public List<ServiceReqRes> res;

  private ApiTraceResponse() {
    this.traceId = "";
    this.collection = "";
    this.res = Collections.emptyList();
  }

  public ApiTraceResponse(String traceId, String collection) {
    this.traceId = traceId;
    this.collection = collection;
    this.res = new ArrayList<>();
  }

  public ApiTraceResponse(String traceId, String collection,
      List<ServiceReqRes> res) {
    this.traceId = traceId;
    this.collection = collection;
    this.res = res;
  }

  public static class ServiceReqRes {
    public String service;
    public String apiPath;
    public String requestEventId;
    public Instant reqTimestamp;
    public String spanId;
    public String parentSpanId;
    public String status;
    public String method;
    public MultivaluedHashMap<String, String> queryParams;

    private ServiceReqRes() {
      this.service = "";
      this.apiPath = "";
      this.requestEventId = "";
      this.reqTimestamp = Instant.now();
      this.spanId = "";
      this.parentSpanId = "";
      this.status = "";
      this.method = "";
      this.queryParams = new MultivaluedHashMap<>();
    }

    public ServiceReqRes(String service, String apiPath, String requestEventId,
        Instant reqTimestamp, String spanId,
        String parentSpanId, String status, String method, MultivaluedHashMap<String, String> queryParams) {
      this.service = service;
      this.apiPath = apiPath;
      this.requestEventId = requestEventId;
      this.reqTimestamp = reqTimestamp;
      this.spanId = spanId;
      this.parentSpanId = parentSpanId;
      this.status = status;
      this.method = method;
      this.queryParams = queryParams;
    }
  }
}
