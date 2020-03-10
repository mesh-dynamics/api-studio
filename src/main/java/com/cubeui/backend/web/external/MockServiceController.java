package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.FnReqResponse;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;
import io.md.dao.Event;

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

    @GetMapping("/{customerId}/{app}/{instanceId}/{service}/{var:.+}")
    public ResponseEntity getData(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                              @PathVariable String app, @PathVariable String instanceId, @PathVariable String service,
                              @PathVariable String var) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/{customerId}/{app}/{instanceId}/{service}/{var:.+}")
    public ResponseEntity postData(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                              @PathVariable String app, @PathVariable String instanceId, @PathVariable String service,
                              @PathVariable String var) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, getBody);
    }

    @PostMapping("/mockFunction")
    public ResponseEntity mockFunction(HttpServletRequest request, @RequestBody Event event) {
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
}
