package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
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
    private CustomerService customerService;

    public AppController(AppRepository appRepository, ServiceRepository serviceRepository, ServiceGraphRepository serviceGraphRepository, TestConfigRepository testConfigRepository, CustomerService customerService) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGraphRepository = serviceGraphRepository;
        this.testConfigRepository = testConfigRepository;
        this.customerService = customerService;
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
            return ok(this.testConfigRepository.findByAppId(id));
        } else {
            throw new RecordNotFoundException("App with ID '" + id + "' not found.");
        }
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
