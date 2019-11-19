package io.cube.agent;

public class CubeTraceInfo {

  final String traceId;
  final String spanId;
  final String parentSpanId;

  CubeTraceInfo(String traceId, String spanId, String parentSpanId) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
  }

}
