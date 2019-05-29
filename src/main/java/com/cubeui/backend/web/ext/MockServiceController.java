package com.cubeui.backend.web.ext;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/ms")
public class MockServiceController {

    private CubeServerService cubeServerService;

    public MockServiceController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/{customerid}/{app}/{instanceid}/{service}/{var:.+}")
    public ResponseEntity get(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping(value = "/{customerid}/{app}/{instanceid}/{service}/{var:.+}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity postForms(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/{customerid}/{app}/{instanceid}/{service}/{var:.+}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity postJson(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/fr", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity funcJson(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

}
