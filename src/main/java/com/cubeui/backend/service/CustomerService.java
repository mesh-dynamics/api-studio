package com.cubeui.backend.service;

import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Optional<Customer> getByName(String name) {
        return customerRepository.findByName(name);
    }

    public Optional<Customer> getById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer save(CustomerDTO customerDTO) {
        Optional<Customer> customer = customerRepository.findByName(customerDTO.getName());
        customer.ifPresent(c -> {
            Optional.ofNullable(customerDTO.getName()).ifPresent(name -> c.setName(name));
            Optional.ofNullable(customerDTO.getEmail()).ifPresent(name -> c.setEmail(name));
            Optional.ofNullable(customerDTO.getDomainURL()).ifPresent(name -> c.setDomainURL(name));
            this.customerRepository.save(c);
        });
        if (customer.isEmpty()){
            customer = Optional.of(this.customerRepository.save(Customer.builder()
                    .name(customerDTO.getName())
                    .email(customerDTO.getEmail())
                    .domainURL(customerDTO.getDomainURL())
                    .build()
            ));
        }
        return customer.get();
    }

    public boolean deleteCustomer(Long id) {
        Optional<Customer> existed = this.customerRepository.findById(id);
        if (existed.isPresent()) {
            this.customerRepository.delete(existed.get());
            return true;
        } else {
            return false;
        }
    }
}
