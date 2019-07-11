package com.cubeui.backend;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.MailService;
import com.cubeui.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private UserService userService;

    private CustomerService customerService;

    private MailService mailService;

    private AppRepository appRepository;

    private InstanceRepository instanceRepository;

    private ServiceRepository serviceRepository;

    private ServiceGraphRepository serviceGraphRepository;

    public DataInitializer(UserService userService, CustomerService customerService, MailService mailService, AppRepository appRepository, InstanceRepository instanceRepository, ServiceRepository serviceRepository, ServiceGraphRepository serviceGraphRepository) {
        this.userService = userService;
        this.customerService = customerService;
        this.mailService = mailService;

        this.appRepository = appRepository;
        this.instanceRepository = instanceRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGraphRepository = serviceGraphRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");

        if(customerService.getByName("CubeCorp").isEmpty()) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setName("CubeCorp");
            customerDTO.setEmail("admin@cubecorp.io");
            customerDTO.setDomainURL("cube.cubecorp.io");
            Customer customer = this.customerService.save(customerDTO);
        }

        if (userService.getByUsername("demo@cubecorp.io").isEmpty()){
            UserDTO userDTO = new UserDTO();
            userDTO.setName("Demo");
            userDTO.setEmail("demo@cubecorp.io");
            userDTO.setPassword("password123");
            userDTO.setCustomerId(1L);
            userDTO.setRoles(Arrays.asList("ROLE_USER"));
            userDTO.setActivated(true);
            User user = this.userService.save(userDTO, true);
            log.info("User with email '{}' created", user.getUsername());
            mailService.sendActivationEmail(user);
        }

        if (userService.getByUsername("admin").isEmpty()){
            UserDTO userDTO = new UserDTO();
            userDTO.setName("Administrator");
            userDTO.setEmail("admin");
            userDTO.setPassword("admin");
            userDTO.setCustomerId(1L);
            userDTO.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
            userDTO.setActivated(true);
            this.userService.save(userDTO, true);
            log.info("User with username '{}' created", userDTO.getEmail());
        }

        if(!appRepository.existsById(4L)) {
            App app = new App();
            app.setId(4L);
            app.setName("MovieInfo");
            app.setCustomer(this.customerService.getById(1L).get());
            this.appRepository.save(app);
        }

        if(!appRepository.existsById(5L)) {
            App app = new App();
            app.setId(5L);
            app.setName("Cube");
            app.setCustomer(this.customerService.getById(1L).get());
            this.appRepository.save(app);
        }

        if(!instanceRepository.existsById(6L)) {
            Instance instance = new Instance();
            instance.setId(6L);
            instance.setName("PROD");
            instance.setGatewayEndpoint("demo.cubecorp.io");
            instance.setCustomer(this.customerService.getById(1L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(7L)) {
            Instance instance = new Instance();
            instance.setId(7L);
            instance.setName("STAGING");
            instance.setGatewayEndpoint("staging.cubecorp.io");
            instance.setCustomer(this.customerService.getById(1L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(8L)) {
            Instance instance = new Instance();
            instance.setId(8L);
            instance.setName("DOGFOODING");
            instance.setGatewayEndpoint("dogfooding.cubecorp.io");
            instance.setCustomer(this.customerService.getById(1L).get());
            this.instanceRepository.save(instance);
        }

        /* MovieInfo App */
        if(!serviceRepository.existsById(9L)) {
            Service service = new Service();
            service.setId(9L);
            service.setName("Reviews");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(10L)) {
            Service service = new Service();
            service.setId(10L);
            service.setName("Ratings");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(11L)) {
            Service service = new Service();
            service.setId(11L);
            service.setName("Details");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(12L)) {
            Service service = new Service();
            service.setId(12L);
            service.setName("RestWrapJDBC");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(13L)) {
            Service service = new Service();
            service.setId(13L);
            service.setName("MovieInfo");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        /* Cube App */
        if(!serviceRepository.existsById(14L)) {
            Service service = new Service();
            service.setId(14L);
            service.setName("Record");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(15L)) {
            Service service = new Service();
            service.setId(15L);
            service.setName("Replay");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(16L)) {
            Service service = new Service();
            service.setId(16L);
            service.setName("Mock");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(17L)) {
            Service service = new Service();
            service.setId(17L);
            service.setName("UI");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        /* MovieInfo - ServiceGraph */
        // MovieInfo - Reviews
        if(!serviceGraphRepository.existsById(18L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(18L);
            serviceGraph.setFromService(serviceRepository.findById(13L).get());
            serviceGraph.setToService(serviceRepository.findById(9L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // MovieInfo - Ratings
        if(!serviceGraphRepository.existsById(19L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(19L);
            serviceGraph.setFromService(serviceRepository.findById(13L).get());
            serviceGraph.setToService(serviceRepository.findById(10L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // MovieInfo - Details
        if(!serviceGraphRepository.existsById(20L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(20L);
            serviceGraph.setFromService(serviceRepository.findById(13L).get());
            serviceGraph.setToService(serviceRepository.findById(11L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // MovieInfo - RestWrapJDBC
        if(!serviceGraphRepository.existsById(21L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(21L);
            serviceGraph.setFromService(serviceRepository.findById(13L).get());
            serviceGraph.setToService(serviceRepository.findById(12L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        /* Cube - ServiceGraph */
        // UI - Record
        if(!serviceGraphRepository.existsById(22L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(22L);
            serviceGraph.setFromService(serviceRepository.findById(17L).get());
            serviceGraph.setToService(serviceRepository.findById(14L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // UI - Replay
        if(!serviceGraphRepository.existsById(23L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(23L);
            serviceGraph.setFromService(serviceRepository.findById(17L).get());
            serviceGraph.setToService(serviceRepository.findById(15L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Mock
        if(!serviceGraphRepository.existsById(24L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(24L);
            serviceGraph.setFromService(serviceRepository.findById(17L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
    }
}
