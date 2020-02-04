package com.cubeui.backend.service;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.ChangePasswordDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.enums.Role;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.service.jwt.JwtActivationTokenProvider;
import com.cubeui.backend.service.utils.RandomUtil;
import com.cubeui.backend.web.exception.ActivationKeyExpiredException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.UserAlreadyActivatedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ServiceDTO class for managing users.
 */
@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerService customerService;
    private final JwtActivationTokenProvider jwtTokenProvider;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CustomerService customerService, JwtActivationTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerService = customerService;
        this.jwtTokenProvider = jwtTokenProvider;
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

    public User save(UserDTO userDTO, boolean isActivated) {
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
        Optional<Customer> customer = customerService.getById(userDTO.getCustomerId());
        user.ifPresent(u -> {
            Optional.ofNullable(userDTO.getName()).ifPresent(name -> u.setName(name));
            Optional.ofNullable(userDTO.getPassword()).ifPresent(password -> u.setPassword(this.passwordEncoder.encode(password)));
            Optional.ofNullable(customer).ifPresent(customerOptional -> u.setCustomer(customerOptional.get()));
            u.setRoles(finalRoles);
            u.setActivated(isActivated);
            this.userRepository.save(u);
        });
        if (user.isEmpty() && customer.isPresent()){
            user = Optional.of(this.userRepository.save(User.builder()
                    .name(userDTO.getName())
                    .username(userDTO.getEmail())
                    .password(this.passwordEncoder.encode(userDTO.getPassword()))
                    .customer(customer.get())
                    .roles(roles)
                    .activationKey(jwtTokenProvider.createActivationToken(userDTO.getEmail()))
                    .activated(isActivated)
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

    public Optional<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository.findByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
//                    userRepository.save(user);
                    return user;
                });
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findByUsername(mail)
                .filter(User::isEnabled)
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
//                    userRepository.save(user);
                    return user;
                });
    }

    public void changePassword(ChangePasswordDTO changePasswordDTO, String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        CharSequence oldPassword = changePasswordDTO.getOldPassword();
        if (optionalUser.isPresent()) {
            if (!passwordEncoder.matches(oldPassword, optionalUser.get().getPassword())) {
                throw new InvalidDataException("Old password does not match");
            } else {
                optionalUser.map(user -> {
                    user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
                    return userRepository.save(user);
                });
            }
        }
    }

    public Optional<User> activateUser(String key) {
        log.debug("Validating and activating user for activation key {}", key);
        // verify that the token hasn't expired, and extract the user email from it
        String userEmail = jwtTokenProvider.validateToken(key);
        if(userEmail != null) {
            return userRepository.findByUsername(userEmail)
              .map(user -> {
                  if(user.isActivated()) {
                    throw new UserAlreadyActivatedException("User already activated");
                  }
                  user.setActivated(true);
//                  user.setActivationKey(null);
  //                    userRepository.save(user);
                  return user;
              });
          } else {
            log.info("Activation key expired");
            throw new ActivationKeyExpiredException("Activation key expired");
          }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedAtBefore(LocalDateTime.now().minusDays(30));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getUsername());
            userRepository.delete(user);
        }
    }
}
