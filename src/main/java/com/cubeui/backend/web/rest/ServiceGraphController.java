package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.ServiceGraphDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.ServiceGraph;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
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
@RequestMapping("/api/service_graph")
//@Secured({"ROLE_USER"})
public class ServiceGraphController {

    private AppRepository appRepository;
    private ServiceRepository serviceRepository;
    private ServiceGraphRepository serviceGraphRepository;

    public ServiceGraphController(AppRepository appRepository, ServiceGraphRepository serviceGraphRepository, ServiceRepository serviceRepository) {
        this.appRepository = appRepository;
        this.serviceGraphRepository = serviceGraphRepository;
        this.serviceRepository = serviceRepository;
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceGraphDTO serviceGraphDTO, HttpServletRequest request) {
        if (serviceGraphDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("ServiceGraph with ID '" + serviceGraphDTO.getId() +"' already exists."));
        }

        Optional<App> app = appRepository.findById(serviceGraphDTO.getAppId());
        Optional<Service> fromService = serviceRepository.findById(serviceGraphDTO.getFromServiceId());
        Optional<Service> toService = serviceRepository.findById(serviceGraphDTO.getToServiceId());
        if (app.isPresent() && fromService.isPresent() && toService.isPresent()) {
            ServiceGraph saved = this.serviceGraphRepository.save(
                    ServiceGraph.builder()
                            .app(app.get())
                            .fromService(fromService.get())
                            .toService(toService.get())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service_graph/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("App with ID '" + serviceGraphDTO.getId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody ServiceGraphDTO serviceGraphDTO, HttpServletRequest request) {
        if (serviceGraphDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("ServiceGraph id not provided"));
        }
        Optional<ServiceGraph> existing = serviceGraphRepository.findById(serviceGraphDTO.getId());
        if (existing.isPresent()) {
            Optional.ofNullable(serviceGraphDTO.getAppId()).ifPresent(appId -> {
                Optional<App> app = Optional.ofNullable(appRepository.findById(appId)).get();
                if (app.isPresent()) {
                    existing.get().setApp(app.get());
                } else {
                    throw new RecordNotFoundException("App with ID '" + serviceGraphDTO.getAppId() + "' not found.");
                }
            });
            Optional.ofNullable(serviceGraphDTO.getFromServiceId()).ifPresent(fromServiceId -> {
                Optional<Service> fromService = Optional.ofNullable(serviceRepository.findById(fromServiceId)).get();
                if (fromService.isPresent()) {
                    existing.get().setFromService(fromService.get());
                } else {
                    throw new RecordNotFoundException("Service with ID '" + serviceGraphDTO.getFromServiceId()+ "' not found.");
                }
            });
            Optional.ofNullable(serviceGraphDTO.getToServiceId()).ifPresent(toServiceId -> {
                Optional<Service> toService = Optional.ofNullable(serviceRepository.findById(toServiceId)).get();
                if (toService.isPresent()) {
                    existing.get().setToService(toService.get());
                } else {
                    throw new RecordNotFoundException("Service with ID '" + serviceGraphDTO.getToServiceId()+ "' not found.");
                }
            });
            this.serviceGraphRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service_graph/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("ServiceGraph with ID '" + serviceGraphDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.serviceGraphRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<ServiceGraph> existed = this.serviceGraphRepository.findById(id);
        this.serviceGraphRepository.delete(existed.get());
        return noContent().build();
    }
}
