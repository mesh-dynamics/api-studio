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
import java.util.Optional;

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

    private InstanceUserRepository instanceUserRepository;

    private AppUserRepository appUserRepository;

    private JiraUserCredentialsRepository jiraUserCredentialsRepository;

    private EmailDomainRepository emailDomainRepository;

    public DataInitializer(UserService userService, CustomerService customerService,
        AppRepository appRepository, InstanceRepository instanceRepository,
        ServiceRepository serviceRepository, ServiceGraphRepository serviceGraphRepository,
        ServiceGroupRepository serviceGroupRepository, PathRepository pathRepository,
        TestConfigRepository testConfigRepository, TestIntermediateServiceRepository testIntermediateServiceRepository,
        TestVirtualizedServiceRepository testVirtualizedServiceRepository, TestPathRepository testPathRepository,
        CustomerRepository customerRepository, UserRepository userRepository,
        InstanceUserRepository instanceUserRepository, AppUserRepository appUserRepository,
        JiraUserCredentialsRepository jiraUserCredentialsRepository, EmailDomainRepository emailDomainRepository) {

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
        this.instanceUserRepository = instanceUserRepository;
        this.appUserRepository = appUserRepository;
        this.jiraUserCredentialsRepository = jiraUserCredentialsRepository;
        this.emailDomainRepository = emailDomainRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");

        Optional<Customer> customer = customerRepository.findByName("Admin");
        if(customer.isEmpty()) {
            CustomerDTO customerDTO = new CustomerDTO();
            //customerDTO.setId(1L);
            customerDTO.setName("Admin");
            customerDTO.setEmail("admin@meshdynamics.io");
            customerDTO.setDomainURL("admin.io");
            customer = Optional.of(this.customerService.save(customerDTO));
       }
//
//        //if (!userRepository.existsById(2L)){
//            UserDTO userDTO = new UserDTO();
//            //userDTO.setId(2L);
//            userDTO.setName("Demo");
//            userDTO.setEmail("demo@cubecorp.io");
//            userDTO.setPassword("password123");
//            userDTO.setCustomerId(1L);
//            userDTO.setRoles(Arrays.asList("ROLE_USER"));
//            userDTO.setActivated(true);
//            User user = this.userService.save(userDTO, true, false);
//            log.info("User with email '{}' created", user.getUsername());
//        //}

        Optional<User> user = userRepository.findByUsername("admin@meshdynamics.io");
        //if (!userRepository.existsById(3L)){
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

//        //if(!appRepository.existsById(4L)) {
//            App app = new App();
//            //app.setId(4L);
//            app.setName("MovieInfo");
//            app.setCustomer(customer);
//            App appMovie = this.appRepository.save(app);
//        //}
//
//        // todo: remove this app while refactoring
//        //if(!appRepository.existsById(5L)) {
//            App appDTOCube = new App();
//            //appDTOCube.setId(5L);
//            appDTOCube.setName("Cube");
//            //app.setCustomer(this.customerService.getById(1L).get());
//            App appCube = this.appRepository.save(appDTOCube);
//        //}
//
//        //if(!instanceRepository.existsById(6L)) {
//            Instance instance = new Instance();
//            //instance.setId(6L);
//            instance.setName("PROD");
//            instance.setGatewayEndpoint("http://demo.dev.cubecorp.io");
//            instance.setApp(appMovie);
//            Instance devInstance = this.instanceRepository.save(instance);
//        //}
//
//        //if(!instanceRepository.existsById(7L)) {
//            Instance instanceStaging1 = new Instance();
//            //instance.setId(7L);
//            instanceStaging1.setName("PROD");
//            instanceStaging1.setGatewayEndpoint("http://staging1.dev.cubecorp.io");
//            instanceStaging1.setApp(appCube);
//            Instance staging1Instance = this.instanceRepository.save(instance);
//        //}
//
//        //if(!instanceRepository.existsById(8L)) {
//            Instance instanceStaging2 = new Instance();
//            //instance.setId(8L);
//            instanceStaging2.setName("STAGING");
//            instanceStaging2.setGatewayEndpoint("http://staging2.dev.cubecorp.io");
//            instanceStaging2.setApp(appCube);
//            Instance staging2Instance = this.instanceRepository.save(instance);
//        //}
//
//        // ServiceGroup - MovieInfo
//        //if(!serviceGroupRepository.existsById(9L)) {
//            ServiceGroup serviceGroup = new ServiceGroup();
//            //serviceGroup.setId(9L);
//            serviceGroup.setName("GLOBAL");
//            serviceGroup.setApp(appMovie);
//            ServiceGroup globalServiceGroup = serviceGroupRepository.save(serviceGroup);
//        //}
//
//        //if(!serviceGroupRepository.existsById(10L)) {
//            ServiceGroup serviceGroupMovieInfo = new ServiceGroup();
//            //serviceGroup.setId(10L);
//            serviceGroupMovieInfo.setName("MovieInfo");
//            serviceGroupMovieInfo.setApp(appMovie);
//            ServiceGroup movieInfoServiceGroup = serviceGroupRepository.save(serviceGroupMovieInfo);
//        //}
//
//        /* MovieInfo App */
//        //if(!serviceRepository.existsById(11L)) {
//            Service service = new Service();
//            //service.setId(11L);
//            service.setServiceGroup(globalServiceGroup);
//            service.setName("Reviews");
//            service.setApp(appMovie);
//            Service reviewsService = this.serviceRepository.save(service);
//        //}
//
//        //if(!serviceRepository.existsById(12L)) {
//            Service serviceRatings = new Service();
//            //service.setId(12L);
//            serviceRatings.setServiceGroup(globalServiceGroup);
//            serviceRatings.setName("Ratings");
//            serviceRatings.setApp(appMovie);
//            Service ratingsService = this.serviceRepository.save(serviceRatings);
//        //}
//
//        //if(!serviceRepository.existsById(13L)) {
//            Service serviceDetails = new Service();
//            //service.setId(13L);
//            serviceDetails.setServiceGroup(globalServiceGroup);
//            serviceDetails.setName("Details");
//            serviceDetails.setApp(appMovie);
//            Service detailsService = this.serviceRepository.save(serviceDetails);
//        //}
//
//        //if(!serviceRepository.existsById(14L)) {
//            Service serviceRestWrapJDBC = new Service();
//            //service.setId(14L);
//            serviceRestWrapJDBC.setServiceGroup(globalServiceGroup);
//            serviceRestWrapJDBC.setName("RestWrapJDBC");
//            serviceRestWrapJDBC.setApp(appMovie);
//            Service restWrapJDBCService = this.serviceRepository.save(serviceRestWrapJDBC);
//        //}
//
//        //if(!serviceRepository.existsById(15L)) {
//            Service servicePostgres = new Service();
//            //service.setId(15L);
//            servicePostgres.setServiceGroup(globalServiceGroup);
//            servicePostgres.setName("Postgres");
//            servicePostgres.setApp(appMovie);
//            Service postgresService = this.serviceRepository.save(servicePostgres);
//        //}
//
//        //if(!serviceRepository.existsById(16L)) {
//            Service serviceMysql = new Service();
//            //service.setId(16L);
//            serviceMysql.setServiceGroup(globalServiceGroup);
//            serviceMysql.setName("Mysql");
//            serviceMysql.setApp(appMovie);
//            Service mysqlService = this.serviceRepository.save(serviceMysql);
//        //}
//
//        //if(!serviceRepository.existsById(17L)) {
//            Service serviceMongoDB = new Service();
//           // service.setId(17L);
//            serviceMongoDB.setServiceGroup(globalServiceGroup);
//            serviceMongoDB.setName("MongoDB");
//            serviceMongoDB.setApp(appMovie);
//            Service mongoDBService = this.serviceRepository.save(serviceMongoDB);
//        //}
//
//        //if(!serviceRepository.existsById(18L)) {
//            Service serviceGoogle_API = new Service();
//            //service.setId(18L);
//            serviceGoogle_API.setServiceGroup(globalServiceGroup);
//            serviceGoogle_API.setName("Google API");
//            serviceGoogle_API.setApp(appMovie);
//            Service google_APIService = this.serviceRepository.save(serviceGoogle_API);
//        //}
//
//        //if(!serviceRepository.existsById(19L)) {
//            Service serviceAuth = new Service();
//            //service.setId(19L);
//            serviceAuth.setServiceGroup(movieInfoServiceGroup);
//            serviceAuth.setName("Auth");
//            serviceAuth.setApp(appMovie);
//            Service authService = this.serviceRepository.save(serviceAuth);
//        //}
//
//        //if(!serviceRepository.existsById(20L)) {
//            Service serviceActions = new Service();
//            //service.setId(20L);
//            serviceActions.setServiceGroup(movieInfoServiceGroup);
//            serviceActions.setName("Actions");
//            serviceActions.setApp(appMovie);
//            Service actionsService = this.serviceRepository.save(serviceActions);
//        //}
//
//        //if(!serviceRepository.existsById(21L)) {
//            Service serviceList = new Service();
//            //service.setId(21L);
//            serviceList.setServiceGroup(movieInfoServiceGroup);
//            serviceList.setName("List");
//            serviceList.setApp(appMovie);
//            Service listService = this.serviceRepository.save(serviceList);
//        //}
//
//        /* MovieInfo - ServiceGraph */
//        // Auth - RestWrapJDBC
//        //if(!serviceGraphRepository.existsById(22L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            //serviceGraph.setId(22L);
//            serviceGraph.setFromService(authService);
//            serviceGraph.setToService(restWrapJDBCService);
//            serviceGraph.setApp(appMovie);
//            ServiceGraph authToRestWrapJDBC = this.serviceGraphRepository.save(serviceGraph);
//        //}
//        // Actions - RestWrapJDBC
//        //if(!serviceGraphRepository.existsById(23L)) {
//            ServiceGraph serviceGraphActionsToRestWrapJDBC = new ServiceGraph();
//            //serviceGraph.setId(23L);
//            serviceGraphActionsToRestWrapJDBC.setFromService(actionsService);
//            serviceGraphActionsToRestWrapJDBC.setToService(restWrapJDBCService);
//            serviceGraphActionsToRestWrapJDBC.setApp(appMovie);
//            ServiceGraph actionsToRestWrapJDBC = this.serviceGraphRepository.save(serviceGraphActionsToRestWrapJDBC);
//        //}
//        // List - RestWrapJDBC
//        //if(!serviceGraphRepository.existsById(24L)) {
//            ServiceGraph serviceGraphListToRestWrapJDBC = new ServiceGraph();
//            //serviceGraph.setId(24L);
//            serviceGraphListToRestWrapJDBC.setFromService(listService);
//            serviceGraphListToRestWrapJDBC.setToService(restWrapJDBCService);
//            serviceGraphListToRestWrapJDBC.setApp(appMovie);
//            ServiceGraph listToRestWrapJDBC = this.serviceGraphRepository.save(serviceGraphListToRestWrapJDBC);
//        //}
//        // RestWrapJDBC - Postgres
//        //if(!serviceGraphRepository.existsById(25L)) {
//            ServiceGraph serviceGraphRestWrapJDBCToPostgres = new ServiceGraph();
//            //serviceGraph.setId(25L);
//            serviceGraphRestWrapJDBCToPostgres.setFromService(restWrapJDBCService);
//            serviceGraphRestWrapJDBCToPostgres.setToService(postgresService);
//            serviceGraphRestWrapJDBCToPostgres.setApp(appMovie);
//            ServiceGraph restWrapJDBCToPostgres = this.serviceGraphRepository.save(serviceGraphRestWrapJDBCToPostgres);
//        //}
//
//        // List - Reviews
//        //if(!serviceGraphRepository.existsById(26L)) {
//            ServiceGraph serviceGraphListToReviews = new ServiceGraph();
//            //serviceGraph.setId(26L);
//            serviceGraphListToReviews.setFromService(listService);
//            serviceGraphListToReviews.setToService(reviewsService);
//            serviceGraphListToReviews.setApp(appMovie);
//            ServiceGraph listToReviews = this.serviceGraphRepository.save(serviceGraphListToReviews);
//        //}
//
//        // List - Details
//        //if(!serviceGraphRepository.existsById(27L)) {
//            ServiceGraph serviceGraphListToDetails = new ServiceGraph();
//            //serviceGraph.setId(27L);
//            serviceGraphListToDetails.setFromService(listService);
//            serviceGraphListToDetails.setToService(detailsService);
//            serviceGraphListToDetails.setApp(appMovie);
//            ServiceGraph listToDetails = this.serviceGraphRepository.save(serviceGraphListToDetails);
//        //}
//
//        // Reviews - Ratings
//        //if(!serviceGraphRepository.existsById(28L)) {
//            ServiceGraph serviceGraphReviewsToRatings = new ServiceGraph();
//            //serviceGraph.setId(28L);
//            serviceGraphReviewsToRatings.setFromService(reviewsService);
//            serviceGraphReviewsToRatings.setToService(ratingsService);
//            serviceGraphReviewsToRatings.setApp(appMovie);
//            ServiceGraph reviewsToRatings = this.serviceGraphRepository.save(serviceGraphReviewsToRatings);
//        //}
//
//        // Details - Google API
//        //if(!serviceGraphRepository.existsById(29L)) {
//            ServiceGraph serviceGraphDetailsToGoogle_API = new ServiceGraph();
//            //serviceGraph.setId(29L);
//            serviceGraphDetailsToGoogle_API.setFromService(detailsService);
//            serviceGraphDetailsToGoogle_API.setToService(google_APIService);
//            serviceGraphDetailsToGoogle_API.setApp(appMovie);
//            ServiceGraph detailsToGoogle_API = this.serviceGraphRepository.save(serviceGraphDetailsToGoogle_API);
//        //}
//
//        // Ratings - Mysql
//        //if(!serviceGraphRepository.existsById(30L)) {
//            ServiceGraph serviceGraphRatingsToMysql = new ServiceGraph();
//            //serviceGraph.setId(30L);
//            serviceGraphRatingsToMysql.setFromService(ratingsService);
//            serviceGraphRatingsToMysql.setToService(mysqlService);
//            serviceGraphRatingsToMysql.setApp(appMovie);
//            ServiceGraph ratingsToMysql = this.serviceGraphRepository.save(serviceGraphRatingsToMysql);
//        //}
//
//        // Ratings - MongoDB
//        //if(!serviceGraphRepository.existsById(31L)) {
//            ServiceGraph serviceGraphRatingsToMongoDB = new ServiceGraph();
//            //serviceGraph.setId(31L);
//            serviceGraphRatingsToMongoDB.setFromService(ratingsService);
//            serviceGraphRatingsToMongoDB.setToService(mongoDBService);
//            serviceGraphRatingsToMongoDB.setApp(appMovie);
//            ServiceGraph ratingsToMongoDB = this.serviceGraphRepository.save(serviceGraphRatingsToMongoDB);
//        //}
//
//        // ServiceGroup - Cube
//        //if(!serviceGroupRepository.existsById(32L)) {
//            ServiceGroup serviceGroupGlobal = new ServiceGroup();
//            //serviceGroup.setId(32L);
//            serviceGroupGlobal.setName("GLOBAL");
//            serviceGroupGlobal.setApp(appCube);
//            ServiceGroup globalServiceGroupCube = serviceGroupRepository.save(serviceGroupGlobal);
//        //}
//
//        //if(!serviceGroupRepository.existsById(33L)) {
//            ServiceGroup serviceGroupUI = new ServiceGroup();
//            //serviceGroup.setId(33L);
//            serviceGroupUI.setName("UI");
//            serviceGroupUI.setApp(appCube);
//            ServiceGroup uiServiceGroupCube = serviceGroupRepository.save(serviceGroupUI);
//        //}
//
//        //if(!serviceGroupRepository.existsById(34L)) {
//            ServiceGroup serviceGroupRecord = new ServiceGroup();
//            //serviceGroup.setId(34L);
//            serviceGroupRecord.setName("Record");
//            serviceGroupRecord.setApp(appCube);
//            ServiceGroup recordServieGroupCube = serviceGroupRepository.save(serviceGroupRecord);
//        //}
//
//        //if(!serviceGroupRepository.existsById(35L)) {
//            ServiceGroup serviceGroupReplay = new ServiceGroup();
//            //serviceGroup.setId(35L);
//            serviceGroupReplay.setName("Replay");
//            serviceGroupReplay.setApp(appCube);
//            ServiceGroup replayServiceGroupCube = serviceGroupRepository.save(serviceGroupReplay);
//        //}
//
//        /* Cube App */
//        //if(!serviceRepository.existsById(36L)) {
//            Service serviceMock = new Service();
//            //service.setId(36L);
//            serviceMock.setServiceGroup(globalServiceGroupCube);
//            serviceMock.setName("Mock");
//            serviceMock.setApp(appCube);
//            Service mockService = this.serviceRepository.save(serviceMock);
//        //}
//
//        //if(!serviceRepository.existsById(37L)) {
//            Service servicePostgresCube = new Service();
//            //service.setId(37L);
//            servicePostgresCube.setServiceGroup(globalServiceGroupCube);
//            servicePostgresCube.setName("Postgres");
//            servicePostgresCube.setApp(appCube);
//            Service postgresCubeService = this.serviceRepository.save(servicePostgresCube);
//        //}
//
//        //if(!serviceRepository.existsById(38L)) {
//            Service serviceSolrCube = new Service();
//            //service.setId(38L);
//            serviceSolrCube.setServiceGroup(globalServiceGroupCube);
//            serviceSolrCube.setName("Solr");
//            serviceSolrCube.setApp(appCube);
//            Service solrCubeService = this.serviceRepository.save(serviceSolrCube);
//        //}
//
//        //if(!serviceRepository.existsById(39L)) {
//            Service serviceAccount_User = new Service();
//            //service.setId(39L);
//            serviceAccount_User.setServiceGroup(uiServiceGroupCube);
//            serviceAccount_User.setName("Account/User");
//            serviceAccount_User.setApp(appCube);
//            Service account_UserService = this.serviceRepository.save(serviceAccount_User);
//        //}
//
//        //if(!serviceRepository.existsById(40L)) {
//            Service serviceApp = new Service();
//            //service.setId(40L);
//            serviceApp.setServiceGroup(uiServiceGroupCube);
//            serviceApp.setName("App");
//            serviceApp.setApp(appCube);
//            Service appService = this.serviceRepository.save(serviceApp);
//        //}
//
//        //if(!serviceRepository.existsById(41L)) {
//            Service serviceInstance = new Service();
//            //service.setId(41L);
//            serviceInstance.setServiceGroup(uiServiceGroupCube);
//            serviceInstance.setName("Instance");
//            serviceInstance.setApp(appCube);
//            Service instanceService = this.serviceRepository.save(serviceInstance);
//        //}
//
//        //if(!serviceRepository.existsById(42L)) {
//            Service service_Graph = new Service();
//            //service.setId(42L);
//            service_Graph.setServiceGroup(uiServiceGroupCube);
//            service_Graph.setName("Service/Graph");
//            service_Graph.setApp(appCube);
//            Service graphService = this.serviceRepository.save(service_Graph);
//        //}
//
//        //if(!serviceRepository.existsById(43L)) {
//            Service serviceTestConfig = new Service();
//            //service.setId(43L);
//            serviceTestConfig.setServiceGroup(uiServiceGroupCube);
//            serviceTestConfig.setName("TestConfig");
//            serviceTestConfig.setApp(appCube);
//            Service testConfigService = this.serviceRepository.save(serviceTestConfig);
//        //}
//
//        //if(!serviceRepository.existsById(44L)) {
//            Service serviceRecord = new Service();
//            //service.setId(44L);
//            serviceRecord.setServiceGroup(uiServiceGroupCube);
//            serviceRecord.setName("Record");
//            serviceRecord.setApp(appCube);
//            Service recordService = this.serviceRepository.save(serviceRecord);
//        //}
//
//        //if(!serviceRepository.existsById(45L)) {
//            Service serviceReplay = new Service();
//            //service.setId(45L);
//            serviceReplay.setServiceGroup(uiServiceGroupCube);
//            serviceReplay.setName("Replay");
//            serviceReplay.setApp(appCube);
//            Service replayService = this.serviceRepository.save(serviceReplay);
//        //}
//
//        //if(!serviceRepository.existsById(46L)) {
//            Service serviceAnalyze = new Service();
//            //service.setId(46L);
//            serviceAnalyze.setServiceGroup(uiServiceGroupCube);
//            serviceAnalyze.setName("Analyze");
//            serviceAnalyze.setApp(appCube);
//            Service analyzeService = this.serviceRepository.save(serviceAnalyze);
//        //}
//
//        //if(!serviceRepository.existsById(47L)) {
//            Service serviceCustomer = new Service();
//            //service.setId(47L);
//            serviceCustomer.setServiceGroup(uiServiceGroupCube);
//            serviceCustomer.setName("Customer");
//            serviceCustomer.setApp(appCube);
//            Service customerService = this.serviceRepository.save(serviceCustomer);
//        //}
//
//        //if(!serviceRepository.existsById(48L)) {
//            Service serviceRecord_SetDefault = new Service();
//            //service.setId(48L);
//            serviceRecord_SetDefault.setServiceGroup(recordServieGroupCube);
//            serviceRecord_SetDefault.setName("Record/SetDefault");
//            serviceRecord_SetDefault.setApp(appCube);
//            Service record_SetDefaultService = this.serviceRepository.save(serviceRecord_SetDefault);
//        //}
//
//        //if(!serviceRepository.existsById(49L)) {
//            Service serviceRecordHTTP = new Service();
//            //service.setId(49L);
//            serviceRecordHTTP.setServiceGroup(recordServieGroupCube);
//            serviceRecordHTTP.setName("Record/HTTP");
//            serviceRecordHTTP.setApp(appCube);
//            Service recordHTTPService = this.serviceRepository.save(serviceRecordHTTP);
//        //}
//
//        //if(!serviceRepository.existsById(50L)) {
//            Service serviceRecord_Java = new Service();
//            //service.setId(50L);
//            serviceRecord_Java.setServiceGroup(recordServieGroupCube);
//            serviceRecord_Java.setName("Record/Java");
//            serviceRecord_Java.setApp(appCube);
//            Service recordJavaService = this.serviceRepository.save(serviceRecord_Java);
//        //}
//
//        //if(!serviceRepository.existsById(51L)) {
//            Service service_Record = new Service();
//            //service.setId(51L);
//            service_Record.setServiceGroup(recordServieGroupCube);
//            service_Record.setName("Record/Record");
//            service_Record.setApp(appCube);
//            Service record_Service = this.serviceRepository.save(service_Record);
//        //}
//
//        //if(!serviceRepository.existsById(52L)) {
//            Service serviceRecordCollection = new Service();
//            //service.setId(52L);
//            serviceRecordCollection.setServiceGroup(recordServieGroupCube);
//            serviceRecordCollection.setName("Record/Collections");
//            serviceRecordCollection.setApp(appCube);
//            Service recordCollectionService = this.serviceRepository.save(serviceRecordCollection);
//        //}
//
//        //if(!serviceRepository.existsById(53L)) {
//            Service serviceReplaySetDefault = new Service();
//            //service.setId(53L);
//            serviceReplaySetDefault.setServiceGroup(replayServiceGroupCube);
//            serviceReplaySetDefault.setName("Replay/SetDefault");
//            serviceReplaySetDefault.setApp(appCube);
//            this.serviceRepository.save(serviceReplaySetDefault);
//        //}
//
//        //if(!serviceRepository.existsById(54L)) {
//            Service serviceReplay_Replay = new Service();
//            //service.setId(54L);
//            serviceReplay_Replay.setServiceGroup(replayServiceGroupCube);
//            serviceReplay_Replay.setName("Replay/Replay");
//            serviceReplay_Replay.setApp(appCube);
//            Service replay_Service = this.serviceRepository.save(serviceReplay_Replay);
//        //}
//
//        //if(!serviceRepository.existsById(55L)) {
//            Service serviceAnalyzeSetup = new Service();
//            //service.setId(55L);
//            serviceAnalyzeSetup.setServiceGroup(replayServiceGroupCube);
//            serviceAnalyzeSetup.setName("Analyze/Setup");
//            serviceAnalyzeSetup.setApp(appCube);
//            Service analyzeSetupService = this.serviceRepository.save(serviceAnalyzeSetup);
//        //}
//
//        //if(!serviceRepository.existsById(56L)) {
//            Service serviceAnalyzeRetrieval = new Service();
//            //service.setId(56L);
//            serviceAnalyzeRetrieval.setServiceGroup(replayServiceGroupCube);
//            serviceAnalyzeRetrieval.setName("Analyze/Retrieval");
//            serviceAnalyzeRetrieval.setApp(appCube);
//            Service AnalyzeRetrievalService = this.serviceRepository.save(serviceAnalyzeRetrieval);
//        //}
//
//        //if(!serviceRepository.existsById(57L)) {
//            Service serviceAnalyzeAnalysis = new Service();
//            //service.setId(57L);
//            serviceAnalyzeAnalysis.setServiceGroup(replayServiceGroupCube);
//            serviceAnalyzeAnalysis.setName("Analyze/Analysis");
//            serviceAnalyzeAnalysis.setApp(appCube);
//            Service analyzeAnalysisService = this.serviceRepository.save(serviceAnalyzeAnalysis);
//        //}
//
//        /* Cube - ServiceGraph */
//        // Mock - Solr
//        //if(!serviceGraphRepository.existsById(58L)) {
//            ServiceGraph serviceGraphMockToSolr = new ServiceGraph();
//            //serviceGraph.setId(58L);
//            serviceGraphMockToSolr.setFromService(mockService);
//            serviceGraphMockToSolr.setToService(solrCubeService);
//            serviceGraphMockToSolr.setApp(appCube);
//            ServiceGraph mockToSolarServiceGraph = this.serviceGraphRepository.save(serviceGraphMockToSolr);
//        //}
//        // Record/SetDefault - Solr
//        //if(!serviceGraphRepository.existsById(59L)) {
//            ServiceGraph serviceGraphRecordSetDefaultToSolr = new ServiceGraph();
//            //serviceGraph.setId(59L);
//            serviceGraphRecordSetDefaultToSolr.setFromService(record_SetDefaultService);
//            serviceGraphRecordSetDefaultToSolr.setToService(solrCubeService);
//            serviceGraphRecordSetDefaultToSolr.setApp(appCube);
//            ServiceGraph recordSetDefaultToSolrServiceGraph = this.serviceGraphRepository.save(serviceGraphRecordSetDefaultToSolr);
//        //}
//        // Record/HTTP - Solr
//        //if(!serviceGraphRepository.existsById(60L)) {
//            ServiceGraph serviceGraphRecordHTTPToSolr = new ServiceGraph();
//            //serviceGraph.setId(60L);
//            serviceGraphRecordHTTPToSolr.setFromService(recordHTTPService);
//            serviceGraphRecordHTTPToSolr.setToService(solrCubeService);
//            serviceGraphRecordHTTPToSolr.setApp(appCube);
//            ServiceGraph recordHTTPToSolrServiceGraph = this.serviceGraphRepository.save(serviceGraphRecordHTTPToSolr);
//        //}
//        // Record/Java - Solr
//        //if(!serviceGraphRepository.existsById(61L)) {
//            ServiceGraph serviceGraphRecordJavaToSolr = new ServiceGraph();
//            //serviceGraph.setId(61L);
//            serviceGraphRecordJavaToSolr.setFromService(recordJavaService);
//            serviceGraphRecordJavaToSolr.setToService(solrCubeService);
//            serviceGraphRecordJavaToSolr.setApp(appCube);
//            ServiceGraph recordJavaToSolrServiceGraph = this.serviceGraphRepository.save(serviceGraphRecordJavaToSolr);
//        //}
//        // Record/Record - Solr
//        if(!serviceGraphRepository.existsById(62L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(62L);
//            serviceGraph.setFromService(serviceRepository.findById(51L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Record/Collections - Solr
//        if(!serviceGraphRepository.existsById(63L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(63L);
//            serviceGraph.setFromService(serviceRepository.findById(52L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Replay/SetDefault - Solr
//        if(!serviceGraphRepository.existsById(64L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(64L);
//            serviceGraph.setFromService(serviceRepository.findById(53L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Replay/Replay - Solr
//        if(!serviceGraphRepository.existsById(65L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(65L);
//            serviceGraph.setFromService(serviceRepository.findById(54L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Analyze/Setup - Solr
//        if(!serviceGraphRepository.existsById(66L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(66L);
//            serviceGraph.setFromService(serviceRepository.findById(55L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Analyze/Retrieval - Solr
//        if(!serviceGraphRepository.existsById(67L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(67L);
//            serviceGraph.setFromService(serviceRepository.findById(56L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Analyze/Analysis - Solr
//        if(!serviceGraphRepository.existsById(68L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(68L);
//            serviceGraph.setFromService(serviceRepository.findById(57L).get());
//            serviceGraph.setToService(serviceRepository.findById(38L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Account/User - Postgres
//        if(!serviceGraphRepository.existsById(69L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(69L);
//            serviceGraph.setFromService(serviceRepository.findById(39L).get());
//            serviceGraph.setToService(serviceRepository.findById(37L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // App - Postgres
//        if(!serviceGraphRepository.existsById(70L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(70L);
//            serviceGraph.setFromService(serviceRepository.findById(40L).get());
//            serviceGraph.setToService(serviceRepository.findById(37L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Instance - Postgres
//        if(!serviceGraphRepository.existsById(71L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(71L);
//            serviceGraph.setFromService(serviceRepository.findById(41L).get());
//            serviceGraph.setToService(serviceRepository.findById(37L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Service/Graph - Postgres
//        if(!serviceGraphRepository.existsById(72L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(72L);
//            serviceGraph.setFromService(serviceRepository.findById(42L).get());
//            serviceGraph.setToService(serviceRepository.findById(37L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // TestConfig - Postgres
//        if(!serviceGraphRepository.existsById(73L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(73L);
//            serviceGraph.setFromService(serviceRepository.findById(43L).get());
//            serviceGraph.setToService(serviceRepository.findById(37L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Customer - Postgres
//        if(!serviceGraphRepository.existsById(74L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(74L);
//            serviceGraph.setFromService(serviceRepository.findById(47L).get());
//            serviceGraph.setToService(serviceRepository.findById(37L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Record - Record/SetDefault
//        if(!serviceGraphRepository.existsById(75L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(75L);
//            serviceGraph.setFromService(serviceRepository.findById(44L).get());
//            serviceGraph.setToService(serviceRepository.findById(48L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Record - Record/HTTP
//        if(!serviceGraphRepository.existsById(76L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(76L);
//            serviceGraph.setFromService(serviceRepository.findById(44L).get());
//            serviceGraph.setToService(serviceRepository.findById(49L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Record - Record/Java
//        if(!serviceGraphRepository.existsById(77L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(77L);
//            serviceGraph.setFromService(serviceRepository.findById(44L).get());
//            serviceGraph.setToService(serviceRepository.findById(50L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Record - Record/Record
//        if(!serviceGraphRepository.existsById(78L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(78L);
//            serviceGraph.setFromService(serviceRepository.findById(44L).get());
//            serviceGraph.setToService(serviceRepository.findById(51L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Record - Record/Collections
//        if(!serviceGraphRepository.existsById(79L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(79L);
//            serviceGraph.setFromService(serviceRepository.findById(44L).get());
//            serviceGraph.setToService(serviceRepository.findById(52L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Replay - Replay/SetDefault
//        if(!serviceGraphRepository.existsById(80L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(80L);
//            serviceGraph.setFromService(serviceRepository.findById(45L).get());
//            serviceGraph.setToService(serviceRepository.findById(53L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Replay - Replay/Replay
//        if(!serviceGraphRepository.existsById(81L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(81L);
//            serviceGraph.setFromService(serviceRepository.findById(45L).get());
//            serviceGraph.setToService(serviceRepository.findById(54L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Analyze - Analyze/Setup
//        if(!serviceGraphRepository.existsById(82L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(82L);
//            serviceGraph.setFromService(serviceRepository.findById(46L).get());
//            serviceGraph.setToService(serviceRepository.findById(55L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Analyze - Analyze/Retrieval
//        if(!serviceGraphRepository.existsById(83L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(83L);
//            serviceGraph.setFromService(serviceRepository.findById(46L).get());
//            serviceGraph.setToService(serviceRepository.findById(56L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Analyze - Analyze/Analysis
//        if(!serviceGraphRepository.existsById(83L)) {
//            ServiceGraph serviceGraph = new ServiceGraph();
//            serviceGraph.setId(83L);
//            serviceGraph.setFromService(serviceRepository.findById(46L).get());
//            serviceGraph.setToService(serviceRepository.findById(57L).get());
//            serviceGraph.setApp(appRepository.findById(5L).get());
//            this.serviceGraphRepository.save(serviceGraph);
//        }
//        // Paths - MovieInfo - Auth
//        if(!pathRepository.existsById(84L)) {
//            Path path = new Path();
//            path.setId(84L);
//            path.setPath("/authenticate");
//            path.setService(serviceRepository.findById(19L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(85L)) {
//            Path path = new Path();
//            path.setId(85L);
//            path.setPath("/health");
//            path.setService(serviceRepository.findById(19L).get());
//            pathRepository.save(path);
//        }
//        // Paths - MovieInfo - List
//        if(!pathRepository.existsById(86L)) {
//            Path path = new Path();
//            path.setId(86L);
//            path.setPath("/listmovies");
//            path.setService(serviceRepository.findById(21L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(87L)) {
//            Path path = new Path();
//            path.setId(87L);
//            path.setPath("/liststores");
//            path.setService(serviceRepository.findById(21L).get());
//            pathRepository.save(path);
//        }
//        // Paths - MovieInfo - Actions
//        if(!pathRepository.existsById(86L)) {
//            Path path = new Path();
//            path.setId(86L);
//            path.setPath("/rentmovie");
//            path.setService(serviceRepository.findById(20L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(87L)) {
//            Path path = new Path();
//            path.setId(87L);
//            path.setPath("/returnmovie");
//            path.setService(serviceRepository.findById(20L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(88L)) {
//            Path path = new Path();
//            path.setId(88L);
//            path.setPath("/overduerental");
//            path.setService(serviceRepository.findById(20L).get());
//            pathRepository.save(path);
//        }
//        // Paths - MovieInfo - RestWrapJDBC
//        if(!pathRepository.existsById(89L)) {
//            Path path = new Path();
//            path.setId(89L);
//            path.setPath("/health");
//            path.setService(serviceRepository.findById(14L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(90L)) {
//            Path path = new Path();
//            path.setId(90L);
//            path.setPath("/initialize");
//            path.setService(serviceRepository.findById(14L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(91L)) {
//            Path path = new Path();
//            path.setId(91L);
//            path.setPath("/query");
//            path.setService(serviceRepository.findById(14L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(92L)) {
//            Path path = new Path();
//            path.setId(92L);
//            path.setPath("/update");
//            path.setService(serviceRepository.findById(14L).get());
//            pathRepository.save(path);
//        }
//        // Paths - MovieInfo - Reviews
//        if(!pathRepository.existsById(93L)) {
//            Path path = new Path();
//            path.setId(93L);
//            path.setPath("/health");
//            path.setService(serviceRepository.findById(11L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(94L)) {
//            Path path = new Path();
//            path.setId(94L);
//            path.setPath("/reviews/*");
//            path.setService(serviceRepository.findById(11L).get());
//            pathRepository.save(path);
//        }
//        // Paths - MovieInfo - Ratings
//        if(!pathRepository.existsById(95L)) {
//            Path path = new Path();
//            path.setId(95L);
//            path.setPath("/health");
//            path.setService(serviceRepository.findById(12L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(96L)) {
//            Path path = new Path();
//            path.setId(96L);
//            path.setPath("/ratings/*");
//            path.setService(serviceRepository.findById(12L).get());
//            pathRepository.save(path);
//        }
//        // Paths - MovieInfo - Details
//        if(!pathRepository.existsById(97L)) {
//            Path path = new Path();
//            path.setId(97L);
//            path.setPath("/health");
//            path.setService(serviceRepository.findById(13L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(98L)) {
//            Path path = new Path();
//            path.setId(98L);
//            path.setPath("/details/*");
//            path.setService(serviceRepository.findById(13L).get());
//            pathRepository.save(path);
//        }
//        // TestConfig - MovieInfo - List
//        if(!testConfigRepository.existsById(99L)) {
//            TestConfig testConfig = new TestConfig();
//            testConfig.setId(99L);
//            testConfig.setApp(appRepository.findById(4L).get());
//            testConfig.setTestConfigName("MovieInfo-List");
//            testConfig.setGatewayService(serviceRepository.findById(21L).get());
//            testConfigRepository.save(testConfig);
//        }
//        if(!testPathRepository.existsById(100L)) {
//            TestPath testPath = new TestPath();
//            testPath.setId(100L);
//            testPath.setPath(pathRepository.findById(86L).get());
//            testPath.setTestConfig(testConfigRepository.findById(99L).get());
//            testPathRepository.save(testPath);
//        }
//        if(!testPathRepository.existsById(101L)) {
//            TestPath testPath = new TestPath();
//            testPath.setId(101L);
//            testPath.setPath(pathRepository.findById(87L).get());
//            testPath.setTestConfig(testConfigRepository.findById(99L).get());
//            testPathRepository.save(testPath);
//        }
//        // Paths - Cube - Record/SetDefault
//        if(!pathRepository.existsById(102L)) {
//            Path path = new Path();
//            path.setId(102L);
//            path.setPath("/cs/health");
//            path.setService(serviceRepository.findById(48L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(103L)) {
//            Path path = new Path();
//            path.setId(103L);
//            path.setPath("/cs/setdefault/*");
//            path.setService(serviceRepository.findById(48L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Record/HTTP
//        if(!pathRepository.existsById(104L)) {
//            Path path = new Path();
//            path.setId(104L);
//            path.setPath("/cs/req");
//            path.setService(serviceRepository.findById(49L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(105L)) {
//            Path path = new Path();
//            path.setId(105L);
//            path.setPath("/cs/res");
//            path.setService(serviceRepository.findById(49L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(106L)) {
//            Path path = new Path();
//            path.setId(106L);
//            path.setPath("/cs/rr/*");
//            path.setService(serviceRepository.findById(49L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Record/Java
//        if(!pathRepository.existsById(107L)) {
//            Path path = new Path();
//            path.setId(107L);
//            path.setPath("/cs/fr");
//            path.setService(serviceRepository.findById(50L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Record/Record
//        if(!pathRepository.existsById(108L)) {
//            Path path = new Path();
//            path.setId(108L);
//            path.setPath("/cs/start/*");
//            path.setService(serviceRepository.findById(51L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(109L)) {
//            Path path = new Path();
//            path.setId(109L);
//            path.setPath("/cs/status/*");
//            path.setService(serviceRepository.findById(51L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(110L)) {
//            Path path = new Path();
//            path.setId(110L);
//            path.setPath("/cs/stop/*");
//            path.setService(serviceRepository.findById(51L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Record/Collections
//        if(!pathRepository.existsById(111L)) {
//            Path path = new Path();
//            path.setId(111L);
//            path.setPath("/cs/recordings");
//            path.setService(serviceRepository.findById(52L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(112L)) {
//            Path path = new Path();
//            path.setId(112L);
//            path.setPath("/cs/currentcollection");
//            path.setService(serviceRepository.findById(52L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(113L)) {
//            Path path = new Path();
//            path.setId(113L);
//            path.setPath("/cs/requests");
//            path.setService(serviceRepository.findById(52L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Replay/SetDefault
//        if(!pathRepository.existsById(114L)) {
//            Path path = new Path();
//            path.setId(114L);
//            path.setPath("/rs/transforms/*");
//            path.setService(serviceRepository.findById(53L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(115L)) {
//            Path path = new Path();
//            path.setId(115L);
//            path.setPath("/rs/health");
//            path.setService(serviceRepository.findById(53L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Replay/Replay
//        if(!pathRepository.existsById(116L)) {
//            Path path = new Path();
//            path.setId(116L);
//            path.setPath("/rs/init/*");
//            path.setService(serviceRepository.findById(54L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(117L)) {
//            Path path = new Path();
//            path.setId(117L);
//            path.setPath("/rs/status/*");
//            path.setService(serviceRepository.findById(54L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(118L)) {
//            Path path = new Path();
//            path.setId(118L);
//            path.setPath("/rs/forcecomplete/*");
//            path.setService(serviceRepository.findById(54L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(119L)) {
//            Path path = new Path();
//            path.setId(119L);
//            path.setPath("/rs/forcestart/*");
//            path.setService(serviceRepository.findById(54L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(120L)) {
//            Path path = new Path();
//            path.setId(120L);
//            path.setPath("/rs/start/*");
//            path.setService(serviceRepository.findById(54L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Analyze/Setup
//        if(!pathRepository.existsById(121L)) {
//            Path path = new Path();
//            path.setId(121L);
//            path.setPath("/as/registerTemplateApp/*");
//            path.setService(serviceRepository.findById(55L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(122L)) {
//            Path path = new Path();
//            path.setId(122L);
//            path.setPath("/as/registerTemplate/*");
//            path.setService(serviceRepository.findById(55L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(123L)) {
//            Path path = new Path();
//            path.setId(123L);
//            path.setPath("/as/health");
//            path.setService(serviceRepository.findById(55L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Analyze/Retrieval
//        if(!pathRepository.existsById(124L)) {
//            Path path = new Path();
//            path.setId(124L);
//            path.setPath("/as/aggrresult/*");
//            path.setService(serviceRepository.findById(56L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(125L)) {
//            Path path = new Path();
//            path.setId(125L);
//            path.setPath("/as/replayRes/*");
//            path.setService(serviceRepository.findById(56L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(126L)) {
//            Path path = new Path();
//            path.setId(126L);
//            path.setPath("/as/analysisRes/*");
//            path.setService(serviceRepository.findById(56L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(127L)) {
//            Path path = new Path();
//            path.setId(127L);
//            path.setPath("/as/timelineres/*");
//            path.setService(serviceRepository.findById(56L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(128L)) {
//            Path path = new Path();
//            path.setId(128L);
//            path.setPath("/as/analysisResByPath/*");
//            path.setService(serviceRepository.findById(56L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(129L)) {
//            Path path = new Path();
//            path.setId(129L);
//            path.setPath("/as/analysisResByReq/*");
//            path.setService(serviceRepository.findById(56L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Analyze/Analysis
//        if(!pathRepository.existsById(130L)) {
//            Path path = new Path();
//            path.setId(130L);
//            path.setPath("/as/registerTemplateApp/*");
//            path.setService(serviceRepository.findById(57L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(131L)) {
//            Path path = new Path();
//            path.setId(131L);
//            path.setPath("/as/registerTemplate/*");
//            path.setService(serviceRepository.findById(57L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(132L)) {
//            Path path = new Path();
//            path.setId(132L);
//            path.setPath("/as/health");
//            path.setService(serviceRepository.findById(57L).get());
//            pathRepository.save(path);
//        }
//        // Paths - Cube - Mock
//        if(!pathRepository.existsById(133L)) {
//            Path path = new Path();
//            path.setId(133L);
//            path.setPath("/ms/health");
//            path.setService(serviceRepository.findById(36L).get());
//            pathRepository.save(path);
//        }
//        if(!pathRepository.existsById(134L)) {
//            Path path = new Path();
//            path.setId(134L);
//            path.setPath("/ms/*");
//            path.setService(serviceRepository.findById(36L).get());
//            pathRepository.save(path);
//        }
//        // TestConfig - Cube - Analyze/Retrieval
//        if(!testConfigRepository.existsById(135L)) {
//            TestConfig testConfig = new TestConfig();
//            testConfig.setId(135L);
//            testConfig.setApp(appRepository.findById(5L).get());
//            testConfig.setTestConfigName("Cube-Analyze/Retrieval");
//            testConfig.setGatewayService(serviceRepository.findById(56L).get());
//            testConfigRepository.save(testConfig);
//        }
//        if(!testPathRepository.existsById(136L)) {
//            TestPath testPath = new TestPath();
//            testPath.setId(136L);
//            testPath.setPath(pathRepository.findById(127L).get());
//            testPath.setTestConfig(testConfigRepository.findById(135L).get());
//            testPathRepository.save(testPath);
//        }
//        if(!testPathRepository.existsById(137L)) {
//            TestPath testPath = new TestPath();
//            testPath.setId(137L);
//            testPath.setPath(pathRepository.findById(128L).get());
//            testPath.setTestConfig(testConfigRepository.findById(135L).get());
//            testPathRepository.save(testPath);
//        }
//
//        if(!instanceRepository.existsById(138L)) {
//            Instance instance = new Instance();
//            instance.setId(138L);
//            instance.setName("DEMO-AS");
//            instance.setGatewayEndpoint("http://demo-as.dev.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceRepository.existsById(139L)) {
//            Instance instance = new Instance();
//            instance.setId(139L);
//            instance.setName("DEMO-PD");
//            instance.setGatewayEndpoint("http://demo-pd.dev.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceRepository.existsById(140L)) {
//            Instance instance = new Instance();
//            instance.setId(140L);
//            instance.setName("DEMO-SM");
//            instance.setGatewayEndpoint("http://demo-sm.dev.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceRepository.existsById(141L)) {
//            Instance instance = new Instance();
//            instance.setId(141L);
//            instance.setName("DEMO.PROD");
//            instance.setGatewayEndpoint("http://demo.prod.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceRepository.existsById(142L)) {
//            Instance instance = new Instance();
//            instance.setId(142L);
//            instance.setName("DEMO.PROD.V2");
//            instance.setGatewayEndpoint("http://demo.prod.v2.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//        // MovieInfo App
//        // Postgres
//        if(!testVirtualizedServiceRepository.existsById(143L)) {
//            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
//            testVirtualizedService.setId(143L);
//            testVirtualizedService.setService(serviceRepository.findById(15L).get());
//            testVirtualizedService.setTestConfig(testConfigRepository.findById(99L).get());
//            testVirtualizedServiceRepository.save(testVirtualizedService);
//        }
//        // Reviews
//        if(!testVirtualizedServiceRepository.existsById(144L)) {
//            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
//            testVirtualizedService.setId(144L);
//            testVirtualizedService.setService(serviceRepository.findById(11L).get());
//            testVirtualizedService.setTestConfig(testConfigRepository.findById(99L).get());
//            testVirtualizedServiceRepository.save(testVirtualizedService);
//        }
//        // Details
//        if(!testVirtualizedServiceRepository.existsById(145L)) {
//            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
//            testVirtualizedService.setId(145L);
//            testVirtualizedService.setService(serviceRepository.findById(13L).get());
//            testVirtualizedService.setTestConfig(testConfigRepository.findById(99L).get());
//            testVirtualizedServiceRepository.save(testVirtualizedService);
//        }
//        // Solr
//        if(!testVirtualizedServiceRepository.existsById(146L)) {
//            TestVirtualizedService testVirtualizedService = new TestVirtualizedService();
//            testVirtualizedService.setId(146L);
//            testVirtualizedService.setService(serviceRepository.findById(38L).get());
//            testVirtualizedService.setTestConfig(testConfigRepository.findById(135L).get());
//            testVirtualizedServiceRepository.save(testVirtualizedService);
//        }
//
//        if (!userRepository.existsById(147L)){
//            UserDTO userDTO = new UserDTO();
//            userDTO.setId(147L);
//            userDTO.setName("Demo");
//            userDTO.setEmail("flipkart@cubecorp.io");
//            userDTO.setPassword("password123");
//            userDTO.setCustomerId(1L);
//            userDTO.setRoles(Arrays.asList("ROLE_USER"));
//            userDTO.setActivated(true);
//            User user = this.userService.save(userDTO, true, false);
//            log.info("User with email '{}' created", user.getUsername());
//        }
//
//        if(!instanceUserRepository.existsById(148L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(148L);
//            instanceUser.setInstance(instanceRepository.findById(6L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(149L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(149L);
//            instanceUser.setInstance(instanceRepository.findById(7L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(150L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(150L);
//            instanceUser.setInstance(instanceRepository.findById(8L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(151L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(151L);
//            instanceUser.setInstance(instanceRepository.findById(138L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(152L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(152L);
//            instanceUser.setInstance(instanceRepository.findById(139L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(153L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(153L);
//            instanceUser.setInstance(instanceRepository.findById(140L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(154L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(154L);
//            instanceUser.setInstance(instanceRepository.findById(141L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(155L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(155L);
//            instanceUser.setInstance(instanceRepository.findById(142L).get());
//            instanceUser.setUser(userRepository.findById(2L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceRepository.existsById(156L)) {
//            Instance instance = new Instance();
//            instance.setId(156L);
//            instance.setName("FLIPKART");
//            instance.setGatewayEndpoint("http://flipkart.prod.v2.cubecorp.io");
//            instance.setApp(this.appRepository.findById(4L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceRepository.existsById(157L)) {
//            Instance instance = new Instance();
//            instance.setId(157L);
//            instance.setName("FLIPKART");
//            instance.setGatewayEndpoint("http://flipkart.prod.v2.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceUserRepository.existsById(158L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(158L);
//            instanceUser.setInstance(instanceRepository.findById(156L).get());
//            instanceUser.setUser(userRepository.findById(147L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(159L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(159L);
//            instanceUser.setInstance(instanceRepository.findById(157L).get());
//            instanceUser.setUser(userRepository.findById(147L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if (!userRepository.existsById(160L)){
//            UserDTO userDTO = new UserDTO();
//            userDTO.setId(160L);
//            userDTO.setName("Demo");
//            userDTO.setEmail("narvar@cubecorp.io");
//            userDTO.setPassword("password123");
//            userDTO.setCustomerId(1L);
//            userDTO.setRoles(Arrays.asList("ROLE_USER"));
//            userDTO.setActivated(true);
//            User user = this.userService.save(userDTO, true, false);
//            log.info("User with email '{}' created", user.getUsername());
//        }
//
//        if(!instanceRepository.existsById(161L)) {
//            Instance instance = new Instance();
//            instance.setId(161L);
//            instance.setName("NARVAR");
//            instance.setGatewayEndpoint("http://narvar.prod.v2.cubecorp.io");
//            instance.setApp(this.appRepository.findById(4L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceRepository.existsById(162L)) {
//            Instance instance = new Instance();
//            instance.setId(162L);
//            instance.setName("NARVAR");
//            instance.setGatewayEndpoint("http://narvar.prod.v2.cubecorp.io");
//            instance.setApp(this.appRepository.findById(5L).get());
//            this.instanceRepository.save(instance);
//        }
//
//        if(!instanceUserRepository.existsById(163L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(163L);
//            instanceUser.setInstance(instanceRepository.findById(161L).get());
//            instanceUser.setUser(userRepository.findById(160L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!instanceUserRepository.existsById(164L)) {
//            InstanceUser instanceUser = new InstanceUser();
//            instanceUser.setId(164L);
//            instanceUser.setInstance(instanceRepository.findById(162L).get());
//            instanceUser.setUser(userRepository.findById(160L).get());
//            instanceUserRepository.save(instanceUser);
//        }
//
//        if(!appUserRepository.existsById(165L)) {
//            AppUser appUser = new AppUser();
//            appUser.setId(165L);
//            appUser.setApp(appRepository.findById(4L).get());
//            appUser.setUser(userRepository.findById(2L).get());
//            appUserRepository.save(appUser);
//        }
//
//        if(!appUserRepository.existsById(166L)) {
//            AppUser appUser = new AppUser();
//            appUser.setId(166L);
//            appUser.setApp(appRepository.findById(5L).get());
//            appUser.setUser(userRepository.findById(2L).get());
//            appUserRepository.save(appUser);
//        }
//
//        if(!appUserRepository.existsById(167L)) {
//            AppUser appUser = new AppUser();
//            appUser.setId(167L);
//            appUser.setApp(appRepository.findById(4L).get());
//            appUser.setUser(userRepository.findById(147L).get());
//            appUserRepository.save(appUser);
//        }
//
//        if(!appUserRepository.existsById(168L)) {
//            AppUser appUser = new AppUser();
//            appUser.setId(168L);
//            appUser.setApp(appRepository.findById(4L).get());
//            appUser.setUser(userRepository.findById(160L).get());
//            appUserRepository.save(appUser);
//        }
//
//        // Intermediate services
//        // RestWrapJDBC
//        if(!testIntermediateServiceRepository.existsById(169L)) {
//            TestIntermediateService intermediateService = new TestIntermediateService();
//            intermediateService.setId(169L);
//            intermediateService.setService(serviceRepository.findById(14L).get());
//            intermediateService.setTestConfig(testConfigRepository.findById(99L).get());
//            testIntermediateServiceRepository.save(intermediateService);
//        }
//
//        // Jira User Credentials
//        if(!jiraUserCredentialsRepository.existsById(170L)) {
//            JiraUserCredentials jiraUserCredentials = new JiraUserCredentials();
//            jiraUserCredentials.setUser(userRepository.getOne(2L));
//            jiraUserCredentials.setUserName("siddhant.mutha@meshdynamics.io");
//            jiraUserCredentials.setAPIKey("fAODfwU3eTmrEDSdz7gM26C4");
//            jiraUserCredentials.setJiraBaseURL("https://cubeio.atlassian.net");
//            jiraUserCredentialsRepository.save(jiraUserCredentials);
//        }
//
//        // Email domain: meshdynamics.io
//        if(!emailDomainRepository.existsById(171L)) {
//            EmailDomain emailDomain = new EmailDomain();
//            emailDomain.setId(171L);
//            emailDomain.setDomain("meshdynamics.io");
//            emailDomain.setCustomer(this.customerService.getById(1L).get());
//            emailDomainRepository.save(emailDomain);
//        }
//
    }
}
