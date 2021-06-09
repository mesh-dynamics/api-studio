package com.cubeui.backend.web.rest;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.service.TokenResponseService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.RefreshTokenRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/token")
@Slf4j
public class RefreshTokenController {
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Autowired
  private TokenResponseService tokenResponseService;

  @PostMapping("")
  public ResponseEntity checkAndUpdateToken(@RequestBody RefreshTokenRequest data) {
    String refreshToken = data.getRefreshToken();
    if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
      log.trace("RefreshToken validation passed ");
      User user = (User)jwtTokenProvider.getUserFromRefreshToken(refreshToken);
      return ok(tokenResponseService.getTokenResponse(user));
    }
    return status(UNAUTHORIZED)
        .body(new ErrorResponse("AcessToken Update failed", "Invalid Refresh Token supplied", UNAUTHORIZED.value()));
  }
}
