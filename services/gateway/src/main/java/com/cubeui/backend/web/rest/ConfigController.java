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

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Config;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.ConfigDTO;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.ConfigRepository;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.security.jwt.JwtTokenValidator;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.ConfigExistsException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import com.cubeui.backend.web.exception.RequiredFieldException;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

  @Autowired
  private ConfigRepository configRepository;
  @Autowired
  private Validation validation;
  @Autowired
  private CustomerRepository customerRepository;
  @Autowired
  private AppRepository appRepository;
  @Autowired
  private ServiceRepository serviceRepository;
  @Autowired
  private JwtTokenValidator jwtTokenValidator;
  @Autowired
  private CustomerService customerService;

  @GetMapping("/get")
  public ResponseEntity getConfigs(HttpServletRequest request,
      @RequestParam(value="customer", required = false) String customer,
      @RequestParam(value="app", required = false) String app,
      @RequestParam(value="service", required = false) String service,
      @RequestParam(value="configType", required = false) String configType,
      @RequestParam(value="key", required = false) String key,
      @RequestParam(value="domain", required = false) String domain,
      Authentication authentication) {
    String userId = null;
    boolean authenticate = false;
    if(customer == null && domain == null) {
      throw new RequiredFieldException("customer and domain both are missing in the api call, One is mandatory for the call");
    }
    if(domain != null) {
      customer = this.customerService.getByDomainUrl(domain).map(Customer::getName).orElseThrow(() -> {
        throw new RecordNotFoundException("Customer with domain '" + domain + "' not found.");
      });
    }
    if(customer.isBlank()) {
      throw new RequiredFieldException(String.format("Customer is empty for customer=%s , domain=%s", customer, domain));
    }
    if(request.getHeader("Authorization") != null) {
      jwtTokenValidator.resolveAndValidateToken(request);
      validation.validateCustomerName(authentication,customer);
      userId = ((User) authentication.getPrincipal()).getUsername();
      authenticate = true;
    }
    Config query = Config.builder().customer(customer).userId(userId)
        .app(app).service(service)
        .configType(configType)
        .authenticate(authenticate)
        .key(key).build();
    List<Config> response = this.configRepository.findAll(Example.of(query));
    return ok(response);
  }

  @PostMapping("/insert")
  public ResponseEntity saveConfig(HttpServletRequest request, @RequestBody ConfigDTO configDTO, Authentication authentication) {
    validation.validateCustomerName(authentication, configDTO.getCustomer());
    String userId = ((User) authentication.getPrincipal()).getUsername();
    Optional<Config> existingConfig = this.configRepository
        .findByKeyAndCustomerAndAppAndConfigTypeAndUserId(configDTO.getKey(), configDTO.getCustomer(),
            configDTO.getApp(), configDTO.getConfigType(), userId);
    if(existingConfig.isPresent()) {
      throw new ConfigExistsException(String.format("key=%s, customer=%s, app=%s, configType=%s",
          configDTO.getKey(), configDTO.getCustomer(), configDTO.getApp(), configDTO.getConfigType()));
    }
    Customer customer = this.customerRepository.findByName(configDTO.getCustomer()).get();
    if(configDTO.getService() != null && configDTO.getApp() == null) {
      return status(BAD_REQUEST).body(new ErrorResponse("App name is mandatory if service value is given"));
    }
    if(configDTO.getApp() != null) {
      Optional<App> app = this.appRepository.findByNameAndCustomerId(configDTO.getApp(), customer.getId());

      if(app.isEmpty()) {
        return status(BAD_REQUEST).body(new ErrorResponse("App with name=" +configDTO.getApp() + " not found" ));
      }

      if(configDTO.getService() != null) {
        Optional<Service> service = this.serviceRepository.findByNameAndAppId(configDTO.getService(), app.get().getId());

        if(service.isEmpty()) {
          return status(BAD_REQUEST).body(new ErrorResponse("Service with name=" +configDTO.getService() + " not found" ));
        }
      }
    }
    Config saved = this.configRepository.save(
        Config.builder()
            .customer(configDTO.getCustomer())
            .userId(userId)
            .app(configDTO.getApp())
            .service(configDTO.getService())
            .configType(configDTO.getConfigType())
            .key(configDTO.getKey())
            .value(configDTO.getValue())
            .authenticate(configDTO.isAuthenticate())
            .build()
    );
    return created(
        ServletUriComponentsBuilder
            .fromContextPath(request)
            .path(request.getServletPath() + "/{id}")
            .buildAndExpand(saved.getId())
            .toUri())
        .body(saved);

  }

  @PostMapping("/delete/{id}")
  public ResponseEntity delete(HttpServletRequest request, @PathVariable Long id, Authentication authentication) {
    String userId = ((User) authentication.getPrincipal()).getUsername();
    Optional<Config> config = this.configRepository.findByIdAndUserId(id, userId);

    if(config.isPresent()) {
      this.configRepository.deleteById(id);
      return ok(String.format("Config deleted for id=%s", id));
    }else {
      throw new RecordNotFoundException(
          String.format("No Config found for id=%s", id));
    }
  }

  @PostMapping("/update/{id}")
  public ResponseEntity update(HttpServletRequest request, @RequestBody ConfigDTO configDTO,
      @PathVariable Long id, Authentication authentication) {
    validation.validateCustomerName(authentication, configDTO.getCustomer());
    String userId = ((User) authentication.getPrincipal()).getUsername();
    Optional<Config> existingConfig = this.configRepository.findByIdAndUserId(id, userId);
    if (existingConfig.isEmpty()) {
      throw new RecordNotFoundException(String.format("No Config found for id=%s", id));
    }
    Optional<Config> configCheck = this.configRepository.findByKeyAndCustomerAndAppAndConfigTypeAndUserId(configDTO.getKey(), configDTO.getCustomer(),
        configDTO.getApp(), configDTO.getConfigType(), userId);
    if(configCheck.isPresent() && !configCheck.get().getId().equals(id)) {
      throw new ConfigExistsException(String.format(" key=%s, customer=%s, app=%s, configType=%s",
          configDTO.getKey(), configDTO.getCustomer(), configDTO.getApp(), configDTO.getConfigType()));
    }
    Config config = existingConfig.get();
    config.setService(configDTO.getService());
    config.setValue(configDTO.getValue());
    config.setKey(configDTO.getKey());
    config.setConfigType(configDTO.getConfigType());
    config.setApp(configDTO.getApp());
    config.setAuthenticate(configDTO.isAuthenticate());
    this.configRepository.save(config);
    return ok(config);
  }

}
