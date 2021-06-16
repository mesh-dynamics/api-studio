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

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.JiraCustomerDTO;
import com.cubeui.backend.domain.JiraCustomerDefaultCredentials;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.JiraCustomerCredentialsRepository;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/jira/customer")
public class JiraCustomerController {

    @Autowired
    JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository;

    @Autowired
    CustomerRepository customerRepository;

    @PostMapping("")
    public ResponseEntity save(@RequestBody JiraCustomerDTO jiraCustomerDTO, HttpServletRequest httpServletRequest) {
        if(jiraCustomerDTO.getId() != null) {
            return status(BAD_REQUEST).body(new ErrorResponse("JiraCustomer with ID " + jiraCustomerDTO.getId() + ", should be empty"));
        }
        Optional<Customer> customer = customerRepository.findById(jiraCustomerDTO.getCustomerId());
        if (customer.isEmpty()) {
            return status(BAD_REQUEST).body(new ErrorResponse("Customer with ID " + jiraCustomerDTO.getCustomerId() + " does not exist"));
        }
        Optional<JiraCustomerDefaultCredentials> jiraCustomerDefaultCredentials = jiraCustomerCredentialsRepository.findByCustomerId(jiraCustomerDTO.getCustomerId());
        if (jiraCustomerDefaultCredentials.isPresent()) {
            return ok(jiraCustomerDefaultCredentials);
        }
        JiraCustomerDefaultCredentials saved = jiraCustomerCredentialsRepository.save(
                JiraCustomerDefaultCredentials.builder()
                        .APIKey(jiraCustomerDTO.getApiKey())
                        .jiraBaseURL(jiraCustomerDTO.getJiraBaseURL())
                        .userName(jiraCustomerDTO.getUserName())
                        .customer(customer.get())
                        .build()
        );
        return created(
                ServletUriComponentsBuilder
                        .fromContextPath(httpServletRequest)
                        .path(httpServletRequest.getServletPath() + "/{id}")
                        .buildAndExpand(saved.getId())
                        .toUri())
                .body(saved);
    }
}
