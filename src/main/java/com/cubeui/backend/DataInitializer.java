package com.cubeui.backend;

import com.cubeui.backend.domain.Product;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.ProductRepository;
import com.cubeui.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.debug("Initializing data...");
        this.userRepository.save(User.builder()
                .username("vineetks")
                .password(this.passwordEncoder.encode("vineetks"))
                .roles(Arrays.asList( "ROLE_USER"))
                .build()
        );

        this.userRepository.save(User.builder()
                .username("admin")
                .password(this.passwordEncoder.encode("admin"))
                .roles(Arrays.asList("ROLE_USER", "ROLE_ADMIN"))
                .build()
        );
        log.debug("printing all users...");
        this.userRepository.findAll().forEach(v -> log.debug(" User :" + v.toString()));

        this.productRepository.saveAndFlush(Product.builder().name("Sandisk Pen drive").price(849).build());
        this.productRepository.saveAndFlush(Product.builder().name("Redmi Note 3").price(11999).build());
    }
}
