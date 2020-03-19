package com.cubeui.backend.web.rest;


import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.AppUserDTO;
import com.cubeui.backend.repository.*;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/app-user")
public class AppUserController {

    private AppUserRepository appUserRepository;
    private AppRepository appRepository;
    private UserRepository userRepository;

    public AppUserController(AppUserRepository appUserRepository, AppRepository appRepository, UserRepository userRepository) {
        this.appUserRepository = appUserRepository;
        this.appRepository = appRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity all(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<List<AppUser>> appUserList = this.appUserRepository.findByUserId(user.getId());
        if(appUserList.isPresent()) {
            return ok(appUserList);
        }
        return status(NOT_FOUND).body(new ErrorResponse("AppUser(s) for user name, '" + user.getName() + "' not found."));
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody AppUserDTO appUserDTO, HttpServletRequest httpServletRequest) {
        if(appUserDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("AppUser with ID " + appUserDTO.getId() + "already exists"));
        }
        Optional<App> app = Optional.empty();
        if(appUserDTO.getAppId() != null) {
            app = appRepository.findById(appUserDTO.getAppId());
            if(app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + appUserDTO.getAppId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field App Id is empty."));
        }
        Optional<User> user = Optional.empty();
        if(appUserDTO.getUserId() != null) {
            user = userRepository.findById(appUserDTO.getUserId());
            if(user.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("User with ID '" + appUserDTO.getUserId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field User Id is empty."));
        }
        Optional<AppUser> appUser = this.appUserRepository.findByAppIdAndUserId(appUserDTO.getAppId(), appUserDTO.getUserId());
        if (appUser.isPresent())
        {
            return ok(appUser);
        }
        AppUser saved = this.appUserRepository.save(
                AppUser.builder()
                        .app(app.get())
                        .user(user.get())
                        .build());
        return created(
                ServletUriComponentsBuilder
                        .fromContextPath(httpServletRequest)
                        .path(httpServletRequest.getServletPath() + "/{id}")
                        .buildAndExpand(saved.getId())
                        .toUri())
                .body(saved);
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody AppUserDTO appUserDTO, HttpServletRequest httpServletRequest) {
        if(appUserDTO.getId() == null) {
            return status(BAD_REQUEST).body(new ErrorResponse("InstanceUser id is mandatory"));
        }
        Optional<AppUser> existing = this.appUserRepository.findById(appUserDTO.getId());
        if(existing.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("AppUser with ID '" + appUserDTO.getId() + "' not found."));
        Optional<App> app = Optional.empty();
        if(appUserDTO.getAppId() != null) {
            app = appRepository.findById(appUserDTO.getAppId());
            if(app.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("App with ID '" + appUserDTO.getAppId() + "' not found."));
        }
        Optional<User> user = Optional.empty();
        if(appUserDTO.getUserId() != null) {
            user = userRepository.findById(appUserDTO.getUserId());
            if(user.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("User with ID '" + appUserDTO.getUserId() + "' not found."));
        }
        app.ifPresent(givenApp -> {
            existing.get().setApp(givenApp);
        });
        user.ifPresent(usr -> {
            existing.get().setUser(usr);
        });
        this.appUserRepository.save(existing.get());
        return created(
                ServletUriComponentsBuilder
                        .fromContextPath(httpServletRequest)
                        .path(httpServletRequest.getServletPath() + "/{id}")
                        .buildAndExpand(existing.get().getId())
                        .toUri())
                .body(existing);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        if(id == null) return status(BAD_REQUEST).body(new ErrorResponse("AppUser id is mandatory"));
        Optional<AppUser> existed = this.appUserRepository.findById(id);
        if(existed.isPresent()) this.appUserRepository.delete(existed.get());
        else return status(BAD_REQUEST).body(new ErrorResponse("AppUser with ID '" + id + "' not found."));
        return noContent().build();
    }
}
