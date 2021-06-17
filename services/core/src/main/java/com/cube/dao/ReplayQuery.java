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

import io.md.utils.Constants;
import io.md.core.Utils;
import io.md.constants.ReplayStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.MultivaluedMap;

public class ReplayQuery {
  public final Optional<String> customerId;
  public final Optional<String> app;
  public final List<String> instanceId;
  public final List<ReplayStatus> status;
  public final List<String> collection;
  public final Optional<String> userId;
  public final Optional<String> testConfigName;
  public final Optional<String> goldenName;
  public final Optional<Instant> startDate;
  public final Optional<Instant> endDate;
  public final Optional<Integer> numResults;
  public final Optional<Integer> start;

  public ReplayQuery(String customerId, MultivaluedMap<String, String> queryParams) {
    this.customerId = Optional.of(customerId);
    this.app = Optional.ofNullable(queryParams.getFirst(Constants.APP_FIELD));
    this.instanceId = Optional.ofNullable(queryParams.get(Constants.INSTANCE_ID_FIELD))
          .orElse(Collections.emptyList());
    this.collection = Optional.ofNullable(queryParams.get(Constants.COLLECTION_FIELD)).orElse(Collections.emptyList());
    this.userId = Optional.ofNullable(queryParams.getFirst(Constants.USER_ID_FIELD));
    this.testConfigName = Optional.ofNullable(queryParams.getFirst(Constants.TEST_CONFIG_NAME_FIELD));
    this.goldenName = Optional.ofNullable(queryParams.getFirst(Constants.GOLDEN_NAME_FIELD));
    Optional<String> endDate = Optional.ofNullable(queryParams.getFirst(Constants.END_DATE_FIELD));
    Optional<String> startDate = Optional.ofNullable(queryParams.getFirst(Constants.START_DATE_FIELD));
    this.endDate = endDate.flatMap(Utils::strToTimeStamp);
    this.startDate =  startDate.flatMap(Utils::strToTimeStamp);
    this.numResults = Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD)).map(Integer::valueOf)
          .or(() -> Optional.of(20));
    this.start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD)).flatMap(Utils::strToInt);
    Optional<String> status = Optional.ofNullable(queryParams.getFirst(Constants.STATUS));
    this.status = status.isPresent() ? List.of(ReplayStatus.valueOf(status.get()))
          : Collections.emptyList();
  }
}
