package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.PathDTO;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.repository.PathRepository;
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
import static org.springframework.http.ResponseEntity.created;

@RestController
@RequestMapping("/api/path")
public class PathController {

    private ServiceRepository serviceRepository;
    private PathRepository pathRepository;

    public PathController(PathRepository pathRepository, ServiceRepository serviceRepository ) {
        this.serviceRepository = serviceRepository;
        this.pathRepository = pathRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.pathRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Path> existed = this.pathRepository.findById(id);
        this.pathRepository.delete(existed.get());
        return noContent().build();
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody PathDTO pathDTO, HttpServletRequest request) {
        if (pathDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Path with ID '" + pathDTO.getId() +"' already exists."));
        }

        Optional<Service> service = serviceRepository.findById(pathDTO.getServiceId());
        if (service.isPresent()) {
            Path saved = this.pathRepository.save(
                    Path.builder()
                            .service(service.get())
                            .name(pathDTO.getName())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/path/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("Service with ID '" + pathDTO.getServiceId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody PathDTO pathDTO, HttpServletRequest request) {
        if (pathDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Path id not provided"));
        }
        Optional<Path> existing = pathRepository.findById(pathDTO.getId());
        Optional<Service> service = serviceRepository.findById(pathDTO.getServiceId());
        if (service.isEmpty()) {
            throw new RecordNotFoundException("Service with ID '" + pathDTO.getServiceId() + "' not found.");
        }
        if (existing.isPresent()) {
            existing.ifPresent(path -> {
                path.setService(service.get());
                path.setName(pathDTO.getName());
            });
            this.pathRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/path/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            throw new RecordNotFoundException("ServiceGroup with ID '" + pathDTO.getId() + "' not found.");
        }
    }
}
