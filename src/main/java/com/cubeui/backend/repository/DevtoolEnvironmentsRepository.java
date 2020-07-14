package com.cubeui.backend.repository;

import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DevtoolEnvironmentsRepository extends JpaRepository<DtEnvironment, Long> {

  //@Query("SELECT j FROM JiraIssueDetails j WHERE (j.user = :user) AND (:replayId is null or j.replayId = :replayId) AND (:apiPath is null or j.apiPath = :apiPath) AND (:requestId is null or j.requestId = :requestId) AND (:jsonPath is null or j.jsonPath = :jsonPath) ")
  Optional<List<DtEnvironment>> findDtEnvironmentsByUser(User user);
}
