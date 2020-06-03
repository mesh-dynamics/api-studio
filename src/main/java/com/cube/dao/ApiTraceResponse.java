package com.cube.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ApiTraceResponse {
  public String traceId;
  public String collection;
  public List<ServiceReqRes> res;

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

    public ServiceReqRes(String service, String apiPath, String requestEventId,
        Instant reqTimestamp, String spanId,
        String parentSpanId, String status) {
      this.service = service;
      this.apiPath = apiPath;
      this.requestEventId = requestEventId;
      this.reqTimestamp = reqTimestamp;
      this.spanId = spanId;
      this.parentSpanId = parentSpanId;
      this.status = status;
    }
  }
}
