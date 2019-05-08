package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.web.AuthenticationRequest;
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

import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/login")
public class LoginController {

    private AuthenticationManager authenticationManager;
    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;

    public LoginController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("")
    public ResponseEntity login(@RequestBody AuthenticationRequest data) {
        try {
            String username = data.getUsername();
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, data.getPassword()));
            User user = this.userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Username " + username + "not found"));
            String token = jwtTokenProvider.createToken(username, new ArrayList<String>(user.getRoles()));

            Map<Object, Object> model = new HashMap<>();
            model.put("username", username);
            model.put("roles", user.getRoles());
            model.put("access_token", token);
            model.put("expires_in", jwtTokenProvider.getValidity());
            model.put("token_type", "Bearer");
            return ok(model);
        } catch (AuthenticationException e) {
//            throw new BadCredentialsException("Invalid username/password supplied");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username/password supplied");
        }
    }
}
