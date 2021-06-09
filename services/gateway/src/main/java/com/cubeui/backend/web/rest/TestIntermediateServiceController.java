package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.TestServiceDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestIntermediateService;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestIntermediateServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/test_intermediate_service")
//@Secured({"ROLE_USER"})
public class TestIntermediateServiceController {

    private TestConfigRepository testConfigRepository;
    private ServiceRepository serviceRepository;
    private TestIntermediateServiceRepository testIntermediateServiceRepository;

    public TestIntermediateServiceController(TestConfigRepository testConfigRepository, ServiceRepository serviceRepository, TestIntermediateServiceRepository testIntermediateServiceRepository) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.testIntermediateServiceRepository = testIntermediateServiceRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.testIntermediateServiceRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody TestServiceDTO testServiceDTO, HttpServletRequest request) {
        if (testServiceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestIntermediateService with ID '" + testServiceDTO.getId() +"' already exists."));
        }
        Optional<Service> service = serviceRepository.findById(testServiceDTO.getServiceId());
        Optional<TestConfig> testConfig = testConfigRepository.findById(testServiceDTO.getTestId());
        if (testConfig.isPresent() && service.isPresent()) {
            Optional<TestIntermediateService> testIntermediateService = this.testIntermediateServiceRepository.findByTestConfigIdAndServiceId(
                    testServiceDTO.getTestId(), testServiceDTO.getServiceId());
            if (testIntermediateService.isPresent()) {
                return ok(testIntermediateService);
            }
            TestIntermediateService saved = this.testIntermediateServiceRepository.save(
                    TestIntermediateService.builder().service(service.get()).testConfig(testConfig.get()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/test_intermediate_service/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            if (service.isEmpty()){
                throw new RecordNotFoundException("Service with ID '" + testServiceDTO.getServiceId() + "' not found.");
            } else {
                throw new RecordNotFoundException("TestConfig with ID '" + testServiceDTO.getTestId() + "' not found.");
            }
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody TestServiceDTO testServiceDTO, HttpServletRequest request) {
        if (testServiceDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestIntermediateService id not provided"));
        }
        Optional<Service> service = serviceRepository.findById(testServiceDTO.getServiceId());
        Optional<TestConfig> testConfig = testConfigRepository.findById(testServiceDTO.getTestId());
        Optional<TestIntermediateService> existing = testIntermediateServiceRepository.findById(testServiceDTO.getId());
        if (service.isEmpty()){
            throw new RecordNotFoundException("Service with ID '" + testServiceDTO.getServiceId() + "' not found.");
        }
        if (testConfig.isEmpty()) {
            throw new RecordNotFoundException("TestConfig with ID '" + testServiceDTO.getTestId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(testIntermediateService -> {
                testIntermediateService.setService(service.get());
                testIntermediateService.setTestConfig(testConfig.get());
            });
            this.testIntermediateServiceRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/test_intermediate_service/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("TestIntermediateService with ID '" + testServiceDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.testIntermediateServiceRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<TestIntermediateService> existed = this.testIntermediateServiceRepository.findById(id);
        this.testIntermediateServiceRepository.delete(existed.get());
        return noContent().build();
    }
}
