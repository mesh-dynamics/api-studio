package com.cubeui.backend.web.rest;

import com.cubeui.backend.service.CubeServerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import static org.springframework.http.ResponseEntity.noContent;

@RestController
@RequestMapping("/as")
public class AnalyzeWSController {

    private CubeServerService cubeServerService;
    private String baseHref =  "/as";

    public AnalyzeWSController(CubeServerService cubeServerService) {
        this.cubeServerService = cubeServerService;
    }

    @GetMapping("/health")
    public ResponseEntity getData1(HttpServletRequest request) {
        return cubeServerService.fetchGetResponse(baseHref + "/health");
    }

    @GetMapping("/aggrresult/{replayid}")
    public ResponseEntity getResultAggregate(@PathVariable("replayid") String replayid) {
        return cubeServerService.fetchGetResponse(baseHref + "/aggrresult/" + replayid);
    }

    @GetMapping("/replayRes/{customerId}/{app}/{service}/{replayId}")
    public ResponseEntity replayResult(@PathVariable("customerId") String customerId,
                                       @PathVariable("app") String app,
                                       @PathVariable("service") String service,
                                       @PathVariable("replayId") String replayId) {
        return cubeServerService.fetchGetResponse(baseHref + "/replayRes/" + customerId +"/" + app + "/" + service + "/" + replayId);
    }

    @GetMapping("/analysisRes/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResult(@PathVariable("recordReqId") String recordReqId,
                                            @PathVariable("replayId") String replayId) {
        return cubeServerService.fetchGetResponse(baseHref + "/analysisRes/" + replayId + "/" + recordReqId);
    }

    @GetMapping("/timelineres/{customer}/{app}/{instanceId}")
    public ResponseEntity getTimelineResults(@PathVariable("customer") String customer,
                                             @PathVariable("app") String app,
                                             @PathVariable("instanceId") String instanceId) {
        return cubeServerService.fetchGetResponse(baseHref + "/timelineres/" + customer + "/" + app + "/" + instanceId);
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

    @PostMapping("/analyze/{replayid}")
//    @Consumes("application/x-www-form-urlencoded")
    public ResponseEntity analyze(@PathVariable("replayid") String replayid, MultivaluedMap<String, String> formParams) {
        return cubeServerService.fetchGetResponse(baseHref + "/analyze/" + replayid);
    }
}
