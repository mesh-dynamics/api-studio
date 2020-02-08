package com.cube.interceptor.config;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

import com.cube.interceptor.reactive_spring.interceptor.OkHttp3Interceptor;

@Configuration
public class SpringLoggingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public OkHttpClient okHttp3Client() {
		return new OkHttpClient.Builder()
			.connectTimeout(10, TimeUnit.SECONDS)
			.readTimeout(30, TimeUnit.SECONDS)
			.writeTimeout(10, TimeUnit.SECONDS)
			.addInterceptor(new OkHttp3Interceptor()).build();
	}

}
