package com.cubeui.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.utils.CubeObjectMapperProvider;

/*
 * Created by IntelliJ IDEA.
 * Date: 2020-04-30
 */
@Configuration
public class JacksonConfig {
	@Bean
	public ObjectMapper jsonObjectMapper() {
		return CubeObjectMapperProvider.getInstance();
	}
}
