package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private RestTemplate restTemplate;
    private CubeServerService cubeServerService;

    public DataController(RestTemplate restTemplate, CubeServerService cubeServerService) {
        this.restTemplate = restTemplate;
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("")
    public ResponseEntity getData1() {
        String urlString = "https://my-json-server.typicode.com/typicode/demo/db";
        return cubeServerService.fetchGetResponse(urlString);
    }

    @PostMapping("")
    public ResponseEntity getData() {
        try {
            URL url = new URL("https://my-json-server.typicode.com/typicode/demo/db");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Content-Type", "application/json");
            int responseCode = con.getResponseCode();
            System.out.println("GET Response Code :: " + responseCode);
            if (responseCode == HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return ok().body(response.toString());
            } else {
                return noContent().build();
            }
        } catch (Exception e){
            return noContent().build();
        }
    }
}
