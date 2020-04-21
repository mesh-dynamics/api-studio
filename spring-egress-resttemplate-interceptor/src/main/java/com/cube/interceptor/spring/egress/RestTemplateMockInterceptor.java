package com.cube.interceptor.spring.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.utils.CommonUtils;

public class RestTemplateMockInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LogManager.getLogger(RestTemplateMockInterceptor.class);

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
		ClientHttpRequestExecution execution) throws IOException {
		URI originalUri = httpRequest.getURI();
		CommonConfig commonConfig = CommonConfig.getInstance();
		String serviceName = CommonUtils.getEgressServiceName(originalUri);
		HttpRequestWrapper newRequest = null;
		try {
			newRequest = commonConfig.getMockingURI(originalUri, serviceName).map(mockURI -> {
				MyHttpRequestWrapper request = new MyHttpRequestWrapper(httpRequest, mockURI);
				commonConfig.authToken.ifPresentOrElse(auth -> {
					MultivaluedMap<String, String> clientHeaders = Utils
						.getMultiMap(httpRequest.getHeaders().entrySet());
					request.putHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER, List.of(auth));
				}, () -> {
					LOGGER.info("Auth token not present for Mocking service");
				});

				return request;
			}).orElse(null);
		} catch (URISyntaxException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Mocking filter issue, exception during setting URI!",
					Constants.EXCEPTION_STACK, e.getMessage())));
			return execution.execute(httpRequest, bytes);
		}
		return execution.execute(newRequest, bytes);
	}

	private class MyHttpRequestWrapper extends HttpRequestWrapper {

		private URI mockURI;
		private final MultivaluedMap<String, String> customHeaders;

		public MyHttpRequestWrapper(HttpRequest httpRequest, URI mockURI) {
			super(httpRequest);
			this.mockURI = mockURI;
			this.customHeaders = new MultivaluedHashMap<>();
		}

		public void putHeader(String name, List<String> value) {
			this.customHeaders.put(name, value);
		}

		@Override
		public URI getURI() {
			return mockURI;
		}

		@Override
		public HttpHeaders getHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.putAll(super.getHeaders());
			headers.putAll(customHeaders);

			return headers;
		}

	}
}
