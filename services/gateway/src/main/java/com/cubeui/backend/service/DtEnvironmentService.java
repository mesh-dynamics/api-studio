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

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.DtEnvServiceCollectionDTO;
import com.cubeui.backend.domain.DTO.DtEnvServiceHostDTO;
import com.cubeui.backend.domain.DTO.DtEnvVarDTO;
import com.cubeui.backend.domain.DTO.DtEnvironmentDTO;
import com.cubeui.backend.domain.DtEnvServiceCollection;
import com.cubeui.backend.domain.DtEnvServiceHost;
import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.security.Validation;
import com.cubeui.backend.web.exception.AppServiceMappingException;
import com.cubeui.backend.web.exception.EnvironmentNameExistsException;
import com.cubeui.backend.web.exception.EnvironmentNotFoundException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DtEnvironmentService {

  @Autowired
  DevtoolEnvironmentsRepository devtoolEnvironmentsRepository;
  @Autowired
  AppRepository appRepository;
  @Autowired
  Validation validation;
  @Autowired
  ServiceRepository serviceRepository;

  public DtEnvironment save(DtEnvironmentDTO environment, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    if(environment.getAppId() == null) {
      throw new InvalidDataException("AppId cannot be null");
    }
    Optional<App> appForId = appRepository.findById(environment.getAppId());
    App app = appForId.orElseThrow(
        () -> new RecordNotFoundException("App with id=" + environment.getAppId() + " not found"));
    validation.validateCustomerName(authentication, app.getCustomer().getName());
    DtEnvironment dtEnvironment = null;

    if(environment.getId() != null) {
      Optional<DtEnvironment> dtEnvironmentOptional
          = devtoolEnvironmentsRepository.findDtEnvironmentById(environment.getId());
      dtEnvironment = dtEnvironmentOptional.orElseThrow(() -> new EnvironmentNotFoundException(environment.getId()));
      validation.validateCustomerName(authentication, dtEnvironment.getUser().getCustomer().getName());
      checkEnvironmentWithSameName(environment.getName(), app.getId(), user.getId(), dtEnvironment.getId());
      dtEnvironment.setName(environment.getName());
    } else {
      checkEnvironmentWithSameName(environment.getName(), app.getId(), user.getId());
      dtEnvironment = new DtEnvironment(environment.getName());
    }
    dtEnvironment.setApp(app);
    List<DtEnvVar> envVarsList = new ArrayList<>(environment.getVars().size());
    for (DtEnvVarDTO envVarDTO : environment.getVars()) {
      DtEnvVar dtEnvVar = new DtEnvVar();
      dtEnvVar.setKey(envVarDTO.getKey());
      dtEnvVar.setValue(envVarDTO.getValue());
      dtEnvVar.setEnvironment(dtEnvironment);
      envVarsList.add(dtEnvVar);
    }
    dtEnvironment.setVars(envVarsList);
    dtEnvironment.setUser(user);
    dtEnvironment.setGlobal(environment.isGlobal());

    List<DtEnvServiceHost> dtEnvServiceHosts = new ArrayList<>(
        environment.getDtEnvServiceHosts().size());
    for (DtEnvServiceHostDTO dtEnvServiceHostDTO : environment.getDtEnvServiceHosts()) {
      Optional<com.cubeui.backend.domain.Service> existingService = serviceRepository
          .findById(dtEnvServiceHostDTO.getServiceId());
      com.cubeui.backend.domain.Service service = existingService.orElseThrow(
          () -> new RecordNotFoundException(
              "No Service found for id:" + dtEnvServiceHostDTO.getServiceId()));
      validateAppServiceMapping(app.getId(), service.getApp().getId());
      DtEnvServiceHost dtEnvServiceHost = new DtEnvServiceHost(dtEnvironment, service,
          dtEnvServiceHostDTO.getHostName());
      dtEnvServiceHosts.add(dtEnvServiceHost);
    }
    dtEnvironment.setDtEnvServiceHosts(dtEnvServiceHosts);

    List<DtEnvServiceCollection> dtEnvServiceCollections = new ArrayList<>(
        environment.getDtEnvServiceCollections().size());
    for (DtEnvServiceCollectionDTO dtEnvServiceCollectionDTO : environment
        .getDtEnvServiceCollections()) {
      Optional<com.cubeui.backend.domain.Service> existingService = serviceRepository
          .findById(dtEnvServiceCollectionDTO.getServiceId());
      com.cubeui.backend.domain.Service service = existingService.orElseThrow(
          () -> new RecordNotFoundException(
              "No Service found for id:" + dtEnvServiceCollectionDTO.getServiceId()));
      validateAppServiceMapping(app.getId(), service.getApp().getId());
      DtEnvServiceCollection dtEnvServiceCollection = new DtEnvServiceCollection(dtEnvironment,
          service, dtEnvServiceCollectionDTO.getPreferredCollection());
      dtEnvServiceCollections.add(dtEnvServiceCollection);
    }
    dtEnvironment.setDtEnvServiceCollections(dtEnvServiceCollections);
    return devtoolEnvironmentsRepository.save(dtEnvironment);
  }

  private void checkEnvironmentWithSameName(String name, Long appId, Long userId) {
    /**
     * To check any global doesn't exist with same name
     */

    List<DtEnvironment> dtEnvironments =
        devtoolEnvironmentsRepository.findDtEnvironmentByNameAndGlobalAndAppId(name, true, appId);
    if (dtEnvironments.size() > 0) {
      throw new EnvironmentNameExistsException(name);
    }
    /**
     * If there is no global environment present with given name
     * check for the local environment with given name
     */
    Optional<DtEnvironment> dtEnvironmentOptional =
        devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndNameAndAppId(userId, name, appId);
    if (dtEnvironmentOptional.isPresent()) {
      throw new EnvironmentNameExistsException(name);
    }
  }

  private void checkEnvironmentWithSameName(String name, Long appId, Long userId, Long id) {
    /**
     * To check any global doesn't exist with same name
     */
    List<DtEnvironment> dtEnvironments=
        devtoolEnvironmentsRepository.findDtEnvironmentByNameAndAppIdAndGlobalAndIdNot(name, appId, true, id);
    if(dtEnvironments.size() > 0) {
      throw new EnvironmentNameExistsException(name);
    }

    /**
     * If there is no global environment present with given name
     * check for the local environment with given name
     */
    Optional<DtEnvironment> dtEnvironmentNameCheckOptional
        = devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndNameAndAppIdAndIdNot(userId, name, appId, id);

    if(dtEnvironmentNameCheckOptional.isPresent()) {
      throw new EnvironmentNameExistsException(name);
    }
  }

  private void validateAppServiceMapping(Long appId, Long serviceAppId) {
    if (serviceAppId != appId) {
      throw new AppServiceMappingException(
          String.format("App id=%s not matching app id of service is=%s", appId, serviceAppId));
    }
  }

  public List<DtEnvironment> getAll (String environmentType, Long appId, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    List<Long> appIds = new ArrayList<>();
    if (appId != null) {
      Optional<App> existingApp = appRepository.findById(appId);
      App app = existingApp.orElseThrow(() -> new RecordNotFoundException("No app found for given id"));
      validation.validateCustomerName(authentication, app.getCustomer().getName());
      appIds.add(app.getId());
    } else {
      Optional<List<App>> apps = appRepository.findByCustomerId(user.getCustomer().getId());
      apps.ifPresent(aps -> aps.stream().map(app-> app.getId()).forEach(id-> appIds.add(id)));
    }
    if(environmentType.equalsIgnoreCase("ALL")) {
      return  devtoolEnvironmentsRepository.findDtEnvironmentByAppIdInAndUserId(appIds, user.getId());
    }
    return devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndAppIdInAndGlobal(user.getId(), appIds, environmentType.equalsIgnoreCase("GLOBAL"));
  }

  public String deleteEnvironment(Long id, Authentication authentication) {
    Optional<DtEnvironment> dtEnvironmentOptional = devtoolEnvironmentsRepository
        .findDtEnvironmentById(id);
    return dtEnvironmentOptional
        .map(dtEnvironment -> {
          validation.validateCustomerName(authentication,
              dtEnvironment.getUser().getCustomer().getName());
          devtoolEnvironmentsRepository.delete(dtEnvironment);
          return "Environment deleted";
        })
        .orElseThrow(() -> new EnvironmentNotFoundException(id));
  }

}
