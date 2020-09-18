package com.cubeui.backend.service;

import com.cubeui.backend.config.ResetPasswordConfiguration;
import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppUser;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.JiraCustomerDefaultCredentials;
import com.cubeui.backend.domain.JiraUserCredentials;
import com.cubeui.backend.domain.DTO.ChangePasswordDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.Instance;
import com.cubeui.backend.domain.InstanceUser;
import com.cubeui.backend.domain.ResetPasswordConfig;
import com.cubeui.backend.domain.UserOldPasswords;
import com.cubeui.backend.domain.enums.Role;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.AppUserRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.InstanceUserRepository;
import com.cubeui.backend.repository.ResetPasswordConfigRepository;
import com.cubeui.backend.repository.UserOldPasswordsRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.repository.JiraCustomerCredentialsRepository;
import com.cubeui.backend.repository.JiraUserCredentialsRepository;
import com.cubeui.backend.service.jwt.JwtActivationTokenProvider;
import com.cubeui.backend.service.utils.RandomUtil;
import com.cubeui.backend.web.exception.ActivationKeyExpiredException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.OldPasswordException;
import com.cubeui.backend.web.exception.ResetPasswordException;
import com.cubeui.backend.web.exception.UserAlreadyActivatedException;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final AppRepository appRepository;
    private final AppUserRepository appUserRepository;
    private final InstanceRepository instanceRepository;
    private final InstanceUserRepository instanceUserRepository;
    private final JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository;
    private final JiraUserCredentialsRepository jiraUserCredentialsRepository;
    private final UserOldPasswordsRepository userOldPasswordsRepository;
    private final ResetPasswordConfigRepository resetPasswordConfigRepository;
    private final ResetPasswordConfiguration resetPasswordConfiguration;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       CustomerService customerService, AppRepository appRepository, AppUserRepository appUserRepository,
                       InstanceRepository instanceRepository, InstanceUserRepository instanceUserRepository, JwtActivationTokenProvider jwtTokenProvider,
                       JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository, JiraUserCredentialsRepository jiraUserCredentialsRepository,
                       UserOldPasswordsRepository userOldPasswordsRepository, ResetPasswordConfigRepository resetPasswordConfigRepository,
                       ResetPasswordConfiguration resetPasswordConfiguration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customerService = customerService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appRepository = appRepository;
        this.appUserRepository = appUserRepository;
        this.instanceRepository = instanceRepository;
        this.instanceUserRepository = instanceUserRepository;
        this.jiraCustomerCredentialsRepository = jiraCustomerCredentialsRepository;
        this.jiraUserCredentialsRepository = jiraUserCredentialsRepository;
        this.userOldPasswordsRepository = userOldPasswordsRepository;
        this.resetPasswordConfigRepository = resetPasswordConfigRepository;
        this.resetPasswordConfiguration = resetPasswordConfiguration;
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

    public User save(UserDTO userDTO, boolean isActivated, boolean createUserAppInstanceMapping) {
    // (createUserAppInstanceMapping is used to avoid a bug in Ubuntu machines)
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
        if(user.isPresent()) {
            // if user already exists, update the fields
            User u = user.get();
            if(userDTO.getName() != null) {
                u.setName(userDTO.getName());
            }
            if(customer.isPresent()) {
                u.setCustomer(customer.get());
            }
            u.setRoles(finalRoles);
            u.setActivated(isActivated);
            if(userDTO.getPassword() != null) {
                checkNewPasswordWithOldOnes(u, userDTO.getPassword());
                u = updatePasswordDataForUser(u, userDTO.getPassword());
            }
            this.userRepository.save(u);
            return u;
        } else {
            // else it's a new user, so create new user entry and assign related apps etc to it
            String encodedPassword = this.passwordEncoder.encode(userDTO.getPassword());
            User newUser = this.userRepository.save(User.builder()
                    .name(userDTO.getName())
                    .username(userDTO.getEmail())
                    .password(encodedPassword)
                    .customer(customer.get())
                    .roles(roles)
                    .activationKey(jwtTokenProvider.createActivationToken(userDTO.getEmail()))
                    .activated(isActivated)
                    .resetPasswordDate(getResetPasswordDate(userDTO.getPassword(), customer.get().getId()))
                    .build());
            Optional<JiraCustomerDefaultCredentials> jiraCustomerDefaultCredentialsOptional = jiraCustomerCredentialsRepository.findByCustomerId(userDTO.getCustomerId());
            jiraCustomerDefaultCredentialsOptional.ifPresent(jiraCustomerDefaultCredentials -> {
                JiraUserCredentials jiraUserCredentials = jiraUserCredentialsRepository.save(
                        JiraUserCredentials.builder()
                            .APIKey(jiraCustomerDefaultCredentials.getAPIKey())
                            .jiraBaseURL(jiraCustomerDefaultCredentials.getJiraBaseURL())
                            .userName(jiraCustomerDefaultCredentials.getUserName())
                            .user(newUser)
                            .build());
            });

            if (createUserAppInstanceMapping) {
                // assign apps and their instances to the user from the customer
                log.debug("assigning apps and instances");
                Optional<List<App>> appsOptional = appRepository
                    .findByCustomerId(customer.get().getId());
                appsOptional.ifPresent(apps -> {
                    apps.forEach(app -> {
                        AppUser appUser = new AppUser();
                        appUser.setApp(app);
                        appUser.setUser(newUser);

                        Optional<List<Instance>> instancesOptional = instanceRepository
                            .findByAppId(app.getId());
                        instancesOptional.ifPresent(instances -> {
                            instances.forEach(instance -> {
                                InstanceUser instanceUser = new InstanceUser();
                                instanceUser.setInstance(instance);
                                instanceUser.setUser(newUser);
                                instanceUserRepository.save(instanceUser);
                            });
                        });

                        appUserRepository.save(appUser);
                    });
                });
            }

            return newUser;
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

    public boolean deleteUser(String email) {
        Optional<User> existed = this.userRepository.findByUsername(email);
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
                .filter(user -> {
                    if(user.getResetDate().isAfter(Instant.now().minusSeconds(86400))) {
                        checkNewPasswordWithOldOnes(user, newPassword);
                        return true;
                    }
                    return false;
                })
                .map(user -> updatePasswordDataForUser(user, newPassword));
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findByUsername(mail)
                .filter(user -> {
                    if(user.isEnabled()) {
                        LocalDateTime dateTime = LocalDateTime.now().minusSeconds(resetPasswordConfiguration.getPasswordResetRequestDays()*86400);
                        if(dateTime.isBefore(user.getUpdatedAt())) {
                            throw new ResetPasswordException("Password request can be proceed only after " + user.getUpdatedAt().plusSeconds(86400));
                        }
                        return true;
                    }
                    return false;
                })
                .map(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
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
                    checkNewPasswordWithOldOnes(user, changePasswordDTO.getNewPassword());
                    user = updatePasswordDataForUser(user, changePasswordDTO.getNewPassword());
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
                  user.setActivationKey(null);
                  userRepository.save(user);
                  return user;
              });
          } else {
            log.info("Activation key expired");
            throw new ActivationKeyExpiredException("Activation key expired");
          }
    }

    public Optional<User> resendActivationMail(String email) {
        Optional<User> userOptional = userRepository.findByUsername(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!user.isActivated()) {
                user.setActivationKey(jwtTokenProvider.createActivationToken(email));
                userRepository.save(user);
                return Optional.of(user);
            } else {
                throw new UserAlreadyActivatedException("User already activated");
            }
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    //@Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedAtBefore(LocalDateTime.now().minusDays(30));
        for (User user : users) {
            log.debug("Deleting not activated user {}", user.getUsername());
            userRepository.delete(user);
        }
    }

    private Instant getResetPasswordDate(String password, Long customerId) {
        Optional<ResetPasswordConfig> resetPasswordConfig = resetPasswordConfigRepository.findByCustomerId(customerId);
        int numOfDays = resetPasswordConfig.map( config -> config.getPasswordResetDaysMin()).orElse(resetPasswordConfiguration.getPasswordResetDaysMin());
        if(password.length() >= resetPasswordConfiguration.getPasswordLength()) {
            numOfDays = resetPasswordConfig.map(config -> config.getPasswordResetDaysMax()).orElse(resetPasswordConfiguration.getPasswordResetDaysMax());
        }
        return Instant.now().plus(numOfDays, ChronoUnit.DAYS);
    }

    private void checkNewPasswordWithOldOnes(User user, String password) {
        Integer oldPasswordsMatchSize = resetPasswordConfigRepository.findByCustomerId(user.getCustomer().getId())
            .map(config -> config.getOldPasswordsMatchSize()).orElse(resetPasswordConfiguration.getOldPasswordsMatchSize());
        List<UserOldPasswords> userOldPasswords = this.userOldPasswordsRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        userOldPasswords = userOldPasswords.stream().limit(oldPasswordsMatchSize)
            .filter(uop -> this.passwordEncoder.matches(password, uop.getPassword()))
                .collect(Collectors.toList());
        if(this.passwordEncoder.matches(password, user.getPassword())) {
            throw new OldPasswordException("Password matches with current password");
        }
        if(!userOldPasswords.isEmpty()) {
            throw new OldPasswordException("Password matches with one of your old "+ oldPasswordsMatchSize + "-passwords");
        }
    }

    private User updatePasswordDataForUser(User user, String password) {
        this.userOldPasswordsRepository.save(UserOldPasswords.builder()
            .user(user)
            .password(user.getPassword())
            .updatedAt(LocalDateTime.now())
            .build());
        user.setPassword(this.passwordEncoder.encode(password));
        user.setResetPasswordDate(getResetPasswordDate(password, user.getCustomer().getId()));
        user.setUpdatedAt(LocalDateTime.now());
        user.setResetKey(null);
        user.setResetDate(null);
        return user;
    }
}
