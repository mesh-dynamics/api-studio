package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.InstanceDTO;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private CustomerService customerService;

    public InstanceController(InstanceRepository instanceRepository, CustomerService customerService) {
        this.instanceRepository = instanceRepository;
        this.customerService = customerService;
    }

    @GetMapping("")
    public ResponseEntity all(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ok(this.instanceRepository.findByCustomerId(user.getCustomer().getId()));
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody InstanceDTO instanceDTO, HttpServletRequest request) {
        if (instanceDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("Instance with ID '" + instanceDTO.getId() +"' already exists."));
        }
        Optional<Customer> customer = customerService.getById(instanceDTO.getCustomerId());
        if(customer.isPresent()) {
            Instance saved = this.instanceRepository.save(Instance.builder()
                    .name(instanceDTO.getName())
                    .customer(customer.get())
                    .gatewayEndpoint(instanceDTO.getGatewayEndpoint())
                    .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/instance/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("Customer with ID '" + instanceDTO.getCustomerId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody InstanceDTO instanceDTO, HttpServletRequest request) {
        Optional<Instance> existing = this.instanceRepository.findById(instanceDTO.getId());
        if (existing.isPresent()) {
            existing.ifPresent(instance -> {
                instance.setName(instanceDTO.getName());
                instance.setCustomer(customerService.getById(instanceDTO.getCustomerId()).get());
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
            throw new RecordNotFoundException("Instance with ID '" + instanceDTO.getId() + "' not found.");
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
