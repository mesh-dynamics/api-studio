package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Recording;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.repository.AppRepository;
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
    private AppRepository appRepository;

    public TestConfigController(TestConfigRepository testConfigRepository, ServiceRepository serviceRepository, AppRepository appRepository) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.appRepository = appRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.testConfigRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody TestConfigDTO testConfigDTO, HttpServletRequest request) {
        if (testConfigDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig with ID '" + testConfigDTO.getId() +"' already exists."));
        }
        Optional<Service> service = serviceRepository.findById(testConfigDTO.getGatewayServiceId());
        Optional<App> app = appRepository.findById(testConfigDTO.getAppId());
        if (service.isPresent() && app.isPresent()) {
            TestConfig saved = this.testConfigRepository.save(
                    TestConfig.builder()
                            .testConfigName(testConfigDTO.getTestConfigName())
                            .app(app.get())
                            .gatewayService(service.get())
                            .description(testConfigDTO.getDescription())
                            .gatewayPathSelection(testConfigDTO.getGatewayPathSelection())
                            .gatewayReqSelection(testConfigDTO.getGatewayReqSelection())
                            .maxRunTimeMin(testConfigDTO.getMaxRunTimeMin())
                            .emailId(testConfigDTO.getEmailId())
                            .slackId(testConfigDTO.getSlackId())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/testConfig/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            if (service.isEmpty()){
                throw new RecordFoundException("Service with ID '" + testConfigDTO.getGatewayServiceId() + "' not found.");
            } else {
                throw new RecordFoundException("App with ID '" + testConfigDTO.getAppId() + "' not found.");
            }
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody TestConfigDTO testConfigDTO, HttpServletRequest request) {
        if (testConfigDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig id not provided"));
        }
        Optional<TestConfig> existing = testConfigRepository.findById(testConfigDTO.getId());
        Optional<Service> service = serviceRepository.findById(testConfigDTO.getGatewayServiceId());
        Optional<App> app = appRepository.findById(testConfigDTO.getAppId());
        if (service.isEmpty()){
            throw new RecordFoundException("Service with ID '" + testConfigDTO.getGatewayServiceId() + "' not found.");
        }
        if (app.isEmpty()) {
            throw new RecordFoundException("App with ID '" + testConfigDTO.getAppId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(testConfig -> {
                testConfig.setTestConfigName(testConfigDTO.getTestConfigName());
                testConfig.setApp(app.get());
                testConfig.setGatewayService(service.get());
                testConfig.setDescription(testConfigDTO.getDescription());
                testConfig.setGatewayPathSelection(testConfigDTO.getGatewayPathSelection());
                testConfig.setGatewayReqSelection(testConfigDTO.getGatewayReqSelection());
                testConfig.setEmailId(testConfigDTO.getEmailId());
                testConfig.setSlackId(testConfigDTO.getSlackId());
                testConfig.setMaxRunTimeMin(testConfigDTO.getMaxRunTimeMin());
            });
            this.testConfigRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/testConfig/{id}")
                            .buildAndExpand(testConfigDTO.getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordFoundException("TestConfig with ID '" + testConfigDTO.getId() + "' not found.");
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
