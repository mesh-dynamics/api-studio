package com.cube.interceptor;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import io.md.utils.CommonUtils;

@Component
@Order(3001)
public class TracingFilter implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
		ClientHttpRequestExecution execution) throws IOException {
		MultivaluedMap<String, String> requestHeaders = Utils
			.getMultiMap(httpRequest.getHeaders().entrySet());
		CommonUtils.injectContext(requestHeaders);
		return execution.execute(httpRequest, bytes);
	}
}
