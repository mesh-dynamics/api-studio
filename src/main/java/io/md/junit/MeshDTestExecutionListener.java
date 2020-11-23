package io.md.junit;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

public class MeshDTestExecutionListener extends AbstractTestExecutionListener {

  String replayId;

  @Override
  public void beforeTestClass(TestContext testContext) throws URISyntaxException {
    Annotation[] annotations = testContext.getTestClass().getAnnotationsByType(AutoConfigureMeshDContext.class);
    if (annotations.length == 1) {
      AutoConfigureMeshDContext meshDContext = testContext.getTestClass().getAnnotation(AutoConfigureMeshDContext.class);

      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      //The token used here is a short lived. It has be updated with appropriate token to make this work
      //headers.set("Authorization",
      //    "Bearer eyJhbGciOjJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTYwMzI1MzczNywiZXhwIjoxNjAzMzQwMTM3fQ.4U0AZyZhpEEy6CyNILDRiBSBY3Wyt0N3WXe78fDd_6Q");

      headers.set("Authorization",
          "Bearer "+meshDContext.authToken());
      headers.set("content-type","application/x-www-form-urlencoded");


      //start a dummy replay with samplerate 0 and get the replay id.
      String url = meshDContext.uriScheme().getScheme() + "://" + meshDContext.meshDHost() + ":" + meshDContext.meshDPort() +
          "/api/rs/start/byGoldenName/" + meshDContext.customer() +"/" + meshDContext.app() + "/" + meshDContext.goldenNames()[0];
      URI uri = new URI(url);

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("sampleRate","0");
      map.add("endPoint","http://test1234.com");
      map.add("instanceId","prod");
      map.add("templateSetVer","DEFAULT");
      map.add("userId","demo@cubecorp.io");


      HttpEntity entity = new HttpEntity(map, headers);
      ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
      JsonNode body = response.getBody();
      replayId = body.get("replayId").asText();
      System.setProperty("collection", body.get("collection").asText());

      //force start a dummy replay to set the recording context to server mock responses
      url = meshDContext.uriScheme().getScheme() + "://" + meshDContext.meshDHost() + ":" + meshDContext.meshDPort() + "/api/rs/forcestart/" + replayId;
      uri = new URI(url);
      headers.set("content-type","text/plain");

      entity = new HttpEntity(headers);
      response = restTemplate.exchange(url, HttpMethod.POST, entity, JsonNode.class);
      body = response.getBody();
      System.out.println("Replay status " + body.get("status").toString());

    }
  }

  @Override
  public void afterTestClass(TestContext testContext) throws URISyntaxException {
    Annotation[] annotations = testContext.getTestClass().getAnnotationsByType(AutoConfigureMeshDContext.class);
    if (annotations.length == 1) {
      AutoConfigureMeshDContext meshDContext = testContext.getTestClass().getAnnotation(AutoConfigureMeshDContext.class);
      RestTemplate restTemplate = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      //The token used here is a short lived. It has be updated with appropriate token to make this work
      //headers.set("Authorization",
      //    "Bearer eyJhbGciOjJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTYwMzI1MzczNywiZXhwIjoxNjAzMzQwMTM3fQ.4U0AZyZhpEEy6CyNILDRiBSBY3Wyt0N3WXe78fDd_6Q");

      headers.set("Authorization",
          "Bearer " + meshDContext.authToken());
      headers.set("content-type","text/plain");


      //start a dummy replay with samplerate 0 and get the replay id.
      String url = meshDContext.uriScheme().getScheme() + "://" + meshDContext.meshDHost() + ":" + meshDContext.meshDPort() + "/api/rs/forcecomplete/" + replayId ;
      URI uri = new URI(url);

      HttpEntity entity = new HttpEntity(headers);
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//      JsonNode body = response.getBody();
//      String replayId = body.get("status").toString();
      System.out.println("replay status " + response.getBody());
    }
  }
}
