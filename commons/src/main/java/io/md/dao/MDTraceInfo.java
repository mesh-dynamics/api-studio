/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  public MDTraceInfo(String traceId, String spanId, String parentSpanId, Iterable<Map.Entry<String, String>> baggageItemsItr) {
    this.traceId = traceId;
    this.spanId = spanId;
    this.parentSpanId = parentSpanId;
    for (Map.Entry<String, String> baggageItem : baggageItemsItr)
      baggageItems.put(baggageItem.getKey(), baggageItem.getValue());
  }

  public String getParentSpanId() {
    String baggageParentSpanId = baggageItems.get(Constants.MD_PARENT_SPAN);
    if ( baggageParentSpanId != null)
      return baggageParentSpanId;
    return parentSpanId;
  }

  @Override
  public String toString() {
    return "MDTraceInfo{" +
            "traceId='" + traceId + '\'' +
            ", spanId='" + spanId + '\'' +
            ", parentSpanId='" + parentSpanId + '\'' +
            '}';
  }
}
