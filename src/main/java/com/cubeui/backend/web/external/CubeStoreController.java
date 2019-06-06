package com.cubeui.backend.web.external;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/cs")
public class CubeStoreController {


    private CubeServerService cubeServerService;

    public CubeStoreController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/status/{customerid}/{app}/{collection}")
    public ResponseEntity status(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/recordings")
    public ResponseEntity recordings(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/currentcollection")
    public ResponseEntity currentcollection(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping(value = "/req")
    public ResponseEntity storereq(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping("/resp")
    public ResponseEntity storeresp(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/fr", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity storeFunc(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/setdefault/{customerid}/{app}/{serviceid}/{method}/{var:.+}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity setDefault(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping("/setdefault/{method}/{var:.+}")
    public ResponseEntity setDefaultFullResp(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/start/{customerid}/{app}/{instanceid}/{collection}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity start(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping("/stop/{customerid}/{app}/{collection}")
    public ResponseEntity stop(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

}
