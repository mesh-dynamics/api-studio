package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.CompareTemplate;
import com.cubeui.backend.domain.DTO.CompareTemplateDTO;
import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.CompareTemplateRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
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
@RequestMapping("/api/compare_template")
//@Secured({"ROLE_USER"})
public class CompareTemplateController {

    private CompareTemplateRepository compareTemplateRepository;
    private ServiceRepository serviceRepository;
    private AppRepository appRepository;

    public CompareTemplateController(CompareTemplateRepository compareTemplateRepository, ServiceRepository serviceRepository, AppRepository appRepository) {
        this.compareTemplateRepository = compareTemplateRepository;
        this.serviceRepository = serviceRepository;
        this.appRepository = appRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.compareTemplateRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody CompareTemplateDTO compareTemplateDTO, HttpServletRequest request) {
        if (compareTemplateDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("CompareTemplate with ID '" + compareTemplateDTO.getId() +"' already exists."));
        }
        Optional<Service> service = serviceRepository.findById(compareTemplateDTO.getServiceId());
        Optional<App> app = appRepository.findById(compareTemplateDTO.getAppId());
        if (service.isPresent() && app.isPresent()) {
            CompareTemplate saved = this.compareTemplateRepository.save(
                    CompareTemplate.builder()
                            .app(app.get())
                            .service(service.get())
                            .path(compareTemplateDTO.getPath())
                            .template(compareTemplateDTO.getTemplate())
                            .type(compareTemplateDTO.getType())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/compare_template/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            if (service.isEmpty()){
                throw new RecordFoundException("Service with ID '" + compareTemplateDTO.getServiceId() + "' not found.");
            } else {
                throw new RecordFoundException("App with ID '" + compareTemplateDTO.getAppId() + "' not found.");
            }
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody CompareTemplateDTO compareTemplateDTO, HttpServletRequest request) {
        if (compareTemplateDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("TestConfig id not provided"));
        }
        Optional<CompareTemplate> existing = compareTemplateRepository.findById(compareTemplateDTO.getId());
        Optional<Service> service = serviceRepository.findById(compareTemplateDTO.getServiceId());
        Optional<App> app = appRepository.findById(compareTemplateDTO.getAppId());
        if (service.isEmpty()){
            throw new RecordFoundException("Service with ID '" + compareTemplateDTO.getServiceId() + "' not found.");
        }
        if (app.isEmpty()) {
            throw new RecordFoundException("App with ID '" + compareTemplateDTO.getAppId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(compareTemplate -> {
                compareTemplate.setApp(app.get());
                compareTemplate.setService(service.get());
                compareTemplate.setPath(compareTemplateDTO.getPath());
                compareTemplate.setTemplate(compareTemplateDTO.getTemplate());
                compareTemplate.setType(compareTemplateDTO.getType());
            });
            this.compareTemplateRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/compare_template/{id}")
                            .buildAndExpand(compareTemplateDTO.getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordFoundException("CompareTemplate with ID '" + compareTemplateDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.compareTemplateRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<CompareTemplate> existed = this.compareTemplateRepository.findById(id);
        this.compareTemplateRepository.delete(existed.get());
        return noContent().build();
    }
}
