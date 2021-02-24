package com.cubeui.backend.service;

import static org.springframework.http.ResponseEntity.ok;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.ServiceGraph;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestIntermediateService;
import com.cubeui.backend.domain.TestPath;
import com.cubeui.backend.domain.TestService;
import com.cubeui.backend.domain.TestVirtualizedService;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.InstanceRepository;
import com.cubeui.backend.repository.PathRepository;
import com.cubeui.backend.repository.ServiceGraphRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.repository.TestIntermediateServiceRepository;
import com.cubeui.backend.repository.TestPathRepository;
import com.cubeui.backend.repository.TestServiceRepository;
import com.cubeui.backend.repository.TestVirtualizedServiceRepository;
import com.cubeui.backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class CustomerService {

    @Value("${md_cloud}")
    private boolean md_cloud = false;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AppRepository appRepository;
    @Autowired
    private InstanceRepository instanceRepository;
    @Autowired
    private ServiceGroupRepository serviceGroupRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private ServiceGraphRepository serviceGraphRepository;
    @Autowired
    private PathRepository pathRepository;
    @Autowired
    private TestConfigRepository testConfigRepository;
    @Autowired
    private TestIntermediateServiceRepository testIntermediateServiceRepository;
    @Autowired
    private TestPathRepository testPathRepository;
    @Autowired
    private TestServiceRepository testServiceRepository;
    @Autowired
    private TestVirtualizedServiceRepository testVirtualizedServiceRepository;
    @Autowired
    private CubeServerService cubeServerService;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private CollectionCreationService collectionCreationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AWSS3AppFileStorageServiceImpl awss3AppFileStorageService;

    public Optional<Customer> getByName(String name) {
        return customerRepository.findByName(name);
    }

    public Optional<Customer> getById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer save(HttpServletRequest httpServletRequest, CustomerDTO customerDTO) {
        Optional<Customer> customer = customerRepository.findByName(customerDTO.getName());
        customer.ifPresent(c -> {
            Optional.ofNullable(customerDTO.getName()).ifPresent(name -> c.setName(name));
            Optional.ofNullable(customerDTO.getEmail()).ifPresent(name -> c.setEmail(name));
            c.setDomainUrls(customerDTO.getDomainURLs());
            this.customerRepository.save(c);
        });
        if (customer.isEmpty()){
            customer = Optional.of(this.customerRepository.save(Customer.builder()
                    .name(customerDTO.getName())
                    .email(customerDTO.getEmail())
                    .domainUrls(customerDTO.getDomainURLs())
                    .build()
            ));
            App defaultApp = this.appRepository.save(
                App.builder()
                    .name("Default")
                    .displayName("Default")
                    .customer(customer.get())
                    .build());
            this.awss3AppFileStorageService.storeFile(null, defaultApp, false);
            if(md_cloud) {
                Optional<App> app = createMovieInfoAppForCustomer(customer.get());
                if(app.isPresent()) {
                    this.awss3AppFileStorageService.storeFile(null, app.get(), false);
                    collectionCreationService.createSampleCollectionForCustomer(httpServletRequest, customer.get(), app.get());
                }
            }
        }
        return customer.get();
    }

    public ResponseEntity deleteCustomer(Customer customer) {
        ResponseEntity responseEntity = deleteAllData(customer.getName());
        if(responseEntity.getStatusCode() == HttpStatus.OK) {
            this.userRepository.deleteByCustomerId(customer.getId());
            this.customerRepository.delete(customer);
            return ok("Customer is removed successfully");
        }
        return responseEntity;
    }

    private ResponseEntity deleteAllData(String customerName) {
        return cubeServerService.fetchPostResponse(httpServletRequest, Optional.empty(),
                "/cs/deleteCustomerData/"+ customerName);
    }

    public Optional<Customer> getByDomainUrl(String domainUrl) {
        return this.customerRepository.findByDomainUrls(domainUrl);
    }

    private Optional<App> createMovieInfoAppForCustomer(Customer customer) {
        Optional<Customer> cubeCorpCustomer = this.customerRepository.findByName("CubeCorp");
        if(cubeCorpCustomer.isEmpty()) {
            log.info("CubeCorp customer doesn't exist");
            return Optional.empty();
        }
        Optional<App> existingMovieInfo = this.appRepository.findByNameAndCustomerId("MovieInfo", cubeCorpCustomer.get().getId());
        if ((existingMovieInfo.isEmpty())) {
            log.info("MovieInfo app doesn't exist");
            return Optional.empty();
        }
        long id = existingMovieInfo.get().getId();
        App movieInfo = this.appRepository.save(
            App.builder()
                .name(existingMovieInfo.get().getName())
                .displayName(existingMovieInfo.get().getDisplayName())
                .customer(customer)
                .build());

        Optional<List<ServiceGroup>> existingServiceGroups = this.serviceGroupRepository.findByAppId(id);
        existingServiceGroups.ifPresent(serviceGroups -> {
            serviceGroups.forEach(s -> {
                ServiceGroup savedServiceGroup = this.serviceGroupRepository.save(
                    ServiceGroup.builder()
                        .app(movieInfo)
                        .name(s.getName())
                        .build());
                Optional<List<com.cubeui.backend.domain.Service>> existingServices = this.serviceRepository.findByServiceGroupId(s.getId());
                existingServices.ifPresent(services ->
                    services.forEach(service -> {
                    com.cubeui.backend.domain.Service savedService = this.serviceRepository.save(
                        com.cubeui.backend.domain.Service.builder()
                            .app(movieInfo)
                            .serviceGroup(savedServiceGroup)
                            .name(service.getName())
                            .build());
                    Optional<List<Path>> existingPaths = this.pathRepository.findByServiceId(service.getId());
                    existingPaths.ifPresent(paths -> {
                        paths.forEach(path -> {
                            Path savedPath = this.pathRepository.save(
                                Path.builder()
                                    .service(savedService)
                                    .path(path.getPath())
                                    .build());
                        });
                    });
                }));
            });
        });

        Optional<List<ServiceGraph>> existingServiceGraph = this.serviceGraphRepository.findByAppId(id);
        existingServiceGraph.ifPresent(serviceGraphs -> {
            serviceGraphs.forEach(service -> {
                com.cubeui.backend.domain.Service existingFromService = service.getFromService();
                com.cubeui.backend.domain.Service existingToService = service.getToService();
                Optional<com.cubeui.backend.domain.Service> fromService =
                    this.serviceRepository.findByNameAndAppId(existingFromService.getName(), movieInfo.getId());
                Optional<com.cubeui.backend.domain.Service> toService =
                    this.serviceRepository.findByNameAndAppId(existingToService.getName(), movieInfo.getId());
                if(fromService.isPresent() && toService.isPresent()) {
                    this.serviceGraphRepository.save(
                        ServiceGraph.builder()
                            .app(movieInfo)
                            .fromService(fromService.get())
                            .toService(toService.get())
                            .build());
                }
            });
        });

        Optional<List<TestConfig>> existingTestingConfig = this.testConfigRepository.findByAppId(id);
        existingTestingConfig.ifPresent(testConfigs -> {
            testConfigs.forEach(testConfig -> {
                TestConfig savedTestConfig = this.testConfigRepository.save(
                    TestConfig.builder()
                        .testConfigName(testConfig.getTestConfigName())
                        .app(movieInfo)
                        .description(testConfig.getDescription())
                        .gatewayReqSelection(testConfig.getGatewayReqSelection())
                        .maxRunTimeMin(testConfig.getMaxRunTimeMin())
                        .emailId(testConfig.getEmailId())
                        .slackId(testConfig.getSlackId())
                        .tag(testConfig.getTag())
                        .dynamicInjectionConfigVersion(testConfig.getDynamicInjectionConfigVersion())
                        .build());
                Optional<List<TestIntermediateService>> existingTestIntermediateServices =
                    this.testIntermediateServiceRepository.findByTestConfigId(testConfig.getId());
                existingTestIntermediateServices.ifPresent(testIntermediateServices -> {
                    testIntermediateServices.stream().parallel().forEach(testIntermediateService -> {
                        com.cubeui.backend.domain.Service existingService = testIntermediateService.getService();
                        Optional<com.cubeui.backend.domain.Service> service = this.serviceRepository.findByNameAndAppId(existingService.getName(), movieInfo.getId());
                        service.ifPresent(s -> this.testIntermediateServiceRepository.save(
                            TestIntermediateService.builder().service(s).testConfig(savedTestConfig).build()));
                    });
                });

                Optional<List<TestService>> existingTestServices =
                    this.testServiceRepository.findByTestConfigId(testConfig.getId());
                existingTestServices.ifPresent(testServices -> {
                    testServices.forEach(testService -> {
                        com.cubeui.backend.domain.Service existingService = testService.getService();
                        Optional<com.cubeui.backend.domain.Service> service = this.serviceRepository.findByNameAndAppId(existingService.getName(), movieInfo.getId());
                        service.ifPresent(s -> this.testServiceRepository.save(
                            TestService.builder().service(s).testConfig(savedTestConfig).build()));
                    });
                });

                Optional<List<TestVirtualizedService>> existingTestVirtualizedServices =
                    this.testVirtualizedServiceRepository.findByTestConfigId(testConfig.getId());
                existingTestVirtualizedServices.ifPresent(testVirtualizedServices -> {
                    testVirtualizedServices.forEach(testVirtualizedService -> {
                        com.cubeui.backend.domain.Service existingService = testVirtualizedService.getService();
                        Optional<com.cubeui.backend.domain.Service> service = this.serviceRepository.findByNameAndAppId(existingService.getName(), movieInfo.getId());
                        service.ifPresent(s -> this.testVirtualizedServiceRepository.save(
                            TestVirtualizedService.builder().service(s).testConfig(savedTestConfig).build()));
                    });
                });

                Optional<List<TestPath>> existingTestPaths = this.testPathRepository.findByTestConfigId(testConfig.getId());
                existingTestPaths.ifPresent(testPaths -> {
                    testPaths.forEach(testPath -> {
                        Path existingPath = testPath.getPath();
                        Optional<com.cubeui.backend.domain.Service> service = this.serviceRepository.findByNameAndAppId(existingPath.getService().getName(), movieInfo.getId());
                        service.ifPresent(s -> {
                            Optional<Path> path = this.pathRepository.findByPathAndServiceId(existingPath.getPath(), s.getId());
                            path.ifPresent(p -> this.testPathRepository.save(TestPath.builder().path(p).testConfig(savedTestConfig).build()));
                        });
                    });
                });
            });
        });
        return Optional.of(movieInfo);
    }


}
