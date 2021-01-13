package com.cubeui.backend;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFile;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.service.AppFileStorageService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    private AppRepository appRepository;

    private HttpServletRequest httpServletRequest;

    private AppFileStorageService appFileStorageService;

    private DevtoolEnvironmentsRepository devtoolEnvironmentsRepository;

    public DataInitializer(UserService userService, CustomerService customerService,
        CustomerRepository customerRepository, UserRepository userRepository,
        HttpServletRequest httpServletRequest, AppRepository appRepository,
        AppFileStorageService appFileStorageService, DevtoolEnvironmentsRepository devtoolEnvironmentsRepository) {

        this.userService = userService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.appRepository = appRepository;
        this.httpServletRequest = httpServletRequest;
        this.appFileStorageService = appFileStorageService;
        this.devtoolEnvironmentsRepository = devtoolEnvironmentsRepository;
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

        Optional<User> user = userRepository.findByUsernameIgnoreCase("admin@meshdynamics.io");
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
         * Remove in next release
         */
        List<DtEnvironment> dtEnvironments = devtoolEnvironmentsRepository.findAll();
        dtEnvironments.forEach(dtEnvironment -> {
            if(dtEnvironment.getApp() == null ) {
                Optional<List<App>> optionalApps = appRepository.findByCustomerId(dtEnvironment.getUser().getCustomer().getId());
                if(optionalApps.isPresent()) {
                    List<App> apps = optionalApps.get();
                    for(int i=0; i < apps.size(); i++) {
                        if(i == apps.size()-1) {
                            dtEnvironment.setApp(apps.get(i));
                            devtoolEnvironmentsRepository.save(dtEnvironment);
                        } else {
                            DtEnvironment dt = new DtEnvironment(dtEnvironment.getName());
                            dt.setApp(apps.get(i));
                            dt.setUser(dtEnvironment.getUser());
                            dt.setVars(dtEnvironment.getVars());
                            devtoolEnvironmentsRepository.save(dt);
                        }
                    }
                }
            }
        });

    }
}
