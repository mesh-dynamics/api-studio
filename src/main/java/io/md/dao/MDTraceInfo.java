package io.md.dao;

public class MDTraceInfo {

  public final String traceId;
  public final String spanId;
  public final String parentSpanId;

  public MDTraceInfo() {
    this.traceId = null;
    this.spanId = null;
    this.parentSpanId = null;
  }

  public MDTraceInfo(String traceId, String spanId, String parentSpanId) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
  }

}
