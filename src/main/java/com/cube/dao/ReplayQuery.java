package com.cube.dao;

import com.cube.core.Utils;
import com.cube.utils.Constants;
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
  public final Optional<String> collection;
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
    this.collection = Optional.ofNullable(queryParams.getFirst(Constants.COLLECTION_FIELD));
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
