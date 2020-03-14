package com.cube.interceptor.spring.egress;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Span;

/**
 * Reference : https://stackoverflow.com/a/52698745/2761431
 */

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute after Client Filter.
 **/
@Component
@Order(3000)
public class RestTemplateDataInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger LOGGER = LogManager.getLogger(RestTemplateDataInterceptor.class);

	private static final RestTemplateConfig config;

	static {
		config = new RestTemplateConfig();
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
		ClientHttpRequestExecution execution) throws IOException {
		String apiPath = Strings.EMPTY, serviceName = Strings.EMPTY;
		MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
		boolean isSampled = false, isVetoed = false;
		MDTraceInfo mdTraceInfo = null;
		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		if (currentSpan.isPresent()) {
			Span span = currentSpan.get();
			//Either baggage has sampling set to true or this service uses its veto power to sample.
			isSampled = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
			isVetoed = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

			if (isSampled || isVetoed) {
				//hdrs
				MultivaluedMap<String, String> requestHeaders = Utils
					.getMultiMap(request.getHeaders().entrySet());

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

				mdTraceInfo = CommonUtils.mdTraceInfoFromContext();

				traceMetaMap = CommonUtils
					.buildTraceInfoMap(serviceName, mdTraceInfo,
						requestHeaders.getFirst(Constants.X_REQUEST_ID));

				logRequest(request, requestHeaders, body, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
					serviceName, queryParams, mdTraceInfo);
			} else {
				LOGGER
					.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Sampling is false!")));
			}
		} else {
			LOGGER
				.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Current Span is empty!")));
		}
		ClientHttpResponse response = execution.execute(request, body);
		if (isSampled || isVetoed) {
			response = new BufferedClientHttpResponse(response);
			logResponse(response, apiPath, traceMetaMap, serviceName, mdTraceInfo);
		}
		return response;
	}

	private void logRequest(HttpRequest request, MultivaluedMap<String, String> requestHeaders,
		byte[] body, String apiPath, String cRequestId, String serviceName,
		MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(request.getMethodValue(), cRequestId,
				Optional.ofNullable(serviceName));

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			mdTraceInfo, body);

	}

	private void logResponse(ClientHttpResponse response, String apiPath,
		MultivaluedMap<String, String> traceMeta, String serviceName, MDTraceInfo mdTraceInfo) throws IOException {
		//hdrs
		MultivaluedMap<String, String> responseHeaders = Utils
			.getMultiMap(response.getHeaders().entrySet());

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(response.getStatusCode().value()),
				Optional.ofNullable(serviceName));
		meta.putAll(traceMeta);

		//body
		byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);
	}

	/**
	 * Wrapper around ClientHttpResponse, buffers the body so it can be read repeatedly (for
	 * interceptor & consuming the result).
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
