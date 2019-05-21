package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import static com.cubeui.backend.security.Constants.CUBE_SERVER_HREF;
import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/as")
public class AnalyzeWSController {

    private CubeServerService cubeServerService;
    private String baseHref =  CUBE_SERVER_HREF + "/as";

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

    @GetMapping("/timelineres/{customer}/{app}/{instanceId}")
    public ResponseEntity getTimelineResults(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(request);
    }

    @PostMapping("registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path:.+}")
//    @Consumes({MediaType.APPLICATION_JSON})
    public ResponseEntity registerTemplate(@PathVariable("appId") String appId,
                                           @PathVariable("customerId") String customerId,
                                           @PathVariable("serviceName") String serviceName,
                                           @PathVariable("path") String path,
                                           @PathVariable("type") String type,
                                           String templateAsJson) {
        return noContent().build();
    }

    @PostMapping("registerTemplateApp/{type}/{customerId}/{appId}")
//    @Consumes({MediaType.APPLICATION_JSON})
    public ResponseEntity registerTemplateApp(@PathVariable("type") String type,
                                              @PathVariable("customerId") String customerId ,
                                              @PathVariable("appId") String appId,
                                              String templateRegistryArray) {
        return noContent().build();
    }

    @PostMapping(value = "/analyze/{replayid}")
    public ResponseEntity analyze(@PathVariable("replayid") String replayid, HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(baseHref + "/analyze/" + replayid);
    }
}
