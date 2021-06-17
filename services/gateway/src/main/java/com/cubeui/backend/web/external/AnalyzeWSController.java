/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.MultipartInputStreamFileResource;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.GoldenSetRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.dao.Recording;
import io.md.dao.RecordingOperationSetSP;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import io.md.dao.Replay;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/as")
@Slf4j
public class AnalyzeWSController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @GetMapping("/status/{replayId}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
        Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/analyze/{replayId}")
    public ResponseEntity analyze(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId,
        Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/aggrresult/{replayId}")
    public ResponseEntity getResultAggregate(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
        Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/replayRes/{customerId}/{app}/{service}/{replayId}")
    public ResponseEntity replayResult(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                       @PathVariable String app, @PathVariable String service, @PathVariable String replayId, Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/registerTemplateApp/{customerId}/{appId}/{version}")
    public ResponseEntity registerTemplateApp(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                              @PathVariable String appId, @PathVariable String version, Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path}")
    public ResponseEntity registerTemplate(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String type,
                                           @PathVariable String customerId, @PathVariable String appId, @PathVariable String serviceName,
                                           @PathVariable String path, Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/getTemplate/{customerId}/{appId}/{templateVersion}/{service}/{type}")
    public ResponseEntity getTemplate(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                          @PathVariable String appId, @PathVariable String templateVersion, @PathVariable String service,
                                          @PathVariable String type, Authentication authentication){
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisRes/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResult(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
                                          @PathVariable String recordReqId, Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResNoTrace/{replayId}/{recordReqId}")
    public ResponseEntity getAnalysisResultWithoutTrace(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
                                            @PathVariable String recordReqId, Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/timelineres/{customer}/{app}")
    public ResponseEntity getTimelineResults(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customer,
                                                        @PathVariable String app, Authentication authentication){
        validation.validateCustomerName(authentication,customer);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResByPath/{replayId}")
    public ResponseEntity getAnalysisResultsByPath(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
        Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/analysisResByReq/{replayId}")
    public ResponseEntity getResultByReq(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId,
        Authentication authentication){
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/saveTemplateSet/{customer}/{app}/{templateSetName}")
    public ResponseEntity saveTemplateSet(HttpServletRequest request, @RequestBody Optional<String> postBody,
        @PathVariable String customer, @PathVariable String app, @PathVariable String templateSetName,
        Authentication authentication, @RequestParam("file") MultipartFile[] files)
        throws IOException {
        validation.validateCustomerName(authentication,customer);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                map.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(map));
    }

    @PostMapping("/cache/flushall")
    public ResponseEntity cacheFlushAll(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/initTemplateOperationSet/{customer}/{app}/{version}")
    public ResponseEntity initTemplateOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customer,
                                                   @PathVariable String app,  @PathVariable String version, Authentication authentication) {
        validation.validateCustomerName(authentication,customer);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/updateTemplateOperationSet/{customerId}/{operationSetId}")
    public ResponseEntity updateTemplateOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody,
        @PathVariable String operationSetId, @PathVariable String customerId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/updateTemplateSet/{templateSetId}/{operationSetId}")
    public ResponseEntity updateTemplateSet(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String templateSetId,
                                            @PathVariable String operationSetId){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/updateGoldenSet/{recordingId}/{replayId}/{collectionUpdOpSetId}/{templateUpdOpSetId}")
    public ResponseEntity updateGoldenSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId,
                                          @PathVariable String replayId, @PathVariable String collectionUpdOpSetId, @PathVariable String templateUpdOpSetId,
                                          Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/sanitizeGoldenSet")
    public ResponseEntity sanitizeRecording(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String recordingId,
                                            @RequestParam String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/create")
    public ResponseEntity createRecordingOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String customer,
                                                      @RequestParam String app, Authentication authentication) {
        validation.validateCustomerName(authentication,customer);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/goldenUpdate/recordingOperationSet/get")
    public ResponseEntity getRecordingOperationSet(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String operationSetId,
                                                   @RequestParam String service, @RequestParam String path){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/update")
    public ResponseEntity updateRecordingOperationSet(HttpServletRequest request, @RequestBody RecordingOperationSetSP postBody, Authentication authentication) {
        validation.validateCustomerName(authentication,postBody.customer);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/updateMultiPath")
    public ResponseEntity updateRecordingOperationSet_1(HttpServletRequest request, @RequestBody List<RecordingOperationSetSP> postBody, Authentication authentication) {
        for (RecordingOperationSetSP recordingOperationset : postBody) {
            validation.validateCustomerName(authentication,recordingOperationset.customer);
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/goldenUpdate/recordingOperationSet/apply")
    public ResponseEntity applyRecordingOperationSet(HttpServletRequest request, @RequestBody Optional<String> postBody, @RequestParam String operationSetId,
                                                     @RequestParam String replayId, @RequestParam String collectionName, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/goldenInsights/{recordingId}")
    public ResponseEntity goldenInsights(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String recordingId,
                                         @RequestParam String service, @RequestParam String apiPath, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/goldenUpdateUnified")
    public  ResponseEntity goldenUpdateUnified(HttpServletRequest request, @RequestBody Optional<String> postBody, Authentication authentication) {
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
                //Get Data for API=/updateGoldenSet from JSON post body
                JSONObject updateGoldenSet = (JSONObject)json.get("updateGoldenSet");
                GoldenSetRequest goldenSetRequest = mapper.readValue(updateGoldenSet.get("body").toString(), GoldenSetRequest.class);
                Optional<String> goldenSetRequestBody = Optional.of(goldenSetRequest.toString());
                JSONObject updateGoldenSetParams = (JSONObject)updateGoldenSet.get("params");
                String replayId = updateGoldenSetParams.get("replayId").toString();
                final Optional<Replay> replay =cubeServerService.getReplay(replayId);
                if(replay.isEmpty())
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No Replay found for replayId=" + replayId);
                validation.validateCustomerName(authentication,replay.get().customerId);
                ResponseEntity response = cubeServerService.fetchPostResponse(request, body, "/as/updateTemplateOperationSet/"+ replay.get().customerId + "/"+operationSetId);
                if (response.getStatusCode() != HttpStatus.OK) {
                    log.error("Error while calling API=/updateTemplateOperationSet/"+operationSetId +", error="+response.getBody());
                    return ResponseEntity.status(response.getStatusCode()).body(new ErrorResponse(response.getBody(), "Error while calling API=/as/updateTemplateOperationSet/"));
                }
                //Get Data for API= /goldenUpdate/recordingOperationSet/updateMultiPath from JSON post body
                jsonObject = (JSONObject)json.get("updateMultiPath");
                RecordingOperationSetSP[] recordingOperationSetSPS = mapper.readValue(jsonObject.get("body").toString(), RecordingOperationSetSP[].class);
                for (RecordingOperationSetSP recordingOperationset : recordingOperationSetSPS) {
                    validation.validateCustomerName(authentication,recordingOperationset.customer);
                }
                body = Optional.of(jsonObject.get("body").toString());
                response = cubeServerService.fetchPostResponse(request, body, "/as/goldenUpdate/recordingOperationSet/updateMultiPath");

                if (response.getStatusCode() != HttpStatus.OK) {
                    log.error("Error while calling API=/goldenUpdate/recordingOperationSet/updateMultiPath"+ ", error="+response.getBody());
                    return ResponseEntity.status(response.getStatusCode()).body(new ErrorResponse(response.getBody(),"Error while calling API=/goldenUpdate/recordingOperationSet/updateMultiPath"));
                }

                String recordingId = updateGoldenSetParams.get("recordingId").toString();
                String collectionUpdOpSetId = updateGoldenSetParams.get("collectionUpdOpSetId").toString();
                String templateUpdOpSetId = updateGoldenSetParams.get("templateUpdOpSetId").toString();
                response = cubeServerService.fetchPostResponse(request, goldenSetRequestBody, "/as/updateGoldenSet/"+ recordingId+ "/"+ replayId + "/"+ collectionUpdOpSetId +"/"+templateUpdOpSetId, "application/x-www-form-urlencoded");
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
    public ResponseEntity getGoldenMetaData(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String recordingId,
        Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/getApiFacets/{customerId}/{appId}")
    public  ResponseEntity getApiFacets(HttpServletRequest request, @RequestBody Optional<String> getBody,
            @PathVariable String customerId, @PathVariable String appId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/getApiTrace/{customerId}/{appId}")
    public  ResponseEntity getApiTrace(HttpServletRequest request, @RequestBody Optional<String> getBody,
        @PathVariable String customerId, @PathVariable String appId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @RequestMapping(value = "/getReqRespMatchResult" , method = {RequestMethod.GET , RequestMethod.POST})
    public ResponseEntity getReqRespMatchResult(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        String method = request.getMethod();
        switch (method){
            case "GET":
                return cubeServerService.fetchGetResponse(request, getBody);
            case "POST":
                return cubeServerService.fetchPostResponse(request, getBody);
            default:
                throw new IllegalArgumentException("Invalid method "+method + " for this request");
        }
    }

    @GetMapping("/learnComparisonRules")
    public ResponseEntity learnComparisonRules(HttpServletRequest request, @RequestBody Optional<String> body,
        Authentication authentication, @QueryParam("replayId") String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("No Replay found for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchGetResponse(request, body);
    }

    @PostMapping("/learnComparisonRules/{customerId}/{app}/{version}")
    public ResponseEntity learnComparisonRules(HttpServletRequest request, @RequestBody Optional<String> body,
        Authentication authentication, @QueryParam("replayId") String replayId,
        @PathVariable String customerId, @PathVariable String app, @PathVariable String version, @RequestParam("file") MultipartFile[] files)
        throws IOException {
        validation.validateCustomerName(authentication, customerId);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                map.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(map));
    }

    @GetMapping("/getTemplateSet/{customerId}/{appId}/{templateVersion}")
    public ResponseEntity getTemplateSet(HttpServletRequest request, @RequestBody Optional<String> body,
        Authentication authentication, @PathVariable String customerId, @PathVariable String appId, @PathVariable String templateVersion) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, body);
    }

    @GetMapping("/getTemplateSetLabels/{customerId}/{appId}")
    public ResponseEntity getTemplateSetLabels(HttpServletRequest request, @RequestBody Optional<String> body,
        Authentication authentication, @PathVariable String customerId, @PathVariable String appId) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, body);
    }

    @GetMapping("/getTemplateSet/{customerId}/{app}")
    public ResponseEntity getTemplateSet(HttpServletRequest request, @RequestBody Optional<String> body,
        Authentication authentication, @PathVariable String customerId, @PathVariable String app) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, body);
    }
}
