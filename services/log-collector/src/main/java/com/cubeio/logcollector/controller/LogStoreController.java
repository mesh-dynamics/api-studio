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

package com.cubeio.logcollector.controller;

import com.cubeio.logcollector.security.Validation;

import com.cubeio.logcollector.utils.LogUtils;
import io.md.logger.LogStoreDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LogStoreController {


  private static Logger LOGGER = LoggerFactory.getLogger(LogStoreController.class);

  @Autowired
  private Validation validation;

  @GetMapping("/health")
  public ResponseEntity healthCheck(HttpServletRequest request) {

    return ResponseEntity.ok("LogStoreController OK");
  }

  @PostMapping("/logStore/{customerId}")
  public ResponseEntity postLogData(HttpServletRequest request, @RequestBody LogStoreDTO postBody,
      @PathVariable String customerId, Authentication authentication) {

    validation.validateCustomerName(authentication, customerId);

    postBody.customerId = customerId;
    LogUtils.log(LOGGER , postBody , Optional.empty());

    return ResponseEntity.ok("Data added");
  }

  @PostMapping("/gaurav")
  public ResponseEntity testGaurav(HttpServletRequest request, @RequestBody byte[] body, Authentication authentication) {

    LOGGER.info("Hi My Name is Gaurav");
    return ResponseEntity.ok("Data added "+Math.random()*100);
  }
}
