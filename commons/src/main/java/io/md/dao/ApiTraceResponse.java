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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;

public class ApiTraceResponse {
  public String traceId;
  public String collection;
  public Instant reqTimestamp;
  public List<ServiceReqRes> res;

  private ApiTraceResponse() {
    this.traceId = "";
    this.collection = "";
    this.reqTimestamp = Instant.now();
    this.res = Collections.emptyList();
  }

  public ApiTraceResponse(String traceId, String collection, Instant reqTimestamp) {
    this.traceId = traceId;
    this.collection = collection;
    this.reqTimestamp = reqTimestamp;
    this.res = new ArrayList<>();
  }

  public ApiTraceResponse(String traceId, String collection, Instant reqTimestamp,
      List<ServiceReqRes> res) {
    this.traceId = traceId;
    this.collection = collection;
    this.reqTimestamp = reqTimestamp;
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
    public Map<String, String> metaData;

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
      this.metaData = new HashMap<>();
    }

    public ServiceReqRes(String service, String apiPath, String requestEventId,
        Instant reqTimestamp, String spanId,
        String parentSpanId, String status, String method,
        MultivaluedHashMap<String, String> queryParams, Map<String, String> metaData) {
      this.service = service;
      this.apiPath = apiPath;
      this.requestEventId = requestEventId;
      this.reqTimestamp = reqTimestamp;
      this.spanId = spanId;
      this.parentSpanId = parentSpanId;
      this.status = status;
      this.method = method;
      this.queryParams = queryParams;
      this.metaData = metaData;
    }
  }
}
