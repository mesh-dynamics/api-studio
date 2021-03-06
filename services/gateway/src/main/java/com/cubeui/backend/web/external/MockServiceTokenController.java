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

package com.cubeui.backend.web.external;

import com.cubeui.backend.security.Validation;
import com.cubeui.backend.security.jwt.InvalidJwtAuthenticationException;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.service.CubeServerService;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mst")
public class MockServiceTokenController {

  @Autowired
  private CubeServerService cubeServerService;
  @Autowired
  private Validation validation;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @RequestMapping(value = "/{token}/{customerId}/{app}/{instanceId}/{service}/**" , consumes = {MediaType.ALL_VALUE})
  public ResponseEntity data(
      HttpServletRequest request, @RequestBody Optional<String> body,
      @PathVariable String token,@PathVariable String customerId,
      @PathVariable String app, @PathVariable String instanceId, @PathVariable String service) {
    Pair<String, Boolean> validateToken =  jwtTokenProvider.validateToken(List.of(token));
    if(validateToken.getSecond()) {
      Authentication authentication = jwtTokenProvider.getAuthentication(token);
      validation.validateCustomerName(authentication,customerId);
    } else {
      throw new InvalidJwtAuthenticationException("Expired or invalid authentication token");
    }
    String path = getPath(request.getRequestURI(), token);
    path = cubeServerService.getPathForHttpMethod(path , request.getMethod() , app, instanceId , service);
    return cubeServerService.fetchResponse(request, body , HttpMethod.POST , path );
  }

  private String getPath(String uri, String token) {
    return uri.replaceFirst(String.format("^/api/mst/%s", token), "/ms");
  }

}
