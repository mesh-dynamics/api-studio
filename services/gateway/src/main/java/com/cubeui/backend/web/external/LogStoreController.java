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

import com.cubeui.backend.domain.DTO.LogStoreDTO;
import com.cubeui.backend.security.Validation;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logStore")
public class LogStoreController {

  private static Logger LOGGER = LogManager.getLogger(LogStoreController.class);

  @Autowired
  private Validation validation;

  @PostMapping("/{customerId}")
  public ResponseEntity postLogData(HttpServletRequest request, @RequestBody LogStoreDTO postBody,
      @PathVariable String customerId, Authentication authentication) {

    validation.validateCustomerName(authentication, customerId);
    LOGGER.log(postBody.level, new ObjectMessage(Map.of("customerId", customerId,
        "app", postBody.app, "instance", postBody.instance, "service", postBody.service,
        "version", postBody.version, "sourceType", postBody.sourceType,
        "logMessage", postBody.logMessage, "clientTimeStamp", postBody.clientTimeStamp)));
    return ResponseEntity.ok("Data added");
  }
}
