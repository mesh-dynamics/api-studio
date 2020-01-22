package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.ChangePasswordDTO;
import com.cubeui.backend.domain.DTO.KeyAndPasswordDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.EmailDomain;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.EmailDomainRepository;
import com.cubeui.backend.service.MailService;
import com.cubeui.backend.service.ReCaptchaAPIService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.service.exception.InvalidReCaptchaException;
import com.cubeui.backend.web.exception.ActivationKeyExpiredException;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.cubeui.backend.security.Constants.PASSWORD_MAX_LENGTH;
import static com.cubeui.backend.security.Constants.PASSWORD_MIN_LENGTH;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.ResponseEntity.*;

@RestController()
@RequestMapping("/api/account")
public class AccountController {

    private UserService userService;
    private MailService mailService;
    private EmailDomainRepository emailDomainRepository;
    private ReCaptchaAPIService reCaptchaAPIService;

    public AccountController(UserService userService,
        MailService mailService, EmailDomainRepository emailDomainRepository, ReCaptchaAPIService reCaptchaAPIService) {
        this.userService = userService;
        this.mailService = mailService;
        this.emailDomainRepository = emailDomainRepository;
        this.reCaptchaAPIService = reCaptchaAPIService;
    }

    Optional<EmailDomain> validateEmailDomain(String email) {
        // todo validate email string
        String[] emailSplit = email.split("@");
        String domain = emailSplit[1];
        Optional<EmailDomain> emailDomain = emailDomainRepository.findByDomain(domain);
        return emailDomain;
    }

    @PostMapping("/create-user")
    public ResponseEntity createUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        Optional<User> existingUser = this.userService.getByUsername(userDTO.getEmail());
        // check existing user
        if (existingUser.isPresent()) {
            throw new DuplicateRecordException("User with email '"
                + existingUser.get().getUsername() + "' already exists.");
        } else {
            // validate email domain and set customer id from email
            Optional<EmailDomain> emailDomain = validateEmailDomain(userDTO.getEmail());
            if (emailDomain.isPresent()) {
                EmailDomain e = emailDomain.get();
                userDTO.setCustomerId(e.getCustomer().getId());

                // set default roles
                List<String> defaultRoles = Arrays.asList("ROLE_USER");
                userDTO.setRoles(defaultRoles);

                // save user
                User saved = this.userService.save(userDTO, false);

                // send activation mail
                mailService.sendActivationEmail(saved);

                return created(
                        ServletUriComponentsBuilder
                                .fromContextPath(request)
                                .path("/api/users/{id}")
                                .buildAndExpand(saved.getId())
                                .toUri())
                        .body(saved);
            } else {
                throw new InvalidDataException("Invalid email");
            }
        }
    }

    @GetMapping("/validate-recaptcha")
    public ResponseEntity validateReCaptcha(HttpServletRequest request) {
        String response = request.getParameter("g-recaptcha-response");
        String clientIPAddress = request.getRemoteAddr();
        reCaptchaAPIService.processResponse(response, clientIPAddress);
        return ok().build();
    }

    @Secured("ROLE_USER")
    @PostMapping("/update-user")
    public ResponseEntity updateUser(@RequestBody UserDTO userDTO, HttpServletRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        if (!userDTO.getEmail().equalsIgnoreCase(userDetails.getUsername())){
            throw new InvalidDataException("You can not update another user");
        }
        Optional<User> user = this.userService.getByUsername(userDTO.getEmail());
        if (user.isPresent()) {
            User saved = this.userService.save(userDTO, true);
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

    /**
     * GET  /activate : activate the registered user.
     *
     * @param activationKey the activation key
     * @throws RecordNotFoundException if the user couldn't be activated
     */
    @GetMapping("/activate")
    public ResponseEntity activateAccount(@RequestParam(value = "key") String activationKey) {
        // TODO: the following logic is flawed and will be redone.
        Optional<User> optionalUser = userService.activateUser(activationKey);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // check if key has expired (48H)
            if(user.getCreatedAt().plusHours(48L).isAfter(LocalDateTime.now())) {
                mailService.sendCreationEmail(user);
                mailService.sendCreationEmailAdmin(user);
                return ok("User activated");
            } else {
                throw new ActivationKeyExpiredException("Activation key expired");
            }
        } else {
            throw new RecordNotFoundException("No user was found for with this activation key");
        }
    }

    @PostMapping(path = "/reset-password/init")
    public ResponseEntity requestPasswordReset(@RequestParam(value = "email") String email) {
        Optional<User> user = userService.requestPasswordReset(email);

        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.get());
            return ok("Email sent to " + user.get().getUsername());
        } else {
            throw new RecordNotFoundException("No user was found for with this email");
        }
    }

    @PostMapping(path = "/reset-password/finish")
    public ResponseEntity finishPasswordReset(@RequestBody KeyAndPasswordDTO keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getPassword())) {
            throw new InvalidDataException("Password length should be between " + PASSWORD_MIN_LENGTH + " and " + PASSWORD_MAX_LENGTH);
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getPassword(), keyAndPassword.getKey());
        return user.map(u -> {
          // send password reset successful mail
          mailService.sendPasswordResetSuccessfulMail(u);
          return ok().build();
        }).orElseThrow(() -> {
            throw new RecordNotFoundException("No user was found for this reset key");
        });
    }

    @Secured("ROLE_USER")
    @PostMapping(path = "/change-password")
    public void changePassword(@RequestBody ChangePasswordDTO changePasswordDTO, @AuthenticationPrincipal UserDetails userDetails) {
        if (!checkPasswordLength(changePasswordDTO.getNewPassword())) {
            throw new InvalidDataException("Password length should be between " + PASSWORD_MIN_LENGTH + " and " + PASSWORD_MAX_LENGTH);
        }
        userService.changePassword(changePasswordDTO, userDetails.getUsername());
    }

    @Secured("ROLE_USER")
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

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
                password.length() >= PASSWORD_MIN_LENGTH &&
                password.length() <= PASSWORD_MAX_LENGTH;
    }
}
