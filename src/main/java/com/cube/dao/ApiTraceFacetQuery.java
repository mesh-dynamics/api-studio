package com.cube.dao;

import io.md.core.Utils;
import io.md.utils.Constants;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;

public class ApiTraceFacetQuery {
  public final String customerId;
  public final String appId;
  public final Optional<String> service;
  public final Optional<String> apiPath;
  public final Optional<String> instanceId;
  public final Optional<Instant> startDate;
  public final Optional<Instant> endDate;
  public  List<String> traceIds;
  public final Optional<String> recordingType;
  public final Optional<String> collection;
  public final Optional<String> runId;

  public ApiTraceFacetQuery(String customerId, String appId) {
    this.customerId = customerId;
    this.appId = appId;
    this.service = Optional.empty();
    this.apiPath = Optional.empty();
    this.instanceId = Optional.empty();
    this.startDate = Optional.empty();
    this.endDate = Optional.empty();
    this.traceIds = Collections.emptyList();
    this.recordingType = Optional.empty();
    this.collection = Optional.empty();
    this.runId = Optional.empty();
  }

  public ApiTraceFacetQuery(String customerId, String appId, MultivaluedMap<String, String> queryParams) {
    this.customerId = customerId;
    this.appId = appId;
    this.service = Optional
        .ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
    this.apiPath = Optional
        .ofNullable(queryParams.getFirst(Constants.API_PATH_FIELD));
    this.instanceId = Optional
        .ofNullable(queryParams.getFirst(Constants.INSTANCE_ID_FIELD));
    Optional<String> endDate = Optional.ofNullable(queryParams.getFirst(Constants.END_DATE_FIELD));
    Optional<String> startDate = Optional.ofNullable(queryParams.getFirst(Constants.START_DATE_FIELD));
    this.endDate = endDate.flatMap(Utils::strToTimeStamp);
    this.startDate = startDate.flatMap(Utils::strToTimeStamp);
    this.traceIds = Optional
        .ofNullable(queryParams.get(Constants.TRACE_ID_FIELD)).orElse(Collections.emptyList());
    this.recordingType = Optional.ofNullable(queryParams.getFirst(Constants.RECORDING_TYPE_FIELD));
    this.collection = Optional.ofNullable(queryParams.getFirst(Constants.COLLECTION_FIELD));
    this.runId =  Optional.ofNullable(queryParams.getFirst(Constants.RUN_ID_FIELD));
  }
  public void withTraceIds(List<String> traceIds) {
    this.traceIds = traceIds;
  }
}
