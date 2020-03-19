package com.cube.interceptor.spring.egress;

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
			MultivaluedMap<String, String> requestHeaders = Utils
				.getMultiMap(clientRequest.headers().entrySet());
			CommonUtils.injectContext(requestHeaders);

			//Need to add the md-context headers to the original request
			//if underlying framework doesn't have MultivaluedMap o/p for headers
			requestHeaders.keySet().removeAll(clientRequest.headers().keySet());
			ClientRequest request = ClientRequest.from(clientRequest)
				.headers(httpHeaders -> httpHeaders.putAll(requestHeaders)).build();

			return next.exchange(request);
		};
	}
}
