package com.cubeui.backend.web.external;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import io.md.dao.RecordingOperationSetSP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.md.dao.Replay;

import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/as")
public class AnalyzeWSController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @GetMapping("/status/{replayId}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/analyze/{replayId}")
    public ResponseEntity analyze(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/aggrresult/{replayId}")
    public ResponseEntity getResultAggregate(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/replayRes/{customerId}/{app}/{service}/{replayId}")
    public ResponseEntity replayResult(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                       @PathVariable String app, @PathVariable String service, @PathVariable String replayId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/registerTemplateApp/{customerId}/{appId}/{version}")
    public ResponseEntity registerTemplateApp(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                              @PathVariable String appId, @PathVariable String version) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path}")
    public ResponseEntity registerTemplate(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String type,
                                           @PathVariable String customerId, @PathVariable String appId, @PathVariable String serviceName,
                                           @PathVariable String path) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/getRespTemplate/{customerId}/{appId}/{templateVersion}/{service}/{type}")
    public ResponseEntity getRespTemplate(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                          @PathVariable String appId, @PathVariable String templateVersion, @PathVariable String service,
                                          @PathVariable String type){
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisRes/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResult(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
                                          @PathVariable String recordReqId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResNoTrace/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResultWithoutTrace(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
                                            @PathVariable String recordReqId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/timelineres/{customer}/{app}")
    public ResponseEntity getTimelineResults(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customer,
                                                        @PathVariable String app){
        validation.validateCustomerName(request,customer);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResByPath/{replayId}")
    public ResponseEntity getAnalysisResultsByPath(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResByReq/{replayId}")
    public ResponseEntity getResultByReq(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/saveTemplateSet/{customer}/{app}")
    public ResponseEntity saveTemplateSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customer,
                                           @PathVariable String app) {
        validation.validateCustomerName(request,customer);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/cache/flushall")
    public ResponseEntity cacheFlushAll(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/initTemplateOperationSet/{customer}/{app}/{version}")
    public ResponseEntity initTemplateOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customer,
                                                   @PathVariable String app,  @PathVariable String version) {
        validation.validateCustomerName(request,customer);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/updateTemplateOperationSet/{operationSetId}")
    public ResponseEntity updateTemplateOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String operationSetId) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/updateTemplateSet/{templateSetId}/{operationSetId}")
    public ResponseEntity updateTemplateSet(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String templateSetId,
                                            @PathVariable String operationSetId){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/updateGoldenSet/{recordingId}/{replayId}/{collectionUpdOpSetId}/{templateUpdOpSetId}")
    public ResponseEntity updateGoldenSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId,
                                          @PathVariable String replayId, @PathVariable String collectionUpdOpSetId, @PathVariable String templateUpdOpSetId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/sanitizeGoldenSet")
    public ResponseEntity sanitizeRecording(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String recordingId,
                                            @RequestParam String replayId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/create")
    public ResponseEntity createRecordingOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String customer,
                                                      @RequestParam String app) {
        validation.validateCustomerName(request,customer);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/goldenUpdate/recordingOperationSet/get")
    public ResponseEntity getRecordingOperationSet(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String operationSetId,
                                                   @RequestParam String service, @RequestParam String path){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/update")
    public ResponseEntity updateRecordingOperationSet(HttpServletRequest request, @RequestBody RecordingOperationSetSP postBody) {
        validation.validateCustomerName(request,postBody.customer);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/updateMultiPath")
    public ResponseEntity updateRecordingOperationSet_1(HttpServletRequest request, @RequestBody List<RecordingOperationSetSP> postBody) {
        for (RecordingOperationSetSP recordingOperationset : postBody) {
            validation.validateCustomerName(request,recordingOperationset.customer);
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/apply")
    public ResponseEntity applyRecordingOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String operationSetId,
                                                     @RequestParam String replayId, @RequestParam String collectionName) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/goldenInsights/{recordingId}")
    public ResponseEntity goldenInsights(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String recordingId,
                                         @RequestParam String service, @RequestParam String apiPath) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }
}
