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

package com.cubeui.backend.web.external;

import static com.cubeui.backend.Utils.addTrailers;
import static org.springframework.http.ResponseEntity.status;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.cubeui.backend.domain.FnReqResponse;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.Event;
import io.md.dao.Recording;

@RestController
@RequestMapping("/api/ms")
public class MockServiceController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;
    @Autowired
    private ObjectMapper jsonMapper;

    @RequestMapping(value = "/{customerId}/{app}/{instanceId}/{service}/**" , consumes = {MediaType.ALL_VALUE})
    public ResponseEntity data(HttpServletRequest request, @RequestBody Optional<String> body, @PathVariable String customerId,
                              @PathVariable String app, @PathVariable String instanceId, @PathVariable String service,
                              Authentication authentication, HttpServletResponse response) {
        validation.validateCustomerName(authentication,customerId);
        String path = cubeServerService.getPathForHttpMethod(request.getRequestURI() , request.getMethod() , app, instanceId , service);
        ResponseEntity responseEntity = cubeServerService.fetchResponse(request, body ,HttpMethod.POST , path );

        /**
         * Extracting trailer data from headers and adding it to httpServletResponse.
         * Workaround as Spring drops trailer information.
         */
        addTrailers(responseEntity, response);

        return responseEntity;
    }

    @PostMapping("/mockEvent")
    public ResponseEntity mockEvent(HttpServletRequest request, @RequestBody Event event, Authentication authentication, HttpServletResponse response) {
        validation.validateCustomerName(authentication, event.customerId);
        final Optional<Event> bodyData = Optional.of(event);
        ResponseEntity responseEntity = cubeServerService.fetchPostResponse(request, bodyData);

        /**
         * Extracting trailer data from headers and adding it to httpServletResponse.
         * Workaround as Spring drops trailer information.
         */
        addTrailers(responseEntity, response);

        return responseEntity;
    }

    @PostMapping("/thrift")
    public ResponseEntity thrift(HttpServletRequest request, @RequestBody Event event, Authentication authentication) {
        validation.validateCustomerName(authentication,event.customerId);
        final Optional<Event> bodyData = Optional.of(event);
        return cubeServerService.fetchPostResponse(request, bodyData);
    }

    @PostMapping("/fr")
    public ResponseEntity funcJson(HttpServletRequest request, @RequestBody Optional<String> getBody, Authentication authentication) {
        FnReqResponse fnReqResponse;
        try {
            String body = getBody.get();
            fnReqResponse = jsonMapper.readValue(body, FnReqResponse.class);
        } catch (Exception e) {
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
        validation.validateCustomerName(authentication,fnReqResponse.customerId);
        return cubeServerService.fetchPostResponse(request, getBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @RequestMapping(value = "/mockWithCollection/{replayCollection}/{recordingId}/{traceId}/{service}/**" , consumes = {MediaType.ALL_VALUE})
    public ResponseEntity mockWithCollection(HttpServletRequest request,
        @RequestBody Optional<String> body, @PathVariable String replayCollection,
        @PathVariable String recordingId, @PathVariable String traceId, @PathVariable String service, Authentication authentication, HttpServletResponse response) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);

        String path = cubeServerService.getPathForHttpMethod(request.getRequestURI() , request.getMethod() , recordingId , traceId , service);
        ResponseEntity responseEntity = cubeServerService.fetchResponse(request, body , HttpMethod.POST , path);

        /**
         * Extracting trailer data from headers and adding it to httpServletResponse.
         * Workaround as Spring drops trailer information.
         */
        addTrailers(responseEntity, response);

        return responseEntity;
    }
}
