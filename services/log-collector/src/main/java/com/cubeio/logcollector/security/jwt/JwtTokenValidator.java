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
