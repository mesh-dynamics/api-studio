package com.cube.examples;

import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.cube.interceptor.spring.egress.WebFluxDataInterceptor;
import com.cube.interceptor.spring.egress.WebFluxTracingInterceptor;


@Component
public class SimpleWebClientConfiguration {

	//private static final String BASE_URL = "http://order-processor:9080";
    private static final String BASE_URL = "http://localhost:8082";

	@Bean
	public WebClient defaultWebClient() {

		return WebClient.builder()
			.baseUrl(BASE_URL)
			.clientConnector(new JettyClientHttpConnector(WebFluxDataInterceptor.defaultHttpClient()))
			.filter(WebFluxTracingInterceptor.logFilter())
			.defaultCookie("cookieKey", "cookieValue", "teapot", "amsterdam")
			.defaultCookie("secretToken", UUID.randomUUID().toString())
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.defaultHeader(HttpHeaders.USER_AGENT, "I'm a teapot")
			.build();
	}
}
