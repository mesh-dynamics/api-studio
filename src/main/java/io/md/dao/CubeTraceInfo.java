package io.md.dao;

public class CubeTraceInfo {

  public final String traceId;
  public final String spanId;
  public final String parentSpanId;

  public CubeTraceInfo(String traceId, String spanId, String parentSpanId) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
  }

}
