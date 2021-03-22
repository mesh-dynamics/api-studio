package com.cubeui.backend.web.external;

import com.cubeui.backend.domain.MultipartInputStreamFileResource;
import com.cubeui.backend.domain.User;
import io.md.dao.EventQuery;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.injection.DynamicInjectionConfig;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import io.md.utils.Constants;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rs")
public class ReplayWSController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @GetMapping("/status/{replayId}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return ResponseEntity.ok(replay);
    }

    @PostMapping("/start/{recordingId}")
    public ResponseEntity start(HttpServletRequest request, @RequestBody MultiValueMap<String, String> postBody, @PathVariable String recordingId, Authentication authentication) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        User user = (User) authentication.getPrincipal();
        postBody.put(Constants.USER_ID_FIELD, List.of(user.getUsername()));
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/start")
    public ResponseEntity startMultiple(HttpServletRequest request, @RequestBody MultiValueMap<String, String> postBody, Authentication authentication) {
        String recordingId = postBody.getFirst(Constants.RECORDING_ID);
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(authentication,recording.get().customerId);
        User user = (User) authentication.getPrincipal();
        postBody.put(Constants.USER_ID_FIELD, List.of(user.getUsername()));
        return cubeServerService.fetchPostResponse(request, Optional.of(postBody));
    }

    @PostMapping("/transforms/{customerId}/{app}/{collection}/{replayId}")
    public ResponseEntity transforms(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                     @PathVariable String app, @PathVariable String collection, @PathVariable String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/forcecomplete/{replayId}")
    public ResponseEntity forceComplete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/forcestart/{replayId}")
    public ResponseEntity forceStart(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/delete/{replayId}")
    public ResponseEntity delete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/start/byGoldenName/{customerId}/{app}/{goldenName}")
    public ResponseEntity startByGoldenName(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                        @PathVariable String app, @PathVariable String goldenName, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/getReplays/{customerId}")
    public ResponseEntity getReplays(HttpServletRequest request, @RequestBody Optional<String> getBody,
            @PathVariable String customerId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/replay/restart/{customerId}/{app}/{replayId}")
    public ResponseEntity restartReplay(HttpServletRequest request, @RequestBody Optional<String> postBody,
        @PathVariable String customerId, @PathVariable String app, @PathVariable String replayId, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/saveReplay")
    public ResponseEntity saveReplay(HttpServletRequest request, @RequestBody Replay replay, Authentication authentication) {
        validation.validateCustomerName(authentication, replay.customerId);
        return cubeServerService.fetchPostResponse(request, Optional.of(replay));
    }

    @GetMapping("/getDynamicInjectionConfig/{customerId}/{app}/{version}")
    public ResponseEntity getDynamicInjectionConfig(HttpServletRequest request,
        @RequestBody Optional<String> getBody, @PathVariable String customerId,
        @PathVariable String app, @PathVariable String version, Authentication authentication) {
        validation.validateCustomerName(authentication,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/saveDynamicInjectionConfigFromJson")
    public ResponseEntity saveDynamicInjectionConfigFromJson(HttpServletRequest request,
        @RequestParam(value = "file") MultipartFile[] files , Authentication authentication)
        throws IOException {
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                map.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(map));
    }

    @PostMapping("/deferredDeleteReplay/{replayId}/{status}")
    public ResponseEntity deferredDeleteReplay(HttpServletRequest request,
        @RequestBody Optional<String> postBody, @PathVariable String replayId, Authentication authentication) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("No Replay found for replayId=" + replayId);
        validation.validateCustomerName(authentication,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/cache/flushall")
    public ResponseEntity cacheFlushAll(HttpServletRequest request, @RequestBody Optional<String> postBody) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/getPotentialDynamicInjectionConfigs")
    public ResponseEntity getPotentialDynamicInjectionConfigs(HttpServletRequest request, @RequestBody
        EventQuery eventQuery, Authentication authentication) {
        validation.validateCustomerName(authentication, eventQuery.getCustomerId());
        return cubeServerService.fetchPostResponse(request,Optional.of(eventQuery));
    }

    @PostMapping("/saveDynamicInjectionConfigFromCsv/{customerId}/{app}/{version}")
    public  ResponseEntity saveDynamicInjectionConfigFromCsv(HttpServletRequest request,
        @PathVariable String customerId, @PathVariable String app, @PathVariable String version,
        @RequestParam(value = "file") MultipartFile[] files, Authentication authentication) throws IOException {
        validation.validateCustomerName(authentication, customerId);
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                map.add("file", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
            }
        }
        return cubeServerService.fetchPostResponse(request, Optional.of(map));
    }

    @GetMapping("/filterDynamicInjectionConfigsByReplay/{customerId}/{app}/{version}")
    public ResponseEntity filterDynamicInjectionConfigsByReplay(HttpServletRequest request, @PathVariable String customerId,
        @PathVariable String app, @PathVariable String version, @RequestBody Optional<String> body, Authentication authentication) {
        validation.validateCustomerName(authentication, customerId);
        return cubeServerService.fetchGetResponse(request, body);

    }
}
