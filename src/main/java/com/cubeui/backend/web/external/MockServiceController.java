package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.FnReqResponse;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.dao.Event;
import io.md.dao.Recording;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.status;

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
                              @PathVariable String app, @PathVariable String instanceId, @PathVariable String service) {
        validation.validateCustomerName(request,customerId);

        String path = cubeServerService.getPathForHttpMethod(request.getRequestURI() , request.getMethod() , app, instanceId , service);
        return cubeServerService.fetchResponse(request, body ,HttpMethod.POST , path );
    }

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

    @RequestMapping(value = "/mockWithCollection/{replayCollection}/{recordingId}/{traceId}/{service}/**" , consumes = {MediaType.ALL_VALUE})
    public ResponseEntity mockWithCollection(HttpServletRequest request,
        @RequestBody Optional<String> body, @PathVariable String replayCollection,
        @PathVariable String recordingId, @PathVariable String traceId, @PathVariable String service) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);

        String path = cubeServerService.getPathForHttpMethod(request.getRequestURI() , request.getMethod() , recordingId , traceId , service);
        return cubeServerService.fetchResponse(request, body , HttpMethod.POST , path);
    }
}
