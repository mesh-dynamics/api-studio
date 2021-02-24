package com.cubeui.backend;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.AppFile;
import com.cubeui.backend.domain.AppFilePath;
import com.cubeui.backend.domain.CustomMultipartFile;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.CustomerDTO;
import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.DtEnvVar;
import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.AppFileRepository;
import com.cubeui.backend.repository.AppRepository;
import com.cubeui.backend.repository.CustomerRepository;
import com.cubeui.backend.repository.DevtoolEnvironmentsRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.service.AWSS3AppFileStorageServiceImpl;
import com.cubeui.backend.service.AppFileStorageService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import com.cubeui.backend.service.exception.FileRetrievalException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
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
import org.springframework.web.multipart.MultipartFile;

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

    private AppFileRepository appFileRepository;

    private AWSS3AppFileStorageServiceImpl awss3AppFileStorageService;

    public DataInitializer(UserService userService, CustomerService customerService,
        CustomerRepository customerRepository, UserRepository userRepository,
        HttpServletRequest httpServletRequest, AppRepository appRepository,
        AppFileStorageService appFileStorageService, DevtoolEnvironmentsRepository devtoolEnvironmentsRepository,
        AppFileRepository appFileRepository, AWSS3AppFileStorageServiceImpl awss3AppFileStorageService) {

        this.userService = userService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.appRepository = appRepository;
        this.httpServletRequest = httpServletRequest;
        this.appFileStorageService = appFileStorageService;
        this.devtoolEnvironmentsRepository = devtoolEnvironmentsRepository;
        this.appFileRepository = appFileRepository;
        this.awss3AppFileStorageService = awss3AppFileStorageService;
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
                            dtEnvironment.setGlobal(false);
                            devtoolEnvironmentsRepository.save(dtEnvironment);
                        } else {
                            DtEnvironment dt = new DtEnvironment(dtEnvironment.getName());
                            dt.setApp(apps.get(i));
                            dt.setUser(dtEnvironment.getUser());
                            List<DtEnvVar> envVarsList = new ArrayList<>(dtEnvironment.getVars().size());
                            for (DtEnvVar dtEnvVar : dtEnvironment.getVars()) {
                                DtEnvVar newDtEnvVar = new DtEnvVar();
                                newDtEnvVar.setKey(dtEnvVar.getKey());
                                newDtEnvVar.setValue(dtEnvVar.getValue());
                                newDtEnvVar.setEnvironment(dt);
                                envVarsList.add(newDtEnvVar);
                            }
                            dt.setVars(envVarsList);
                            dt.setGlobal(false);
                            devtoolEnvironmentsRepository.save(dt);
                        }
                    }
                }
            }
        });

        /** TODO
         *  delete in next release
         */
        List<AppFile> appFiles = this.appFileRepository.findAll();
        appFiles.forEach(appFile -> {
            Optional<AppFilePath> appFilePath = this.appFileStorageService.getFilePathByAppId(appFile.getApp().getId());
            if(appFilePath.isEmpty()) {
                MultipartFile multipartFile = convertToMultiPartFile(appFile);
                this.awss3AppFileStorageService.storeFile(multipartFile, appFile.getApp(), false);
            }
        });
    }

    public static MultipartFile convertToMultiPartFile(AppFile appFile) {
        byte[] bytes = decompressBytes(appFile.getData());
        return new CustomMultipartFile(bytes, appFile.getFileName(), appFile.getFileType());
    }

    public static byte[] decompressBytes(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            outputStream.close();
        } catch (IOException | DataFormatException ex) {
            log.error("Error while decompressing the file ", ex.getMessage());
            throw new FileRetrievalException("Error while decompressing the file " + ex.getMessage());
        }
        return outputStream.toByteArray();
    }
}
