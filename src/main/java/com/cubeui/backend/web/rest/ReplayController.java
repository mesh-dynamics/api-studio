package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.ReplayDTO;
import com.cubeui.backend.domain.Replay;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.repository.ReplayRepository;
import com.cubeui.backend.repository.TestConfigRepository;
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

    private TestConfigRepository testConfigRepository;
    private ReplayRepository replayRepository;

    public ReplayController(TestConfigRepository testConfigRepository, ReplayRepository replayRepository) {
        this.testConfigRepository = testConfigRepository;
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
        Optional<TestConfig> testConfig = testConfigRepository.findById(replayDTO.getTestId());
        if (testConfig.isPresent()) {
            Replay saved = this.replayRepository.save(
                    Replay.builder().replayName(replayDTO.getReplayName()).analysis(replayDTO.getAnalysis()).completedAt(replayDTO.getCompletedAt())
                            .testConfig(testConfig.get()).reqCount(replayDTO.getReqCount()).reqFailed(replayDTO.getReqFailed())
                            .reqSent(replayDTO.getReqSent()).sampleRate(replayDTO.getSampleRate()).status(replayDTO.getStatus()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/replay/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordFoundException("TestConfig with ID '" + replayDTO.getTestId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ReplayDTO replayDTO, HttpServletRequest request) {
        if (replayDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Replay id not provided"));
        }
        Optional<Replay> existing = replayRepository.findById(replayDTO.getId());
        Optional<TestConfig> testConfig = testConfigRepository.findById(replayDTO.getTestId());
        if (testConfig.isEmpty()){
            throw new RecordFoundException("TestConfig with ID '" + replayDTO.getTestId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(replay -> {
                replay.setTestConfig(testConfig.get());
                replay.setAnalysis(replayDTO.getAnalysis());
                replay.setStatus(replayDTO.getStatus());
                replay.setReplayName(replayDTO.getReplayName());
                replay.setReqCount(replayDTO.getReqCount());
                replay.setReqFailed(replayDTO.getReqFailed());
                replay.setReqSent(replayDTO.getReqSent());
                replay.setSampleRate(replayDTO.getSampleRate());
                replay.setCompletedAt(replayDTO.getCompletedAt());
            });
            this.replayRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/replay/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
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
