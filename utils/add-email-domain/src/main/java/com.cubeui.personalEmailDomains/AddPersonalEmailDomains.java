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

package com.cubeui.personalEmailDomains;

import static org.springframework.http.ResponseEntity.status;

import com.cubeui.personalEmailDomains.domains.JsonData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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

public class AddPersonalEmailDomains {
  private RestTemplate restTemplate = new RestTemplate();

  private <T> ResponseEntity fetchResponse(String path, HttpMethod method, String token, Optional<T> requestBody) throws Exception{
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

  public static void main(String[] args)  throws  Exception {
    String url;
    String path;

    if (args.length != 2 ) {
      System.out.println("Enter the url: e.g https://demo.dev.cubecorp.io");
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

      url = reader.readLine();

      System.out.println("Enter the json file path");
      reader = new BufferedReader(new InputStreamReader(System.in));
      path = reader.readLine();
    } else {
      url = args[0];
      path = args[1];
    }
    AddPersonalEmailDomains addPersonalEmailDomains = new AddPersonalEmailDomains();
    try {
      JSONObject json = new JSONObject();
      json.put("username", "admin@meshdynamics.io");
      json.put("password", "admin");
      ResponseEntity login = addPersonalEmailDomains.fetchResponse(url + "/api/login", HttpMethod.POST, "",
          Optional.of(json));

      String access_token = addPersonalEmailDomains.getDataField(login, "access_token").toString();
      String token = "Bearer " + access_token;
      ObjectMapper mapper = new ObjectMapper();
      JsonData data = mapper.readValue(new File(path), JsonData.class);
      addPersonalEmailDomains.fetchResponse(url+"/api/addDomains/save", HttpMethod.POST, token, Optional.of(data.getDomains()));
    } catch (Exception e){
      e.printStackTrace();
    }
  }
  private Object getDataField(ResponseEntity response, String field) throws ParseException {
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
