package com.cubeui.backend;

import com.cubeui.backend.domain.DTO.UserDTO;
import com.cubeui.backend.domain.Product;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.ProductRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private UserService userService;
    private ProductRepository productRepository;

    public DataInitializer(UserService userService, ProductRepository productRepository) {
        this.userService = userService;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("vineetks");
        userDTO.setPassword("vineetks");
        userDTO.setRoles(Arrays.asList("ROLE_USER"));
        this.userService.save(userDTO);

        userDTO.setUsername("admin");
        userDTO.setPassword("admin");
        userDTO.setRoles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
        this.userService.save(userDTO);
        log.debug("printing all users...");
        this.userService.getAllUsers().forEach(v -> log.debug(" User :" + v.toString()));

        this.productRepository.saveAndFlush(Product.builder().name("Sandisk Pen drive").price(849).build());
        this.productRepository.saveAndFlush(Product.builder().name("Redmi Note 3").price(11999).build());
    }
}
