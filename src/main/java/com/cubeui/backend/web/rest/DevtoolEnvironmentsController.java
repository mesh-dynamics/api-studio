package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.DtEnvVarDTO;
import com.cubeui.backend.domain.DTO.DtEnvironmentDTO;
import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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

  @PostMapping("/save")
  public ResponseEntity saveEnvironments(@RequestBody List<DtEnvironmentDTO> environments){
    List<DtEnvironment> dtEnvironmentList = new ArrayList<>(environments.size());
    for(DtEnvironmentDTO environmentDTO : environments) {
      DtEnvironment dtEnvironment = new DtEnvironment();
      dtEnvironment.setName(environmentDTO.getName());
      List<DtEnvVar> envVarsList = dtEnvironment.getVars();
      for(DtEnvVarDTO envVarDTO : environmentDTO.getVars()) {
        DtEnvVar dtEnvVar = new DtEnvVar();
        dtEnvVar.setKey(envVarDTO.getKey());
        dtEnvVar.setValue(envVarDTO.getValue());
        dtEnvVar.setEnvironment(dtEnvironment);
        envVarsList.add(dtEnvVar);
      }
      dtEnvironment.setVars(envVarsList);
      dtEnvironmentList.add(dtEnvironment);
    }
    devtoolEnvironmentsRepository.saveAll(dtEnvironmentList);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/get")
  public ResponseEntity getEnvironments(Authentication authentication){
    User user = (User) authentication.getPrincipal();
    Optional<List<DtEnvironment>> environments = devtoolEnvironmentsRepository
        .findDtEnvironmentsByUser((user));
    if(environments.isPresent()) {
      return ResponseEntity.of(environments);
    } else {
      return ResponseEntity.ok().body(Collections.emptyList());
    }
  }

}
