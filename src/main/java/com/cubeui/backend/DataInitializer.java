package com.cubeui.backend;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFile;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.EmailDomainRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.service.AppFileStorageService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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

    private AppRepository appRepository;

    private HttpServletRequest httpServletRequest;

    private AppFileStorageService appFileStorageService;

    public DataInitializer(UserService userService, CustomerService customerService,
        CustomerRepository customerRepository, UserRepository userRepository,
        EmailDomainRepository emailDomainRepository, AppRepository appRepository,
        HttpServletRequest httpServletRequest, AppFileStorageService appFileStorageService) {

        this.userService = userService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.emailDomainRepository = emailDomainRepository;
        this.appRepository = appRepository;
        this.httpServletRequest = httpServletRequest;
        this.appFileStorageService = appFileStorageService;
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
            customer = Optional.of(this.customerService.save(httpServletRequest, customerDTO));
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
         * Remove in next Release
         */
        List<App> apps = appRepository.findAll();
        apps.forEach(app -> {
            if (app.getDisplayName() == null) {
                app.setDisplayName(app.getName());
                this.appRepository.save(app);
            }
            Optional<AppFile> appFile = this.appFileStorageService.getFileByAppId(app.getId());
            if(appFile.isEmpty()) {
                this.appFileStorageService.storeFile(null, app);
            }
        });

    }
}
