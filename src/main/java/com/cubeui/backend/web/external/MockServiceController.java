package com.cubeui.backend.web.external;

import static org.springframework.http.ResponseEntity.status;

import io.md.dao.Recording;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cubeui.backend.domain.FnReqResponse;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.Event;

@RestController
@RequestMapping("/api/ms")
public class MockServiceController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;
    @Autowired
    private ObjectMapper jsonMapper;

    @GetMapping("/{customerId}/{app}/{instanceId}/{service}/**")
    public ResponseEntity getData(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                              @PathVariable String app, @PathVariable String instanceId, @PathVariable String service) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/{customerId}/{app}/{instanceId}/{service}/**")
    public ResponseEntity postData(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                              @PathVariable String app, @PathVariable String instanceId, @PathVariable String service) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, getBody);
    }

    @PostMapping("/mockEvent")
    public ResponseEntity mockEvent(HttpServletRequest request, @RequestBody Event event) {
        validation.validateCustomerName(request,event.customerId);
        final Optional<Event> bodyData = Optional.of(event);
        return cubeServerService.fetchPostResponse(request, bodyData);
    }

    @PostMapping("/thrift")
    public ResponseEntity thrift(HttpServletRequest request, @RequestBody Event event) {
        validation.validateCustomerName(request,event.customerId);
        final Optional<Event> bodyData = Optional.of(event);
        return cubeServerService.fetchPostResponse(request, bodyData);
    }

    @PostMapping("/fr")
    public ResponseEntity funcJson(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        FnReqResponse fnReqResponse;
        try {
            String body = getBody.get();
            fnReqResponse = jsonMapper.readValue(body, FnReqResponse.class);
        } catch (Exception e) {
            return status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
        validation.validateCustomerName(request,fnReqResponse.customerId);
        return cubeServerService.fetchPostResponse(request, getBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/mockWithCollection/{replayCollection}/{recordingId}/{service}/**")
    public ResponseEntity getmockWithCollection(HttpServletRequest request,
        @RequestBody Optional<String> getBody, @PathVariable String replayCollection,
        @PathVariable String recordingId, @PathVariable String service) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/mockWithCollection/{replayCollection}/{recordingId}/{service}/**")
    public ResponseEntity postMockWithCollection(HttpServletRequest request,
        @RequestBody Optional<String> getBody, @PathVariable String replayCollection,
        @PathVariable String recordingId, @PathVariable String service) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }
}
