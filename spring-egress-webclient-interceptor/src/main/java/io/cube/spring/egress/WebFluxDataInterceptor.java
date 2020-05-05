package com.cube.interceptor.spring.egress;

import java.net.URI;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Span;

/**
 * Reference : https://www.baeldung.com/spring-log-webclient-calls
 */

/**
 * Order is to specify in which order the interceptors are to be executed. Lower the order, early
 * the interceptor is executed. We want Tracing interceptor to execute after Client interceptor.
 **/
@Component
@Order(3000)
public class WebFluxDataInterceptor {

	private static final Logger LOGGER = LogManager.getLogger(WebFluxDataInterceptor.class);

	private static final WebClientConfig config;

	static {
		config = new WebClientConfig();
	}

	@Bean
	public static HttpClient defaultHttpClient() {

		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
		HttpClient httpClient = new HttpClient(sslContextFactory) {

			MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
			MultivaluedMap<String, String> responseHeaders = new MultivaluedHashMap<>();
			MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
			MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
			MDTraceInfo mdTraceInfo = null;
			String apiPath = "";
			byte[] requestBody = new byte[0];
			byte[] responseBody = new byte[0];
			URI uri = null;
			String serviceName = "";
			MultivaluedMap<String, String> requestMeta = new MultivaluedHashMap<>();
			MultivaluedMap<String, String> responseMeta = new MultivaluedHashMap<>();
			boolean isSampled = false;
			boolean isVetoed = false;

			@Override
			public Request newRequest(URI uri) {
				Request request = super.newRequest(uri);
				requestHeaders = Utils.getHeaders(request.getHeaders());
				Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
				if (currentSpan.isPresent()) {
					Span span = currentSpan.get();
					//Either baggage has sampling set to true or this service uses its veto power to sample.
					isSampled = BooleanUtils
						.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
					isVetoed = BooleanUtils
						.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

					if (isSampled || isVetoed) {
						return enhance(request, span);
					}
				}
				return request;
			}

			private Request enhance(Request request, Span span) {

				request.onRequestBegin(theRequest -> {
					uri = theRequest.getURI();

					//query params
					queryParams = Utils.getQueryParams(uri);

					//path
					apiPath = uri.getPath();

					//serviceName to be host+port for outgoing calls
					serviceName =
						uri.getPort() != -1
							? String.join(":", uri.getHost(), String.valueOf(uri.getPort()))
							: uri.getHost();
				});

				request.onRequestHeaders(theRequest -> {
					if (span.context() != null) {
						JaegerSpanContext spanContext = (JaegerSpanContext) span.context();
						mdTraceInfo = new MDTraceInfo(spanContext.getTraceId(),
							spanContext.toSpanId(), String.valueOf(spanContext.getParentId()));
					}

					traceMetaMap = CommonUtils.buildTraceInfoMap(serviceName, mdTraceInfo,
						requestHeaders.getFirst(Constants.X_REQUEST_ID));

					//headers
					requestHeaders = Utils.getHeaders(theRequest.getHeaders());

					//meta
					requestMeta = Utils
						.getRequestMeta(theRequest.getMethod(),
							traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
							Optional.ofNullable(serviceName));
				});

				request.onRequestContent((theRequest, content) -> {
					//body
					if (content != null && content.hasArray()) {
						requestBody = content.array();
					}
				});

				request.onComplete(theRequest -> {
					Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, requestMeta,
						mdTraceInfo, requestBody);
				});

				request.onResponseBegin(theResponse -> {

				});

				request.onResponseHeaders(theResponse -> {
					//headers
					responseHeaders = Utils.getHeaders(theResponse.getHeaders());

					//meta
					responseMeta = Utils
						.getResponseMeta(apiPath, String.valueOf(theResponse.getStatus()),
							Optional.ofNullable(serviceName));
					responseMeta.putAll(traceMetaMap);
				});

				request.onResponseContent((theResponse, content) -> {
					//body
					if (content != null && content.hasArray()) {
						responseBody = content.array();
					}
				});

				request.onComplete(theResponse -> {
					Utils.createAndLogRespEvent(apiPath, responseHeaders, responseMeta, mdTraceInfo,
						responseBody);
				});
				return request;
			}

		};

		return httpClient;
	}

}
