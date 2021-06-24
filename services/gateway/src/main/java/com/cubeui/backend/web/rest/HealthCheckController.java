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

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cubeui.backend.service.CubeServerService;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private CubeServerService cubeServerService;

    private static final Supplier<Map<String,String>> trailersSup = ()->Map.of("Mesh" , "Dynamics" , "City" , "Bangalore");
    private static final String[] trailerHeaderFields = trailersSup.get().keySet().toArray(new String[0]);
    public HealthCheckController() {

    }

    @RequestMapping("")
    public ResponseEntity all(HttpServletRequest request , @RequestBody Optional<String> body , HttpServletResponse response) {
        //Adding protocol , body and trailers for debugging purpose. To see what protocol (Http1.1/Http2) incoming connection is received
        response.setTrailerFields(trailersSup);
        String resp = "All Is Well! "+request.getProtocol() + " body:"+body.orElse(null);// + " urls:"+cubeServerService.getUrls();
        return ResponseEntity.status(200).header("Trailer" , trailerHeaderFields).body(resp);
    }
}
