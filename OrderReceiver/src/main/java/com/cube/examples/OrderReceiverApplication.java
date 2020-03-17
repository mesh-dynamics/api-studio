package com.cube.examples;


import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {"com.cube.examples", "com.cube.interceptor.spring.ingress"})
public class OrderReceiverApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderReceiverApplication.class, args);
	}

	@Bean
	public RestTemplate getRestTemplate() {
		RestTemplate restTemplate = new RestTemplate();
//		restTemplate.setInterceptors(
//			List.of(new com.cube.interceptor.spring.egress.RestTemplateTracingInterceptor(),
//				new com.cube.interceptor.spring.egress.RestTemplateDataInterceptor()));
		return restTemplate;
	}
}
