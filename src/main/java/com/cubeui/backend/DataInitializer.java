package com.cubeui.backend;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.repository.*;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import java.util.List;
import java.util.Set;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Setter
@Getter
@Data
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private UserService userService;

    private CustomerService customerService;

    private CustomerRepository customerRepository;

    private UserRepository userRepository;

    private EmailDomainRepository emailDomainRepository;

    public DataInitializer(UserService userService, CustomerService customerService,
        CustomerRepository customerRepository, UserRepository userRepository,
        EmailDomainRepository emailDomainRepository
        ) {

        this.userService = userService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.emailDomainRepository = emailDomainRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");

        Optional<Customer> customer = customerRepository.findByName("Admin");
        if(customer.isEmpty()) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setName("Admin");
            customerDTO.setEmail("admin@meshdynamics.io");
            customerDTO.setDomainURLs(Set.of("admin.io"));
            customer = Optional.of(this.customerService.save(customerDTO));
       }

        Optional<User> user = userRepository.findByUsername("admin@meshdynamics.io");
        if (user.isEmpty()) {
            UserDTO userDTOAdmin = new UserDTO();
            //userDTO.setId(3L);
            userDTOAdmin.setName("Administrator");
            userDTOAdmin.setEmail("admin@meshdynamics.io");
            userDTOAdmin.setPassword("admin");
            userDTOAdmin.setCustomerId(customer.get().getId());
            userDTOAdmin.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
            userDTOAdmin.setActivated(true);
            this.userService.save(userDTOAdmin, true, false);
            log.info("User with username '{}' created", userDTOAdmin.getEmail());
        }
        /**TODO
         * need to remove in next release
         */
        List<EmailDomain> emailDomainList = emailDomainRepository.findAll();
        MultiValueMap<Long, String> domainMap= new LinkedMultiValueMap<>();
        emailDomainList.forEach(emailDomain -> domainMap.add(emailDomain.getCustomer().getId(), emailDomain.getDomain()));
        domainMap.forEach((customerId, domains) -> {
            Customer existingCustomer = customerRepository.findById(customerId).get();
            Set<String> domainUrls = existingCustomer.getDomainUrls();
            domains.forEach(d -> {
                if(!domainUrls.contains(d)) {
                    domainUrls.add(d);
                }
            });
            existingCustomer.setDomainUrls(domainUrls);
            customerRepository.save(existingCustomer);
        });
    }
}
