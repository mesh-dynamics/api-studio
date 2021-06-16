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
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFile;
import com.cubeui.backend.domain.AppUser;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.domain.DTO.Response.AppFileResponse;
import com.cubeui.backend.domain.DTO.Response.AppServicePathResponse;
import com.cubeui.backend.domain.DTO.Response.AppServiceResponse;
import com.cubeui.backend.domain.DTO.Response.DTO.TestConfigDTO;
import com.cubeui.backend.domain.DTO.Response.Mapper.TestConfigMapper;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.domain.InstanceUser;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.PathPrefix;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestIntermediateService;
import com.cubeui.backend.domain.TestPath;
import com.cubeui.backend.domain.TestService;
import com.cubeui.backend.domain.TestVirtualizedService;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.AppUserRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.InstanceUserRepository;
import com.cubeui.backend.repository.PathPrefixRepository;
import com.cubeui.backend.repository.PathRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.repository.TestIntermediateServiceRepository;
import com.cubeui.backend.repository.TestPathRepository;
import com.cubeui.backend.repository.TestServiceRepository;
import com.cubeui.backend.repository.TestVirtualizedServiceRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.AppFileStorageService;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import com.cubeui.readJson.dataModel.ServiceGroups;
import com.cubeui.readJson.dataModel.Services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/app")
@Slf4j
public class AppController {

    private AppRepository appRepository;
    private ServiceRepository serviceRepository;
    private ServiceGroupRepository serviceGroupRepository;
    private ServiceGraphRepository serviceGraphRepository;
    private TestConfigRepository testConfigRepository;
    private TestServiceRepository testServiceRepository;
    private TestIntermediateServiceRepository testIntermediateServiceRepository;
    private TestVirtualizedServiceRepository testVirtualizedServiceRepository;
    private TestPathRepository testPathRepository;
    private CustomerService customerService;
    private InstanceRepository instanceRepository;
    private InstanceUserRepository instanceUserRepository;
    private UserService userService;
    private AppUserRepository appUserRepository;
    private Validation validation;
    private AppFileStorageService appFileStorageService;
    private final UserRepository userRepository;
    private final CubeServerService cubeServerService;
    private final PathRepository pathRepository;
    private final PathPrefixRepository pathPrefixRepository;
    public AppController(AppRepository appRepository, ServiceRepository serviceRepository,
        ServiceGroupRepository serviceGroupRepository,
        ServiceGraphRepository serviceGraphRepository, TestConfigRepository testConfigRepository,
        TestServiceRepository testServiceRepository,
        TestIntermediateServiceRepository testIntermediateServiceRepository,
        TestVirtualizedServiceRepository testVirtualizedServiceRepository,
        TestPathRepository testPathRepository, CustomerService customerService,
        InstanceRepository instanceRepository, InstanceUserRepository instanceUserRepository,
        UserService userService, AppUserRepository appUserRepository, Validation validation,
        AppFileStorageService appFileStorageService, UserRepository userRepository, CubeServerService cubeServerService,
        PathRepository pathRepository, PathPrefixRepository pathPrefixRepository) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGroupRepository = serviceGroupRepository;
        this.serviceGraphRepository = serviceGraphRepository;
        this.testConfigRepository = testConfigRepository;
        this.testServiceRepository = testServiceRepository;
        this.testIntermediateServiceRepository = testIntermediateServiceRepository;
        this.testVirtualizedServiceRepository = testVirtualizedServiceRepository;
        this.testPathRepository = testPathRepository;
        this.customerService = customerService;
        this.instanceRepository = instanceRepository;
        this.instanceUserRepository = instanceUserRepository;
        this.userService = userService;
        this.appUserRepository = appUserRepository;
        this.validation = validation;
        this.appFileStorageService = appFileStorageService;
        this.userRepository = userRepository;
        this.cubeServerService = cubeServerService;
        this.pathRepository = pathRepository;
        this.pathPrefixRepository = pathPrefixRepository;
    }

    @GetMapping("")
    public ResponseEntity all(HttpServletRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<List<AppUser>> appUsers = this.appUserRepository.findByUserId(user.getId());
        List<App> apps = appUsers.get().stream().map(AppUser::getApp).collect(Collectors.toList());
        List<String> appNames = apps.stream().map(App::getName).collect(Collectors.toList());
        ResponseEntity<byte[]> responseEntity = cubeServerService.fetchPostResponse(request, Optional.of(appNames),  "/cs/getAppConfigurations/" + user.getCustomer().getName(),
            MediaType.APPLICATION_JSON);
        if(responseEntity.getStatusCode() == HttpStatus.OK) {
            return  ResponseEntity.ok(cubeServerService.getAppResponse(responseEntity, apps));
        }
        return responseEntity;
    }

    @GetMapping("/images")
    public ResponseEntity getImages(HttpServletRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<List<AppUser>> appUsers = this.appUserRepository.findByUserId(user.getId());
        List<Long> appIds = appUsers.map(aus ->
            aus.stream().map(au -> au.getApp().getId()).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
        List<AppFile> files = this.appFileStorageService.getFilesFoAppIds(appIds);
        List<AppFileResponse> appFileResponses = new ArrayList<>(files.size());
        for(AppFile file: files) {
            AppFileResponse appFileResponse = new AppFileResponse(file.getFileName(),
                file.getFileType(), file.getData(), file.getApp().getName());
            appFileResponses.add(appFileResponse);
        }
        return ResponseEntity.ok(appFileResponses);
    }

    @PostMapping("")
    public ResponseEntity save(@RequestParam("app") AppDTO appDTO, HttpServletRequest request, Authentication authentication,
        @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (appDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("App with ID '" + appDTO.getId() +"' already exists."));
        }
        if(appDTO.getDisplayName() == null) return status(FORBIDDEN).body(new ErrorResponse("Mandatory field Name is empty."));
        Optional<Customer> customer = Optional.empty();
        User user = (User)authentication.getPrincipal();
        if(appDTO.getCustomerName() != null) {
            customer = customerService.getByName(appDTO.getCustomerName());
            if(customer.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Customer with name '" + appDTO.getCustomerName() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field CustomerName is empty."));
        }
        validation.validateCustomerName(authentication, customer.get().getName());
        Optional<App> app = this.appRepository.findByDisplayNameAndCustomerId(appDTO.getDisplayName(), customer.get().getId());
        if (app.isPresent())
        {
            log.info("App with same display name exists");
            return ok(app);
        }
        String name = appDTO.getDisplayName().replaceAll("\\s+", "");
        app = this.appRepository.findByNameAndCustomerId(name, customer.get().getId());
        if (app.isPresent())
        {
            log.info("App with same name exists");
            return ok(app);
        }
        App saved = this.appRepository.save(
            App.builder()
                .name(name)
                .customer(customer.get())
                .displayName(appDTO.getDisplayName())
                .userId(user.getUsername())
                .build());

        this.appFileStorageService.storeFile(file, saved);
        this.cubeServerService.saveEmptyTemplateSetForApp(request, saved);
        Optional<List<User>> optionalUsers = this.userRepository.findByCustomerId(customer.get().getId());
        optionalUsers.ifPresent(users -> {
            users.forEach(u -> {
                AppUser appUser = new AppUser();
                appUser.setApp(saved);
                appUser.setUser(u);
                appUserRepository.save(appUser);
            });
        });
        userService.createHistoryForEachUserForAnApp(request, saved);
        return created(
            ServletUriComponentsBuilder
                .fromContextPath(request)
                .path("/api/app/{id}")
                .buildAndExpand(saved.getId())
                .toUri())
            .body(saved);
    }

    @PutMapping("")
    public ResponseEntity update(@RequestParam("app") AppDTO appDTO, HttpServletRequest request, Authentication authentication,
        @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (appDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("App id not provided"));
        }
        Optional<App> existing = appRepository.findById(appDTO.getId());
        if(existing.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + appDTO.getId() + "' not found."));
        Optional<Customer> customer = Optional.empty();
        User user = (User)authentication.getPrincipal();
        if(appDTO.getCustomerName() != null) {
            customer = customerService.getByName(appDTO.getCustomerName());
            if(customer.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Customer with Name '" + appDTO.getCustomerName() + "' not found."));
        }
        App existingApp = existing.get();
        customer.ifPresent(givenCustomer -> {
            validation.validateCustomerName(authentication, givenCustomer.getName());
            existingApp.setCustomer(givenCustomer);
        });
        Long customerId = existingApp.getCustomer().getId();
        Optional.ofNullable(appDTO.getDisplayName()).ifPresent((displayName -> {
            Optional<App> app = this.appRepository.findByDisplayNameAndCustomerIdAndIdNot(displayName, customerId, appDTO.getId());
            if(app.isPresent()){
                throw new DuplicateRecordException("App with same display name exists");
            }
            existingApp.setDisplayName(displayName);
        }));
        existingApp.setUserId(user.getUsername());
        this.appRepository.save(existingApp);
        if(file != null) {
            this.appFileStorageService.deleteFileByAppId(appDTO.getId());
            this.appFileStorageService.storeFile(file, existingApp);
        }
        return created(
            ServletUriComponentsBuilder
                .fromContextPath(request)
                .path("/api/app/{id}")
                .buildAndExpand(existing.get().getId())
                .toUri())
            .body(existing);
    }

    @GetMapping("/{id}/services")
    public ResponseEntity getServices(@PathVariable("id") Long id) {
        Optional<App> selectedApp = appRepository.findById(id);
        if (selectedApp.isPresent()) {
            List<Service> services = this.serviceRepository.findByAppId(id)
                .orElseGet(Collections::emptyList);
            List<AppServiceResponse> response = services.stream().map(service -> {
                Optional<List<PathPrefix>> pathPrefixes = this.pathPrefixRepository
                    .findByServiceId(service.getId());
                List<String> prefixes = pathPrefixes.map(pp -> pp.stream().map(
                    PathPrefix::getPrefix).collect(Collectors.toList()))
                    .orElse(Collections.emptyList());
                return new AppServiceResponse(service, prefixes);
            }).collect(Collectors.toList());
            return ok(response);
        } else {
            throw new RecordNotFoundException("App with ID '" + id + "' not found.");
        }
    }

    @GetMapping("/{id}/services/paths")
    public ResponseEntity getPaths(@PathVariable("id") Long id,
        @QueryParam("serviceName") String serviceName) {
        Optional<App> selectedApp = appRepository.findById(id);
        if (selectedApp.isPresent()) {
            List<Service> existingServices = this.serviceRepository
                .findByAppId(id).orElseGet(Collections::emptyList);
            List<AppServicePathResponse> response = existingServices.stream().filter(
                service -> serviceName == null || service.getName()
                    .equals(serviceName)).map(service -> {
                List<Path> existingPaths = this.pathRepository
                    .findByServiceId(service.getId()).orElseGet(Collections::emptyList);
                List<String> outputPaths = existingPaths.stream().map(Path::getPath)
                    .collect(Collectors.toList());

                return new AppServicePathResponse(service, outputPaths);
            }).collect(Collectors.toList());

            return ok(response);
        } else {
            throw new RecordNotFoundException("App with ID '" + id + "' not found.");
        }
    }

    @GetMapping("/{id}/service-graphs")
    public ResponseEntity getServiceGraphs(@PathVariable("id") Long id) {
        Optional<App> selectedApp = appRepository.findById(id);
        if(selectedApp.isPresent()) {
            return ok(this.serviceGraphRepository.findByAppId(id));
        } else {
            throw new RecordNotFoundException("App with ID '" + id + "' not found.");
        }
    }

    @GetMapping("/{id}/test-configs")
    public ResponseEntity getTestConfigs(@PathVariable("id") Long id) {
        Optional<App> selectedApp = appRepository.findById(id);
        if(selectedApp.isPresent()) {
            List<TestConfigDTO> testConfigDTOs = new ArrayList<>();
            Optional<List<TestConfig>> testConfigs = this.testConfigRepository.findByAppId(id);
            for(TestConfig testConfig : testConfigs.get()) {
                List<TestIntermediateService> testIntermediateServices = this.testIntermediateServiceRepository.findByTestConfigId(testConfig.getId()).orElseGet(Collections::emptyList);
                List<TestVirtualizedService> testVirtualizedServices = this.testVirtualizedServiceRepository.findByTestConfigId(testConfig.getId()).orElseGet(Collections::emptyList);
                List<TestService> testServices = this.testServiceRepository.findByTestConfigId(testConfig.getId()).orElseGet(Collections::emptyList);
                List<TestPath> testPaths = this.testPathRepository.findByTestConfigId(testConfig.getId()).orElseGet(Collections::emptyList);
                TestConfigDTO testConfigDTO = TestConfigMapper.INSTANCE.testConfigToTestConfigDTO(testConfig);

                List<String> testServiceNames = testServices.stream()
                    .map(testService -> testService.getService().getName())
                    .collect(Collectors.toList());

                List<String> testIntermediateServiceNames = testIntermediateServices.stream()
                    .map(testIntermediateService -> testIntermediateService.getService().getName())
                    .collect(Collectors.toList());

                List<String> testVirtualizedServiceNames = testVirtualizedServices.stream()
                    .map(testVirtualizedService -> testVirtualizedService.getService().getName())
                    .collect(Collectors.toList());

                List<String> testPathURLs = testPaths.stream()
                    .map(testPath -> testPath.getPath().getPath())
                    .collect(Collectors.toList());

                testConfigDTO.setTestServices(testServiceNames);
                testConfigDTO.setTestPaths(testPathURLs);
                testConfigDTO.setTestIntermediateServices(testIntermediateServiceNames);
                testConfigDTO.setTestMockServices(testVirtualizedServiceNames);

                testConfigDTOs.add(testConfigDTO);
            }
            return ok(Optional.of(testConfigDTOs));
        } else {
            throw new RecordNotFoundException("App with ID '" + id + "' not found.");
        }
    }

    @GetMapping("/{id}/instances")
    public ResponseEntity getInstances(@PathVariable("id") Long id, Authentication authentication) {
        Optional<App> selectedApp = appRepository.findById(id);
        if(selectedApp.isEmpty()) throw new RecordNotFoundException("App with ID '" + id + "' not found.");
        User user = (User) authentication.getPrincipal();
        List<Instance> instancesList = new ArrayList<Instance>();
        Optional<List<InstanceUser>> instanceUserList = instanceUserRepository.findByUserId(user.getId());
        if(instanceUserList.isEmpty()) return ok(Optional.ofNullable(instancesList));
        List<Instance> instances = this.instanceRepository.findByAppId(id).get();
        instances.forEach(instance -> {
            instanceUserList.get().forEach(instanceUser -> {
                if(instance.getId().equals(instanceUser.getInstance().getId())) instancesList.add(instance);
            });
        });
        return ok(Optional.ofNullable(instancesList));
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        Optional<App> existing = this.appRepository.findById(id);
        return existing.map(app -> {
            Optional<AppFile> appFile = this.appFileStorageService.getFileByAppId(app.getId());
            return appFile.map(af -> ResponseEntity.ok(af)).orElseThrow(() -> new RecordNotFoundException("There is no app image found for given id"));
        }).orElseThrow(() -> new RecordNotFoundException("There is no app found for given id"));
    }


    @DeleteMapping("deleteByDisplayName/{customerId}/{displayName}")
    public ResponseEntity deleteByDisplayName(@PathVariable String customerId,
        @PathVariable String displayName, Authentication authentication) {
        User user = (User)authentication.getPrincipal();
        validation.validateCustomerName(authentication, customerId);
        Optional<Customer> customer = this.customerService.getByName(customerId);
        return customer.map(givenCustomer -> {
            Optional<App> existed = this.appRepository.findByDisplayNameAndCustomerId(displayName, givenCustomer.getId());
            return existed.map((app) -> {
                this.appRepository.delete(app);
                return ResponseEntity.ok(app);
            }).orElseThrow(() -> new RecordNotFoundException("There is no app found for given name"));
        }).orElseThrow(() -> new RecordNotFoundException("There is no customer found for given name"));
    }
}
