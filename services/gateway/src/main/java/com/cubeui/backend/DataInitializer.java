/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend;

import com.cubeui.backend.domain.App;
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
import com.cubeui.backend.service.AppFileStorageService;
import com.cubeui.backend.service.CustomerService;
import com.cubeui.backend.service.UserService;
import java.util.ArrayList;
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

    private AppRepository appRepository;

    private HttpServletRequest httpServletRequest;

    private AppFileStorageService appFileStorageService;

    private DevtoolEnvironmentsRepository devtoolEnvironmentsRepository;

    private AppFileRepository appFileRepository;


    public DataInitializer(UserService userService, CustomerService customerService,
        CustomerRepository customerRepository, UserRepository userRepository,
        HttpServletRequest httpServletRequest, AppRepository appRepository,
        AppFileStorageService appFileStorageService, DevtoolEnvironmentsRepository devtoolEnvironmentsRepository,
        AppFileRepository appFileRepository) {

        this.userService = userService;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.appRepository = appRepository;
        this.httpServletRequest = httpServletRequest;
        this.appFileStorageService = appFileStorageService;
        this.devtoolEnvironmentsRepository = devtoolEnvironmentsRepository;
        this.appFileRepository = appFileRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");

        Optional<Customer> customer = customerRepository.findByName("Admin");
        if(customer.isEmpty()) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setName("Admin");
            customerDTO.setEmail("admin");
            customerDTO.setDomainURLs(Set.of("admin.io"));
            customer = Optional.of(this.customerService.save(httpServletRequest, customerDTO));
       }

        Optional<User> user = userRepository.findByUsernameIgnoreCase("admin");
        if (user.isEmpty()) {
            UserDTO userDTOAdmin = new UserDTO();
            //userDTO.setId(3L);
            userDTOAdmin.setName("Administrator");
            userDTOAdmin.setEmail("admin");
            userDTOAdmin.setPassword("admin");
            userDTOAdmin.setCustomerId(customer.get().getId());
            userDTOAdmin.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
            userDTOAdmin.setActivated(true);
            User saved = this.userService.save(userDTOAdmin, true, true);
            //userService.createHistoryForEachApp(httpServletRequest, saved);

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
    }
}
