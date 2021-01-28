package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.MultipartInputStreamFileResource;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import com.fasterxml.jackson.core.type.TypeReference;
import io.md.core.ConfigApplicationAcknowledge;
import io.md.dao.CustomerAppConfig;
import io.md.dao.DynamicInjectionEventDao;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingType;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
import io.md.dao.UserReqRespContainer;
import io.md.dao.agent.config.AgentConfigTagInfo;
import io.md.dao.agent.config.ConfigDAO;
import io.md.dao.DefaultEvent;
import io.md.dao.Event;
import io.md.dao.EventQuery;

import io.md.utils.Constants;
import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/cs")
public class CubeStoreController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;
    @Autowired
    private DevtoolEnvironmentsRepository devtoolEnvironmentsRepository;

    @GetMapping("/status/{customerId}/{app}/{name}/{label}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                 @PathVariable String app, @PathVariable String name, @PathVariable String label, Authentication authentication){
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/start/{customerId}/{app}/{instanceId}/{templateSetVersion}")
    public ResponseEntity start(HttpServletRequest request, @RequestBody MultiValueMap<String, String> postBody, @PathVariable String customerId,
                                  @PathVariable String app, @PathVariable String instanceId,
                                  @PathVariable String templateSetVersion, Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        User user = (User) authentication.getPrincipal();
        postBody.put(Constants.USER_ID_FIELD, List.of(user.getUsername()));
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/stop/{recordingId}")
    public ResponseEntity stop(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/getEvents")
    public ResponseEntity getEvents(HttpServletRequest request, @RequestBody EventQuery postBody, Authentication authentication) {
        validation.validateCustomerName(authentication,postBody.getCustomerId());
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/rr/{var}")
    public ResponseEntity storerr(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String var, @RequestParam String customerId,
        Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/rrbatch")
    public ResponseEntity storeRrBatch(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/fr")
    public ResponseEntity storeFunc(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/storeEventBatch")
    public ResponseEntity storeEventBatch(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/storeEvent")
    public ResponseEntity storeEvent(HttpServletRequest request, @RequestBody Event postBody, Authentication authentication) {
        validation.validateCustomerName(authentication, postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/deleteEventByReqId")
    public ResponseEntity deleteEventByReqId(HttpServletRequest request, @RequestBody Event postBody , Authentication authentication){

        validation.validateCustomerName(authentication, postBody.customerId);

        /*
            Any validation regarding the the ownership of event by that customer is done at datastore level (solr).
            delete query will have the customerid to ensure that only event belonging to that customer is deleted.
         */

        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/deleteEventByTraceId")
    public ResponseEntity deleteEventByTraceId(HttpServletRequest request, @RequestBody Event postBody , Authentication authentication){

        validation.validateCustomerName(authentication, postBody.customerId);

        /*
            Any validation regarding the the ownership of event by that customer is done at datastore level (solr).
            delete query will have the customerid to ensure that only event belonging to that customer is deleted.
         */

        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }



    @PostMapping("/frbatch")
    public ResponseEntity storeFuncBatch(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/event/setDefaultResponse")
    public ResponseEntity setDefaultRespForEvent(HttpServletRequest request, @RequestBody DefaultEvent postBody, Authentication authentication) {
        validation.validateCustomerName(authentication, postBody.getEvent().customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @GetMapping("/searchRecording")
    public ResponseEntity searchRecording(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam MultiValueMap<String, String> queryMap,
        Authentication authentication){
        String customerId = queryMap.getFirst(Constants.CUSTOMER_ID_FIELD);
        validation.validateCustomerName(authentication,customerId);
        String app = queryMap.getFirst(Constants.APP_FIELD);
        User user = (User)authentication.getPrincipal();
        ResponseEntity responseEntity = cubeServerService.fetchGetResponse(request, getBody);
        String recordingType = queryMap.getFirst(Constants.RECORDING_TYPE_FIELD);
        if(recordingType != null && app != null && recordingType.equals(RecordingType.History.toString())) {
            List<Object> recordings = cubeServerService.getListData(responseEntity, request.getRequestURI(),
                Optional.of("recordings"), new TypeReference<List<Recording>>(){}).orElse(Collections.emptyList());
            if(recordings.isEmpty()) {
                MultiValueMap<String, String> formParams= new LinkedMultiValueMap<>();
                formParams.set("name", "History-" + user.getUsername());
                formParams.set("label", new Date().toString());
                formParams.set("userId", user.getUsername());
                formParams.set("recordingType", RecordingType.History.toString());
                ResponseEntity response = cubeServerService.createRecording(request,
                    customerId, app,
                    user.getUsername(),Optional.of(formParams));
                Optional<Recording> recording = cubeServerService.getRecordingFromResponseEntity(response, RecordingType.History.toString());
                if(recording.isPresent()) {
                    return ResponseEntity.ok(Map.of("recordings", List.of(recording.get())));
                }
            }
        }
        return responseEntity;
    }

    @GetMapping("/currentcollection")
    public ResponseEntity currentcollection(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String customerId,
        Authentication authentication){
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/delete/{recordingId}")
    public ResponseEntity delete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/warmupcache")
    public ResponseEntity warmUpCache(HttpServletRequest request, @RequestBody Optional<String> getBody){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody){
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/updateGoldenFields/{recordingId}")
    public ResponseEntity updateGoldenFields(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId,
        Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/resumeRecording/{recordingId}")
    public  ResponseEntity resumeRecordingByNameLabel(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId,
        Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return  cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/setCurrentAgentConfigTag")
    public ResponseEntity setAgentConfigTag(HttpServletRequest request, @RequestBody AgentConfigTagInfo postBody, Authentication authentication) {
        validation.validateCustomerName(authentication, postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/storeAgentConfig")
    public ResponseEntity storeAgentConfig(HttpServletRequest request, @RequestBody ConfigDAO postBody, Authentication authentication) {
        validation.validateCustomerName(authentication,postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @GetMapping("/fetchAgentConfig/{customerId}/{app}/{service}/{instanceId}")
    public ResponseEntity fetchAgentConfig(HttpServletRequest request, @RequestBody Optional<String> getBody,
            @PathVariable String customerId, @PathVariable String app, @PathVariable String service,
            @PathVariable String instanceId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/ackConfigApplication")
    public ResponseEntity acknowledgeConfigApplication(HttpServletRequest request,
            @RequestBody ConfigApplicationAcknowledge postBody, Authentication authentication) {
        validation.validateCustomerName(authentication,postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/createUserHistory/{customerId}/{app}")
    public ResponseEntity createUserHistory(HttpServletRequest request,
            @RequestBody Optional<String> postBody, @PathVariable String customerId,
            @PathVariable String app, @RequestParam MultiValueMap<String, String> queryMap, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        String userId = ((User) authentication.getPrincipal()).getUsername();
        queryMap.set("recordingType", RecordingType.History.toString());
        return cubeServerService.createRecording(request,
            customerId, app,userId,Optional.of(queryMap));
    }

    @GetMapping("/getAgentSamplingFacets/{customerId}/{app}/{service}/{instanceId}")
    public ResponseEntity getAgentSamplingFacets(HttpServletRequest request,
        @RequestBody Optional<String> getBody, @PathVariable String customerId,
        @PathVariable String app, @PathVariable String service, @PathVariable String instanceId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/afterResponse/{recordingId}")
    public ResponseEntity afterResponse(HttpServletRequest request,
        @RequestBody List<UserReqRespContainer> postBody, @PathVariable String recordingId,
        @RequestParam(value="environmentName", required = false) String environmentName,
        Authentication authentication)
        throws InvalidEventException {
        return  saveReqRespEvents(request, postBody, recordingId, authentication, "/cs/afterResponse/");
    }

    @PostMapping("/storeUserReqResp/{recordingId}")
    public ResponseEntity storeUserReqResp(HttpServletRequest request,
        @RequestBody List<UserReqRespContainer> postBody, @PathVariable String recordingId,
        Authentication authentication) throws InvalidEventException {
        return saveReqRespEvents(request, postBody, recordingId, authentication, "/cs/storeUserReqResp/");
    }

    private ResponseEntity saveReqRespEvents(HttpServletRequest request, List<UserReqRespContainer> postBody,
        String recordingId, Authentication authentication, String path)
        throws InvalidEventException {
        if(postBody == null || postBody.size() < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("post Body cannot be null or empty" + recordingId);
        }

        Optional<Recording> recording = Optional.empty();
        User user = (User) authentication.getPrincipal();
        if(recordingId.equals("History")) {
            String userId = user.getUsername();
            Event requestEvent = postBody.get(0).request;
            if(requestEvent == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No Request Event found in body for recordingId=" + recordingId);
            }
            requestEvent.validateEvent();
            validation.validateCustomerName(authentication,requestEvent.customerId);
            String query =  String.format("customerId=%s&app=%s&userId=%s&recordingType=%s&archived=%s",
                requestEvent.customerId, requestEvent.app, userId, RecordingType.History.toString(), false);
            recording = cubeServerService.searchRecording(query);
            if(recording.isEmpty()) {
                MultiValueMap<String, String> formParams= new LinkedMultiValueMap<>();
                formParams.set("name", "History-" + userId);
                formParams.set("label", new Date().toString());
                formParams.set("userId", userId);
                formParams.set("recordingType", RecordingType.History.toString());
                ResponseEntity responseEntity = cubeServerService.createRecording(request,
                    requestEvent.customerId, requestEvent.app,
                    userId,Optional.of(formParams));
                recording = cubeServerService.getRecordingFromResponseEntity(responseEntity, query);
            }

        } else {
            recording = cubeServerService.getRecording(recordingId);
        }
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("No Recording Object found for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return  cubeServerService.fetchPostResponse(request, Optional.of(postBody), path.concat(recording.get().id));
    }

    @GetMapping("/status/{recordingId}")
    public ResponseEntity status(HttpServletRequest request, @PathVariable String recordingId, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return ResponseEntity.ok(recording);
    }

    @PostMapping("/saveResult")
    public ResponseEntity saveResult(HttpServletRequest request, @RequestBody ReqRespMatchResult reqRespMatchResult, Authentication authentication) {
        Optional<Replay> replay = cubeServerService.getReplay(reqRespMatchResult.replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No Replay found for replayId=" + reqRespMatchResult.replayId);
        validation.validateCustomerName(authentication, replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(reqRespMatchResult));
    }

    @GetMapping("/getCurrentRecordOrReplay/{customerId}/{app}/{instanceId}")
    public ResponseEntity getCurrentRecordOrReplay(HttpServletRequest request,
        @RequestBody Optional<String> getBody, @PathVariable String customerId,
        @PathVariable String app, @PathVariable String instanceId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/forcestop/{recordingId}")
    public ResponseEntity forceStop(HttpServletRequest request,
        @RequestBody Optional<String> postBody,@PathVariable String recordingId, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No Recording found for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/stopRecordingByNameLabel")
    public ResponseEntity stopRecordingByNameLabel(HttpServletRequest request,
        @RequestBody Optional<String> postBody,@RequestParam String customerId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/fetchAgentConfigWithFacets/{customerId}/{app}")
    public ResponseEntity fetchAgentConfigWithFacets(HttpServletRequest request,
        @RequestBody Optional<String> getBody, @PathVariable String customerId,
        @PathVariable String app, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/cache/flushall")
    public ResponseEntity cacheFlushAll(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/deleteAgentConfig/{customerId}/{app}/{service}/{instanceId}")
    public ResponseEntity deleteAgentConfig(HttpServletRequest request, @RequestBody Optional<String> getBody,
        @PathVariable String customerId, @PathVariable String app, @PathVariable String service,
        @PathVariable String instanceId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchPostResponse(request, getBody);
    }

    @GetMapping("/getAppConfiguration/{customerId}/{app}")
    public ResponseEntity getAppConfiguration(HttpServletRequest request,
                                                    @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                                    @PathVariable String app, Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }
    
    @PostMapping("/setAppConfiguration")
    public ResponseEntity setAppConfiguration(HttpServletRequest request, @RequestBody CustomerAppConfig custAppCfg , Authentication authentication) {

        validation.validateCustomerName(authentication, custAppCfg.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(custAppCfg));
    }

    @PostMapping("/preRequest/{recordingOrReplayId}/{runId}")
    public ResponseEntity preRequest(HttpServletRequest request, @PathVariable String recordingOrReplayId,
        @PathVariable String runId, @RequestBody DynamicInjectionEventDao dynamicInjectionEventDao,
        Authentication authentication) {
        if(dynamicInjectionEventDao == null || dynamicInjectionEventDao.getRequestEvent() == null) {
           return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("post Body  or requestEvent cannot be null");
        }
        validation.validateCustomerName(authentication, dynamicInjectionEventDao.getRequestEvent().customerId);
        if(dynamicInjectionEventDao.getInjectionConfigVersion() == null ||
            dynamicInjectionEventDao.getInjectionConfigVersion().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("InjectionConfigVersion cannot be null or empty");
        }

        Long userId = ((User) authentication.getPrincipal()).getId();

        Map<String, String> contextMap = new HashMap<>();
        Optional<DtEnvironment> dtEnvironment = Optional.ofNullable(dynamicInjectionEventDao.getEnvironmentName())
            .flatMap(env -> devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndName(userId,  env));
        dtEnvironment.ifPresent(dt -> {
            List<DtEnvVar> dtEnvVars = dt.getVars();
            dtEnvVars.forEach(dtEnvVar -> contextMap.put(dtEnvVar.getKey(), dtEnvVar.getValue()));
        });
        if (dynamicInjectionEventDao.getContextMap() != null) {
            dynamicInjectionEventDao.getContextMap().forEach((k, v) -> contextMap.put(k, v));
        }
        dynamicInjectionEventDao.setContextMap(contextMap);
        return cubeServerService.fetchPostResponse(request, Optional.of(dynamicInjectionEventDao));
    }

    @PostMapping("/protoDescriptorFileUpload/{customerId}/{app}")
    public ResponseEntity protoDescriptorFileUpload(HttpServletRequest request,
        @PathVariable String customerId, @PathVariable String app, @RequestParam("protoDescriptorFile") MultipartFile[] files,
        Authentication authentication) throws IOException {
        validation.validateCustomerName(authentication, customerId);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                map.add("protoDescriptorFile", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(map));
    }

    @PostMapping("/copyRecording/{recordingId}")
    public ResponseEntity GoldenToCollection(HttpServletRequest request,
        @PathVariable String recordingId, Authentication authentication, @RequestBody Optional<String> postBody) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No Recording found for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        final User user = (User) authentication.getPrincipal();
        return cubeServerService.fetchPostResponse(request, postBody,
            String.format("/cs/copyRecording/%s/%s", recordingId, user.getUsername()));
    }

    @PostMapping("/mergeRecordings/{firstRecordingId}/{secondRecordingId}")
    public  ResponseEntity mergeRecordings(HttpServletRequest request,
        @PathVariable String firstRecordingId, @PathVariable String secondRecordingId,
        Authentication authentication, @RequestBody Optional<String> postBody) {
        Optional<Recording> recording = cubeServerService.getRecording(firstRecordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No Recording found for recordingId=" + firstRecordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);

        recording = cubeServerService.getRecording(secondRecordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No Recording found for recordingId=" + secondRecordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/deduplicate/{recordingId}")
    public ResponseEntity deduplication(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("No Recording Object found for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }
    
    @PostMapping("/populateCache")
    public ResponseEntity populateCache(HttpServletRequest request, @RequestBody RecordOrReplay recordOrReplay , Authentication authentication) {
        validation.validateCustomerName(authentication,recordOrReplay.getCustomerId().orElse(null));
        return cubeServerService.fetchPostResponse(request, Optional.of(recordOrReplay));
    }

    @GetMapping("/getProtoDescriptor/{customerId}/{app}")
    public ResponseEntity getProtoDescriptor(HttpServletRequest request, @RequestBody Optional<String> postBody , Authentication authentication,
        @PathVariable String customerId, @PathVariable String app) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, postBody);
    }
}



