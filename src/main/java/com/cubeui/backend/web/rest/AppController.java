package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.service.UserService;
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
@RequestMapping("/api/app")
//@Secured({"ROLE_USER"})
public class AppController {

    private AppRepository appRepository;
    private InstanceRepository instanceRepository;
    private UserService userService;

    public AppController(AppRepository appRepository, InstanceRepository instanceRepository, UserService userService) {
        this.appRepository = appRepository;
        this.instanceRepository = instanceRepository;
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity all() {
        return ok(this.appRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody AppDTO appDTO, HttpServletRequest request) {
        if (appDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("App with ID '" + appDTO.getId() +"' already exists."));
        }
        Optional<Instance> instance = instanceRepository.findById(appDTO.getInstanceId());
        Optional<User> user = userService.getById(appDTO.getCustomerId());
        if (instance.isPresent() && user.isPresent()) {
            App saved = this.appRepository.save(App.builder().name(appDTO.getName()).customer(user.get()).instance(instance.get()).build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/app/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .build();
        } else {
            if (instance.isEmpty()){
                throw new RecordFoundException("Instance with ID '" + appDTO.getInstanceId() + "' not found.");
            } else {
                throw new RecordFoundException("User with ID '" + appDTO.getCustomerId() + "' not found.");
            }
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody AppDTO appDTO, HttpServletRequest request) {
        if (appDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("App id not provided"));
        }
        Optional<App> app = appRepository.findById(appDTO.getId());
        if (app.isPresent()) {
            app.ifPresent(app1 -> {
                app1.setInstance(instanceRepository.findById(appDTO.getInstanceId()).get());
                app1.setCustomer(userService.getById(appDTO.getCustomerId()).get());
                app1.setName(appDTO.getName());
            });
            this.appRepository.save(app.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/app/{id}")
                            .buildAndExpand(app.get().getId())
                            .toUri())
                    .build();
        } else {
            throw new RecordFoundException("App with ID '" + appDTO.getId() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.appRepository.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<App> existed = this.appRepository.findById(id);
        this.appRepository.delete(existed.get());
        return noContent().build();
    }
}
