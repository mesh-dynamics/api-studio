package com.cubeui.backend.web.external;

import io.md.dao.Replay;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.CubeServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

@RestController
@RequestMapping("/api/rs")
public class ReplayWSController {

    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private Validation validation;

    @GetMapping("/status/{customerId}/{app}/{collection}/{replayId}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String customerId,
                                 @PathVariable String app, @PathVariable String collection, @PathVariable String replayId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/start/{recordingId}")
    public ResponseEntity start(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId) {
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/transforms/{customerId}/{app}/{collection}/{replayId}")
    public ResponseEntity transforms(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                     @PathVariable String app, @PathVariable String collection, @PathVariable String replayId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/forcecomplete/{replayId}")
    public ResponseEntity forceComplete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/forcestart/{replayId}")
    public ResponseEntity forceStart(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Replay replay =cubeServerService.getReplay(replayId);
        if(replay == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        return cubeServerService.fetchGetResponse(request, getBody);
    }

}
