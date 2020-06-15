package com.cubeui.backend.web.external;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import io.md.core.ConfigApplicationAcknowledge;
import io.md.core.ValidateAgentStore;
import io.md.dao.agent.config.AgentConfigTagInfo;
import io.md.dao.agent.config.ConfigDAO;
import io.md.dao.DefaultEvent;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/api/cs")
public class CubeStoreController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @GetMapping("/status/{customerId}/{app}/{name}/{label}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                 @PathVariable String app, @PathVariable String name, @PathVariable String label){
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/start/{customerId}/{app}/{instanceId}/{templateSetVersion}")
    public ResponseEntity start(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                  @PathVariable String app, @PathVariable String instanceId,
                                  @PathVariable String templateSetVersion) {
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/stop/{recordingId}")
    public ResponseEntity stop(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/getEvents")
    public ResponseEntity getEvents(HttpServletRequest request, @RequestBody EventQuery postBody) {
        validation.validateCustomerName(request,postBody.getCustomerId());
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/rr/{var}")
    public ResponseEntity storerr(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String var, @RequestParam String customerId) {
        validation.validateCustomerName(request,customerId);
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
    public ResponseEntity storeEvent(HttpServletRequest request, @RequestBody Event postBody) {
        validation.validateCustomerName(request, postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/frbatch")
    public ResponseEntity storeFuncBatch(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/event/setDefaultResponse")
    public ResponseEntity setDefaultRespForEvent(HttpServletRequest request, @RequestBody DefaultEvent postBody) {
        validation.validateCustomerName(request, postBody.getEvent().customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @GetMapping("/searchRecording")
    public ResponseEntity searchRecording(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String customerId){
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @GetMapping("/currentcollection")
    public ResponseEntity currentcollection(HttpServletRequest request, @RequestBody Optional<String> getBody, @RequestParam String customerId){
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/softDelete/{recordingId}")
    public ResponseEntity softDelete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId) {
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
    public ResponseEntity updateGoldenFields(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/resumeRecording/{recordingId}")
    public  ResponseEntity resumeRecordingByNameLabel(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId) {
        return  cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/setCurrentAgentConfigTag")
    public ResponseEntity setAgentConfigTag(HttpServletRequest request, @RequestBody AgentConfigTagInfo postBody) {
        validation.validateCustomerName(request, postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/storeAgentConfig")
    public ResponseEntity storeAgentConfig(HttpServletRequest request, @RequestBody ConfigDAO postBody) {
        validation.validateCustomerName(request,postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @GetMapping("/fetchAgentConfig/{customerId}/{app}/{service}/{instanceId}")
    public ResponseEntity fetchAgentConfig(HttpServletRequest request, @RequestBody Optional<String> getBody,
            @PathVariable String customerId, @PathVariable String app, @PathVariable String service,
            @PathVariable String instanceId) {
        validation.validateCustomerName(request, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/ackConfigApplication")
    public ResponseEntity acknowledgeConfigApplication(HttpServletRequest request,
            @RequestBody ConfigApplicationAcknowledge postBody) {
        validation.validateCustomerName(request,postBody.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }
}
