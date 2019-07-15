package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.ServiceDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
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
    private ServiceGroupRepository serviceGroupRepository;

    public ServiceController(AppRepository appRepository, ServiceRepository serviceRepository, ServiceGroupRepository serviceGroupRepository) {
        this.appRepository = appRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGroupRepository = serviceGroupRepository;
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request) {
        if (serviceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service with ID '" + serviceDTO.getId() +"' already exists."));
        }

        Optional<App> app = appRepository.findById(serviceDTO.getAppId());
        Optional<ServiceGroup> serviceGroup = serviceGroupRepository.findById(serviceDTO.getServiceGroupId());
        if (app.isPresent() && serviceGroup.isPresent()) {
            Service saved = this.serviceRepository.save(
                    Service.builder()
                            .app(app.get())
                            .serviceGroup(serviceGroup.get())
                            .name(serviceDTO.getName())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("App with ID '" + serviceDTO.getAppId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request) {
        if (serviceDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service id not provided"));
        }
        Optional<Service> existing = serviceRepository.findById(serviceDTO.getId());
        Optional<App> app = appRepository.findById(serviceDTO.getAppId());
        Optional<ServiceGroup> serviceGroup = serviceGroupRepository.findById(serviceDTO.getServiceGroupId());
        if (app.isEmpty()) {
            throw new RecordNotFoundException("App with ID '" + serviceDTO.getAppId() + "' not found.");
        }
        if(serviceGroup.isEmpty()) {
            throw new RecordNotFoundException("ServiceGroup with ID '" + serviceDTO.getServiceGroupId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(service -> {
                service.setApp(app.get());
                service.setServiceGroup(serviceGroup.get());
                service.setName(serviceDTO.getName());
            });
            this.serviceRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("Service with ID '" + serviceDTO.getId() + "' not found.");
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
