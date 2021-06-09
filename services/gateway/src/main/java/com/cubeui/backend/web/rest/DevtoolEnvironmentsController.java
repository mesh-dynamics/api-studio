package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.DTO.DtEnvironmentDTO;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.service.DtEnvironmentService;
import java.util.Map;
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
  private DtEnvironmentService dtEnvironmentService;

  @PostMapping("/insert")
  public ResponseEntity insertEnvironments(@RequestBody DtEnvironmentDTO environment, Authentication authentication) {
      DtEnvironment dtEnvironmentSaved = dtEnvironmentService.save(environment, authentication);
      return ResponseEntity.ok().body(Map.of("id", dtEnvironmentSaved.getId()));
  }

  @PostMapping("/update")
  public ResponseEntity updateEnvironments(@RequestBody DtEnvironmentDTO environment,
      Authentication authentication){
    dtEnvironmentService.save(environment, authentication);
    return ResponseEntity.ok().build();
  }



  @PostMapping("/delete/{id}")
  public ResponseEntity<String> deleteEnvironment(@PathVariable @NotEmpty Long id, Authentication authentication) {
    return ResponseEntity.ok(dtEnvironmentService.deleteEnvironment(id, authentication));
  }

  @GetMapping("/getAll")
  public ResponseEntity getEnvironments(Authentication authentication,
      @RequestParam(required=false, defaultValue = "ALL") String environmentType,
      @RequestParam(required=false) Long appId) {
    return ResponseEntity.ok(dtEnvironmentService.getAll(environmentType, appId, authentication));
  }

}
