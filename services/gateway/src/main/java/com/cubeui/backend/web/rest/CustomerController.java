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
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.JiraCustomerDefaultCredentials;
import com.cubeui.backend.repository.JiraCustomerCredentialsRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@Slf4j
@RequestMapping("/api/customer")
@Secured("ROLE_ADMIN")
public class CustomerController {

    private CustomerService customerService;
    private JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository;

    public CustomerController(CustomerService customerService,
                              JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository) {
        this.customerService = customerService;
        this.jiraCustomerCredentialsRepository = jiraCustomerCredentialsRepository;
    }

    @GetMapping("")
    public ResponseEntity getAll() {
        return ok(this.customerService.getAllCustomers());
    }

    @PostMapping("/save")
    public ResponseEntity save(@RequestBody CustomerDTO customerDTO, HttpServletRequest request) {
        Optional<Customer> customer = this.customerService.getByName(customerDTO.getName());
        if (customer.isPresent())
        {
            return ok(customer);
        }
        if (customer.isEmpty()) {
            for(String domainUrl: customerDTO.getDomainURLs()) {
                Optional<Customer> existingCustomerForDomainUrl = this.customerService
                    .getByDomainUrl(domainUrl);
                if (existingCustomerForDomainUrl.isPresent()) {
                    return status(FORBIDDEN).body(new ErrorResponse(
                        "Customer with domain '" + domainUrl + "' already exists."));
                }
            }
            Customer saved = this.customerService.save(request, customerDTO);

            Optional<JiraCustomerDefaultCredentials> jiraCustomerDefaultCredentials = jiraCustomerCredentialsRepository.findByCustomerId(saved.getId());
            if(jiraCustomerDefaultCredentials.isEmpty()) {
                log.info("Customer is created without jira credentials");
            }
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/customers/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            return status(FORBIDDEN).body(new ErrorResponse("Customer with name '" + customer.get().getName() + "' already exists."));
        }
    }

    @PostMapping("/update")
    public ResponseEntity update(@RequestBody CustomerDTO customerDTO, HttpServletRequest request) {
        Optional<Customer> customer = this.customerService.getByName(customerDTO.getName());
        if (customer.isPresent()) {
            Customer saved = this.customerService.save(request, customerDTO);
            return created(
                    ServletUriComponentsBuilder
                            .fromContextPath(request)
                            .path("/api/customers/{id}")
                            .buildAndExpand(saved.getId())
                            .toUri())
                    .body(saved);
        } else {
            throw new RecordNotFoundException("Customer with name '" + customerDTO.getName() + "' not found.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity get(@PathVariable("id") Long id) {
        return ok(this.customerService.getById(id)
                .orElseThrow(() -> new RecordNotFoundException("Customer with id '" + id + "' not found.")));
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        Optional<Customer> existed = this.customerService.getById(id);
        return existed.map(customer -> this.customerService.deleteCustomer(customer))
            .orElseThrow(() -> new RecordNotFoundException("Customer with id '" + id + "' not found."));
    }

    @PostMapping("/deleteByCustomerName/{customerName}")
    public ResponseEntity deleteByCustomerName(@PathVariable("customerName") String customerName) {
        Optional<Customer> existed = this.customerService.getByName(customerName);
        return existed.map(customer -> this.customerService.deleteCustomer(customer))
            .orElseThrow(() ->new RecordNotFoundException("Customer with name '" + customerName + "' not found."));
    }

    @PostMapping("/deleteByCustomerDomain/{customerDomain}")
    public ResponseEntity deleteByCustomerDomain(@PathVariable("customerDomain") String customerDomain) {
        Optional<Customer> existed = this.customerService.getByDomainUrl(customerDomain);
        return existed.map(customer -> this.customerService.deleteCustomer(customer))
            .orElseThrow(() ->new RecordNotFoundException("Customer with domain '" + customerDomain + "' not found."));
    }
}
