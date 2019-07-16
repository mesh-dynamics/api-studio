package com.cubeui.backend;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.repository.*;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.MailService;
import com.cubeui.backend.service.UserService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Setter
@Getter
@Data
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

    private ServiceGroupRepository serviceGroupRepository;

    private PathRepository pathRepository;

    public DataInitializer(UserService userService, CustomerService customerService, MailService mailService, AppRepository appRepository, InstanceRepository instanceRepository, ServiceRepository serviceRepository, ServiceGraphRepository serviceGraphRepository, ServiceGroupRepository serviceGroupRepository, PathRepository pathRepository) {
        this.userService = userService;
        this.customerService = customerService;
        this.mailService = mailService;

        this.appRepository = appRepository;
        this.instanceRepository = instanceRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGraphRepository = serviceGraphRepository;
        this.serviceGroupRepository = serviceGroupRepository;
        this.pathRepository = pathRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");

        if(customerService.getByName("CubeCorp").isEmpty()) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setId(1L);
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

        // ServiceGroup - MovieInfo
        if(!serviceGroupRepository.existsById(9L)) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setId(9L);
            serviceGroup.setName("GLOBAL");
            serviceGroup.setApp(appRepository.findById(4L).get());
            serviceGroupRepository.save(serviceGroup);
        }

        if(!serviceGroupRepository.existsById(10L)) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setId(10L);
            serviceGroup.setName("MovieInfo");
            serviceGroup.setApp(appRepository.findById(4L).get());
            serviceGroupRepository.save(serviceGroup);
        }

        /* MovieInfo App */
        if(!serviceRepository.existsById(11L)) {
            Service service = new Service();
            service.setId(11L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("Reviews");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(12L)) {
            Service service = new Service();
            service.setId(12L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("Ratings");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(13L)) {
            Service service = new Service();
            service.setId(13L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("Details");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(14L)) {
            Service service = new Service();
            service.setId(14L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("RestWrapJDBC");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(15L)) {
            Service service = new Service();
            service.setId(15L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("Postgres");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(16L)) {
            Service service = new Service();
            service.setId(16L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("Mysql");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(17L)) {
            Service service = new Service();
            service.setId(17L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("MongoDB");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(18L)) {
            Service service = new Service();
            service.setId(18L);
            service.setServiceGroup(serviceGroupRepository.findById(9L).get());
            service.setName("Google API");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(19L)) {
            Service service = new Service();
            service.setId(19L);
            service.setServiceGroup(serviceGroupRepository.findById(10L).get());
            service.setName("Auth");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(20L)) {
            Service service = new Service();
            service.setId(20L);
            service.setServiceGroup(serviceGroupRepository.findById(10L).get());
            service.setName("Actions");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(21L)) {
            Service service = new Service();
            service.setId(21L);
            service.setServiceGroup(serviceGroupRepository.findById(10L).get());
            service.setName("List");
            service.setApp(appRepository.findById(4L).get());
            this.serviceRepository.save(service);
        }

        /* MovieInfo - ServiceGraph */
        // Auth - RestWrapJDBC
        if(!serviceGraphRepository.existsById(22L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(22L);
            serviceGraph.setFromService(serviceRepository.findById(19L).get());
            serviceGraph.setToService(serviceRepository.findById(14L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Actions - RestWrapJDBC
        if(!serviceGraphRepository.existsById(23L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(23L);
            serviceGraph.setFromService(serviceRepository.findById(20L).get());
            serviceGraph.setToService(serviceRepository.findById(14L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // List - RestWrapJDBC
        if(!serviceGraphRepository.existsById(24L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(24L);
            serviceGraph.setFromService(serviceRepository.findById(21L).get());
            serviceGraph.setToService(serviceRepository.findById(14L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // RestWrapJDBC - Postgres
        if(!serviceGraphRepository.existsById(25L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(25L);
            serviceGraph.setFromService(serviceRepository.findById(14L).get());
            serviceGraph.setToService(serviceRepository.findById(15L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // List - Reviews
        if(!serviceGraphRepository.existsById(26L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(26L);
            serviceGraph.setFromService(serviceRepository.findById(21L).get());
            serviceGraph.setToService(serviceRepository.findById(11L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // List - Details
        if(!serviceGraphRepository.existsById(27L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(27L);
            serviceGraph.setFromService(serviceRepository.findById(21L).get());
            serviceGraph.setToService(serviceRepository.findById(13L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // Reviews - Ratings
        if(!serviceGraphRepository.existsById(28L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(28L);
            serviceGraph.setFromService(serviceRepository.findById(11L).get());
            serviceGraph.setToService(serviceRepository.findById(12L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // Details - Google API
        if(!serviceGraphRepository.existsById(29L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(29L);
            serviceGraph.setFromService(serviceRepository.findById(13L).get());
            serviceGraph.setToService(serviceRepository.findById(18L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // Ratings - Mysql
        if(!serviceGraphRepository.existsById(30L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(30L);
            serviceGraph.setFromService(serviceRepository.findById(12L).get());
            serviceGraph.setToService(serviceRepository.findById(16L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // Ratings - MongoDB
        if(!serviceGraphRepository.existsById(31L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(31L);
            serviceGraph.setFromService(serviceRepository.findById(12L).get());
            serviceGraph.setToService(serviceRepository.findById(17L).get());
            serviceGraph.setApp(appRepository.findById(4L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }

        // ServiceGroup - Cube
        if(!serviceGroupRepository.existsById(32L)) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setId(32L);
            serviceGroup.setName("GLOBAL");
            serviceGroup.setApp(appRepository.findById(5L).get());
            serviceGroupRepository.save(serviceGroup);
        }

        if(!serviceGroupRepository.existsById(33L)) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setId(33L);
            serviceGroup.setName("UI");
            serviceGroup.setApp(appRepository.findById(5L).get());
            serviceGroupRepository.save(serviceGroup);
        }

        if(!serviceGroupRepository.existsById(34L)) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setId(34L);
            serviceGroup.setName("Record");
            serviceGroup.setApp(appRepository.findById(5L).get());
            serviceGroupRepository.save(serviceGroup);
        }

        if(!serviceGroupRepository.existsById(35L)) {
            ServiceGroup serviceGroup = new ServiceGroup();
            serviceGroup.setId(35L);
            serviceGroup.setName("Replay");
            serviceGroup.setApp(appRepository.findById(5L).get());
            serviceGroupRepository.save(serviceGroup);
        }

        /* Cube App */
        if(!serviceRepository.existsById(36L)) {
            Service service = new Service();
            service.setId(36L);
            service.setServiceGroup(serviceGroupRepository.findById(32L).get());
            service.setName("Mock");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(37L)) {
            Service service = new Service();
            service.setId(37L);
            service.setServiceGroup(serviceGroupRepository.findById(32L).get());
            service.setName("Postgres");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(38L)) {
            Service service = new Service();
            service.setId(38L);
            service.setServiceGroup(serviceGroupRepository.findById(32L).get());
            service.setName("Solr");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(39L)) {
            Service service = new Service();
            service.setId(39L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Account/User");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(40L)) {
            Service service = new Service();
            service.setId(40L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("App");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(41L)) {
            Service service = new Service();
            service.setId(41L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Instance");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(42L)) {
            Service service = new Service();
            service.setId(42L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Service/Graph");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(43L)) {
            Service service = new Service();
            service.setId(43L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("TestConfig");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(44L)) {
            Service service = new Service();
            service.setId(44L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Record");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(45L)) {
            Service service = new Service();
            service.setId(45L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Replay");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(46L)) {
            Service service = new Service();
            service.setId(46L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Analyze");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(47L)) {
            Service service = new Service();
            service.setId(47L);
            service.setServiceGroup(serviceGroupRepository.findById(33L).get());
            service.setName("Customer");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(48L)) {
            Service service = new Service();
            service.setId(48L);
            service.setServiceGroup(serviceGroupRepository.findById(34L).get());
            service.setName("Record/SetDefault");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(49L)) {
            Service service = new Service();
            service.setId(49L);
            service.setServiceGroup(serviceGroupRepository.findById(34L).get());
            service.setName("Record/HTTP");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(50L)) {
            Service service = new Service();
            service.setId(50L);
            service.setServiceGroup(serviceGroupRepository.findById(34L).get());
            service.setName("Record/Java");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(51L)) {
            Service service = new Service();
            service.setId(51L);
            service.setServiceGroup(serviceGroupRepository.findById(34L).get());
            service.setName("Record/Record");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(52L)) {
            Service service = new Service();
            service.setId(52L);
            service.setServiceGroup(serviceGroupRepository.findById(34L).get());
            service.setName("Record/Collections");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(53L)) {
            Service service = new Service();
            service.setId(53L);
            service.setServiceGroup(serviceGroupRepository.findById(35L).get());
            service.setName("Replay/SetDefault");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(54L)) {
            Service service = new Service();
            service.setId(54L);
            service.setServiceGroup(serviceGroupRepository.findById(35L).get());
            service.setName("Replay/Replay");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(55L)) {
            Service service = new Service();
            service.setId(55L);
            service.setServiceGroup(serviceGroupRepository.findById(35L).get());
            service.setName("Analyze/Setup");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(56L)) {
            Service service = new Service();
            service.setId(56L);
            service.setServiceGroup(serviceGroupRepository.findById(35L).get());
            service.setName("Analyze/Retrieval");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        if(!serviceRepository.existsById(57L)) {
            Service service = new Service();
            service.setId(57L);
            service.setServiceGroup(serviceGroupRepository.findById(35L).get());
            service.setName("Analyze/Analysis");
            service.setApp(appRepository.findById(5L).get());
            this.serviceRepository.save(service);
        }

        /* Cube - ServiceGraph */
        // Mock - Solr
        if(!serviceGraphRepository.existsById(58L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(58L);
            serviceGraph.setFromService(serviceRepository.findById(36L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record/SetDefault - Solr
        if(!serviceGraphRepository.existsById(59L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(59L);
            serviceGraph.setFromService(serviceRepository.findById(48L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record/HTTP - Solr
        if(!serviceGraphRepository.existsById(60L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(60L);
            serviceGraph.setFromService(serviceRepository.findById(49L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record/Java - Solr
        if(!serviceGraphRepository.existsById(61L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(61L);
            serviceGraph.setFromService(serviceRepository.findById(50L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record/Record - Solr
        if(!serviceGraphRepository.existsById(62L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(62L);
            serviceGraph.setFromService(serviceRepository.findById(51L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record/Collections - Solr
        if(!serviceGraphRepository.existsById(63L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(63L);
            serviceGraph.setFromService(serviceRepository.findById(52L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Replay/SetDefault - Solr
        if(!serviceGraphRepository.existsById(64L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(64L);
            serviceGraph.setFromService(serviceRepository.findById(53L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Replay/Replay - Solr
        if(!serviceGraphRepository.existsById(65L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(65L);
            serviceGraph.setFromService(serviceRepository.findById(54L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Analyze/Setup - Solr
        if(!serviceGraphRepository.existsById(66L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(66L);
            serviceGraph.setFromService(serviceRepository.findById(55L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Analyze/Retrieval - Solr
        if(!serviceGraphRepository.existsById(67L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(67L);
            serviceGraph.setFromService(serviceRepository.findById(56L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Analyze/Analysis - Solr
        if(!serviceGraphRepository.existsById(68L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(68L);
            serviceGraph.setFromService(serviceRepository.findById(57L).get());
            serviceGraph.setToService(serviceRepository.findById(38L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Account/User - Postgres
        if(!serviceGraphRepository.existsById(69L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(69L);
            serviceGraph.setFromService(serviceRepository.findById(39L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // App - Solr
        if(!serviceGraphRepository.existsById(70L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(70L);
            serviceGraph.setFromService(serviceRepository.findById(40L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Instance - Solr
        if(!serviceGraphRepository.existsById(71L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(71L);
            serviceGraph.setFromService(serviceRepository.findById(41L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Service/Graph - Solr
        if(!serviceGraphRepository.existsById(72L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(72L);
            serviceGraph.setFromService(serviceRepository.findById(42L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // TestConfig - Solr
        if(!serviceGraphRepository.existsById(73L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(73L);
            serviceGraph.setFromService(serviceRepository.findById(43L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Customer - Solr
        if(!serviceGraphRepository.existsById(74L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(74L);
            serviceGraph.setFromService(serviceRepository.findById(47L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record - Record/SetDefault
        if(!serviceGraphRepository.existsById(75L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(75L);
            serviceGraph.setFromService(serviceRepository.findById(44L).get());
            serviceGraph.setToService(serviceRepository.findById(48L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record - Record/HTTP
        if(!serviceGraphRepository.existsById(76L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(76L);
            serviceGraph.setFromService(serviceRepository.findById(44L).get());
            serviceGraph.setToService(serviceRepository.findById(49L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record - Record/Java
        if(!serviceGraphRepository.existsById(77L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(77L);
            serviceGraph.setFromService(serviceRepository.findById(44L).get());
            serviceGraph.setToService(serviceRepository.findById(50L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record - Record/Record
        if(!serviceGraphRepository.existsById(78L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(78L);
            serviceGraph.setFromService(serviceRepository.findById(44L).get());
            serviceGraph.setToService(serviceRepository.findById(51L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Record - Record/Collections
        if(!serviceGraphRepository.existsById(79L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(79L);
            serviceGraph.setFromService(serviceRepository.findById(44L).get());
            serviceGraph.setToService(serviceRepository.findById(52L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Replay - Replay/SetDefault
        if(!serviceGraphRepository.existsById(80L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(80L);
            serviceGraph.setFromService(serviceRepository.findById(45L).get());
            serviceGraph.setToService(serviceRepository.findById(53L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Replay - Replay/Replay
        if(!serviceGraphRepository.existsById(81L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(81L);
            serviceGraph.setFromService(serviceRepository.findById(45L).get());
            serviceGraph.setToService(serviceRepository.findById(54L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Analyze - Analyze/Setup
        if(!serviceGraphRepository.existsById(82L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(82L);
            serviceGraph.setFromService(serviceRepository.findById(46L).get());
            serviceGraph.setToService(serviceRepository.findById(55L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Analyze - Analyze/Retrieval
        if(!serviceGraphRepository.existsById(83L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(83L);
            serviceGraph.setFromService(serviceRepository.findById(46L).get());
            serviceGraph.setToService(serviceRepository.findById(56L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Analyze - Analyze/Analysis
        if(!serviceGraphRepository.existsById(83L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(83L);
            serviceGraph.setFromService(serviceRepository.findById(46L).get());
            serviceGraph.setToService(serviceRepository.findById(57L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
    }
}
