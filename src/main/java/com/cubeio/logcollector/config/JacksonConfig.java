package com.cubeio.logcollector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.utils.CubeObjectMapperProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
	@Bean
	public ObjectMapper jsonObjectMapper() {
		return CubeObjectMapperProvider.getInstance();
	}
}
