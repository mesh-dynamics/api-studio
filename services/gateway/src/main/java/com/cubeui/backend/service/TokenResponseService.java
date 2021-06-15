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
