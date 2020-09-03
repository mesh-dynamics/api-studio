package com.cubeui.backend.service;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenResponseService {

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  public Map getTokenResponse(User user) {
    String token = jwtTokenProvider.createToken(user.getUsername(), new ArrayList<>(user.getRoles()));
    String updatedRefreshToken = jwtTokenProvider.createRefreshToken(user);

    Map<Object, Object> model = new HashMap<>();
    model.put("username", user.getUsername());
    model.put("roles", user.getRoles());
    model.put("access_token", token);
    model.put("expires_in", jwtTokenProvider.getValidity());
    model.put("token_type", "Bearer");
    model.put("customer_name", user.getCustomer().getName());
    model.put("refresh_token", updatedRefreshToken);
    return model;
  }
}
