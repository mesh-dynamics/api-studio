package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import io.md.core.ConfigApplicationAcknowledge;
import io.md.dao.DynamicInjectionEventDao;
import io.md.dao.Event.EventBuilder.InvalidEventException;
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

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

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
    public ResponseEntity start(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                  @PathVariable String app, @PathVariable String instanceId,
                                  @PathVariable String templateSetVersion, Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
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

    @PostMapping("/deleteEventByReqId/{reqId}")
    public ResponseEntity deleteEventByReqId(HttpServletRequest request, @RequestBody Event postBody , @PathVariable String reqId, Authentication authentication){

        validation.validateCustomerName(authentication, postBody.customerId);

        /*
            Any validation regarding the the ownership of event by that customer is done at datastore level (solr).
            delete query will have the customerid to ensure that only event belonging to that customer is deleted.
         */

        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/deleteEventByTraceId/{traceId}")
    public ResponseEntity deleteEventByTraceId(HttpServletRequest request, @RequestBody Event postBody , @PathVariable String traceId, Authentication authentication){

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
    public ResponseEntity searchRecording(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String customerId,
        Authentication authentication){
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/currentcollection")
    public ResponseEntity currentcollection(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String customerId,
        Authentication authentication){
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/softDelete/{recordingId}")
    public ResponseEntity softDelete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId,
        Authentication authentication) {
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

    @PostMapping("/storeUserReqResp/{recordingId}")
    public ResponseEntity storeUserReqResp(HttpServletRequest request,
        @RequestBody List<UserReqRespContainer> postBody, @PathVariable String recordingId,
        @RequestParam(value="environmentName", required = false) String environmentName,
        Authentication authentication)
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
        ResponseEntity responseEntity = cubeServerService.fetchPostResponse(request, Optional.of(postBody), "/cs/storeUserReqResp/" + recording.get().id);
        if(environmentName != null && responseEntity.getStatusCode() == HttpStatus.OK) {
            Optional<DtEnvironment> dtEnvironmentOptional
                = devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndName(user.getId(), environmentName);
            dtEnvironmentOptional.ifPresent(dt -> {
                Map<String, String> extractionMap = cubeServerService.getExtractionMap(responseEntity);
                List<DtEnvVar> vars = dt.getVars();
                Map<String, String> varsMap = new HashMap<>();
                vars.forEach(dtEnvVar -> {
                    varsMap.put(dtEnvVar.getKey(), dtEnvVar.getValue());
                });
                varsMap.putAll(extractionMap);
                List<DtEnvVar> updatedVars = new ArrayList<>();
                varsMap.forEach((key, value) -> {
                    DtEnvVar dtEnvVar = new DtEnvVar();
                    dtEnvVar.setKey(key);
                    dtEnvVar.setValue(value);
                    dtEnvVar.setEnvironment(dt);
                    updatedVars.add(dtEnvVar);
                });
                dt.setVars(updatedVars);
                devtoolEnvironmentsRepository.save(dt);
            });

        }
        return responseEntity;
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

    @PostMapping("/injectEvent/{replayId}/{runId}")
    public ResponseEntity injectEvent(HttpServletRequest request, @PathVariable String replayId,
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
}
