package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.TestDTO;
import com.cubeui.backend.domain.Recording;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.Test;
import com.cubeui.backend.repository.RecordingRepository;
import com.cubeui.backend.repository.ServiceRepository;
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
@RequestMapping("/api/test")
//@Secured({"ROLE_USER"})
public class TestController {

    private TestRepository testRepository;
    private ServiceRepository serviceRepository;
    private RecordingRepository recordingRepository;

    public TestController(TestRepository testRepository, ServiceRepository serviceRepository, RecordingRepository recordingRepository) {
        this.testRepository = testRepository;
        this.serviceRepository = serviceRepository;
        this.recordingRepository = recordingRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.testRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody TestDTO testDTO, HttpServletRequest request) {
        if (testDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Test with ID '" + testDTO.getId() +"' already exists."));
        }
        Optional<Service> service = serviceRepository.findById(testDTO.getGatewayServiceId());
        Optional<Recording> recording = recordingRepository.findById(testDTO.getCollectionId());
        if (service.isPresent() && recording.isPresent()) {
            Test saved = this.testRepository.save(
                    Test.builder().collection(recording.get()).gatewayService(service.get()).description(testDTO.getDescription())
                            .endpoint(testDTO.getEndpoint()).gatewayPathSelection(testDTO.getGatewayPathSelection())
                            .testConfigName(testDTO.getTestConfigName()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/test/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            if (service.isEmpty()){
                throw new RecordFoundException("Service with ID '" + testDTO.getGatewayServiceId() + "' not found.");
            } else {
                throw new RecordFoundException("Recording with ID '" + testDTO.getCollectionId() + "' not found.");
            }
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody TestDTO testDTO, HttpServletRequest request) {
        if (testDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Test id not provided"));
        }
        Optional<Test> test = testRepository.findById(testDTO.getId());
        Optional<Service> service = serviceRepository.findById(testDTO.getGatewayServiceId());
        Optional<Recording> recording = recordingRepository.findById(testDTO.getCollectionId());
        if (service.isEmpty()){
            throw new RecordFoundException("Service with ID '" + testDTO.getGatewayServiceId() + "' not found.");
        }
        if (recording.isEmpty()) {
            throw new RecordFoundException("Recording with ID '" + testDTO.getCollectionId() + "' not found.");
        }
        if (test.isPresent()) {
            test.ifPresent(test1 -> {
                test1.setCollection(recording.get());
                test1.setGatewayService(service.get());
                test1.setDescription(testDTO.getDescription());
                test1.setEndpoint(testDTO.getEndpoint());
                test1.setGatewayPathSelection(testDTO.getGatewayPathSelection());
                test1.setTestConfigName(testDTO.getTestConfigName());
            });
            this.testRepository.save(test.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/test/{id}")
                            .buildAndExpand(recording.get().getId())
                            .toUri())
                    .body(test);
        } else {
            throw new RecordFoundException("Test with ID '" + testDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.testRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Test> existed = this.testRepository.findById(id);
        this.testRepository.delete(existed.get());
        return noContent().build();
    }
}
