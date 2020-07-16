package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.DtEnvVarDTO;
import com.cubeui.backend.domain.DTO.DtEnvironmentDTO;
import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.web.exception.EnvironmentNameExitsException;
import com.cubeui.backend.web.exception.EnvironmentNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/environment")
public class DevtoolEnvironmentsController {

  private DevtoolEnvironmentsRepository devtoolEnvironmentsRepository;

  public DevtoolEnvironmentsController(DevtoolEnvironmentsRepository devtoolEnvironmentsRepository) {
    this.devtoolEnvironmentsRepository = devtoolEnvironmentsRepository;
  }

  @PostMapping("/insert")
  public ResponseEntity insertEnvironments(@RequestBody DtEnvironmentDTO environment, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    Optional<DtEnvironment> dtEnvironmentOptional
        = devtoolEnvironmentsRepository
        .findDtEnvironmentByUserAndName(user, environment.getName());
    if (dtEnvironmentOptional.isPresent()) {
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
    Optional<DtEnvironment> dtEnvironmentOptional
        = devtoolEnvironmentsRepository.findDtEnvironmentById(id);

    if (dtEnvironmentOptional.isEmpty()) {
      throw new EnvironmentNotFoundException(id);
    }

    DtEnvironment dtEnvironmentById = dtEnvironmentOptional.get();

    // check if some other environment has the same name
    Optional<DtEnvironment> dtEnvironmentNameCheckOptional = devtoolEnvironmentsRepository.findDtEnvironmentByUserAndNameAndIdNot(user, environment.getName(), dtEnvironmentById.getId());

    // check if the object with the same name isn't this one
    //Boolean nameAlreadyPresent = dtEnvironmentNameCheckOptional.map(dtEnvironmentByName -> (!dtEnvironmentById.getId().equals(dtEnvironmentByName.getId()))).orElse(false);

    if(dtEnvironmentNameCheckOptional.isPresent()) {
      throw new EnvironmentNameExitsException(dtEnvironmentById.getName());
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

    devtoolEnvironmentsRepository.save(dtEnvironmentById);
    return ResponseEntity.ok().build();
  }



  @GetMapping("/delete/{id}")
  public ResponseEntity<String> deleteEnvironment(@PathVariable @NotEmpty Long id, Authentication authentication) {
    User user = (User) authentication.getPrincipal();
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
  public ResponseEntity getEnvironments(Authentication authentication){
    User user = (User) authentication.getPrincipal();
    Optional<List<DtEnvironment>> environmentOptional = devtoolEnvironmentsRepository
        .findDtEnvironmentsByUser((user));
    return environmentOptional
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.ok().body(Collections.emptyList()));
  }

}
