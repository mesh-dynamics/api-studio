package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.web.AuthenticationRequest;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private AuthenticationManager authenticationManager;
    private JwtTokenProvider jwtTokenProvider;
    private UserService userService;

    public LoginController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

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
            String token = jwtTokenProvider.createToken(username, new ArrayList<>(user.getRoles()));

            Map<Object, Object> model = new HashMap<>();
            model.put("username", username);
            model.put("roles", user.getRoles());
            model.put("access_token", token);
            model.put("expires_in", jwtTokenProvider.getValidity());
            model.put("token_type", "Bearer");
            return ok(model);
        } catch (AuthenticationException ex) {
            return status(UNAUTHORIZED)
                    .body(new ErrorResponse(ex.getMessage(), "Invalid username/password supplied", UNAUTHORIZED.value()));
        }
    }
}
