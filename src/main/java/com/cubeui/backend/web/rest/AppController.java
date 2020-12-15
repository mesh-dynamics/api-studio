package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.domain.DTO.Response.DTO.TestConfigDTO;
import com.cubeui.backend.domain.DTO.Response.Mapper.TestConfigMapper;
import com.cubeui.backend.domain.enums.Role;
import com.cubeui.backend.repository.*;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.service.AppFileStorageService;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.io.IOException;
import java.util.UUID;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/app")
@Slf4j
public class AppController {

    private AppRepository appRepository;
    private ServiceRepository serviceRepository;
    private ServiceGraphRepository serviceGraphRepository;
    private TestConfigRepository testConfigRepository;
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
    public AppController(AppRepository appRepository, ServiceRepository serviceRepository,
        ServiceGraphRepository serviceGraphRepository, TestConfigRepository testConfigRepository,
        TestIntermediateServiceRepository testIntermediateServiceRepository,
        TestVirtualizedServiceRepository testVirtualizedServiceRepository,
        TestPathRepository testPathRepository, CustomerService customerService,
        InstanceRepository instanceRepository, InstanceUserRepository instanceUserRepository,
        UserService userService, AppUserRepository appUserRepository, Validation validation,
        AppFileStorageService appFileStorageService, UserRepository userRepository, CubeServerService cubeServerService) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGraphRepository = serviceGraphRepository;
        this.testConfigRepository = testConfigRepository;
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
    }

    @GetMapping("")
    public ResponseEntity all(HttpServletRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<List<AppUser>> appUsers = this.appUserRepository.findByUserId(user.getId());
        List<App> apps = appUsers.get().stream().map(AppUser::getApp).collect(Collectors.toList());
        List<Long> appIds = apps.stream().map(App::getId).collect(Collectors.toList());
        List<String> appNames = apps.stream().map(App::getName).collect(Collectors.toList());
        ResponseEntity responseEntity = cubeServerService.fetchPostResponse(request, Optional.of(appNames),  "/cs/getAppConfigurations/" + user.getCustomer().getName(),
            MediaType.APPLICATION_JSON);
        List<AppFile> files = this.appFileStorageService.getFilesFoAppIds(appIds);
        if(responseEntity.getStatusCode() == HttpStatus.OK) {
           return  ResponseEntity.ok(cubeServerService.getAppFileResponse(responseEntity, files));
        }
        return responseEntity;
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
        if(!user.getRoles().contains(Role.ROLE_ADMIN)) {
            validation.validateCustomerName(authentication, customer.get().getName());
        }
        Optional<App> app = this.appRepository.findByDisplayNameAndCustomerId(appDTO.getDisplayName(), customer.get().getId());
        if (app.isPresent())
        {
            log.info("App with same display name exists");
            return ok(app);
        }
        String name = UUID.randomUUID().toString();
        App saved = this.appRepository.save(
                App.builder()
                        .name(name)
                        .customer(customer.get())
                        .displayName(appDTO.getDisplayName())
                        .userId(user.getUsername())
                        .build());
        this.appFileStorageService.storeFile(file, saved);
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
            if(!user.getRoles().contains(Role.ROLE_ADMIN)) {
                validation.validateCustomerName(authentication, givenCustomer.getName());
            }
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
            this.appFileStorageService.storeFile(file, existing.get());
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
        if(selectedApp.isPresent()) {
            return ok(this.serviceRepository.findByAppId(id));
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
            List<TestConfigDTO> testConfigDTOs = new ArrayList<TestConfigDTO>();
            Optional<List<TestConfig>> testConfigs = this.testConfigRepository.findByAppId(id);
            for(TestConfig testConfig : testConfigs.get()) {
                Optional<List<TestIntermediateService>> testIntermediateServices = this.testIntermediateServiceRepository.findByTestConfigId(testConfig.getId());
                Optional<List<TestVirtualizedService>> testVirtualizedServices = this.testVirtualizedServiceRepository.findByTestConfigId(testConfig.getId());
                Optional<List<TestPath>> testPaths = this.testPathRepository.findByTestConfigId(testConfig.getId());
                TestConfigDTO testConfigDTO = TestConfigMapper.INSTANCE.testConfigToTestConfigDTO(testConfig);
                List<String> testIntermediateServiceNames = new ArrayList<String>();
                List<String> testVirtualizedServiceNames = new ArrayList<String>();
                List<String> testPathURLS = new ArrayList<String>();
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
                testConfigDTO.setTestIntermediateServices(testIntermediateServiceNames);
                testConfigDTO.setTestMockServices(testVirtualizedServiceNames);
                testConfigDTO.setTestPaths(testPathURLS);
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
        if(!user.getRoles().contains(Role.ROLE_ADMIN)) {
            validation.validateCustomerName(authentication, customerId);
        }
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
