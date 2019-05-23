package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.InstanceDTO;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.repository.InstanceRepository;
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
@RequestMapping("/api/instance")
public class InstanceController {

    private InstanceRepository instanceRepository;

    public InstanceController(InstanceRepository instanceRepository) {
        this.instanceRepository = instanceRepository;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.instanceRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody InstanceDTO instanceDTO, HttpServletRequest request) {
        if (instanceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Instance with ID '" + instanceDTO.getId() +"' already exists."));
        }
        Instance saved = this.instanceRepository.save(Instance.builder().name(instanceDTO.getName()).gatewayEndpoint(instanceDTO.getGatewayEndpoint()).build());
        return created(
            ServletUriComponentsBuilder
                .fromContextPath(request)
                .path("/api/instance/{id}")
                .buildAndExpand(saved.getId())
                .toUri())
            .body(saved);
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody InstanceDTO instanceDTO, HttpServletRequest request) {
        Optional<Instance> existing = this.instanceRepository.findById(instanceDTO.getId());
        if (existing.isPresent()) {
            existing.ifPresent(instance -> {
                instance.setName(instanceDTO.getName());
                instance.setGatewayEndpoint(instanceDTO.getGatewayEndpoint());
                this.instanceRepository.save(instance);
            });
            this.instanceRepository.save(existing.get());
            return created(
                ServletUriComponentsBuilder
                        .fromContextPath(request)
                        .path("/api/instance/{id}")
                        .buildAndExpand(existing.get().getId())
                        .toUri())
                .body("Instance with ID '" + existing.get().getId() + "' updated");
        } else {
            throw new RecordFoundException("Instance with ID '" + instanceDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.instanceRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Instance> existed = this.instanceRepository.findById(id);
        this.instanceRepository.delete(existed.get());
        return ok().body("Instance with ID '" + id + "' removed successfully");
    }
}
