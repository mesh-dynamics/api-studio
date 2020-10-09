package com.cubeui.backend.security.jwt;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.web.exception.ResetPasswordException;
import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenValidator {

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  public void resolveAndValidateToken(HttpServletRequest request) {
    String token = jwtTokenProvider.resolveToken(request);
    if (token != null && jwtTokenProvider.validateToken(token)) {
      log.trace("Token validation passed ");
      User user = (User)jwtTokenProvider.getUser(request);
      if(user.getResetPasswordDate() != null && user.getResetPasswordDate().isBefore(
          Instant.now())) {
        throw new ResetPasswordException("The User needs to reset his password");
      }
      Authentication auth = jwtTokenProvider.getAuthentication(token);

      if (auth != null) {
        SecurityContextHolder.getContext().setAuthentication(auth);
      }
    }
  }

}
