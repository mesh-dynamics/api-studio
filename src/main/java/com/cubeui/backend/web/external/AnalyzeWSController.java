package com.cubeui.backend.web.external;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.GoldenSetRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.dao.Recording;
import io.md.dao.RecordingOperationSetSP;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.md.dao.Replay;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/as")
@Slf4j
public class AnalyzeWSController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @GetMapping("/status/{replayId}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/analyze/{replayId}")
    public ResponseEntity analyze(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/aggrresult/{replayId}")
    public ResponseEntity getResultAggregate(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/replayRes/{customerId}/{app}/{service}/{replayId}")
    public ResponseEntity replayResult(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                       @PathVariable String app, @PathVariable String service, @PathVariable String replayId){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
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
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResNoTrace/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResultWithoutTrace(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
                                            @PathVariable String recordReqId){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
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
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResByReq/{replayId}")
    public ResponseEntity getResultByReq(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
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
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/sanitizeGoldenSet")
    public ResponseEntity sanitizeRecording(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String recordingId,
                                            @RequestParam String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
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
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/goldenInsights/{recordingId}")
    public ResponseEntity goldenInsights(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String recordingId,
                                         @RequestParam String service, @RequestParam String apiPath) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/goldenUpdateUnified")
    public  ResponseEntity goldenUpdateUnified(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        try {
            JSONParser parser = new JSONParser();
            ObjectMapper mapper = new ObjectMapper();
            if (postBody.isPresent()) {
                JSONObject json = (JSONObject) parser.parse(postBody.get());
                //Get Data for Api= /updateTemplateOperationSet from JSON post body
                JSONObject jsonObject = (JSONObject)json.get("templateOperationSet");
                Optional<String> body = Optional.of(jsonObject.get("body").toString());
                JSONObject params = (JSONObject)jsonObject.get("params");

                String operationSetId = params.get("operationSetId").toString();
                ResponseEntity response = cubeServerService.fetchPostResponse(request, body, "/as/updateTemplateOperationSet/"+operationSetId);
                if (response.getStatusCode() != HttpStatus.OK) {
                    log.error("Error while calling API=/updateTemplateOperationSet/"+operationSetId +", error="+response.getBody());
                    return ResponseEntity.status(response.getStatusCode()).body(new ErrorResponse(response.getBody(), "Error while calling API=/as/updateTemplateOperationSet/"));
                }
                //Get Data for API= /goldenUpdate/recordingOperationSet/updateMultiPath from JSON post body
                jsonObject = (JSONObject)json.get("updateMultiPath");
                RecordingOperationSetSP[] recordingOperationSetSPS = mapper.readValue(jsonObject.get("body").toString(), RecordingOperationSetSP[].class);
                for (RecordingOperationSetSP recordingOperationset : recordingOperationSetSPS) {
                    validation.validateCustomerName(request,recordingOperationset.customer);
                }
                body = Optional.of(jsonObject.get("body").toString());
                response = cubeServerService.fetchPostResponse(request, body, "/as/goldenUpdate/recordingOperationSet/updateMultiPath");

                if (response.getStatusCode() != HttpStatus.OK) {
                    log.error("Error while calling API=/goldenUpdate/recordingOperationSet/updateMultiPath"+ ", error="+response.getBody());
                    return ResponseEntity.status(response.getStatusCode()).body(new ErrorResponse(response.getBody(),"Error while calling API=/goldenUpdate/recordingOperationSet/updateMultiPath"));
                }

                //Get Data for API=/updateGoldenSet from JSON post body
                jsonObject = (JSONObject)json.get("updateGoldenSet");
                GoldenSetRequest goldenSetRequest = mapper.readValue(jsonObject.get("body").toString(), GoldenSetRequest.class);
                body = Optional.of(goldenSetRequest.toString());
                params = (JSONObject)jsonObject.get("params");

                String recordingId = params.get("recordingId").toString();
                String replayId = params.get("replayId").toString();
                String collectionUpdOpSetId = params.get("collectionUpdOpSetId").toString();
                String templateUpdOpSetId = params.get("templateUpdOpSetId").toString();
                final Optional<Replay> replay =cubeServerService.getReplay(replayId);
                if(replay.isEmpty())
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error while retrieving Replay Object for replayId=" + replayId);
                validation.validateCustomerName(request,replay.get().customerId);
                response = cubeServerService.fetchPostResponse(request, body, "/as/updateGoldenSet/"+ recordingId+ "/"+ replayId + "/"+ collectionUpdOpSetId +"/"+templateUpdOpSetId, "application/x-www-form-urlencoded");
                return response;
            }
        } catch (ParseException e) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (JsonParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (JsonMappingException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getLocalizedMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(e.getLocalizedMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empty post Body");
    }
    @GetMapping("/getGoldenMetaData/{recordingId}")
    public ResponseEntity getGoldenMetaData(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String recordingId) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/getApiFacets/{customerId}/{appId}")
    public  ResponseEntity getApiFacets(HttpServletRequest request, @RequestBody Optional<String> getBody,
            @PathVariable String customerId, @PathVariable String appId) {
        validation.validateCustomerName(request, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/getApiTrace/{customerId}/{appId}")
    public  ResponseEntity getApiTrace(HttpServletRequest request, @RequestBody Optional<String> getBody,
        @PathVariable String customerId, @PathVariable String appId) {
        validation.validateCustomerName(request, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }
}
