package com.cubeui.backend.web.rest;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.EmailDomain;
import com.cubeui.backend.domain.JiraCustomerDefaultCredentials;
import com.cubeui.backend.repository.EmailDomainRepository;
import com.cubeui.backend.repository.JiraCustomerCredentialsRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.web.ErrorResponse;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.ResponseEntity.*;

@RestController
@Slf4j
@RequestMapping("/api/customer")
public class CustomerController {

    private CustomerService customerService;
    private EmailDomainRepository emailDomainRepository;
    private JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository;

    public CustomerController(CustomerService customerService, EmailDomainRepository emailDomainRepository,
                              JiraCustomerCredentialsRepository jiraCustomerCredentialsRepository) {
        this.customerService = customerService;
        this.emailDomainRepository = emailDomainRepository;
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
            Customer saved = this.customerService.save(customerDTO);

            EmailDomain domain = new EmailDomain();
            domain.setDomain(customerDTO.getDomainURL());
            domain.setCustomer(saved);
            this.emailDomainRepository.save(domain);
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
            Customer saved = this.customerService.save(customerDTO);
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

    @GetMapping("/delete/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {
        if (customerService.deleteCustomer(id)) {
            return ok("Customer '" + id + "' removed successfully");
        } else {
            throw new RecordNotFoundException("Customer with id '" + id + "' not found.");
        }
    }
}
