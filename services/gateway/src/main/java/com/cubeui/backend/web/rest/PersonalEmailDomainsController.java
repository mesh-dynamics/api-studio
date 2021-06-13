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

import com.cubeui.backend.domain.PersonalEmailDomains;
import com.cubeui.backend.repository.PersonalEmailDomainsRepository;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/addDomains")
@Secured("ROLE_ADMIN")
public class PersonalEmailDomainsController {
  @Autowired
  private PersonalEmailDomainsRepository personalEmailDomainsRepository;

  @PostMapping("/save")
  public ResponseEntity save(@RequestBody List<String> domains, HttpServletRequest request) {
    final List<PersonalEmailDomains> savedList = new ArrayList<>();
    for(String domain: domains) {
      if(this.personalEmailDomainsRepository.findByDomain(domain).isEmpty()) {
        PersonalEmailDomains saved = this.personalEmailDomainsRepository
            .save(PersonalEmailDomains.builder().domain(domain).build());
        savedList.add(saved);
        log.info(String.format("Domain=%s is added", domain));
      }
    }
    return ResponseEntity.ok(savedList);
  }

}
