package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.service.TokenResponseService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.web.AuthenticationRequest;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private UserService userService;
    @Autowired
    private TokenResponseService tokenResponseService;

    @PostMapping("")
    public ResponseEntity login(@RequestBody AuthenticationRequest data) {
        try {
            String username = data.getUsername();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.getPassword()));
            User user = this.userService.getByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Username " + username + "not found"));
            if (!user.isActivated()){
                return status(UNAUTHORIZED)
                        .body(new ErrorResponse("Authentication Failed", "User not activated yet. Please check your email", UNAUTHORIZED.value()));

            }
            /**TODO
             * the below logic can be removed once every user reset date gets set to some value
             */
            if(user.getResetPasswordDate() == null) {
                user.setResetPasswordDate(this.userService.getResetPasswordDate(data.getPassword(), user.getCustomer().getId()));
            }
            return ok(tokenResponseService.getTokenResponse(user));
        } catch (AuthenticationException ex) {
            return status(UNAUTHORIZED)
                    .body(new ErrorResponse(ex.getMessage(), "Invalid username/password supplied", UNAUTHORIZED.value()));
        }
    }
}
