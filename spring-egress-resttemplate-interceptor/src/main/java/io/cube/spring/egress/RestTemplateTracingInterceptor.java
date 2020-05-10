package io.cube.spring.egress;

import java.io.IOException;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import io.cube.spring.egress.RestTemplateMockInterceptor.MyHttpRequestWrapper;
import io.md.utils.CommonUtils;

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute after Client Filter.
 **/
@Component
@Order(3001)
public class RestTemplateTracingInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LoggerFactory
		.getLogger(RestTemplateTracingInterceptor.class);

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
		ClientHttpRequestExecution execution) throws IOException {
		try {
			MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);

			//Need to add the md-context headers to the original request
			//if underlying framework doesn't have MultivaluedMap o/p for headers
			//httpRequest.getHeaders().putAll(mdTraceHeaders);

			for (String key : mdTraceHeaders.keySet()) {
				((MyHttpRequestWrapper) httpRequest).putHeader(key, mdTraceHeaders.get(key));
			}
		} catch (Exception ex) {
			LOGGER.error("Exception occured during logging, proceeding to the application!", ex);
		}

		return execution.execute(httpRequest, bytes);
	}
}
