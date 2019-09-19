package com.cubeui.backend;

import com.cubeui.backend.domain.*;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.repository.*;
import com.cubeui.backend.service.CustomerService;
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

    private AppRepository appRepository;

    private InstanceRepository instanceRepository;

    private ServiceRepository serviceRepository;

    private ServiceGraphRepository serviceGraphRepository;

    private ServiceGroupRepository serviceGroupRepository;

    private PathRepository pathRepository;

    private TestConfigRepository testConfigRepository;

    private TestIntermediateServiceRepository testIntermediateServiceRepository;

    private TestVirtualizedServiceRepository testVirtualizedServiceRepository;

    private TestPathRepository testPathRepository;

    private CustomerRepository customerRepository;

    private UserRepository userRepository;

    public DataInitializer(UserService userService, CustomerService customerService, AppRepository appRepository, InstanceRepository instanceRepository, ServiceRepository serviceRepository, ServiceGraphRepository serviceGraphRepository, ServiceGroupRepository serviceGroupRepository, PathRepository pathRepository, TestConfigRepository testConfigRepository, TestIntermediateServiceRepository testIntermediateServiceRepository, TestVirtualizedServiceRepository testVirtualizedServiceRepository, TestPathRepository testPathRepository, CustomerRepository customerRepository, UserRepository userRepository) {
        this.userService = userService;
        this.customerService = customerService;

        this.appRepository = appRepository;
        this.instanceRepository = instanceRepository;
        this.serviceRepository = serviceRepository;
        this.serviceGraphRepository = serviceGraphRepository;
        this.serviceGroupRepository = serviceGroupRepository;
        this.pathRepository = pathRepository;
        this.testConfigRepository = testConfigRepository;
        this.testIntermediateServiceRepository = testIntermediateServiceRepository;
        this.testVirtualizedServiceRepository = testVirtualizedServiceRepository;
        this.testPathRepository = testPathRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");

        if(!customerRepository.existsById(1L)) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setId(1L);
            customerDTO.setName("CubeCorp");
            customerDTO.setEmail("admin@cubecorp.io");
            customerDTO.setDomainURL("cube.cubecorp.io");
            Customer customer = this.customerService.save(customerDTO);
        }

        if (!userRepository.existsById(2L)){
            UserDTO userDTO = new UserDTO();
            userDTO.setId(2L);
            userDTO.setName("Demo");
            userDTO.setEmail("demo@cubecorp.io");
            userDTO.setPassword("password123");
            userDTO.setCustomerId(1L);
            userDTO.setRoles(Arrays.asList("ROLE_USER"));
            userDTO.setActivated(true);
            User user = this.userService.save(userDTO, true);
            log.info("User with email '{}' created", user.getUsername());
        }

        if (!userRepository.existsById(3L)){
            UserDTO userDTO = new UserDTO();
            userDTO.setId(3L);
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
            instance.setGatewayEndpoint("http://demo.dev.cubecorp.io");
            instance.setApp(this.appRepository.findById(4L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(7L)) {
            Instance instance = new Instance();
            instance.setId(7L);
            instance.setName("PROD");
            instance.setGatewayEndpoint("http://staging1.dev.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(8L)) {
            Instance instance = new Instance();
            instance.setId(8L);
            instance.setName("STAGING");
            instance.setGatewayEndpoint("http://staging2.dev.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
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
        // App - Postgres
        if(!serviceGraphRepository.existsById(70L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(70L);
            serviceGraph.setFromService(serviceRepository.findById(40L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Instance - Postgres
        if(!serviceGraphRepository.existsById(71L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(71L);
            serviceGraph.setFromService(serviceRepository.findById(41L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Service/Graph - Postgres
        if(!serviceGraphRepository.existsById(72L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(72L);
            serviceGraph.setFromService(serviceRepository.findById(42L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // TestConfig - Postgres
        if(!serviceGraphRepository.existsById(73L)) {
            ServiceGraph serviceGraph = new ServiceGraph();
            serviceGraph.setId(73L);
            serviceGraph.setFromService(serviceRepository.findById(43L).get());
            serviceGraph.setToService(serviceRepository.findById(37L).get());
            serviceGraph.setApp(appRepository.findById(5L).get());
            this.serviceGraphRepository.save(serviceGraph);
        }
        // Customer - Postgres
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
        // Paths - MovieInfo - Auth
        if(!pathRepository.existsById(84L)) {
            Path path = new Path();
            path.setId(84L);
            path.setPath("/authenticate");
            path.setService(serviceRepository.findById(19L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(85L)) {
            Path path = new Path();
            path.setId(85L);
            path.setPath("/health");
            path.setService(serviceRepository.findById(19L).get());
            pathRepository.save(path);
        }
        // Paths - MovieInfo - List
        if(!pathRepository.existsById(86L)) {
            Path path = new Path();
            path.setId(86L);
            path.setPath("/listmovies");
            path.setService(serviceRepository.findById(21L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(87L)) {
            Path path = new Path();
            path.setId(87L);
            path.setPath("/liststores");
            path.setService(serviceRepository.findById(21L).get());
            pathRepository.save(path);
        }
        // Paths - MovieInfo - Actions
        if(!pathRepository.existsById(86L)) {
            Path path = new Path();
            path.setId(86L);
            path.setPath("/rentmovie");
            path.setService(serviceRepository.findById(20L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(87L)) {
            Path path = new Path();
            path.setId(87L);
            path.setPath("/returnmovie");
            path.setService(serviceRepository.findById(20L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(88L)) {
            Path path = new Path();
            path.setId(88L);
            path.setPath("/overduerental");
            path.setService(serviceRepository.findById(20L).get());
            pathRepository.save(path);
        }
        // Paths - MovieInfo - RestWrapJDBC
        if(!pathRepository.existsById(89L)) {
            Path path = new Path();
            path.setId(89L);
            path.setPath("/health");
            path.setService(serviceRepository.findById(14L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(90L)) {
            Path path = new Path();
            path.setId(90L);
            path.setPath("/initialize");
            path.setService(serviceRepository.findById(14L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(91L)) {
            Path path = new Path();
            path.setId(91L);
            path.setPath("/query");
            path.setService(serviceRepository.findById(14L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(92L)) {
            Path path = new Path();
            path.setId(92L);
            path.setPath("/update");
            path.setService(serviceRepository.findById(14L).get());
            pathRepository.save(path);
        }
        // Paths - MovieInfo - Reviews
        if(!pathRepository.existsById(93L)) {
            Path path = new Path();
            path.setId(93L);
            path.setPath("/health");
            path.setService(serviceRepository.findById(11L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(94L)) {
            Path path = new Path();
            path.setId(94L);
            path.setPath("/reviews/*");
            path.setService(serviceRepository.findById(11L).get());
            pathRepository.save(path);
        }
        // Paths - MovieInfo - Ratings
        if(!pathRepository.existsById(95L)) {
            Path path = new Path();
            path.setId(95L);
            path.setPath("/health");
            path.setService(serviceRepository.findById(12L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(96L)) {
            Path path = new Path();
            path.setId(96L);
            path.setPath("/ratings/*");
            path.setService(serviceRepository.findById(12L).get());
            pathRepository.save(path);
        }
        // Paths - MovieInfo - Details
        if(!pathRepository.existsById(97L)) {
            Path path = new Path();
            path.setId(97L);
            path.setPath("/health");
            path.setService(serviceRepository.findById(13L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(98L)) {
            Path path = new Path();
            path.setId(98L);
            path.setPath("/details/*");
            path.setService(serviceRepository.findById(13L).get());
            pathRepository.save(path);
        }
        // TestConfig - MovieInfo - List
        if(!testConfigRepository.existsById(99L)) {
            TestConfig testConfig = new TestConfig();
            testConfig.setId(99L);
            testConfig.setApp(appRepository.findById(4L).get());
            testConfig.setTestConfigName("MovieInfo-List");
            testConfig.setGatewayService(serviceRepository.findById(21L).get());
            testConfigRepository.save(testConfig);
        }
        if(!testPathRepository.existsById(100L)) {
            TestPath testPath = new TestPath();
            testPath.setId(100L);
            testPath.setPath(pathRepository.findById(86L).get());
            testPath.setTestConfig(testConfigRepository.findById(99L).get());
            testPathRepository.save(testPath);
        }
        if(!testPathRepository.existsById(101L)) {
            TestPath testPath = new TestPath();
            testPath.setId(101L);
            testPath.setPath(pathRepository.findById(87L).get());
            testPath.setTestConfig(testConfigRepository.findById(99L).get());
            testPathRepository.save(testPath);
        }
        // Paths - Cube - Record/SetDefault
        if(!pathRepository.existsById(102L)) {
            Path path = new Path();
            path.setId(102L);
            path.setPath("/cs/health");
            path.setService(serviceRepository.findById(48L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(103L)) {
            Path path = new Path();
            path.setId(103L);
            path.setPath("/cs/setdefault/*");
            path.setService(serviceRepository.findById(48L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Record/HTTP
        if(!pathRepository.existsById(104L)) {
            Path path = new Path();
            path.setId(104L);
            path.setPath("/cs/req");
            path.setService(serviceRepository.findById(49L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(105L)) {
            Path path = new Path();
            path.setId(105L);
            path.setPath("/cs/res");
            path.setService(serviceRepository.findById(49L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(106L)) {
            Path path = new Path();
            path.setId(106L);
            path.setPath("/cs/rr/*");
            path.setService(serviceRepository.findById(49L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Record/Java
        if(!pathRepository.existsById(107L)) {
            Path path = new Path();
            path.setId(107L);
            path.setPath("/cs/fr");
            path.setService(serviceRepository.findById(50L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Record/Record
        if(!pathRepository.existsById(108L)) {
            Path path = new Path();
            path.setId(108L);
            path.setPath("/cs/start/*");
            path.setService(serviceRepository.findById(51L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(109L)) {
            Path path = new Path();
            path.setId(109L);
            path.setPath("/cs/status/*");
            path.setService(serviceRepository.findById(51L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(110L)) {
            Path path = new Path();
            path.setId(110L);
            path.setPath("/cs/stop/*");
            path.setService(serviceRepository.findById(51L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Record/Collections
        if(!pathRepository.existsById(111L)) {
            Path path = new Path();
            path.setId(111L);
            path.setPath("/cs/recordings");
            path.setService(serviceRepository.findById(52L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(112L)) {
            Path path = new Path();
            path.setId(112L);
            path.setPath("/cs/currentcollection");
            path.setService(serviceRepository.findById(52L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(113L)) {
            Path path = new Path();
            path.setId(113L);
            path.setPath("/cs/requests");
            path.setService(serviceRepository.findById(52L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Replay/SetDefault
        if(!pathRepository.existsById(114L)) {
            Path path = new Path();
            path.setId(114L);
            path.setPath("/rs/transforms/*");
            path.setService(serviceRepository.findById(53L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(115L)) {
            Path path = new Path();
            path.setId(115L);
            path.setPath("/rs/health");
            path.setService(serviceRepository.findById(53L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Replay/Replay
        if(!pathRepository.existsById(116L)) {
            Path path = new Path();
            path.setId(116L);
            path.setPath("/rs/init/*");
            path.setService(serviceRepository.findById(54L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(117L)) {
            Path path = new Path();
            path.setId(117L);
            path.setPath("/rs/status/*");
            path.setService(serviceRepository.findById(54L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(118L)) {
            Path path = new Path();
            path.setId(118L);
            path.setPath("/rs/forcecomplete/*");
            path.setService(serviceRepository.findById(54L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(119L)) {
            Path path = new Path();
            path.setId(119L);
            path.setPath("/rs/forcestart/*");
            path.setService(serviceRepository.findById(54L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(120L)) {
            Path path = new Path();
            path.setId(120L);
            path.setPath("/rs/start/*");
            path.setService(serviceRepository.findById(54L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Analyze/Setup
        if(!pathRepository.existsById(121L)) {
            Path path = new Path();
            path.setId(121L);
            path.setPath("/as/registerTemplateApp/*");
            path.setService(serviceRepository.findById(55L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(122L)) {
            Path path = new Path();
            path.setId(122L);
            path.setPath("/as/registerTemplate/*");
            path.setService(serviceRepository.findById(55L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(123L)) {
            Path path = new Path();
            path.setId(123L);
            path.setPath("/as/health");
            path.setService(serviceRepository.findById(55L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Analyze/Retrieval
        if(!pathRepository.existsById(124L)) {
            Path path = new Path();
            path.setId(124L);
            path.setPath("/as/aggrresult/*");
            path.setService(serviceRepository.findById(56L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(125L)) {
            Path path = new Path();
            path.setId(125L);
            path.setPath("/as/replayRes/*");
            path.setService(serviceRepository.findById(56L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(126L)) {
            Path path = new Path();
            path.setId(126L);
            path.setPath("/as/analysisRes/*");
            path.setService(serviceRepository.findById(56L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(127L)) {
            Path path = new Path();
            path.setId(127L);
            path.setPath("/as/timelineres/*");
            path.setService(serviceRepository.findById(56L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(128L)) {
            Path path = new Path();
            path.setId(128L);
            path.setPath("/as/analysisResByPath/*");
            path.setService(serviceRepository.findById(56L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(129L)) {
            Path path = new Path();
            path.setId(129L);
            path.setPath("/as/analysisResByReq/*");
            path.setService(serviceRepository.findById(56L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Analyze/Analysis
        if(!pathRepository.existsById(130L)) {
            Path path = new Path();
            path.setId(130L);
            path.setPath("/as/registerTemplateApp/*");
            path.setService(serviceRepository.findById(57L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(131L)) {
            Path path = new Path();
            path.setId(131L);
            path.setPath("/as/registerTemplate/*");
            path.setService(serviceRepository.findById(57L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(132L)) {
            Path path = new Path();
            path.setId(132L);
            path.setPath("/as/health");
            path.setService(serviceRepository.findById(57L).get());
            pathRepository.save(path);
        }
        // Paths - Cube - Mock
        if(!pathRepository.existsById(133L)) {
            Path path = new Path();
            path.setId(133L);
            path.setPath("/ms/health");
            path.setService(serviceRepository.findById(36L).get());
            pathRepository.save(path);
        }
        if(!pathRepository.existsById(134L)) {
            Path path = new Path();
            path.setId(134L);
            path.setPath("/ms/*");
            path.setService(serviceRepository.findById(36L).get());
            pathRepository.save(path);
        }
        // TestConfig - Cube - Analyze/Retrieval
        if(!testConfigRepository.existsById(135L)) {
            TestConfig testConfig = new TestConfig();
            testConfig.setId(135L);
            testConfig.setApp(appRepository.findById(5L).get());
            testConfig.setTestConfigName("Cube-Analyze/Retrieval");
            testConfig.setGatewayService(serviceRepository.findById(56L).get());
            testConfigRepository.save(testConfig);
        }
        if(!testPathRepository.existsById(136L)) {
            TestPath testPath = new TestPath();
            testPath.setId(136L);
            testPath.setPath(pathRepository.findById(127L).get());
            testPath.setTestConfig(testConfigRepository.findById(135L).get());
            testPathRepository.save(testPath);
        }
        if(!testPathRepository.existsById(137L)) {
            TestPath testPath = new TestPath();
            testPath.setId(137L);
            testPath.setPath(pathRepository.findById(128L).get());
            testPath.setTestConfig(testConfigRepository.findById(135L).get());
            testPathRepository.save(testPath);
        }

        if(!instanceRepository.existsById(138L)) {
            Instance instance = new Instance();
            instance.setId(138L);
            instance.setName("DEMO-AS");
            instance.setGatewayEndpoint("http://demo-as.dev.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(139L)) {
            Instance instance = new Instance();
            instance.setId(139L);
            instance.setName("DEMO-PD");
            instance.setGatewayEndpoint("http://demo-pd.dev.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(140L)) {
            Instance instance = new Instance();
            instance.setId(140L);
            instance.setName("DEMO-SM");
            instance.setGatewayEndpoint("http://demo-sm.dev.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(141L)) {
            Instance instance = new Instance();
            instance.setId(141L);
            instance.setName("DEMO.PROD");
            instance.setGatewayEndpoint("http://demo.prod.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
            this.instanceRepository.save(instance);
        }

        if(!instanceRepository.existsById(142L)) {
            Instance instance = new Instance();
            instance.setId(142L);
            instance.setName("DEMO.PROD.V2");
            instance.setGatewayEndpoint("http://demo.prod.v2.cubecorp.io");
            instance.setApp(this.appRepository.findById(5L).get());
            this.instanceRepository.save(instance);
        }
        // MovieInfo App
        // RestWrapJDBC
        if(!testVirtualizedServiceRepository.existsById(143L)) {
            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
            testVirtualizedService.setId(143L);
            testVirtualizedService.setService(serviceRepository.findById(14L).get());
            testVirtualizedService.setTestConfig(testConfigRepository.findById(99L).get());
            testVirtualizedServiceRepository.save(testVirtualizedService);
        }
        // Reviews
        if(!testVirtualizedServiceRepository.existsById(144L)) {
            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
            testVirtualizedService.setId(144L);
            testVirtualizedService.setService(serviceRepository.findById(11L).get());
            testVirtualizedService.setTestConfig(testConfigRepository.findById(99L).get());
            testVirtualizedServiceRepository.save(testVirtualizedService);
        }
        // Details
        if(!testVirtualizedServiceRepository.existsById(145L)) {
            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
            testVirtualizedService.setId(145L);
            testVirtualizedService.setService(serviceRepository.findById(13L).get());
            testVirtualizedService.setTestConfig(testConfigRepository.findById(99L).get());
            testVirtualizedServiceRepository.save(testVirtualizedService);
        }
    }
}
