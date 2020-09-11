package com.cubeui.backend.config;

import com.cubeui.backend.security.filters.NoContentTypeFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoContentTypeFilterConfig {

    @Bean
    public FilterRegistrationBean getBean(){

        FilterRegistrationBean bean = new FilterRegistrationBean();
        bean.setFilter(new NoContentTypeFilter());
        return bean;
    }
}
