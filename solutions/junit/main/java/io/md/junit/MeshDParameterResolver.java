package io.md.junit;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class MeshDParameterResolver implements ParameterResolver {

  private final ObjectMapper objectMapper = new ObjectMapper();


  private String covertStringArrayToString(String[] traceIds) {
    if (traceIds.length > 0) {
      StringBuilder traceIdsBuilder = new StringBuilder();

      for (String n : traceIds) {
        //traceIdsBuilder.append("'").append(n.replace("'", "\\'")).append("',");
        traceIdsBuilder.append("\"").append(n.replace("\"", "\"\"")).append("\",");
        // can also do the following
        // nameBuilder.append("'").append(n.replace("'", "''")).append("',");
      }

      traceIdsBuilder.deleteCharAt(0);
      traceIdsBuilder.deleteCharAt(traceIdsBuilder.length() - 1);
      traceIdsBuilder.deleteCharAt(traceIdsBuilder.length() - 1);

      return traceIdsBuilder.toString();
    } else {
      return "";
    }
  }

  private JsonNode getMeshRequestData(String[] traceIds, String path) throws URISyntaxException, IOException {

    String requestBody = "{\"customerId\":\"CubeCorp\","
        + "\"app\":\"springboot_demo\","
        + "\"eventTypes\":[],"
        + "\"services\":[],"
        + "\"traceIds\":[\""
        + covertStringArrayToString(traceIds)
        +"\"],"
        + "\"reqIds\":[],"
        + "\"collection\" : \"" + System.getProperty("collection") + "\","
        + "\"paths\":[\""
        + path
        + "\"]}";


    final String url = "https://demo.dev.cubecorp.io" + "/api/cs/getEvents";
    URI uri = new URI(url);

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    //The token used here is a short lived. It has be updated with appropriate token to make this work
    //headers.set("Authorization",
    //    "Bearer eyJhbGciOjJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTYwMzI1MzczNywiZXhwIjoxNjAzMzQwMTM3fQ.4U0AZyZhpEEy6CyNILDRiBSBY3Wyt0N3WXe78fDd_6Q");

    headers.set("Authorization",
        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s");
    headers.set("content-type","application/json");

    HttpEntity entity = new HttpEntity(requestBody, headers);
    ResponseEntity<String> responseEntityStr = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);


    JsonNode root = objectMapper.readTree(responseEntityStr.getBody());
    return objectMapper.readTree(responseEntityStr.getBody()).get("objects");
  }


  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    boolean ret = false;
    if (parameterContext.getParameter().getType() == MeshDRequest[].class) {
      ret = true;
    } else if (parameterContext.getParameter().getType() == MeshDResponse[].class) {
      ret = true;
    }
      return ret;
  }

  @SneakyThrows
  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    TreeMap<String, MeshDRequest> traceIdVsRequest = new TreeMap<>();
    TreeMap<String, MeshDResponse> traceIdVsResponse = new TreeMap<>();
    Annotation[] annotations = extensionContext.getTestMethod().get().getAnnotationsByType(MeshTestCaseId.class);
    if (annotations.length == 1 ) {
      MeshTestCaseId meshDTestCaseId= extensionContext.getTestMethod().get().getAnnotation(MeshTestCaseId.class);
      String traceIds[] = meshDTestCaseId.traceIds();
      String path = meshDTestCaseId.path();
      JsonNode meshRespObjs =getMeshRequestData(traceIds, path);
      meshRespObjs.forEach(
          new Consumer<JsonNode>() {
            @Override
            public void accept(JsonNode event)
            {
              JsonNode payload = event.get("payload").get(1);
              if (event.get("eventType").asText().equalsIgnoreCase("HttpRequest")) {

//                MockMvcRequestBuilders.request(HttpMethod.resolve(payload.get("method").asText()),
//                    payload.get("path").asText()).headers();
                MeshDRequest request = new MeshDRequest(payload.get("body"),
                    payload.get("hdrs"), payload.get("queryParams"),
                    payload.get("formParams"), payload.get("method").asText(),
                    payload.get("path").asText(), event.get("traceId").asText());
                traceIdVsRequest.put(event.get("traceId").asText(), request);
              } else if (event.get("eventType").asText().equalsIgnoreCase("HttpResponse")) {
                MeshDResponse response = new MeshDResponse(payload.get("body"),
                    payload.get("hdrs"), payload.get("status").asInt(),
                    event.get("traceId").asText());
                traceIdVsResponse.put(event.get("traceId").asText(), response);
              }
            }

          }
      );
    }

    Object ret = null;
    if (parameterContext.getParameter().getType() == MeshDRequest[].class) {
      MeshDRequest[] requests = new MeshDRequest[traceIdVsRequest.values().size()];
      ret = traceIdVsRequest.values().toArray(requests);
    } else if (parameterContext.getParameter().getType() == MeshDResponse[].class) {
      MeshDResponse[] responses = new MeshDResponse[traceIdVsResponse.values().size()];
      ret = traceIdVsResponse.values().toArray(responses);
    }
    return ret;
  }
}
