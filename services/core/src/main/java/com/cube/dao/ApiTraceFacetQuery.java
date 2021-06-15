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

package com.cube.dao;

import io.md.core.Utils;
import io.md.utils.Constants;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
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
  public Collection<String> traceIds;
  public final Optional<String> recordingType;
  public Collection<String> collections;
  public  Collection<String> runIds;

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
    this.collections = Collections.emptyList();
    this.runIds = Collections.emptyList();
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
    this.collections = Optional.ofNullable(queryParams.get(Constants.COLLECTION_FIELD)).orElse(Collections.emptyList());
    this.runIds =  Optional.ofNullable(queryParams.get(Constants.RUN_ID_FIELD)).orElse(Collections.emptyList());
  }
  public void withTraceIds(Collection<String> traceIds) {
    this.traceIds = traceIds;
  }

  public void withCollections(Collection<String> collections) {
    this.collections = collections;
  }

  public void withRunIds(Collection<String> runIds) {
    this.runIds = runIds;
  }
}
