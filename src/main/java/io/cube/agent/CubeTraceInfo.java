package io.cube.agent;

public class CubeTraceInfo {

  public final String traceId;
  public final String spanId;
  public final String parentSpanId;

  CubeTraceInfo(String traceId, String spanId, String parentSpanId) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
  }

}
