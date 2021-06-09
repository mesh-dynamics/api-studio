package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.ServiceGroupDTO;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.http.ResponseEntity.created;

@RestController
@RequestMapping("/api/service-group")
public class ServiceGroupController {

    private AppRepository appRepository;
    private ServiceGroupRepository serviceGroupRepository;

    public ServiceGroupController(AppRepository appRepository, ServiceGroupRepository serviceGroupRepository) {
        this.appRepository = appRepository;
        this.serviceGroupRepository = serviceGroupRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.serviceGroupRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<ServiceGroup> existed = this.serviceGroupRepository.findById(id);
        this.serviceGroupRepository.delete(existed.get());
        return noContent().build();
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceGroupDTO serviceGroupDTO, HttpServletRequest request) {
        if (serviceGroupDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("ServiceGroup with ID '" + serviceGroupDTO.getId() +"' already exists."));
        }
        Optional<App> app = null;
        if(serviceGroupDTO.getAppId() != null) {
            app = appRepository.findById(serviceGroupDTO.getAppId());
            if(app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + serviceGroupDTO.getAppId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field App Id is empty."));
        }
        Optional<ServiceGroup> serviceGroup = this.serviceGroupRepository.findByNameAndAppId(serviceGroupDTO.getName(), serviceGroupDTO.getAppId());
        if (serviceGroup.isPresent()) {
            return ok(serviceGroup);
        }
        ServiceGroup saved = this.serviceGroupRepository.save(
                ServiceGroup.builder()
                        .app(app.get())
                        .name(serviceGroupDTO.getName())
                        .build());
        return created(
                ServletUriComponentsBuilder
                        .fromContextPath(request)
                        .path(request.getServletPath() + "/{id}")
                        .buildAndExpand(saved.getId())
                        .toUri())
                .body(saved);
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceGroupDTO serviceGroupDTO, HttpServletRequest request) {
        if (serviceGroupDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service id not provided"));
        }
        Optional<ServiceGroup> existing = serviceGroupRepository.findById(serviceGroupDTO.getId());
        Optional<App> app = null;
        if(serviceGroupDTO.getAppId() != null) {
            app = appRepository.findById(serviceGroupDTO.getAppId());
            if(app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + serviceGroupDTO.getAppId() + "' not found."));
        }
        if (existing.isPresent()) {
            existing.get().setApp(app.get());
            Optional.ofNullable(serviceGroupDTO.getName()).ifPresent(updatedName -> {
                existing.get().setName(updatedName);
            });
            this.serviceGroupRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path(request.getServletPath() + "/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("ServiceGroup with ID '" + serviceGroupDTO.getId() + "' not found."));
        }
    }
}
