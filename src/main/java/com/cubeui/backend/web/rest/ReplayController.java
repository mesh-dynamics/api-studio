package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.RecordingDTO;
import com.cubeui.backend.domain.DTO.ReplayDTO;
import com.cubeui.backend.domain.Recording;
import com.cubeui.backend.domain.Replay;
import com.cubeui.backend.domain.Test;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.RecordingRepository;
import com.cubeui.backend.repository.ReplayRepository;
import com.cubeui.backend.repository.TestRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.RecordFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/replay")
//@Secured({"ROLE_USER"})
public class ReplayController {

    private TestRepository testRepository;
    private ReplayRepository replayRepository;

    public ReplayController(TestRepository testRepository, ReplayRepository replayRepository) {
        this.testRepository = testRepository;
        this.replayRepository = replayRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.replayRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ReplayDTO replayDTO, HttpServletRequest request) {
        if (replayDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Replay with ID '" + replayDTO.getId() +"' already exists."));
        }
        Optional<Test> test = testRepository.findById(replayDTO.getTestId());
        if (test.isPresent()) {
            Replay saved = this.replayRepository.save(
                    Replay.builder().replayName(replayDTO.getReplayName()).analysis(replayDTO.getAnalysis()).completedAt(replayDTO.getCompletedAt())
                            .test(test.get()).reqCount(replayDTO.getReqCount()).reqFailed(replayDTO.getReqFailed())
                            .reqSent(replayDTO.getReqSent()).sampleRate(replayDTO.getSampleRate()).status(replayDTO.getStatus()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/replay/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordFoundException("Test with ID '" + replayDTO.getTestId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ReplayDTO replayDTO, HttpServletRequest request) {
        if (replayDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Replay id not provided"));
        }
        Optional<Replay> replay = replayRepository.findById(replayDTO.getId());
        if (replay.isPresent()) {
            replay.ifPresent(rep -> {
                rep.setTest(testRepository.findById(replayDTO.getTestId()).get());
                rep.setAnalysis(replayDTO.getAnalysis());
                rep.setStatus(replayDTO.getStatus());
                rep.setReplayName(replayDTO.getReplayName());
                rep.setReqCount(replayDTO.getReqCount());
                rep.setReqFailed(replayDTO.getReqFailed());
                rep.setReqSent(replayDTO.getReqSent());
                rep.setSampleRate(replayDTO.getSampleRate());
                rep.setCompletedAt(replayDTO.getCompletedAt());
            });
            this.replayRepository.save(replay.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/replay/{id}")
                            .buildAndExpand(replay.get().getId())
                            .toUri())
                    .body(replay);
        } else {
            throw new RecordFoundException("Replay with ID '" + replayDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.replayRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Replay> existed = this.replayRepository.findById(id);
        this.replayRepository.delete(existed.get());
        return noContent().build();
    }
}
