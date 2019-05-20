package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.domain.DTO.ServiceDTO;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.service.UserService;
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
@RequestMapping("/api/service")
//@Secured({"ROLE_USER"})
public class ServiceController {

    private AppRepository appRepository;
    private ServiceRepository serviceRepository;

    public ServiceController(AppRepository appRepository, ServiceRepository serviceRepository) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.serviceRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request) {
        if (serviceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service with ID '" + serviceDTO.getId() +"' already exists."));
        }

        Optional<App> app = appRepository.findById(serviceDTO.getAppId());
        if (app.isPresent()) {
            Service saved = this.serviceRepository.save(
                    Service.builder().app(app.get()).name(serviceDTO.getName()).type(serviceDTO.getType()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordFoundException("User with ID '" + serviceDTO.getId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request) {
        if (serviceDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service id not provided"));
        }
        Optional<Service> service = serviceRepository.findById(serviceDTO.getId());
        Optional<App> app = appRepository.findById(serviceDTO.getAppId());
        if (app.isEmpty()){
            throw new RecordFoundException("App with ID '" + serviceDTO.getAppId() + "' not found.");
        }
        if (service.isPresent()) {
            service.ifPresent(service1 -> {
                service1.setApp(app.get());
                service1.setName(serviceDTO.getName());
                service1.setType(serviceDTO.getType());
            });
            this.appRepository.save(app.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service/{id}")
                            .buildAndExpand(app.get().getId())
                            .toUri())
                    .body(service);
        } else {
            throw new RecordFoundException("Service with ID '" + serviceDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.serviceRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Service> existed = this.serviceRepository.findById(id);
        this.serviceRepository.delete(existed.get());
        return noContent().build();
    }
}
