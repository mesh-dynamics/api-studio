package com.cubeui.backend;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private UserService userService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");
        UserDTO userDTO = new UserDTO();
        if (userService.getByUsername("vineetks").isEmpty()){
            userDTO.setName("Vineet Kumar Singh");
            userDTO.setEmail("vineetks");
            userDTO.setPassword("vineetks");
            userDTO.setRoles(Arrays.asList("ROLE_USER"));
            this.userService.save(userDTO);
        }

        if (userService.getByUsername("admin").isEmpty()){
            userDTO.setName("Administrator");
            userDTO.setEmail("admin");
            userDTO.setPassword("admin");
            userDTO.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
            this.userService.save(userDTO);
        }

        log.debug("printing all users...");
        this.userService.getAllUsers().forEach(v -> log.debug(" User :" + v.toString()));
    }
}
