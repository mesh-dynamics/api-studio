package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.ChangePasswordDTO;
import com.cubeui.backend.domain.DTO.KeyAndPasswordDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.service.MailService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
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

    public AccountController(UserService userService, MailService mailService) {
        this.userService = userService;
        this.mailService = mailService;
    }

    @PostMapping("/create-user")
    public ResponseEntity createUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        Optional<User> optionalUser = this.userService.getByUsername(userDTO.getEmail());
        if (optionalUser.isPresent()) {
            throw new DuplicateRecordException("User with username '" + optionalUser.get().getUsername() + "' already exists.");
        } else {
            User saved = this.userService.save(userDTO, false);
            mailService.sendActivationEmail(saved);
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/users/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        }
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

    @GetMapping("/activate")
    public ResponseEntity activateAccount(@RequestParam(value = "key") String activationKey) {
        Optional<User> optionalUser = userService.activateUser(activationKey);
        if (optionalUser.isPresent()) {
            mailService.sendCreationEmail(optionalUser.get());
            return ok("User activated");
        } else {
            throw new RecordNotFoundException("No user was found for with this activation key");
        }
    }

    @PostMapping(path = "/account/reset-password/init")
    public ResponseEntity requestPasswordReset(@RequestBody String mail) {
        Optional<User> user = userService.requestPasswordReset(mail);

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
        return user.map(ResponseEntity::ok).orElseThrow(() -> {
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
