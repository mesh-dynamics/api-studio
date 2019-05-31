package com.cubeui.backend;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.service.MailService;
import com.cubeui.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private UserService userService;

    @Autowired
    private MailService mailService;

    public DataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        mailService.sendEmail();
        log.debug("Initializing data...");
        UserDTO userDTO = new UserDTO();
        if (userService.getByUsername("vineetks.iitk@gmail.com").isEmpty()){
            userDTO.setName("Vineet Kumar Singh");
            userDTO.setEmail("vineetks.iitk@gmail.com");
            userDTO.setPassword("vineetks");
            userDTO.setRoles(Arrays.asList("ROLE_USER"));
            userDTO.setActivated(false);
            User user = this.userService.save(userDTO, false);
            mailService.sendActivationEmail(user);
        }

        if (userService.getByUsername("admin").isEmpty()){
            userDTO.setName("Administrator");
            userDTO.setEmail("admin");
            userDTO.setPassword("admin");
            userDTO.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
            userDTO.setActivated(true);
            this.userService.save(userDTO, true);
        }

        log.debug("printing all users...");
        this.userService.getAllUsers().forEach(v -> log.debug(" User :" + v.toString()));
    }
}
