package com.cubeio.logcollector.security.jwt;

import com.cubeio.logcollector.domain.User;
import com.cubeio.logcollector.domain.exception.ResetPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class JwtTokenValidator {

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  public void resolveAndValidateToken(HttpServletRequest request) {
    List<String> tokens = jwtTokenProvider.resolveToken(request);
    Pair<String, Boolean> token =  jwtTokenProvider.validateToken(tokens);
    if (token.getSecond()) {
      log.trace("Token validation passed ");
      User user = (User)jwtTokenProvider.getUser(request);
      if(user.getResetPasswordDate() != null && user.getResetPasswordDate().isBefore(
          Instant.now())) {
        throw new ResetPasswordException("The User needs to reset his password");
      }
      Authentication auth = jwtTokenProvider.getAuthentication(token.getFirst());

      if (auth != null) {
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
  }

}
