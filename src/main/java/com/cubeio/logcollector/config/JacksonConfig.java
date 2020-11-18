package com.cubeio.logcollector.config;

import com.cubeio.logcollector.utils.CubeObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
	@Bean
	public ObjectMapper jsonObjectMapper() {
		return CubeObjectMapperProvider.getInstance();
	}
}
