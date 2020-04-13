package io.md.dao;

import java.util.HashMap;
import java.util.Map;

public class MDTraceInfo {

  public final String traceId;
  public final String spanId;
  public final String parentSpanId;
  public final Map<String, String> baggageItems = new HashMap<>();

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

  public MDTraceInfo(String traceId, String spanId, String parentSpanId, Iterable<Map.Entry<String, String>> baggageItemsItr) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
    for (Map.Entry<String, String> baggageItem : baggageItemsItr)
      baggageItems.entrySet().add(baggageItem);
  }
}
