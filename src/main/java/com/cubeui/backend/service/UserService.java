package com.cubeui.backend.service;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.Role;
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
 * Service class for managing users.
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
        if (userDTO.getRoles() != null) {
            Set<String> allRoles = Role.getAllRoles();
            roles = userDTO.getRoles().stream()
                    .map(String::toUpperCase)
                    .filter(allRoles::contains)
                    .collect(Collectors.toSet());
        }
        Optional<User> user = userRepository.findByUsername(userDTO.getUsername());
        if (user.isPresent()){
            return this.userRepository.save(User.builder()
                    .id(user.get().getId())
                    .username(userDTO.getUsername())
                    .password(this.passwordEncoder.encode(userDTO.getPassword()))
                    .roles(roles)
                    .build()
            );
        } else {
            return this.userRepository.save(User.builder()
                    .username(userDTO.getUsername())
                    .password(this.passwordEncoder.encode(userDTO.getPassword()))
                    .roles(roles)
                    .build()
            );
        }

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
