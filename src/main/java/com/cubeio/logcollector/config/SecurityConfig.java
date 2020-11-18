package com.cubeio.logcollector.config;

import com.cubeio.logcollector.security.jwt.JwtConfigurer;
import com.cubeio.logcollector.security.jwt.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {


    @Autowired
    JwtTokenValidator jwtTokenValidator;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //@formatter:off
        http
            .httpBasic().disable()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
                .authorizeRequests()
                //.antMatchers("/gaurav").permitAll()
                .antMatchers("/api/health").permitAll()
                .antMatchers("/api/login").permitAll()
                .antMatchers("/api/token").permitAll()
                .antMatchers("/api/config/get").permitAll()
                .antMatchers("/api/account/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .apply(new JwtConfigurer(jwtTokenValidator));
        //@formatter:on
        http.cors();
    }


}