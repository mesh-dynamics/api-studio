package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.ServiceGraphDTO;
import com.cubeui.backend.domain.ServiceGraph;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.RecordNotFoundException;
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
    private ServiceGraphRepository serviceGraphRepository;

    public ServiceGraphController(AppRepository appRepository, ServiceGraphRepository serviceGraphRepository) {
        this.appRepository = appRepository;
        this.serviceGraphRepository = serviceGraphRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.serviceGraphRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody ServiceGraphDTO serviceGraphDTO, HttpServletRequest request) {
        if (serviceGraphDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("ServiceGraph with ID '" + serviceGraphDTO.getId() +"' already exists."));
        }

        Optional<App> app = appRepository.findById(serviceGraphDTO.getAppId());
        if (app.isPresent()) {
            ServiceGraph saved = this.serviceGraphRepository.save(
                    ServiceGraph.builder().app(app.get()).serviceGraph(serviceGraphDTO.getServiceGraph()).build());
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
        Optional<App> app = appRepository.findById(serviceGraphDTO.getAppId());
        if (app.isEmpty()){
            throw new RecordNotFoundException("App with ID '" + serviceGraphDTO.getAppId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(serviceGraph -> {
                serviceGraph.setApp(app.get());
                serviceGraph.setServiceGraph(serviceGraphDTO.getServiceGraph());
            });
            this.serviceGraphRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/service_graph/{id}")
                            .buildAndExpand(app.get().getId())
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
