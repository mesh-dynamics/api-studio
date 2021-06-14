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
