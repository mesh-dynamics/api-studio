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
