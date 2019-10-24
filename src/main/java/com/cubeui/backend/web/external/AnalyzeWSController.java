package com.cubeui.backend.web.external;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/api/as")
public class AnalyzeWSController {

    private CubeServerService cubeServerService;

    public AnalyzeWSController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/aggrresult/{replayid}")
    public ResponseEntity getResultAggregate(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/replayRes/{customerId}/{app}/{service}/{replayId}")
    public ResponseEntity replayResult(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/analysisRes/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResult(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/analysisResByPath/{replayId}")
    public ResponseEntity getAnalysisResultByPath(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @GetMapping("/timelineres/{customer}/{app}")
    public ResponseEntity getTimelineResults(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping("/registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path:.+}")
    public ResponseEntity registerTemplate(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping("/registerTemplateApp/{type}/{customerId}/{appId}")
    public ResponseEntity registerTemplateApp(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }

    @PostMapping(value = "/analyze/{replayid}")
    public ResponseEntity analyze(HttpServletRequest request, @RequestBody String requestBody) {
        return cubeServerService.fetchPostResponse(request, Optional.ofNullable(requestBody));
    }
}
