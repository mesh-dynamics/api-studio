package com.cubeui.backend.service;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.DTO.JiraCreateIssueDTO;
import com.cubeui.backend.domain.JiraUserCredentials;
import com.cubeui.backend.web.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
public class JiraAPIService {

    //@Value("${jira.baseUrl}")
    //private String jiraBaseUrl = CUBE_SERVER_HREF;
    private String jiraBaseUrl = "https://cubeio.atlassian.net/rest/api/3";
    private RestTemplate restTemplate;

    public JiraAPIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity fetchGetResponse(String path){
        try {
            URI uri = new URI(path);
            String result = restTemplate.getForObject(uri, String.class);
            return ok().body(result);
        } catch (URISyntaxException e){
            return noContent().build();
        } catch (HttpClientErrorException e){
            return status(e.getStatusCode()).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (Exception e){
            return status(NOT_FOUND).body(e);
        }
    }

    public ResponseEntity fetchGetResponse(HttpServletRequest request) {
        return fetchResponse(request, Optional.empty(), HttpMethod.GET);
    }

    public ResponseEntity fetchPostResponse(HttpServletRequest request, Optional<String> requestBody) {
        return fetchResponse(request, requestBody, HttpMethod.POST);
    }

    private ResponseEntity fetchResponse(HttpServletRequest request, Optional<String> requestBody, HttpMethod method){
        String path = jiraBaseUrl + "/issue";
        if (request.getQueryString() != null) {
            path += "?" + request.getQueryString();
        }
        try {
            URI uri = new URI(path);
            HttpHeaders headers = new HttpHeaders();
            request.getHeaderNames().asIterator().forEachRemaining(key -> headers.set(key, request.getHeader(key)));
//            MultiValueMap<String, String[]> map = new LinkedMultiValueMap<>();
//            request.getParameterMap().forEach(map::add);
            HttpEntity<String> entity;
            entity = requestBody.map(body -> new HttpEntity<>(body, headers)).orElseGet(() -> new HttpEntity<>(headers));
//            return restTemplate.postForEntity(uri, entity, String.class);
            return restTemplate.exchange(uri, method, entity, String.class);
        } catch (URISyntaxException e){
            return noContent().build();
        } catch (HttpClientErrorException e){
            return status(e.getStatusCode()).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (Exception e){
            return status(NOT_FOUND).body(e);
        }
    }

    public ResponseEntity<Map> createIssue(JiraCreateIssueDTO createIssueRequest, JiraUserCredentials userCredentials) {
        // get username + api key
        // build payload
        // build request
        // make api call
        // get req id and store in db

        String payload = null;
        try {
            payload = buildPayload(createIssueRequest);
        } catch (JsonProcessingException e) {
            // TODO
            e.printStackTrace();
        }

        // generate auth header
        String auth = userCredentials.getUserName() + ":" + userCredentials.getAPIKey();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);

        // make rest api call with payload
        String jiraUrl = userCredentials.getJiraBaseURL() + "/rest/api/3/issue";

        // add headers
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", authHeader);
        headers.add("Content-Type", "application/json");

        HttpEntity<String> entity = null;
        entity = new HttpEntity<String>(payload, headers);

        ResponseEntity<Map> result = restTemplate
            .exchange(jiraUrl, HttpMethod.POST, entity, Map.class);

        return result;
    }

    //api to get issue details
    //api to get project list

    private String buildPayload(JiraCreateIssueDTO createIssueRequest)
        throws JsonProcessingException {
        // build payload
        JsonNodeFactory jnf = JsonNodeFactory.instance;
        ObjectNode payloadObject = jnf.objectNode();

        ObjectNode fields = payloadObject.putObject("fields");
        fields.put("summary", createIssueRequest.getSummary());

        ObjectNode issueType = fields.putObject("issuetype");
        issueType.put("id", createIssueRequest.getIssueTypeId());

        ObjectNode project = fields.putObject("project");
        project.put("id", createIssueRequest.getProjectId());

        //fields.put("customfield_10011", createIssueRequest.getEpicName()); // Epic Name

        ObjectNode description = fields.putObject("description");
        description.put("type", "doc");
        description.put("version", 1);
        ArrayNode contentArr = description.putArray("content");
        ObjectNode content = contentArr.addObject();
        content.put("type", "paragraph");
        ArrayNode paraContentArr = content.putArray("content");
        ObjectNode paraContent = paraContentArr.addObject();
        paraContent.put("text", createIssueRequest.getDescription());
        paraContent.put("type", "text");
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(payloadObject);
    }

  public ResponseEntity getProjectsList(JiraUserCredentials userCredentials) {
    // generate auth header
    String auth = userCredentials.getUserName() + ":" + userCredentials.getAPIKey();
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
    String authHeader = "Basic " + new String(encodedAuth);

    // make rest api call with payload
    String jiraUrl = userCredentials.getJiraBaseURL() + "/rest/api/3/project/search";

    // add headers
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", authHeader);
    headers.add("Content-Type", "application/json");

    HttpEntity<String> entity = null;
    entity = new HttpEntity<String>(headers);

    return restTemplate
        .exchange(jiraUrl, HttpMethod.GET, entity, Map.class);
  }
}
