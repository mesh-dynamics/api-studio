/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.web.rest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.Response.Mapper.TestConfigMapper;
import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestIntermediateService;
import com.cubeui.backend.domain.TestPath;
import com.cubeui.backend.domain.TestService;
import com.cubeui.backend.domain.TestVirtualizedService;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.PathRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.repository.TestIntermediateServiceRepository;
import com.cubeui.backend.repository.TestPathRepository;
import com.cubeui.backend.repository.TestServiceRepository;
import com.cubeui.backend.repository.TestVirtualizedServiceRepository;
import com.cubeui.backend.security.Constants;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.TestConfigService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import com.cubeui.readJson.dataModel.TestConfigs;

@RestController
@RequestMapping("/api/test_config")
//@Secured({"ROLE_USER"})
public class TestConfigController {

    private TestConfigRepository testConfigRepository;
    private ServiceGroupRepository serviceGroupRepository;
    private ServiceRepository serviceRepository;
    private AppRepository appRepository;
    private CustomerService customerService;
    private TestIntermediateServiceRepository testIntermediateServiceRepository;
    private TestVirtualizedServiceRepository testVirtualizedServiceRepository;
    private TestPathRepository testPathRepository;
    private TestServiceRepository testServiceRepository;
    private TestConfigService testConfigService;
    private PathRepository pathRepository;

    public TestConfigController(TestConfigRepository testConfigRepository, ServiceGroupRepository serviceGroupRepository, ServiceRepository serviceRepository, AppRepository appRepository,
        CustomerService customerService, TestIntermediateServiceRepository testIntermediateServiceRepository,
        TestVirtualizedServiceRepository testVirtualizedServiceRepository, TestPathRepository testPathRepository, TestServiceRepository testServiceRepository, TestConfigService testConfigService, PathRepository pathRepository) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGroupRepository = serviceGroupRepository;
        this.appRepository = appRepository;
        this.customerService = customerService;
        this.testIntermediateServiceRepository = testIntermediateServiceRepository;
        this.testVirtualizedServiceRepository = testVirtualizedServiceRepository;
        this.testPathRepository = testPathRepository;
        this.testServiceRepository = testServiceRepository;
        this.testConfigService = testConfigService;
        this.pathRepository = pathRepository;
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
            TestConfig saved = this.testConfigService.saveTestConfig(testConfigDTO, app.get());
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
                testConfig.setTag(testConfigDTO.getTag());
                testConfig.setDynamicInjectionConfigVersion(testConfigDTO.getDynamicInjectionConfigVersion());
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
        if (existed.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("TestConfig with ID '" + id +"' does not exist."));
        existed.ifPresent(value -> this.testConfigRepository.delete(value));
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

    @PostMapping("/create/{customerName}/{appName}")
    public ResponseEntity createTestConfig(@RequestBody TestConfigs inputTestConfig,
        @PathVariable String customerName, @PathVariable String appName,
        HttpServletRequest request) {

        return createOrUpdateTestConfig(customerName, appName, inputTestConfig, false, request);
    }

    @PostMapping("/update/{customerName}/{appName}")
    public ResponseEntity updateTestConfig(@RequestBody TestConfigs inputTestConfig,
        @PathVariable String customerName, @PathVariable String appName,
        HttpServletRequest request) {

        return createOrUpdateTestConfig(customerName, appName, inputTestConfig, true, request);
    }

    private ResponseEntity createOrUpdateTestConfig(String customerName, String appName,
        TestConfigs inputTestConfig, boolean update, HttpServletRequest request) {

        Optional<Customer> customer = this.customerService.getByName(customerName);
        if (customer.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("Customer with Name '" + customerName + "' not found."));
        }
        Optional<App> optionalApp = this.appRepository
            .findByNameAndCustomerId(appName, customer.get().getId());
        if (optionalApp.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("App with Name '" + appName + "' not found."));
        }

        if (StringUtils.isBlank(inputTestConfig.getTestConfigName())) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("Invalid Test Config name "));
        }

        App app = optionalApp.get();
        Optional<TestConfig> optionalTestConfig = this.testConfigRepository
            .findByTestConfigNameAndAppId(
                inputTestConfig.getTestConfigName(), app.getId());

        if (!update && optionalTestConfig.isPresent()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse(
                    "Test Config with name '" + inputTestConfig.getTestConfigName()
                        + "' already exists."));
        }

        if (update && optionalTestConfig.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse(
                    "Test Config with name '" + inputTestConfig.getTestConfigName()
                        + "' does not exist."));
        }

        TestConfig testConfig = optionalTestConfig.orElseGet(() -> {
            TestConfigDTO testConfigDTO = new TestConfigDTO();
            testConfigDTO.setTestConfigName(inputTestConfig.getTestConfigName());
            testConfigDTO.setAppId(app.getId());
            return this.testConfigService
                .saveTestConfig(testConfigDTO, app);
        });

        handleServices(inputTestConfig, app, testConfig, update);

        handlePaths(inputTestConfig, app, testConfig, update);

        handleVirtualServices(inputTestConfig, app, testConfig);

        handleIntermediateServices(inputTestConfig, app, testConfig);

        return created(
            ServletUriComponentsBuilder
                .fromContextPath(request)
                .path("/api/test_config/{id}")
                .buildAndExpand(testConfig.getId())
                .toUri())
            .body(testConfig);
    }

    private void handleIntermediateServices(TestConfigs inputTestConfig, App app,
        TestConfig testConfig) {
        if (inputTestConfig.getTest_intermediate_services() != null) {
            for (String testIntermediateService : inputTestConfig.getTest_intermediate_services()) {
                Service savedService = getOrCreateService(testIntermediateService, app);
                //check if intermediate service already part of test path. Handles both create and update.
                this.testIntermediateServiceRepository
                    .findByTestConfigIdAndServiceId(testConfig.getId(), savedService.getId())
                    .orElseGet(() ->
                        this.testConfigService
                            .saveTestIntermediateService(testConfig, savedService));

            }
        }
    }

    private void handleVirtualServices(TestConfigs inputTestConfig, App app,
        TestConfig testConfig) {
        if (inputTestConfig.getTest_virtualized_services() != null) {
            for (String testVirtualizedService : inputTestConfig.getTest_virtualized_services()) {
                Service savedService = getOrCreateService(testVirtualizedService, app);

                //check if virtual service already part of test path. Handles both create and update.
                this.testVirtualizedServiceRepository
                    .findByTestConfigIdAndServiceId(testConfig.getId(), savedService.getId())
                    .orElseGet(() ->
                        this.testConfigService
                            .saveVirtualizedTestService(testConfig, savedService));

            }
        }
    }

    private void handlePaths(TestConfigs inputTestConfig, App app, TestConfig testConfig, boolean update) {
        ServiceGroup savedServiceGroup;

        List<String> inputPaths = inputTestConfig.getPaths();
        List<TestPath> updatedTestPaths = new ArrayList<>();
        if (inputPaths != null) {
            for (String path : inputPaths) {
                Optional<List<Path>> optionalPath = this.pathRepository.findByPath(path);
                if (optionalPath.isEmpty() || optionalPath.get().size() == 0) {
                    //create a default service group if not already present.
                    savedServiceGroup = createDefaultServiceGroup(app);

                    //create a default service if not already present
                    Service savedService = createDefaultService(app, savedServiceGroup);

                    updatedTestPaths.add(this.testConfigService.saveTestPath(testConfig,
                        this.testConfigService.savePath(path, savedService)));

                } else {
                    optionalPath.get().forEach(foundPath ->
                        //check if path already part of test path. Handles both create and update.
                        updatedTestPaths.add(this.testPathRepository
                            .findByTestConfigIdAndPathId(testConfig.getId(), foundPath.getId())
                            .orElseGet(
                                () -> this.testConfigService.saveTestPath(testConfig, foundPath)))
                    );
                }
            }

            //In case of update remove any existing test path that is not part of the input list.
            //input should always be entire list for update.
            if (update) {
                List<TestPath> existingTestPaths = new ArrayList<>(this.testPathRepository
                    .findByTestConfigId(testConfig.getId()).orElseGet(Collections::emptyList));

                existingTestPaths.removeAll(updatedTestPaths);

                if (existingTestPaths.size() > 0) {
                    for (TestPath testPath : existingTestPaths) {
                        this.testPathRepository.deleteTestPathById(testPath.getId());
                    }
                }
            }

        }
    }

    private void handleServices(TestConfigs inputTestConfig, App app, TestConfig testConfig, boolean update) {
        List<String> inputServices = inputTestConfig.getServices();
        List<TestService> updatedServices = new ArrayList<>();
        if (inputServices != null) {
            for (String service : inputServices) {
                Service savedService = getOrCreateService(service, app);
                //check if service already part of test service. Handles both create and update.
                updatedServices.add(this.testServiceRepository
                    .findByTestConfigIdAndServiceId(testConfig.getId(), savedService.getId())
                    .orElseGet(
                        () -> this.testConfigService.saveTestService(testConfig, savedService)));
            }

            //In case of update remove any existing test service that is not part of the input list.
            //input should always be entire list for update.
            if (update) {
                List<TestService> existingTestServices = new ArrayList<>(this.testServiceRepository
                    .findByTestConfigId(testConfig.getId()).orElseGet(Collections::emptyList));

                existingTestServices.removeAll(updatedServices);

                if (existingTestServices.size() > 0) {
                    for (TestService testService : existingTestServices) {
                        this.testServiceRepository.deleteTestServiceById(testService.getId());
                    }
                }
            }
        }

    }

    private Service getOrCreateService(String service, App app) {
        //check if service already present
        Optional<Service> optionalService = this.serviceRepository
            .findByNameAndAppId(service, app.getId());

        return optionalService.orElseGet(() -> {
            //create a default service group if not already present.
            ServiceGroup savedServiceGroup = createDefaultServiceGroup(app);
            return this.testConfigService.saveService(service, app, savedServiceGroup);
        });
    }

    @NotNull
    private Service createDefaultService(App app, ServiceGroup savedServiceGroup) {
        return this.serviceRepository
            .findByNameAndAppId(Constants.DEFAULT_SERVICE, app.getId()).orElseGet(() ->
                this.testConfigService
                    .saveService(Constants.DEFAULT_SERVICE, app, savedServiceGroup));
    }

    @NotNull
    private ServiceGroup createDefaultServiceGroup(App app) {
        return this.serviceGroupRepository
            .findByNameAndAppId(Constants.DEFAULT_SERVICE_GROUP, app.getId()).orElseGet(() ->
                this.testConfigService
                    .saveServiceGroup(Constants.DEFAULT_SERVICE_GROUP, app));
    }
}
