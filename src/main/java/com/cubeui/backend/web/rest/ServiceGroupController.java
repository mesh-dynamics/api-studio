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

        Optional<App> app = appRepository.findById(serviceGroupDTO.getAppId());
        if (app.isPresent()) {
            ServiceGroup saved = this.serviceGroupRepository.save(
                    ServiceGroup.builder()
                            .app(app.get())
                            .name(serviceGroupDTO.getName())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service-group/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("App with ID '" + serviceGroupDTO.getAppId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceGroupDTO serviceGroupDTO, HttpServletRequest request) {
        if (serviceGroupDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Service id not provided"));
        }
        Optional<ServiceGroup> existing = serviceGroupRepository.findById(serviceGroupDTO.getId());
        Optional<App> app = appRepository.findById(serviceGroupDTO.getAppId());
        if (app.isEmpty()) {
            throw new RecordNotFoundException("App with ID '" + serviceGroupDTO.getAppId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(service -> {
                service.setApp(app.get());
                service.setName(serviceGroupDTO.getName());
            });
            this.serviceGroupRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service-group/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("ServiceGroup with ID '" + serviceGroupDTO.getId() + "' not found.");
        }
    }
}
