package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.DtEnvServiceCollectionDTO;
import com.cubeui.backend.domain.DTO.DtEnvServiceHostDTO;
import com.cubeui.backend.domain.DTO.DtEnvVarDTO;
import com.cubeui.backend.domain.DTO.DtEnvironmentDTO;
import com.cubeui.backend.domain.DtEnvServiceCollection;
import com.cubeui.backend.domain.DtEnvServiceHost;
import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.Service;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.web.exception.EnvironmentNameExitsException;
import com.cubeui.backend.web.exception.EnvironmentNotFoundException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dtEnvironment")
public class DevtoolEnvironmentsController {

  @Autowired
  private DevtoolEnvironmentsRepository devtoolEnvironmentsRepository;
  @Autowired
  private AppRepository appRepository;
  @Autowired
  private ServiceRepository serviceRepository;

  @PostMapping("/insert")
  public ResponseEntity insertEnvironments(@RequestBody DtEnvironmentDTO environment, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    /**
     * To check any global doesn't exist with same name
     */
    Optional<List<App>> apps = appRepository.findByCustomerId(user.getCustomer().getId());
    List<Long> appIds = apps.map(aps -> aps.stream().map(app -> app.getId()).collect(
          Collectors.toList())).orElse(Collections.emptyList());
    List<DtEnvironment> dtEnvironments=
        devtoolEnvironmentsRepository.findDtEnvironmentByNameAndAppIdsAndGlobal(environment.getName(), appIds, true);
    if(dtEnvironments.size() > 0) {
      throw new EnvironmentNameExitsException(environment.getName());
    }
    /**
     * If there is no global environment present with given name
     * check for the local environment with given name
     */
    Optional<DtEnvironment> dtEnvironmentOptional =
        devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndName(user.getId(), environment.getName());
    if(dtEnvironmentOptional.isPresent()) {
      throw new EnvironmentNameExitsException(environment.getName());
    }
    try {
      DtEnvironment dtEnvironment = new DtEnvironment(environment.getName());
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

      List<DtEnvServiceHost> dtEnvServiceHosts = new ArrayList<>(environment.getDtEnvServiceHosts().size());
      for(DtEnvServiceHostDTO dtEnvServiceHostDTO : environment.getDtEnvServiceHosts()) {
        Optional<Service> existingService = serviceRepository.findById(dtEnvServiceHostDTO.getServiceId());
        existingService.ifPresent(service -> {
          DtEnvServiceHost dtEnvServiceHost = new DtEnvServiceHost(dtEnvironment, service, dtEnvServiceHostDTO.getHostName());
          dtEnvServiceHosts.add(dtEnvServiceHost);
        });
      }
      dtEnvironment.setDtEnvServiceHosts(dtEnvServiceHosts);

      List<DtEnvServiceCollection> dtEnvServiceCollections = new ArrayList<>(environment.getDtEnvServiceCollections().size());
      for(DtEnvServiceCollectionDTO dtEnvServiceCollectionDTO : environment.getDtEnvServiceCollections()) {
        Optional<Service> existingService = serviceRepository.findById(dtEnvServiceCollectionDTO.getServiceId());
        existingService.ifPresent(service -> {
          DtEnvServiceCollection dtEnvServiceCollection = new DtEnvServiceCollection(dtEnvironment, service, dtEnvServiceCollectionDTO.getPreferredCollection());
          dtEnvServiceCollections.add(dtEnvServiceCollection);
        });
      }
      dtEnvironment.setDtEnvServiceCollections(dtEnvServiceCollections);
      Optional<App> app = appRepository.findById(environment.getAppId());
      app.ifPresent(ap -> dtEnvironment.setApp(ap));
      DtEnvironment dtEnvironmentSaved = devtoolEnvironmentsRepository.save(dtEnvironment);
      return ResponseEntity.ok().body(Map.of("id", dtEnvironmentSaved.getId()));
    } catch (Exception e) {
      throw e;
    }
  }

  @PostMapping("/update/{id}")
  public ResponseEntity updateEnvironments(@RequestBody DtEnvironmentDTO environment,
      Authentication authentication, @PathVariable @NotEmpty Long id){
    User user = (User) authentication.getPrincipal();

    // fetch environment with the given id
    Optional<DtEnvironment> dtEnvironmentOptional
        = devtoolEnvironmentsRepository.findDtEnvironmentById(id);
    if (dtEnvironmentOptional.isEmpty()) {
      throw new EnvironmentNotFoundException(id);
    }

    DtEnvironment dtEnvironmentById = dtEnvironmentOptional.get();
    /**
     * check if some other global environment has the same name
     */

    Optional<List<App>> apps = appRepository.findByCustomerId(user.getCustomer().getId());
    List<Long> appIds = apps.map(aps -> aps.stream().map(app -> app.getId()).collect(
        Collectors.toList())).orElse(Collections.emptyList());
    List<DtEnvironment> dtEnvironments=
        devtoolEnvironmentsRepository.findDtEnvironmentByNameAndAppIdsAndGlobalAndIdNot(environment.getName(), appIds, true, dtEnvironmentById.getId());
    if(dtEnvironments.size() > 0) {
      throw new EnvironmentNameExitsException(environment.getName());
    }

    // check if some local other environment has the same name
    Optional<DtEnvironment> dtEnvironmentNameCheckOptional
        = devtoolEnvironmentsRepository.findDtEnvironmentByUserIdAndNameAndIdNot(user.getId(), environment.getName(), dtEnvironmentById.getId());

    if(dtEnvironmentNameCheckOptional.isPresent()) {
      throw new EnvironmentNameExitsException(environment.getName());
    }

    dtEnvironmentById.setName(environment.getName());
    List<DtEnvVar> envVarsList = new ArrayList<>(environment.getVars().size());
    for (DtEnvVarDTO envVarDTO : environment.getVars()) {
      DtEnvVar dtEnvVar = new DtEnvVar();
      dtEnvVar.setKey(envVarDTO.getKey());
      dtEnvVar.setValue(envVarDTO.getValue());
      dtEnvVar.setEnvironment(dtEnvironmentById);
      envVarsList.add(dtEnvVar);
    }
    dtEnvironmentById.setVars(envVarsList);
    dtEnvironmentById.setUser(user);
    dtEnvironmentById.setGlobal(environment.isGlobal());
    List<DtEnvServiceHost> dtEnvServiceHosts = new ArrayList<>(environment.getDtEnvServiceHosts().size());
    for(DtEnvServiceHostDTO dtEnvServiceHostDTO : environment.getDtEnvServiceHosts()) {
      Optional<Service> existingService = serviceRepository.findById(dtEnvServiceHostDTO.getServiceId());
      existingService.ifPresent(service -> {
        DtEnvServiceHost dtEnvServiceHost = new DtEnvServiceHost(dtEnvironmentById, service, dtEnvServiceHostDTO.getHostName());
        dtEnvServiceHosts.add(dtEnvServiceHost);
      });
    }
    dtEnvironmentById.setDtEnvServiceHosts(dtEnvServiceHosts);

    List<DtEnvServiceCollection> dtEnvServiceCollections = new ArrayList<>(environment.getDtEnvServiceCollections().size());
    for(DtEnvServiceCollectionDTO dtEnvServiceCollectionDTO : environment.getDtEnvServiceCollections()) {
      Optional<Service> existingService = serviceRepository.findById(dtEnvServiceCollectionDTO.getServiceId());
      existingService.ifPresent(service -> {
        DtEnvServiceCollection dtEnvServiceCollection = new DtEnvServiceCollection(dtEnvironmentById, service, dtEnvServiceCollectionDTO.getPreferredCollection());
        dtEnvServiceCollections.add(dtEnvServiceCollection);
      });
    }
    dtEnvironmentById.setDtEnvServiceCollections(dtEnvServiceCollections);
    Optional<App> app = appRepository.findById(environment.getAppId());
    app.ifPresent(ap -> dtEnvironmentById.setApp(ap));
    devtoolEnvironmentsRepository.save(dtEnvironmentById);
    return ResponseEntity.ok().build();
  }



  @PostMapping("/delete/{id}")
  public ResponseEntity<String> deleteEnvironment(@PathVariable @NotEmpty Long id, Authentication authentication) {
    Optional<DtEnvironment> dtEnvironmentOptional = devtoolEnvironmentsRepository
        .findDtEnvironmentById(id);
    return dtEnvironmentOptional
        .map(dtEnvironment ->  {
          devtoolEnvironmentsRepository.delete(dtEnvironment);
          return ResponseEntity.ok("Environment deleted");
        })
        .orElseThrow(() -> new EnvironmentNotFoundException(id));
  }

  @GetMapping("/getAll")
  public ResponseEntity getEnvironments(Authentication authentication,
      @RequestParam(required=false, defaultValue = "ALL") String environmentType,
      @RequestParam(required=false) Long appId) {
    User user = (User) authentication.getPrincipal();
    List<Long> appIds = new ArrayList<>();
    if (appId != null) {
      Optional<App> existingApp = appRepository.findById(appId);
      existingApp.ifPresentOrElse(app ->
        appIds.add(app.getId()),() -> new RecordNotFoundException("No app found for given id"));
    } else {
      Optional<List<App>> apps = appRepository.findByCustomerId(user.getCustomer().getId());
      appIds.addAll(apps.map(aps -> aps.stream().map(app -> app.getId()).collect(
          Collectors.toList())).orElse(Collections.emptyList()));
    }
    if(environmentType.equalsIgnoreCase("ALL")) {
      return ResponseEntity.ok(devtoolEnvironmentsRepository.findDtEnvironmentByAppIdsOrUserId(appIds, user.getId()));
    }else if(environmentType.equalsIgnoreCase("GLOBAL")) {
      return ResponseEntity.ok(devtoolEnvironmentsRepository.findDtEnvironmentByAppIdsOrUserIdAndGlobal(appIds, user.getId(), true));
    }
    return ResponseEntity.ok(devtoolEnvironmentsRepository.findDtEnvironmentByAppIdsOrUserIdAndGlobal(appIds, user.getId(), false));

//    Optional<List<DtEnvironment>> environmentOptional = devtoolEnvironmentsRepository
//        .findDtEnvironmentsByUserId((user.getId()));
//    return environmentOptional
//        .map(ResponseEntity::ok)
//        .orElse(ResponseEntity.ok().body(Collections.emptyList()));
  }

}
