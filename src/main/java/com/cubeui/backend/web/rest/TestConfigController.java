package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.Response.Mapper.TestConfigMapper;
import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestIntermediateService;
import com.cubeui.backend.domain.TestPath;
import com.cubeui.backend.domain.TestService;
import com.cubeui.backend.domain.TestVirtualizedService;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.repository.TestIntermediateServiceRepository;
import com.cubeui.backend.repository.TestPathRepository;
import com.cubeui.backend.repository.TestServiceRepository;
import com.cubeui.backend.repository.TestVirtualizedServiceRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
    private TestIntermediateServiceRepository testIntermediateServiceRepository;
    private TestVirtualizedServiceRepository testVirtualizedServiceRepository;
    private TestPathRepository testPathRepository;
    private TestServiceRepository testServiceRepository;

    public TestConfigController(TestConfigRepository testConfigRepository, ServiceRepository serviceRepository, AppRepository appRepository,
                CustomerService customerService, TestIntermediateServiceRepository testIntermediateServiceRepository,
                TestVirtualizedServiceRepository testVirtualizedServiceRepository, TestPathRepository testPathRepository, TestServiceRepository testServiceRepository) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.appRepository = appRepository;
        this.customerService = customerService;
        this.testIntermediateServiceRepository = testIntermediateServiceRepository;
        this.testVirtualizedServiceRepository = testVirtualizedServiceRepository;
        this.testPathRepository = testPathRepository;
        this.testServiceRepository = testServiceRepository;
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody TestConfigDTO testConfigDTO, HttpServletRequest request) {
        if (testConfigDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig with ID '" + testConfigDTO.getId() +"' already exists."));
        }
        Optional<App> app = appRepository.findById(testConfigDTO.getAppId());
        if (app.isPresent() &&
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
                            //.gatewayService(service.get())
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
            throw new RecordNotFoundException("App with ID '" + testConfigDTO.getAppId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody TestConfigDTO testConfigDTO, HttpServletRequest request) {
        if (testConfigDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig id not provided"));
        }
        Optional<TestConfig> existing = testConfigRepository.findById(testConfigDTO.getId());
        Optional<App> app = appRepository.findById(testConfigDTO.getAppId());
        if (app.isEmpty()) {
            throw new RecordNotFoundException("App with ID '" + testConfigDTO.getAppId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(testConfig -> {
                testConfig.setTestConfigName(testConfigDTO.getTestConfigName());
                testConfig.setApp(app.get());
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
        TestConfig testConfigValue = testConfig.get();
        com.cubeui.backend.domain.DTO.Response.DTO.TestConfigDTO testConfigDTO = TestConfigMapper.INSTANCE.testConfigToTestConfigDTO(testConfigValue);
        Optional<List<TestIntermediateService>> testIntermediateServices = this.testIntermediateServiceRepository.findByTestConfigId(testConfigValue.getId());
        Optional<List<TestVirtualizedService>> testVirtualizedServices = this.testVirtualizedServiceRepository.findByTestConfigId(testConfigValue.getId());
        Optional<List<TestPath>> testPaths = this.testPathRepository.findByTestConfigId(testConfigValue.getId());
        Optional<List<TestService>> testServices = this.testServiceRepository.findByTestConfigId(testConfigValue.getId());
        List<String> testIntermediateServiceNames = new ArrayList<String>();
        List<String> testVirtualizedServiceNames = new ArrayList<String>();
        Set<String> testPathURLS = new HashSet<>();
        List<String> testServiceValues = new ArrayList<>();
        if(testIntermediateServices.isPresent()) {
            for(TestIntermediateService testIntermediateService : testIntermediateServices.get()) {
                testIntermediateServiceNames.add(testIntermediateService.getService().getName());
            }
        }
        if(testVirtualizedServices.isPresent()) {
            for(TestVirtualizedService testVirtualizedService : testVirtualizedServices.get()) {
                testVirtualizedServiceNames.add(testVirtualizedService.getService().getName());
            }
        }
        if(testPaths.isPresent()) {
            for(TestPath testPath : testPaths.get()) {
                testPathURLS.add(testPath.getPath().getPath());
            }
        }
        if(testServices.isPresent()) {
            for(TestService testService : testServices.get()) {
                testServiceValues.add(testService.getService().getName());
            }
        }

        testConfigDTO.setTestIntermediateServices(testIntermediateServiceNames);
        testConfigDTO.setTestMockServices(testVirtualizedServiceNames);
        testConfigDTO.setTestPaths(testPathURLS.stream().collect(Collectors.toList()));
        testConfigDTO.setTestServices(testServiceValues);
        return ok(testConfigDTO);
    }
}
