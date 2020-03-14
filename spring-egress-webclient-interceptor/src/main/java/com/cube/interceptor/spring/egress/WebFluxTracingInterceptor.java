package com.cube.interceptor.spring.egress;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
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
			HttpHeaders headers = clientRequest.headers();
			CommonUtils.injectContext(Utils.getMultiMap(headers.entrySet()));
			return next.exchange(clientRequest);
		};
	}
}
