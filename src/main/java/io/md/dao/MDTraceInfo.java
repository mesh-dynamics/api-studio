package io.md.dao;

import java.util.HashMap;
import java.util.Map;

import io.md.constants.Constants;

public class MDTraceInfo {

  public final String traceId;
  public final String spanId;
  public final String parentSpanId;
  public Map<String, String> baggageItems = new HashMap<>();

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

  public MDTraceInfo(String traceId, String spanId, String parentSpanId,
      Map<String, String> baggageItems) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
    this.baggageItems = baggageItems;
  }

  public String getParentSpanId() {
    String baggageParentSpanId = baggageItems.get(Constants.MD_PARENT_SPAN);
    if ( baggageParentSpanId != null)
      return baggageParentSpanId;
    return parentSpanId;
  }
}
