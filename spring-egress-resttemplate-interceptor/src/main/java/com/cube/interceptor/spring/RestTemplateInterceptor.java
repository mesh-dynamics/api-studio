package com.cube.interceptor.spring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import io.md.constants.Constants;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.utils.Utils;

/**
 * Reference : https://stackoverflow.com/a/52698745/2761431
 */

@Component
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
		ClientHttpRequestExecution execution) throws IOException {

		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils
			.getMultiMap(request.getHeaders().entrySet());
		boolean isSampled = Utils.isSampled(requestHeaders);

		String apiPath = Strings.EMPTY, serviceName = Strings.EMPTY;
		MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
		if (isSampled) {
			URI uri = request.getURI();

			//query params
			MultivaluedMap<String, String> queryParams = Utils.getQueryParams(uri);

			//path
			apiPath = uri.getPath();

			//serviceName to be host+port for outgoing calls
			serviceName =
				uri.getPort() != -1
					? String.join(":", uri.getHost(), String.valueOf(uri.getPort()))
					: uri.getHost();

			traceMetaMap = Utils.getTraceInfoMetaMap(request);

			logRequest(request, requestHeaders, body, apiPath,
				traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
				serviceName, queryParams);
		}
		ClientHttpResponse response = execution.execute(request, body);
		if (isSampled) {
			response = new BufferedClientHttpResponse(response);
			logResponse(response, apiPath, traceMetaMap, serviceName);
		}
		return response;
	}

	private void logRequest(HttpRequest request, MultivaluedMap<String, String> requestHeaders,
		byte[] body, String apiPath, String cRequestId,
		String serviceName, MultivaluedMap<String, String> queryParams)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(request.getMethodValue(), cRequestId,
				Optional.ofNullable(serviceName));

		//body
		String requestBody = null;
		if (body.length > 0) {
			requestBody = new String(body, StandardCharsets.UTF_8);
		}

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			requestBody);

	}

	private void logResponse(ClientHttpResponse response, String apiPath,
		MultivaluedMap<String, String> traceMeta, String serviceName) throws IOException {
		//hdrs
		MultivaluedMap<String, String> responseHeaders = Utils
			.getMultiMap(response.getHeaders().entrySet());

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(response.getStatusCode()),
				Optional.ofNullable(serviceName));
		meta.putAll(traceMeta);

		//body
		String responseBody = StreamUtils
			.copyToString(response.getBody(), StandardCharsets.UTF_8);

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, responseBody);

	}

	/**
	 * Wrapper around ClientHttpResponse, buffers the body so it can be read repeatedly (for interceptor
	 * & consuming the result).
	 */
	private static class BufferedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse response;
		private byte[] body;

		public BufferedClientHttpResponse(ClientHttpResponse response) {
			this.response = response;
		}

		@Override
		public HttpStatus getStatusCode() throws IOException {
			return response.getStatusCode();
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			response.close();
		}

		@Override
		public InputStream getBody() throws IOException {
			if (body == null) {
				body = StreamUtils.copyToByteArray(response.getBody());
			}
			return new ByteArrayInputStream(body);
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}
	}
}
