package com.cubeui.backend.web.ext;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/rs")
public class ReplayWSController {

    private CubeServerService cubeServerService;

    public ReplayWSController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/status/{customerid}/{app}/{collection}/{replayid}")
    public ResponseEntity status(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping(value = "/init/{customerid}/{app}/{collection}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity init(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/transforms/{customerid}/{app}/{collection}/{replayid}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity upsertTransforms(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping("/forcecomplete/{replayid}")
    public ResponseEntity forceComplete(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/start/{customerid}/{app}/{collection}/{replayid}", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity start(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }
}
