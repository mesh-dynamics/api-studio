package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.AppDTO;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
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
    private UserService userService;

    public AppController(AppRepository appRepository, UserService userService) {
        this.appRepository = appRepository;
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
        Optional<User> user = userService.getById(appDTO.getCustomerId());
        if (user.isPresent()) {
            App saved = this.appRepository.save(
                    App.builder()
                            .name(appDTO.getName())
                            .customer(user.get())
                            .build());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/app/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordFoundException("User with ID '" + appDTO.getCustomerId() + "' not found.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody AppDTO appDTO, HttpServletRequest request) {
        if (appDTO.getId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("App id not provided"));
        }
        Optional<App> existing = appRepository.findById(appDTO.getId());
        if (existing.isPresent()) {
            existing.ifPresent(app -> {
                app.setCustomer(userService.getById(appDTO.getCustomerId()).get());
                app.setName(appDTO.getName());
            });
            this.appRepository.save(existing.get());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/app/{id}")
                            .buildAndExpand(existing.get().getId())
                            .toUri())
                    .body(existing);
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
        existed.ifPresent((app) -> this.appRepository.delete(app));
        return noContent().build();
    }
}
