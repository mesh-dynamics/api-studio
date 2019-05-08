package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.Role;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.ResponseEntity.*;

@RestController()
@RequestMapping("/api/user")
@Secured("ROLE_ADMIN")
public class UserController {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("")
    public ResponseEntity getAll() {
        return ok(this.userRepository.findAll());
    }

    @PostMapping("")
    public ResponseEntity save(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        Optional<User> user = this.userRepository.findByUsername(userDTO.getUsername());
        if (user.isEmpty()) {
            Set<String> roles = new HashSet<>();
            if (userDTO.getRoles() != null) {
                Set<String> allRoles = Role.getAllRoles();
                roles = userDTO.getRoles().stream()
                        .filter(allRoles::contains)
                        .collect(Collectors.toSet());
            }
            User saved = this.userRepository.save(User.builder()
                    .username(userDTO.getUsername())
                    .password(this.passwordEncoder.encode(userDTO.getPassword()))
                    .roles(roles)
                    .build()
            );
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/users/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User with username '" + user.get().getUsername() + "' already exists.");
        }
    }

    @PutMapping("")
    public ResponseEntity update(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        Optional<User> user = this.userRepository.findByUsername(userDTO.getUsername());
        if (user.isPresent()) {
            Set<String> roles = new HashSet<>();
            if (userDTO.getRoles() != null) {
                Set<String> allRoles = Role.getAllRoles();
                roles = userDTO.getRoles().stream()
                        .filter(allRoles::contains)
                        .collect(Collectors.toSet());
            }
            User saved = this.userRepository.save(User.builder()
                    .username(userDTO.getUsername())
                    .password(this.passwordEncoder.encode(userDTO.getPassword()))
                    .roles(roles)
                    .build()
            );
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/users/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body("User '" + saved.getUsername() + "' updated");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with username '" + user.get().getUsername() + "' not found.");

        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.userRepository.findById(id));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<User> existed = this.userRepository.findById(id);
        this.userRepository.delete(existed.get());
        return ok("User '" + existed.get().getUsername() + "' removed successfully");
    }

    @GetMapping("/current")
    public ResponseEntity currentUser(@AuthenticationPrincipal UserDetails userDetails){
        Map<Object, Object> model = new HashMap<>();
        model.put("username", userDetails.getUsername());
        model.put("roles", userDetails.getAuthorities()
            .stream()
            .map(a -> ((GrantedAuthority) a).getAuthority())
            .collect(toList())
        );
        return ok(model);
    }
}
