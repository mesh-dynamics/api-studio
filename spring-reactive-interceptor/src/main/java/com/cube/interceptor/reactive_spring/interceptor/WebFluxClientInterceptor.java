package com.cube.interceptor.reactive_spring.interceptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.md.constants.Constants;

import com.cube.interceptor.utils.Utils;

/**
 * Reference : https://www.baeldung.com/spring-log-webclient-calls
 */

@Component
public class WebFluxClientInterceptor {

	@Bean
	public static HttpClient defaultHttpClient() {

		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
		HttpClient httpClient = new HttpClient(sslContextFactory) {

			MultivaluedMap<String, String> requestHeaders = Utils.createEmptyMultivaluedMap();
			MultivaluedMap<String, String> responseHeaders = Utils.createEmptyMultivaluedMap();
			MultivaluedMap<String, String> queryParams = Utils.createEmptyMultivaluedMap();
			MultivaluedMap<String, String> traceMetaMap = Utils.createEmptyMultivaluedMap();
			String apiPath = "";
			String requestBody = "";
			String responseBody = "";
			URI uri = null;
			String serviceName = "";
			MultivaluedMap<String, String> requestMeta = Utils.createEmptyMultivaluedMap();
			MultivaluedMap<String, String> responseMeta = Utils.createEmptyMultivaluedMap();
			boolean isSampled = true;


			@Override
			public Request newRequest(URI uri) {
				Request request = super.newRequest(uri);
				requestHeaders = getHeaders(request.getHeaders());
				isSampled = Utils.isSampled(requestHeaders);
				if (isSampled) {
					return enhance(request);
				}
				return request;
			}

			private Request enhance(Request request) {

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
					traceMetaMap = getTraceInfoMetaMap(theRequest.getHeaders());

					//headers
					requestHeaders = getHeaders(theRequest.getHeaders());

					//meta
					requestMeta = Utils
						.getRequestMeta(theRequest.getMethod(),
							traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
							Optional.ofNullable(serviceName));
				});

				request.onRequestContent((theRequest, content) -> {
					//body
					requestBody = StandardCharsets.UTF_8.decode(content).toString();
				});

				request.onComplete(theRequest -> {
					Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, requestMeta,
						requestBody);
				});

				request.onResponseBegin(theResponse -> {

				});

				request.onResponseHeaders(theResponse -> {
					//headers
					responseHeaders = getHeaders(theResponse.getHeaders());

					//meta
					responseMeta = Utils
						.getResponseMeta(apiPath, String.valueOf(theResponse.getStatus()),
							Optional.ofNullable(serviceName));
					responseMeta.putAll(traceMetaMap);
				});

				request.onResponseContent((theResponse, content) -> {
					//body
					responseBody = StandardCharsets.UTF_8.decode(content).toString();
				});

				request.onComplete(theResponse -> {
					Utils.createAndLogRespEvent(apiPath, responseHeaders, responseMeta,
						responseBody);
				});
				return request;
			}

			private MultivaluedMap<String, String> getHeaders(HttpFields headers) {
				MultivaluedMap<String, String> headerMap = Utils.createEmptyMultivaluedMap();
				headers.stream().forEach(header -> {
					String key = header.getName();
					for (String value : header.getValues()) {
						headerMap.add(key, value);
					}

				});
				return headerMap;
			}

			private MultivaluedMap<String, String> getTraceInfoMetaMap(HttpFields headers) {
				String traceId = headers.get(Constants.DEFAULT_TRACE_FIELD);
				String spanId = headers.get(Constants.DEFAULT_SPAN_FIELD);
				String parentSpanId = headers.get(Constants.DEFAULT_PARENT_SPAN_FIELD);
				String xRequestId = headers.get(Constants.X_REQUEST_ID);
				return Utils.buildTraceInfoMap(traceId, spanId, parentSpanId, xRequestId);
			}

		};

		return httpClient;
	}

}
