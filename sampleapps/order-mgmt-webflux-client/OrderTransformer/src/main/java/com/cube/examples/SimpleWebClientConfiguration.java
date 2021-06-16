/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
