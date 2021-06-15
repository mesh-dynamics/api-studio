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

  public static <T> ResponseEntity fetchResponse(String path, HttpMethod method, String token, Optional<T> requestBody, String... args) throws Exception{
    ResponseEntity response;
    try {
      URI uri = new URI(path);
      HttpHeaders headers = new HttpHeaders();
      headers.add("Content-Type", args.length > 0 ? args[0] : "application/json");
      headers.add("Authorization", token);
      HttpEntity<T> entity = requestBody.map(body -> new HttpEntity<>(body, headers)).orElseGet(() -> new HttpEntity<>(headers));
      return restTemplate.exchange(uri, method, entity, String.class);
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
