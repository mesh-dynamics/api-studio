package com.cube.interceptor.spring.egress;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import io.md.utils.CommonUtils;

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute after Client Filter.
 **/
@Component
@Order(3001)
public class RestTemplateTracingInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
		ClientHttpRequestExecution execution) throws IOException {
		MultivaluedMap<String, String> requestHeaders = Utils
			.getMultiMap(httpRequest.getHeaders().entrySet());
		CommonUtils.injectContext(requestHeaders);

		//Need to add the md-context headers to the original request
		//if underlying framework doesn't have MultivaluedMap o/p for headers
		requestHeaders.keySet().removeAll(httpRequest.getHeaders().keySet());
		httpRequest.getHeaders().putAll(requestHeaders);

		return execution.execute(httpRequest, bytes);
	}
}
