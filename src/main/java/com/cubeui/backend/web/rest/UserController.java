package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController()
@RequestMapping("/api/user")
@Secured("ROLE_ADMIN")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("")
    public ResponseEntity getAll() {
        return ok(this.userService.getAllUsers());
    }

    @PostMapping("/save")
    public ResponseEntity save(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        // todo: check whether the customer is present since it is used in the save call below
        Optional<User> user = this.userService.getByUsername(userDTO.getEmail());
        if (user.isEmpty()) {
            User saved = this.userService.save(userDTO, false);
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/users/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            return status(FORBIDDEN).body(new ErrorResponse("User with username '" + user.get().getUsername() + "' already exists."));
        }
    }

    @PostMapping("/update")
    public ResponseEntity update(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        Optional<User> user = this.userService.getByUsername(userDTO.getEmail());
        if (user.isPresent()) {
            User saved = this.userService.save(userDTO, userDTO.isActivated());
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/users/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body("User '" + saved.getUsername() + "' updated");
        } else {
            throw new RecordNotFoundException("User with username '" + userDTO.getEmail() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.userService.getById(id)
                .orElseThrow(() -> new RecordNotFoundException("User with id '" + id + "' not found.")));
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        if (userService.deleteUser(id)) {
            return ok("User '" + id + "' removed successfully");
        } else {
            throw new RecordNotFoundException("User with id '" + id + "' not found.");
        }
    }
}