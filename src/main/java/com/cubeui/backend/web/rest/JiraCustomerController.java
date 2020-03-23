package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.JiraCustomerDTO;
import com.cubeui.backend.domain.JiraCustomer;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.JiraCustomerRepository;
import com.cubeui.backend.web.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@RequestMapping("/api/jira/customer")
public class JiraCustomerController {

    @Autowired
    JiraCustomerRepository jiraCustomerRepository;

    @Autowired
    CustomerRepository customerRepository;

    @GetMapping("")
    public ResponseEntity all(HttpServletRequest httpServletRequest) {
        return ok(jiraCustomerRepository.findAll());
    }


    @PostMapping("")
    public ResponseEntity save(@RequestBody JiraCustomerDTO jiraCustomerDTO, HttpServletRequest httpServletRequest) {
        if(jiraCustomerDTO.getId() != null) {
            return status(FORBIDDEN).body(new ErrorResponse("JiraCustomer with ID " + jiraCustomerDTO.getId() + ", should be empty"));
        }
        if (jiraCustomerDTO.getCustomerId() == null) {
            return status(FORBIDDEN).body(new ErrorResponse("Customer Id is null"));
        }
        Optional<Customer> customer = customerRepository.findById(jiraCustomerDTO.getCustomerId());
        if (customer.isEmpty()) {
            return status(FORBIDDEN).body(new ErrorResponse("Customer with ID " + jiraCustomerDTO.getCustomerId() + " does not exist"));
        }
        Optional<JiraCustomer> jiraCustomer = jiraCustomerRepository.findByCustomerId(jiraCustomerDTO.getCustomerId());
        if (jiraCustomer.isPresent()) {
            return ok(jiraCustomer);
        }
        JiraCustomer saved = jiraCustomerRepository.save(
                JiraCustomer.builder()
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
