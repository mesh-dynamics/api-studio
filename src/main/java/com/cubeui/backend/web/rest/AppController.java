package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.domain.DTO.Response.DTO.TestConfigDTO;
import com.cubeui.backend.domain.DTO.Response.Mapper.TestConfigMapper;
import com.cubeui.backend.repository.*;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/app")
//@Secured({"ROLE_USER"})
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

    public AppController(AppRepository appRepository, ServiceRepository serviceRepository, ServiceGraphRepository serviceGraphRepository, TestConfigRepository testConfigRepository, TestIntermediateServiceRepository testIntermediateServiceRepository, TestVirtualizedServiceRepository testVirtualizedServiceRepository, TestPathRepository testPathRepository, CustomerService customerService, InstanceRepository instanceRepository, InstanceUserRepository instanceUserRepository) {
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
    }

    @GetMapping("")
    public ResponseEntity all(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ok(this.appRepository.findByCustomerId(user.getCustomer().getId()));
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody AppDTO appDTO, HttpServletRequest request) {
        if (appDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("App with ID '" + appDTO.getId() +"' already exists."));
        }
        Optional<Customer> customer = customerService.getById(appDTO.getCustomerId());
        if (customer.isPresent()) {
            App saved = this.appRepository.save(
                    App.builder()
                            .name(appDTO.getName())
                            .customer(customer.get())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/app/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("Customer with ID '" + appDTO.getCustomerId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody AppDTO appDTO, HttpServletRequest request) {
        if (appDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("App id not provided"));
        }
        Optional<App> existing = appRepository.findById(appDTO.getId());
        if (existing.isPresent()) {
            existing.ifPresent(app -> {
                app.setCustomer(customerService.getById(appDTO.getCustomerId()).get());
                app.setName(appDTO.getName());
            });
            this.appRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/app/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("App with ID '" + appDTO.getId() + "' not found.");
        }
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
        return ok(this.appRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<App> existed = this.appRepository.findById(id);
        existed.ifPresent((app) -> this.appRepository.delete(app));
        return noContent().build();
    }
}
