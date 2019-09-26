package com.cubeui.backend.web.rest;


import com.cubeui.backend.domain.DTO.InstanceUserDTO;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.domain.InstanceUser;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.InstanceUserRepository;
import com.cubeui.backend.repository.UserRepository;
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
@RequestMapping("/api/instance-user")
public class InstanceUserController {

    private InstanceUserRepository instanceUserRepository;
    private InstanceRepository instanceRepository;
    private UserRepository userRepository;

    public InstanceUserController(InstanceUserRepository instanceUserRepository, InstanceRepository instanceRepository, UserRepository userRepository) {
        this.instanceUserRepository = instanceUserRepository;
        this.instanceRepository = instanceRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity all(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Optional<List<InstanceUser>> instanceUserList = this.instanceUserRepository.findByUserId(user.getId());
        if(instanceUserList.isPresent()) {
            return ok(instanceUserList);
        }
        return status(NOT_FOUND).body(new ErrorResponse("InstanceUser(s) for user name, '" + user.getName() + "' not found."));
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody InstanceUserDTO instanceUserDTO, HttpServletRequest httpServletRequest) {
        if(instanceUserDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("InstanceUser with ID " + instanceUserDTO.getId() + "already exists"));
        }
        Optional<Instance> instance = null;
        if(instanceUserDTO.getInstanceId() != null) {
            instance = instanceRepository.findById(instanceUserDTO.getInstanceId());
            if(instance.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Instance with ID '" + instanceUserDTO.getInstanceId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field Instance Id is empty."));
        }
        Optional<User> user = null;
        if(instanceUserDTO.getUserId() != null) {
            user = userRepository.findById(instanceUserDTO.getUserId());
            if(user.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("User with ID '" + instanceUserDTO.getUserId() + "' not found."));
        } else {
            return status(BAD_REQUEST).body(new ErrorResponse("Mandatory field User Id is empty."));
        }
        InstanceUser saved = this.instanceUserRepository.save(
                InstanceUser.builder()
                        .instance(instance.get())
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
    public ResponseEntity update(@RequestBody InstanceUserDTO instanceUserDTO, HttpServletRequest httpServletRequest) {
        if(instanceUserDTO.getId() == null) {
            return status(BAD_REQUEST).body(new ErrorResponse("InstanceUser id is mandatory"));
        }
        Optional<InstanceUser> existing = this.instanceUserRepository.findById(instanceUserDTO.getId());
        if(existing.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("InstanceUser with ID '" + instanceUserDTO.getId() + "' not found."));
        Optional<Instance> instance = null;
        if(instanceUserDTO.getInstanceId() != null) {
            instance = instanceRepository.findById(instanceUserDTO.getInstanceId());
            if(instance.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("Instance with ID '" + instanceUserDTO.getInstanceId() + "' not found."));
        }
        Optional<User> user = null;
        if(instanceUserDTO.getUserId() != null) {
            user = userRepository.findById(instanceUserDTO.getUserId());
            if(user.isEmpty()) return status(BAD_REQUEST).body(new ErrorResponse("User with ID '" + instanceUserDTO.getUserId() + "' not found."));
        }
        Optional.ofNullable(instance).ifPresent(ins -> {
            existing.get().setInstance(ins.get());
        });
        Optional.ofNullable(user).ifPresent(usr -> {
            existing.get().setUser(usr.get());
        });
        this.instanceUserRepository.save(existing.get());
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
        if(id == null) return status(BAD_REQUEST).body(new ErrorResponse("InstanceUser id is mandatory"));
        Optional<InstanceUser> existed = this.instanceUserRepository.findById(id);
        if(existed.isPresent()) this.instanceUserRepository.delete(existed.get());
        else return status(BAD_REQUEST).body(new ErrorResponse("InstanceUser with ID '" + id + "' not found."));
        return noContent().build();
    }
}
