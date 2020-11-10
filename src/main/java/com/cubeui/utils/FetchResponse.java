package com.cubeui.utils;

import static org.springframework.http.ResponseEntity.status;

import java.net.URI;
import java.util.Optional;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class FetchResponse {
  private static RestTemplate restTemplate= new RestTemplate();

  public static <T> ResponseEntity fetchResponse(String path, HttpMethod method, String token, Optional<T> requestBody) throws Exception{
    ResponseEntity response;
    try {
      URI uri = new URI(path);
      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", "application/json");
      headers.add("Authorization", token);
      HttpEntity<T> entity = requestBody.map(body -> new HttpEntity<>(body, headers)).orElseGet(() -> new HttpEntity<>(headers));
      response = restTemplate.exchange(uri, method, entity, String.class);
      return response;
    } catch (HttpClientErrorException e){
      response = status(e.getStatusCode()).body("HttpClientErrorException");
      return response;
    } catch (Exception e){
      throw e;
    }
  }

  public static Object getDataField(ResponseEntity response, String field) throws ParseException {
    try {
      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(response.getBody().toString());
      return json.get(field).toString();
    } catch (ParseException e) {
      e.printStackTrace();
      throw e;
    }
  }

}
