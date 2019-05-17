package com.cubeui.backend.service;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.enums.Role;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ServiceDTO class for managing users.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User save(UserDTO userDTO) {
        Set<String> roles = new HashSet<>();
//        roles.add("ROLE_USER");
        if (userDTO.getRoles() != null) {
            Set<String> allRoles = Role.getAllRoles();
            roles = userDTO.getRoles().stream()
                    .map(String::toUpperCase)
                    .filter(allRoles::contains)
                    .collect(Collectors.toSet());
        }
        final Set<String> finalRoles = roles;
        Optional<User> user = userRepository.findByUsername(userDTO.getEmail());
        user.ifPresent(u -> {
            u.setName(userDTO.getName());
            u.setPassword(this.passwordEncoder.encode(userDTO.getPassword()));
            u.setRoles(finalRoles);
            this.userRepository.save(u);
        });
        if (user.isEmpty()){
            user = Optional.of(this.userRepository.save(User.builder()
                    .name(userDTO.getName())
                    .username(userDTO.getEmail())
                    .password(this.passwordEncoder.encode(userDTO.getPassword()))
                    .roles(roles)
                    .build()
            ));
        }
        return user.get();
    }

    public boolean deleteUser(Long id) {
        Optional<User> existed = this.userRepository.findById(id);
        if (existed.isPresent()) {
            this.userRepository.delete(existed.get());
            return true;
        } else {
            return false;
        }

    }
}
