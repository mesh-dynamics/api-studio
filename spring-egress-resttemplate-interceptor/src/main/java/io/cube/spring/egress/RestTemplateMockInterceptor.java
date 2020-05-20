package io.cube.spring.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute after Mock Filter.
 **/
@Order(2999)
public class RestTemplateMockInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LoggerFactory
		.getLogger(RestTemplateMockInterceptor.class);

	@Override
	public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
		ClientHttpRequestExecution execution) throws IOException {
		URI originalUri = httpRequest.getURI();
		CommonConfig commonConfig = CommonConfig.getInstance();
		String serviceName = CommonUtils.getEgressServiceName(originalUri);
		HttpRequestWrapper newRequest = null;
		Span[] clientSpan = {null};
		Scope[] clientScope = {null};

		try {
			newRequest = commonConfig.getMockingURI(originalUri, serviceName).map(mockURI -> {

				Optional<Span> ingressSpan = CommonUtils.getCurrentSpan();

				//Empty ingress span pertains to DB initialization scenarios.
				SpanContext spanContext = ingressSpan.map(Span::context)
					.orElse(CommonUtils.createDefSpanContext());

				clientSpan[0] = CommonUtils
					.startClientSpan(Constants.MD_CHILD_SPAN, spanContext, false);

				clientScope[0] = CommonUtils.activateSpan(clientSpan[0]);

				MyHttpRequestWrapper request = new MyHttpRequestWrapper(httpRequest, mockURI);
				commonConfig.authToken.ifPresent(auth -> {
					request.putHeader(io.cube.agent.Constants.AUTHORIZATION_HEADER,
						Arrays.asList(auth));
				});

				if (!commonConfig.authToken.isPresent()) {
					LOGGER.info("Auth token not present for Mocking service");
				}

				return request;
			}).orElse(null);

		} catch (URISyntaxException e) {
			LOGGER.error("Mocking filter issue, exception during setting URI!", e);
			if (clientSpan[0] != null) {
				clientSpan[0].finish();
			}

			if (clientScope[0] != null) {
				clientScope[0].close();
			}
			return execution.execute(httpRequest, bytes);
		}
		ClientHttpResponse response = (newRequest == null) ? execution.execute(httpRequest, bytes)
			: execution.execute(newRequest, bytes);

		if (clientSpan[0] != null) {
			clientSpan[0].finish();
		}

		if (clientScope[0] != null) {
			clientScope[0].close();
		}

		return response;
	}

	public class MyHttpRequestWrapper extends HttpRequestWrapper {

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
