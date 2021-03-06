/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.ChangePasswordDTO;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.KeyAndPasswordDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.PersonalEmailDomains;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.PersonalEmailDomainsRepository;
import com.cubeui.backend.service.CubeServerService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.MailTemplateService;
import com.cubeui.backend.service.ReCaptchaAPIService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.service.utils.Utils;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import io.md.utils.Constants;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
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

@Slf4j
@RestController()
@RequestMapping("/api/account")
public class AccountController {

    @Value("${md_cloud}")
    private boolean md_cloud = false;
    @Value("${spring.mail.emailsender}")
    private String emailSender;

    private UserService userService;
    private MailTemplateService mailTemplateService;
    private ReCaptchaAPIService reCaptchaAPIService;
    private CubeServerService cubeServerService;
    private AppRepository appRepository;
    private CustomerService customerService;
    private PersonalEmailDomainsRepository personalEmailDomainsRepository;

    public AccountController(UserService userService,
        MailTemplateService mailTemplateService, ReCaptchaAPIService reCaptchaAPIService,
        CubeServerService cubeServerService, AppRepository appRepository, CustomerService customerService,
        PersonalEmailDomainsRepository personalEmailDomainsRepository) {
        this.userService = userService;
        this.mailTemplateService = mailTemplateService;
        this.reCaptchaAPIService = reCaptchaAPIService;
        this.cubeServerService = cubeServerService;
        this.appRepository = appRepository;
        this.customerService = customerService;
        this.personalEmailDomainsRepository = personalEmailDomainsRepository;
    }

    Optional<Customer> validateEmailDomain(String domain) {
        // todo validate email string
        return customerService.getByDomainUrl(domain);
    }

    @PostMapping("/create-user")
    public ResponseEntity createUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        log.info("Create user called for email: " + userDTO.getEmail());
        Optional<User> existingUser = this.userService.getByUsername(userDTO.getEmail());

        // check existing user
        if (existingUser.isPresent()) {
            log.error("User already exists");
            throw new DuplicateRecordException("User with email '"
                + existingUser.get().getUsername() + "' already exists.");
        } else {
            // validate email domain and set customer id from email
            log.info("Validating email domain");
            String domain = Utils.getDomainFromEmail(userDTO.getEmail());
            Optional<PersonalEmailDomains> personalEmailDomains = personalEmailDomainsRepository.findByDomain(domain);
            Optional<Customer> customerOptional = Optional.empty();
            if(personalEmailDomains.isEmpty()) {
                customerOptional = validateEmailDomain(domain);
            }
            if(md_cloud && customerOptional.isEmpty()) {
                String customerName = personalEmailDomains.map(personalEmailDomain -> userDTO.getEmail()).orElse(domain);
                CustomerDTO customerDTO = new CustomerDTO();
                customerDTO.setDomainURLs(Set.of(domain));
                customerDTO.setEmail(userDTO.getEmail());
                customerDTO.setName(customerName);

                customerOptional = Optional.of(this.customerService.save(request, customerDTO));
            }
            Customer customer = customerOptional.orElseThrow(() -> {
                log.error("Invalid email");
                throw new InvalidDataException("Invalid email");
            });
            String customerName = customer.getName();
            log.info("Customer: " + customerName);
            userDTO.setCustomerId(customer.getId());

            // set default roles
            List<String> defaultRoles = Arrays.asList("ROLE_USER");
            userDTO.setRoles(defaultRoles);

            // save user
            User saved = this.userService.save(userDTO, false, true);

            userService.createHistoryForEachApp(request, saved);

            // send activation mail
            log.info("Sending activation mail");
            mailTemplateService.sendActivationEmail(saved);

            return created(
                ServletUriComponentsBuilder
                    .fromContextPath(request)
                    .path("/api/users/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri())
                .body(saved);

        }
    }

    @GetMapping("/validate-recaptcha")
    public ResponseEntity validateReCaptcha(HttpServletRequest request) {
        String response = request.getParameter("g-recaptcha-response");
        String clientIPAddress = request.getRemoteAddr();
        reCaptchaAPIService.processResponse(response, clientIPAddress);
        return ok().build();
    }

    @GetMapping("/getUser/{userName}")
    public ResponseEntity getUser(HttpServletRequest request, @PathVariable String userName) {
        Optional<User> existingUser = this.userService.getByUsername(userName);
        return ok(existingUser);
    }

    @Secured("ROLE_USER")
    @PostMapping("/update-user")
    public ResponseEntity updateUser(@RequestBody UserDTO userDTO, HttpServletRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        if (!userDTO.getEmail().equalsIgnoreCase(userDetails.getUsername())){
            throw new InvalidDataException("You can not update another user");
        }
        Optional<User> user = this.userService.getByUsername(userDTO.getEmail());
        if (user.isPresent()) {
            User saved = this.userService.save(userDTO, true, true);
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
     * POST  /activate : activate the registered user.
     *
     * @param activationKey the activation key
     * @throws RecordNotFoundException if the user couldn't be activated
     */
    @PostMapping("/activate")
    public ResponseEntity activateAccount(@RequestParam(value = "key") String activationKey) {
        log.info("Activate user called with key: " + activationKey);
        Optional<User> optionalUser = userService.activateUser(activationKey);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            mailTemplateService.sendCreationEmail(user);
            mailTemplateService.sendCreationEmailAdmin(user);
            return ok("User activated");
        } else {
            throw new RecordNotFoundException("No user was found for with this activation key");
        }
    }

    @PostMapping("/resend-activation-mail")
    public ResponseEntity resendActivationMail(@RequestParam(value="email") String email) {
        log.info("Resend activation mail for " + email);
        Optional<User> user = userService.resendActivationMail(email);
        mailTemplateService.sendActivationEmail(user.get());
        return ok("User activation mail sent");
    }

    @PostMapping(path = "/reset-password/init")
    public ResponseEntity requestPasswordReset(@RequestParam(value = "email") String email) {
        Optional<User> user = userService.requestPasswordReset(email);

        if (user.isPresent()) {
            mailTemplateService.sendPasswordResetMail(user.get());
        }
        JSONObject object =  new JSONObject();
        object.put(Constants.MESSAGE, "Email sent to " + email);
        object.put("sender", emailSender);
        return ok(object.toString());
    }

    @PostMapping(path = "/reset-password/finish")
    public ResponseEntity finishPasswordReset(@RequestBody KeyAndPasswordDTO keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getPassword())) {
            throw new InvalidDataException("Password length should be between " + PASSWORD_MIN_LENGTH + " and " + PASSWORD_MAX_LENGTH);
        }
        Optional<User> user = userService.completePasswordReset(keyAndPassword.getPassword(), keyAndPassword.getKey());
        return user.map(u -> {
          // send password reset successful mail
          mailTemplateService.sendPasswordResetSuccessfulMail(u);
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
