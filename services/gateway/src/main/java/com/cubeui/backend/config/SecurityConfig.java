package com.cubeui.backend.config;

import com.cubeui.backend.security.jwt.JwtConfigurer;
import com.cubeui.backend.security.jwt.JwtTokenProvider;
import com.cubeui.backend.security.jwt.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;

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
                .antMatchers("/api/health").permitAll()
                .antMatchers("/api/login").permitAll()
                .antMatchers("/api/mst/**").permitAll()
                .antMatchers("/api/token").permitAll()
                .antMatchers("/api/config/get").permitAll()
//                .antMatchers("/api/data").permitAll()
                .antMatchers("/api/account/**").permitAll()
//                .antMatchers("/as/**").permitAll()
//                .antMatchers("/cs/**").permitAll()
//                .antMatchers("/ms/**").permitAll()
//                .antMatchers("/rs/**").permitAll()
//                .antMatchers(HttpMethod.GET, "/products/**").permitAll()
//                .antMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("USER")
                .anyRequest().authenticated()
            .and()
            .apply(new JwtConfigurer(jwtTokenValidator));
        //@formatter:on
        http.cors();
    }


}