package com.cubeui.backend.repository;

import com.cubeui.backend.domain.JiraIssueDetails;
import com.cubeui.backend.domain.JiraUserCredentials;
import com.cubeui.backend.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "jiraissuedetails", collectionResourceRel = "jiraissuedetails", itemResourceRel = "jiraissuedetail")
public interface JiraIssueDetailsRepository extends JpaRepository<JiraIssueDetails, Long> {

  @Query("SELECT j FROM JiraIssueDetails j WHERE (j.user = :user) AND (:replayId is null or j.replayId = :replayId) AND (:apiPath is null or j.apiPath = :apiPath) AND (:requestId is null or j.requestId = :requestId) AND (:jsonPath is null or j.jsonPath = :jsonPath) ")
  Optional<List<JiraIssueDetails>> findIssueDetails(User user, String replayId, String apiPath, String requestId, String jsonPath);

}
