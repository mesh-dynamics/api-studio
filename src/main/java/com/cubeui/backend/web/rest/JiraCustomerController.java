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
