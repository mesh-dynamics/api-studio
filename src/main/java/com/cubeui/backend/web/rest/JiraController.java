package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.JiraCreateIssueDTO;
import com.cubeui.backend.domain.JiraIssueDetails;
import com.cubeui.backend.domain.JiraUserCredentials;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.JiraIssueDetailsRepository;
import com.cubeui.backend.repository.JiraUserCredentialsRepository;
import com.cubeui.backend.service.JiraAPIService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.QueryParam;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jira")
public class JiraController {


  private JiraAPIService jiraAPIService;

  private JiraUserCredentialsRepository jiraUserCredentialsRepository;

  private JiraIssueDetailsRepository jiraIssueDetailsRepository;

  public JiraController(JiraAPIService jiraAPIService,
      JiraUserCredentialsRepository jiraUserCredentialsRepository,
        JiraIssueDetailsRepository jiraIssueDetailsRepository){
    this.jiraAPIService = jiraAPIService;
    this.jiraUserCredentialsRepository = jiraUserCredentialsRepository;
    this.jiraIssueDetailsRepository = jiraIssueDetailsRepository;
  }

  @PostMapping("issue/create")
  public ResponseEntity createIssue(@RequestBody JiraCreateIssueDTO createIssueRequest, Authentication authentication){
    User user = (User) authentication.getPrincipal();
    // fetch jira user credentials
    Optional<JiraUserCredentials> jiraUserCredentialsList = jiraUserCredentialsRepository
        .findByUserId(user.getId());
    return jiraUserCredentialsList.map(jiraUserCredentials -> {
      // make call to Jira API to create a new issue
      ResponseEntity<Map> response = jiraAPIService.createIssue(createIssueRequest, jiraUserCredentials);
      if(!response.getStatusCode().is2xxSuccessful()) {
        return response;
      }

      Map respBody = response.getBody();
      String issueKey = respBody.get("key").toString();
      // form issue url
      String issueUrl = String.format("%s/browse/%s", jiraUserCredentials.getJiraBaseURL(), issueKey);
      respBody.put("url", issueUrl);
      // store issue details in db
      storeIssueDetails(createIssueRequest, jiraUserCredentials, respBody);
      return ResponseEntity.ok(respBody);
    }).orElse(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
        "error", "Jira user credentials not found"
    )));

  }

  private void storeIssueDetails(JiraCreateIssueDTO createIssueRequest,
      JiraUserCredentials jiraUserCredentials, Map respBody) {
    JiraIssueDetails jiraIssueDetails = new JiraIssueDetails();

    String issueId = String.valueOf(respBody.get("id"));
    String issueKey = String.valueOf(respBody.get("key"));
    String issueUrl = String.valueOf(respBody.get("url"));
    jiraIssueDetails.setIssueId(issueId);
    jiraIssueDetails.setIssueKey(issueKey);
    jiraIssueDetails.setIssueUrl(issueUrl);

    jiraIssueDetails.setUser(jiraUserCredentials.getUser());
    jiraIssueDetails.setReplayId(createIssueRequest.getReplayId());
    jiraIssueDetails.setApiPath(createIssueRequest.getApiPath());
    jiraIssueDetails.setRequestId(createIssueRequest.getRequestId());
    jiraIssueDetails.setJsonPath(createIssueRequest.getJsonPath());
    jiraIssueDetailsRepository.save(jiraIssueDetails);
  }

  @GetMapping("issue/getdetails")
  public ResponseEntity getIssueDetails(@QueryParam("replayId") String replayId, @QueryParam("apiPath") String apiPath,
      @QueryParam("requestId") String requestId, @QueryParam("jsonPath") String jsonPath, @AuthenticationPrincipal  User user){
    Optional<List<JiraIssueDetails>> jiraIssueDetailsList = jiraIssueDetailsRepository
        .findIssueDetails(user, replayId, apiPath, requestId, jsonPath);
    return ResponseEntity.of(jiraIssueDetailsList);
  }

  @GetMapping("projects")
  public ResponseEntity getProjectsList(Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    Optional<JiraUserCredentials> jiraUserCredentialsList = jiraUserCredentialsRepository
        .findByUserId(user.getId());
    return jiraUserCredentialsList.map(jiraUserCredentials -> {
      return jiraAPIService.getProjectsList(jiraUserCredentials);
    }).orElse(ResponseEntity.badRequest().build()); // TODO
  }
}

