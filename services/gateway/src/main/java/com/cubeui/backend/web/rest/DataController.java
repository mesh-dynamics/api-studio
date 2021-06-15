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

package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.web.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@Slf4j
@RestController
@RequestMapping("/api/data")
public class DataController {

    private CubeServerService cubeServerService;

    public DataController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("")
    public ResponseEntity getData1() {
        String urlString = "https://my-json-server.typicode.com/typicode/demo/db";
        log.info("Fetching data from: {}", urlString);
        return cubeServerService.fetchGetResponse(urlString, null);
    }

    @PostMapping("")
    public ResponseEntity getData() {
        try {
            URL url = new URL("https://my-json-server.typicode.com/typicode/demo/db");
            log.info("Fetching data from: {}", url.getPath());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(HttpMethod.GET.toString());
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Content-Type", "application/json");
            int responseCode = con.getResponseCode();
            log.info("GET Response Code :: " + responseCode);
            if (responseCode == HTTP_OK) { // success
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return ok().body(response.toString());
            } else {
                return status(responseCode)
                        .body(new ErrorResponse("Error Fetching Data", con.getResponseMessage(), responseCode));
            }
        } catch (Exception ex){
            return status(INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Connection Failed", ex.getMessage(), INTERNAL_SERVER_ERROR.value()));
        }
    }
}
