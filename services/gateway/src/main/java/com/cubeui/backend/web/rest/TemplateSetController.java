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

import com.cubeui.backend.domain.App;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.service.CubeServerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templateSet")
@Slf4j
@Secured("ROLE_ADMIN")
public class TemplateSetController {
  @Autowired
  private AppRepository appRepository;
  @Autowired
  private CubeServerService cubeServerService;
  @Autowired
  private ObjectMapper jsonMapper;

  @PostMapping("")
  public ResponseEntity addDefaultTemplateSetForExistingApps(HttpServletRequest request) {
    List<App> apps = appRepository.findAll();
    addDefaultTemplateSet(request, apps);
    return ResponseEntity.ok("Data updated");
  }

  @Async("threadPoolTaskExecutor")
  public void addDefaultTemplateSet(HttpServletRequest request, List<App> apps) {
    apps.forEach(app -> {
      ResponseEntity<byte[]> response = cubeServerService.getTemplateSetLabels(app);
      if(response.getStatusCode() == HttpStatus.OK) {
        try {
          final String body = new String(response.getBody());
          JsonNode json = jsonMapper.readTree(body);
          int numResults = Integer.parseInt(json.get("numResults").toString());
          if(numResults < 1) {
            cubeServerService.saveEmptyTemplateSetForApp(request, app);
          }
        } catch (Exception e) {
          log.info("Error in reading Json" + ", app="  + app.getName() + " message"  + e.getMessage());
        }
      }
    });
  }

}
