package com.cubeui.backend.web.external;

import io.md.dao.Recording;
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

    @GetMapping("/status/{replayId}")
    public ResponseEntity status(HttpServletRequest request, @RequestBody Optional<String> getBody, @PathVariable String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return ResponseEntity.ok(replay);
    }

    @PostMapping("/start/{recordingId}")
    public ResponseEntity start(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String recordingId) {
        Optional<Recording> recording = cubeServerService.getRecording(recordingId);
        if(recording.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Recording Object for recordingId=" + recordingId);
        validation.validateCustomerName(request,recording.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/transforms/{customerId}/{app}/{collection}/{replayId}")
    public ResponseEntity transforms(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                                     @PathVariable String app, @PathVariable String collection, @PathVariable String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        validation.validateCustomerName(request,customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/forcecomplete/{replayId}")
    public ResponseEntity forceComplete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/forcestart/{replayId}")
    public ResponseEntity forceStart(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/health")
    public ResponseEntity health(HttpServletRequest request, @RequestBody Optional<String> getBody) {
        return cubeServerService.fetchGetResponse(request, getBody);
    }

    @PostMapping("/softDelete/{replayId}")
    public ResponseEntity softDelete(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String replayId) {
        final Optional<Replay> replay =cubeServerService.getReplay(replayId);
        if(replay.isEmpty())
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error while retrieving Replay Object for replayId=" + replayId);
        validation.validateCustomerName(request,replay.get().customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @PostMapping("/start/byGoldenName/{customerId}/{app}/{goldenName}")
    public ResponseEntity startByGoldenName(HttpServletRequest request, @RequestBody Optional<String> postBody, @PathVariable String customerId,
                        @PathVariable String app, @PathVariable String goldenName) {
        validation.validateCustomerName(request, customerId);
        return cubeServerService.fetchPostResponse(request, postBody);
    }

    @GetMapping("/getReplays/{customerId}")
    public ResponseEntity getReplays(HttpServletRequest request, @RequestBody Optional<String> getBody,
            @PathVariable String customerId) {
        validation.validateCustomerName(request, customerId);
        return cubeServerService.fetchGetResponse(request, getBody);
    }
}
