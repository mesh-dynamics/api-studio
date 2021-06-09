package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.PathDTO;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.repository.PathRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;
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
        Optional<Service> service =  null;
        if(pathDTO.getServiceId() != null) {
            service = serviceRepository.findById(pathDTO.getServiceId());
            if (service.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Service with ID '" + pathDTO.getServiceId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field Service Id is empty."));
        }
        Optional<Path> path = this.pathRepository.findByPathAndServiceId(pathDTO.getPath(), pathDTO.getServiceId());
        if(path.isPresent()) {
            return ok(path);
        }
        Path saved = this.pathRepository.save(
                Path.builder()
                        .service(service.get())
                        .path(pathDTO.getPath())
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
    public ResponseEntity update(@RequestBody PathDTO pathDTO, HttpServletRequest request) {
        if (pathDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Path id not provided"));
        }
        Optional<Path> existing = pathRepository.findById(pathDTO.getId());
        Optional<Service> service =  null;
        if(pathDTO.getServiceId() != null) {
            service = serviceRepository.findById(pathDTO.getServiceId());
            if (service.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Service with ID '" + pathDTO.getServiceId() + "' not found."));
        }
        if (existing.isPresent()) {
            existing.get().setService(service.get());
            Optional.ofNullable(pathDTO.getPath()).ifPresent(updatedPathString -> {
                existing.get().setPath(updatedPathString);
            });
            this.pathRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path(request.getServletPath() + "/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Path with ID '" + pathDTO.getId() + "' not found."));
        }
    }
}
