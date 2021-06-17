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

package com.cubeui.backend.web.rest;


import com.cubeui.backend.domain.ApiAccessToken;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.ApiAccessTokenRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access_token")
@Slf4j
@Transactional
public class ApiAccessTokenController {

  private final String MESHD_AGENT_USER = "MeshDAgentUser";
  private final long VALIDITY_IN_SECONDS = 60 * 60 * 24 * 365 * 10;

  @Autowired
  private ApiAccessTokenRepository apiAccessTokenRepository;
  @Autowired
  private JwtTokenProvider jwtTokenProvider;
  @Autowired
  private CustomerService customerService;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @PostMapping("")
  public ResponseEntity create(Authentication authentication,
      @RequestParam(required=false, defaultValue = "false") Boolean updateToken) {

    final User user = (User) authentication.getPrincipal();
    Optional<Customer> customerOptional = customerService.getById(user.getCustomer().getId());
    Customer customer = customerOptional.orElseThrow(() -> new RecordNotFoundException("Error while fetching customer"));
    Set<String> domains = customer.getDomainUrls();
    Optional<User> meshDUser = Optional.empty();
    for(String domain : domains) {
      String userName = MESHD_AGENT_USER.concat("@").concat(domain);
      meshDUser = userRepository.findByUsernameIgnoreCase(userName);
      if(meshDUser.isPresent()) {
        break;
      }
    }
    User meshUser = meshDUser.orElseGet(() -> createNewUser(customer));
    if(updateToken) {
      apiAccessTokenRepository.deleteByUserId(meshUser.getId());
    }
    ApiAccessToken apiAccessToken = apiAccessTokenRepository.findByUserId(meshUser.getId())
        .orElseGet(() -> createApiAccessToken(meshUser));
    return ResponseEntity.ok(buildResponse(apiAccessToken));
  }

  private Map buildResponse(ApiAccessToken apiAccessToken){
    return Map.of("token", apiAccessToken.getToken());

  }

  private User createNewUser(Customer customer) {
    String encodedPassword = this.passwordEncoder.encode(MESHD_AGENT_USER.concat(UUID.randomUUID().toString()));
    return this.userRepository.save(User.builder()
        .name(MESHD_AGENT_USER)
        .username(MESHD_AGENT_USER.concat("@").concat(customer.getDomainUrls().iterator().next()))
        .customer(customer)
        .password(encodedPassword)
        .activated(false)
        .build());
  }

  private ApiAccessToken createApiAccessToken(User user) {
    String token = this.jwtTokenProvider.createToken(user, VALIDITY_IN_SECONDS);
    return apiAccessTokenRepository.save(
        ApiAccessToken.builder()
            .token(token)
            .user(user)
            .build());
  }
}
