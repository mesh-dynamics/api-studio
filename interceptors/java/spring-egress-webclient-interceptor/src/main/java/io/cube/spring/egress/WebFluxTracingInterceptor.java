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

package com.cube.interceptor.spring.egress;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import io.md.utils.CommonUtils;

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute after Client Filter.
 **/
@Component
@Order(3001)
public class WebFluxTracingInterceptor {

	public static ExchangeFilterFunction logFilter() {
		return (clientRequest, next) -> {
			MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);

			//Need to add the md-context headers to the original request
			//if underlying framework doesn't have MultivaluedMap o/p for headers
			ClientRequest request = ClientRequest.from(clientRequest)
				.headers(httpHeaders -> httpHeaders.putAll(mdTraceHeaders)).build();

			return next.exchange(request);
		};
	}
}
