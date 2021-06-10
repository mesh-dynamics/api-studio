package com.cubeui.personalEmailDomains;

import com.cubeui.personalEmailDomains.domains.JsonData;
import com.cubeui.utils.FetchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Optional;
import org.json.simple.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public class AddPersonalEmailDomains {

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
    try {
      JSONObject json = new JSONObject();
      json.put("username", "admin@meshdynamics.io");
      json.put("password", "admin");
      ResponseEntity login = FetchResponse.fetchResponse(url + "/api/login", HttpMethod.POST, "",
          Optional.of(json));

      String access_token = FetchResponse.getDataField(login, "access_token").toString();
      String token = "Bearer " + access_token;
      ObjectMapper mapper = new ObjectMapper();
      JsonData data = mapper.readValue(new File(path), JsonData.class);
      FetchResponse.fetchResponse(url+"/api/addDomains/save", HttpMethod.POST, token, Optional.of(data.getDomains()));
    } catch (Exception e){
      e.printStackTrace();
    }
  }

}
