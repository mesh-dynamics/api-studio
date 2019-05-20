package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.RecordingDTO;
import com.cubeui.backend.domain.Recording;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.RecordingRepository;
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
@RequestMapping("/api/recording")
//@Secured({"ROLE_USER"})
public class RecordingController {

    private AppRepository appRepository;
    private RecordingRepository recordingRepository;

    public RecordingController(AppRepository appRepository, RecordingRepository recordingRepository) {
        this.appRepository = appRepository;
        this.recordingRepository = recordingRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.recordingRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody RecordingDTO recordingDTO, HttpServletRequest request) {
        if (recordingDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Recording with ID '" + recordingDTO.getId() +"' already exists."));
        }
        Optional<App> app = appRepository.findById(recordingDTO.getAppId());
        if (app.isPresent()) {
            Recording saved = this.recordingRepository.save(
                    Recording.builder().app(app.get()).collectionName(recordingDTO.getCollectionName())
                            .status(recordingDTO.getStatus()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/recording/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordFoundException("App with ID '" + recordingDTO.getAppId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody RecordingDTO recordingDTO, HttpServletRequest request) {
        if (recordingDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Recording id not provided"));
        }
        Optional<Recording> recording = recordingRepository.findById(recordingDTO.getId());
        if (recording.isPresent()) {
            recording.ifPresent(rec -> {
                rec.setApp(appRepository.findById(recordingDTO.getAppId()).get());
                rec.setCollectionName(recordingDTO.getCollectionName());
                rec.setStatus(recordingDTO.getStatus());
            });
            this.recordingRepository.save(recording.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/recording/{id}")
                            .buildAndExpand(recording.get().getId())
                            .toUri())
                    .body(recording);
        } else {
            throw new RecordFoundException("Recording with ID '" + recordingDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.recordingRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Recording> existed = this.recordingRepository.findById(id);
        this.recordingRepository.delete(existed.get());
        return noContent().build();
    }
}
