package com.cubeui.backend.web.rest;

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
import com.cubeui.backend.service.utils.TestConfigService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import com.cubeui.readJson.dataModel.TestConfigs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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
    private TestConfigService testConfigService;
    private PathRepository pathRepository;
    private ServiceGroupRepository serviceGroupRepository;

    public TestConfigController(TestConfigRepository testConfigRepository, ServiceRepository serviceRepository, AppRepository appRepository,
                CustomerService customerService, TestIntermediateServiceRepository testIntermediateServiceRepository,
                TestVirtualizedServiceRepository testVirtualizedServiceRepository, TestPathRepository testPathRepository, TestServiceRepository testServiceRepository, TestConfigService testConfigService, PathRepository pathRepository, ServiceGroupRepository serviceGroupRepository) {
        this.testConfigRepository = testConfigRepository;
        this.serviceRepository = serviceRepository;
        this.appRepository = appRepository;
        this.customerService = customerService;
        this.testIntermediateServiceRepository = testIntermediateServiceRepository;
        this.testVirtualizedServiceRepository = testVirtualizedServiceRepository;
        this.testPathRepository = testPathRepository;
        this.testServiceRepository = testServiceRepository;
        this.pathRepository = pathRepository;
        this.serviceGroupRepository = serviceGroupRepository;
        this.testConfigService = testConfigService;
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

    @PostMapping("/create/{customerName}/{appName}")
    public ResponseEntity createOrUpdateTestConfig(@RequestBody TestConfigs inputTestConfig,
        @PathVariable String customerName, @PathVariable String appName,
        HttpServletRequest request) {
        Optional<Customer> customer = this.customerService.getByName(customerName);
        if (customer.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("Customer with Name '" + customerName + "' not found."));
        }
        Optional<App> app = this.appRepository
            .findByNameAndCustomerId(appName, customer.get().getId());
        if (app.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("App with Name '" + appName + "' not found."));
        }

        if (StringUtils.isBlank(inputTestConfig.getTestConfigName())) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("Invalid Test Config name "));
        }

        Optional<TestConfig> config = this.testConfigRepository.findByTestConfigNameAndAppId(
            inputTestConfig.getTestConfigName(), app.get().getId());

        if (config.isPresent()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse(
                    "Test Config with name '" + inputTestConfig.getTestConfigName()
                        + "'already exists."));
        }

        TestConfigDTO testConfigDTO = new TestConfigDTO();
        testConfigDTO.setTestConfigName(inputTestConfig.getTestConfigName());
        testConfigDTO.setAppId(app.get().getId());

        TestConfig savedTestConfig = this.testConfigService
            .saveTestConfig(testConfigDTO, app.get());

        createOrUpdateTestConfig(inputTestConfig, savedTestConfig, app.get());

        return created(
            ServletUriComponentsBuilder
                .fromContextPath(request)
                .path("/api/test_config/{id}")
                .buildAndExpand(savedTestConfig.getId())
                .toUri())
            .body(savedTestConfig);
    }

    @PostMapping("/update/{customerName}/{appName}")
    public ResponseEntity updateTestConfig(@RequestBody TestConfigs inputTestConfig,
        @PathVariable String customerName, @PathVariable String appName,
        HttpServletRequest request) {
        Optional<Customer> customer = this.customerService.getByName(customerName);
        if (customer.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("Customer with Name '" + customerName + "' not found."));
        }
        Optional<App> app = this.appRepository
            .findByNameAndCustomerId(appName, customer.get().getId());
        if (app.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("App with Name '" + appName + "' not found."));
        }

        if (StringUtils.isBlank(inputTestConfig.getTestConfigName())) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse("Invalid Test Config name "));
        }

        Optional<TestConfig> testConfig = this.testConfigRepository.findByTestConfigNameAndAppId(
            inputTestConfig.getTestConfigName(), app.get().getId());

        if (testConfig.isEmpty()) {
            return status(BAD_REQUEST)
                .body(new ErrorResponse(
                    "Test Config with name '" + inputTestConfig.getTestConfigName()
                        + "'does not exist."));
        }

        createOrUpdateTestConfig(inputTestConfig, testConfig.get(), app.get());

        return ok(testConfig.get());
    }

    private void createOrUpdateTestConfig(TestConfigs inputTestConfig, TestConfig testConfig,
        App app) {
        ServiceGroup savedServiceGroup = null;
        Service savedService = null;
        MultiValuedMap<String, Long> pathMap = new ArrayListValuedHashMap<>();
        for (String service : inputTestConfig.getServices()) {
            savedService = getOrCreateService(service, app);

            //check if service already part of test service. Handles both create and update.
            Optional<TestService> optionalTestService = this.testServiceRepository
                .findByTestConfigIdAndServiceId(testConfig.getId(), savedService.getId());
            optionalTestService
                .orElse(this.testConfigService.saveTestService(testConfig, savedService));

            Optional<List<Path>> optionalPaths = this.pathRepository
                .findByServiceId(savedService.getId());
            if (optionalPaths.isPresent()) {
                for (Path path : optionalPaths.get()) {
                    pathMap.put(path.getPath(), path.getId());
                }
            }

        }

        for (String path : inputTestConfig.getPaths()) {
            if (pathMap.containsKey(path)) {
                for (Long pathId : pathMap.get(path)) {
                    Optional<Path> optionalPath = this.pathRepository.findById(pathId);
                    optionalPath.ifPresent(
                        foundPath -> {
                            //check if path already part of test path. Handles both create and update.
                            Optional<TestPath> optionalTestPath = this.testPathRepository
                                .findByTestConfigIdAndPathId(testConfig.getId(),
                                    optionalPath.get().getId());
                            optionalTestPath
                                .orElse(this.testConfigService.saveTestPath(testConfig, foundPath));
                        });
                }
            } else {
                Optional<List<Path>> optionalPath = this.pathRepository.findByPath(path);
                if (optionalPath.isEmpty() || optionalPath.get().size() == 0) {
                    //create a default service group if not already present.
                    savedServiceGroup = createDefaultServiceGroup(app);

                    //create a default service if not already present
                    savedService = createDefaultService(app, savedServiceGroup);

                    this.testConfigService.saveTestPath(testConfig,
                        this.testConfigService.savePath(path, savedService));

                } else {
                    optionalPath.get().forEach(foundPath -> {
                        //check if path already part of test path. Handles both create and update.
                        Optional<TestPath> optionalTestPath = this.testPathRepository
                            .findByTestConfigIdAndPathId(testConfig.getId(), foundPath.getId());
                        optionalTestPath
                            .orElse(this.testConfigService.saveTestPath(testConfig, foundPath));
                    });
                }
            }
        }

        for (String testVirtualizedService : inputTestConfig.getTest_virtualized_services()) {
            savedService = getOrCreateService(testVirtualizedService, app);

            //check if virtual service already part of test path. Handles both create and update.
            Optional<TestVirtualizedService> optionalTestVirtualizedService = this.testVirtualizedServiceRepository
                .findByTestConfigIdAndServiceId(testConfig.getId(), savedService.getId());
            optionalTestVirtualizedService.orElse(
                this.testConfigService.saveVirtualizedTestService(testConfig, savedService));

        }

        for (String testIntermediateService : inputTestConfig.getTest_intermediate_services()) {
            savedService = getOrCreateService(testIntermediateService, app);

            //check if intermediate service already part of test path. Handles both create and update.
            Optional<TestIntermediateService> optionalTestIntermediateService = this.testIntermediateServiceRepository
                .findByTestConfigIdAndServiceId(testConfig.getId(), savedService.getId());
            optionalTestIntermediateService.orElse(
                this.testConfigService.saveTestIntermediateService(testConfig, savedService));

        }
    }

    private Service getOrCreateService(String service, App app) {
        ServiceGroup savedServiceGroup = null;
        //check if service already present
        Optional<Service> optionalService = this.serviceRepository
            .findByNameAndAppId(service, app.getId());

        if (optionalService.isEmpty()) {
            //create a default service group if not already present.
            savedServiceGroup = createDefaultServiceGroup(app);
        }

        return optionalService
            .orElse(this.testConfigService.saveService(service, app, savedServiceGroup));
    }

    @NotNull
    private Service createDefaultService(App app, ServiceGroup savedServiceGroup) {
        Service savedService;
        Optional<Service> defaultService = this.serviceRepository
            .findByNameAndAppId(Constants.DEFAULT_SERVICE, app.getId());
        savedService = defaultService
            .orElse(this.testConfigService
                .saveService(Constants.DEFAULT_SERVICE, app, savedServiceGroup));
        return savedService;
    }

    @NotNull
    private ServiceGroup createDefaultServiceGroup(App app) {
        ServiceGroup savedServiceGroup;
        Optional<ServiceGroup> defaultServiceGroup = this.serviceGroupRepository
            .findByNameAndAppId(Constants.DEFAULT_SERVICE_GROUP, app.getId());
        savedServiceGroup = defaultServiceGroup
            .orElse(this.testConfigService
                .saveServiceGroup(Constants.DEFAULT_SERVICE_GROUP, app));
        return savedServiceGroup;
    }
}
