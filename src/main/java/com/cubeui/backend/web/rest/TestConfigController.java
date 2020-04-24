package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/test_config")
//@Secured({"ROLE_USER"})
public class TestConfigController {

    private TestConfigRepository testConfigRepository;
    private ServiceRepository serviceRepository;
    private AppRepository appRepository;
    private CustomerService customerService;

    public TestConfigController(TestConfigRepository testConfigRepository, ServiceRepository serviceRepository, AppRepository appRepository,
                CustomerService customerService) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.appRepository = appRepository;
        this.customerService = customerService;
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody TestConfigDTO testConfigDTO, HttpServletRequest request) {
        if (testConfigDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig with ID '" + testConfigDTO.getId() +"' already exists."));
        }
        Optional<Service> service = serviceRepository.findById(testConfigDTO.getGatewayServiceId());
        Optional<App> app = appRepository.findById(testConfigDTO.getAppId());
        if (service.isPresent() && app.isPresent() &&
                StringUtils.isNotBlank(testConfigDTO.getTestConfigName())) {
            Optional<TestConfig> testConfig = this.testConfigRepository.findByTestConfigNameAndAppId(
                    testConfigDTO.getTestConfigName(), testConfigDTO.getAppId());
            if (testConfig.isPresent()) {
                return ok(testConfig);
            }
            TestConfig saved = this.testConfigRepository.save(
                    TestConfig.builder()
                            .testConfigName(testConfigDTO.getTestConfigName())
                            .app(app.get())
                            .gatewayService(service.get())
                            .description(testConfigDTO.getDescription())
                            .gatewayReqSelection(testConfigDTO.getGatewayReqSelection())
                            .maxRunTimeMin(testConfigDTO.getMaxRunTimeMin())
                            .emailId(testConfigDTO.getEmailId())
                            .slackId(testConfigDTO.getSlackId())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/test_config/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            if (service.isEmpty()){
                throw new RecordNotFoundException("Service with ID '" + testConfigDTO.getGatewayServiceId() + "' not found.");
            } else {
                throw new RecordNotFoundException("App with ID '" + testConfigDTO.getAppId() + "' not found.");
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
            throw new RecordNotFoundException("Service with ID '" + testConfigDTO.getGatewayServiceId() + "' not found.");
        }
        if (app.isEmpty()) {
            throw new RecordNotFoundException("App with ID '" + testConfigDTO.getAppId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(testConfig -> {
                testConfig.setTestConfigName(testConfigDTO.getTestConfigName());
                testConfig.setApp(app.get());
                testConfig.setGatewayService(service.get());
                testConfig.setDescription(testConfigDTO.getDescription());
                testConfig.setGatewayReqSelection(testConfigDTO.getGatewayReqSelection());
                testConfig.setEmailId(testConfigDTO.getEmailId());
                testConfig.setSlackId(testConfigDTO.getSlackId());
                testConfig.setMaxRunTimeMin(testConfigDTO.getMaxRunTimeMin());
            });
            this.testConfigRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/test_config/{id}")
                            .buildAndExpand(testConfigDTO.getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("TestConfig with ID '" + testConfigDTO.getId() + "' not found.");
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

    @GetMapping("/{customerName}/{appName}/{testConfigName}")
    public ResponseEntity getTestConfigByCustAppTestConfigName(@PathVariable String customerName,
                    @PathVariable String appName, @PathVariable String testConfigName) {
        Optional<Customer> customer = this.customerService.getByName(customerName);
        if (customer.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Customer with Name '" + customerName + "' not found."));
        Optional<App> app = this.appRepository.findByNameAndCustomerId(appName, customer.get().getId());
        if (app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with Name '" + appName + "' not found."));
        Optional<TestConfig> testConfig = this.testConfigRepository.findByTestConfigNameAndAppId(testConfigName,app.get().getId());
        if (testConfig.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("TestCOnfig with Name '" + testConfigName + "' not found."));
        return ok(testConfig);
    }
}
