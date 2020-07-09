package com.cubeui.backend;

//import io.md.cube.spring.egress.RestTemplateMockInterceptor;
//mport io.md.cube.spring.egress.RestTemplateTracingInterceptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.cubeui.backend.security.Constants.SPRING_PROFILE_DEFAULT;
import static com.cubeui.backend.security.Constants.SPRING_PROFILE_DEVELOPMENT;

@Slf4j
@EnableAsync
//@SpringBootApplication(scanBasePackages = {"com.cubeui.backend", "io.md.cube"})
@SpringBootApplication
public class BackendApplication {

    @Value("${allowed.origins}")
    private String allowedOrigin;

    @Autowired RestTemplate restTemplate;

    public static void main(String[] args) {
//        SpringApplication.run(BackendApplication.class, args);
        SpringApplication app = new SpringApplication(BackendApplication.class);
        addDefaultProfile(app);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                final List<String> allowedOrigins = new ArrayList<>();
                if (!allowedOrigin.isEmpty()) {
                    String[] origins_from_env = allowedOrigin.split(",");
                    allowedOrigins.addAll(Arrays.asList(origins_from_env));
                } else {
                    try {
                        File file = ResourceUtils.getFile("classpath:webCorsAllowedOrigins.txt");
                        Files.lines(Paths.get(file.toURI()), StandardCharsets.UTF_8)
                            .forEach(line -> allowedOrigins.add(line));
                    } catch (FileNotFoundException e) {
                        log.info("Allowed Origins File not found");
                    } catch (IOException e) {
                        log.info("Allowed Origins File Content Exception");
                    }
                }
                registry.addMapping("/**").allowedOrigins(allowedOrigins.toArray(String[]::new));
            }
        };
    }

    @Bean(name = "appRestClient")
    public RestTemplate getRestTemplate() {
        ArrayList<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        //interceptors.add(new RestTemplateMockInterceptor());
        //interceptors.add(new RestTemplateTracingInterceptor());
        //restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info("\n" +
                        "-------------------------------------------------------------\n\t" +
                        "Application '{}' is running! Access URLs:\n\t" +
                        "Local: \t\t{}://localhost:{}{}\n\t" +
                        "External: \t{}://{}:{}{}\n\t" +
                        "Profile(s): \t{}\n" +
                        "-------------------------------------------------------------",
                env.getProperty("spring.application.name"), protocol, serverPort, contextPath, protocol, hostAddress,
                serverPort, contextPath, env.getActiveProfiles());
    }

    private static void addDefaultProfile(SpringApplication app) {
        Map<String, Object> defProperties = new HashMap<>();
        defProperties.put(SPRING_PROFILE_DEFAULT, SPRING_PROFILE_DEVELOPMENT);
        app.setDefaultProperties(defProperties);
    }
}