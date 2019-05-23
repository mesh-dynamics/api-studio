package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Recording;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.repository.RecordingRepository;
import com.cubeui.backend.repository.ServiceRepository;
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
@RequestMapping("/api/test")
//@Secured({"ROLE_USER"})
public class TestConfigController {

    private TestConfigRepository testConfigRepository;
    private ServiceRepository serviceRepository;
    private RecordingRepository recordingRepository;

    public TestConfigController(TestConfigRepository testConfigRepository, ServiceRepository serviceRepository, RecordingRepository recordingRepository) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.recordingRepository = recordingRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.testConfigRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody TestConfigDTO testDTO, HttpServletRequest request) {
        if (testDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig with ID '" + testDTO.getId() +"' already exists."));
        }
        Optional<Service> service = serviceRepository.findById(testDTO.getGatewayServiceId());
        Optional<Recording> recording = recordingRepository.findById(testDTO.getCollectionId());
        if (service.isPresent() && recording.isPresent()) {
            TestConfig saved = this.testConfigRepository.save(
                    TestConfig.builder().collection(recording.get()).gatewayService(service.get()).description(testDTO.getDescription())
                            .endpoint(testDTO.getEndpoint()).gatewayPathSelection(testDTO.getGatewayPathSelection())
                            .testConfigName(testDTO.getTestConfigName()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/testConfig/{id}")
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
    public ResponseEntity update(@RequestBody TestConfigDTO testDTO, HttpServletRequest request) {
        if (testDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig id not provided"));
        }
        Optional<TestConfig> testConfig = testConfigRepository.findById(testDTO.getId());
        Optional<Service> service = serviceRepository.findById(testDTO.getGatewayServiceId());
        Optional<Recording> recording = recordingRepository.findById(testDTO.getCollectionId());
        if (service.isEmpty()){
            throw new RecordFoundException("Service with ID '" + testDTO.getGatewayServiceId() + "' not found.");
        }
        if (recording.isEmpty()) {
            throw new RecordFoundException("Recording with ID '" + testDTO.getCollectionId() + "' not found.");
        }
        if (testConfig.isPresent()) {
            testConfig.ifPresent(test1 -> {
                test1.setCollection(recording.get());
                test1.setGatewayService(service.get());
                test1.setDescription(testDTO.getDescription());
                test1.setEndpoint(testDTO.getEndpoint());
                test1.setGatewayPathSelection(testDTO.getGatewayPathSelection());
                test1.setTestConfigName(testDTO.getTestConfigName());
            });
            this.testConfigRepository.save(testConfig.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/testConfig/{id}")
                            .buildAndExpand(recording.get().getId())
                            .toUri())
                    .body(testConfig);
        } else {
            throw new RecordFoundException("TestConfig with ID '" + testDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.testConfigRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<TestConfig> existed = this.testConfigRepository.findById(id);
        this.testConfigRepository.delete(existed.get());
        return noContent().build();
    }
}
