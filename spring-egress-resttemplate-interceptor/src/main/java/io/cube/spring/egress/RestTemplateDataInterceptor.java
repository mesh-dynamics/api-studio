package io.cube.spring.egress;

import static io.md.utils.Utils.getTraceInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

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

	private static final Logger LOGGER = LoggerFactory
		.getLogger(RestTemplateDataInterceptor.class);

	private static final MDRestTemplateConfig config;

	static {
		config = new MDRestTemplateConfig();
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
		ClientHttpRequestExecution execution) throws IOException {
		String apiPath = "", serviceName = "";
		MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
		boolean isSampled = false, isVetoed = false;
		MDTraceInfo mdTraceInfo = null;
		Optional<Span> ingressSpan = Optional.empty();
		Span clientSpan = null;
		Scope clientScope = null;

		try {

			// Do not log request in case the egress service is to be mocked
			String service = CommonUtils.getEgressServiceName(request.getURI());
			CommonConfig commonConfig = CommonConfig.getInstance();
			if (commonConfig.shouldMockService(service)) {
				LOGGER.info("Mocking in progress, not logging the request!");
				return execution.execute(request, body);
			}

			ingressSpan = CommonUtils.getCurrentSpan();

			//Empty ingress span pertains to DB initialization scenarios.
			SpanContext spanContext = ingressSpan.map(Span::context)
					.orElse(CommonUtils.createDefSpanContext());

			clientSpan = CommonUtils
					.startClientSpan(Constants.MD_CHILD_SPAN, spanContext, false);

			clientScope = CommonUtils.activateSpan(clientSpan);

			//Either baggage has sampling set to true or this service uses its veto power to sample.
			isSampled = BooleanUtils
				.toBoolean(clientSpan.getBaggageItem(Constants.MD_IS_SAMPLED));
			isVetoed = BooleanUtils
				.toBoolean(clientSpan.getBaggageItem(Constants.MD_IS_VETOED));

			//Empty ingress span pertains to DB initialization scenarios.
			//So need to record all calls as these will not be driven by replay driver.
			if (isSampled || isVetoed || !ingressSpan.isPresent()) {
				//this is local baggage item
				clientSpan.setBaggageItem(Constants.MD_IS_VETOED, null);
				//hdrs
				MultivaluedMap<String, String> requestHeaders = Utils
					.getMultiMap(request.getHeaders().entrySet());

				URI uri = request.getURI();

				//query params
				MultivaluedMap<String, String> queryParams = Utils.getQueryParams(uri);

				//path
				apiPath = uri.getPath();

				//serviceName to be host+port for outgoing calls
				serviceName = CommonUtils.getEgressServiceName(uri);

				mdTraceInfo = getTraceInfo(clientSpan);

				traceMetaMap = CommonUtils
					.buildTraceInfoMap(serviceName, mdTraceInfo,
						requestHeaders.getFirst(Constants.X_REQUEST_ID));

				logRequest(request, requestHeaders, body, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
					serviceName, queryParams, mdTraceInfo);

				//Setting the current span id as parent span id value
				//This is intentionally set after the capture as it should be parent for subsequent capture
				clientSpan
					.setBaggageItem(Constants.MD_PARENT_SPAN, clientSpan.context().toSpanId());
			} else {
				LOGGER.debug("Sampling is false!");
			}
		} catch (Exception ex) {
			LOGGER.error("Exception occured during logging request!", ex);
		}

		ClientHttpResponse response = null;
		try {
			 response = execution.execute(request, body);
		} catch (Exception ex) {
			if (clientSpan != null) {
				clientSpan.finish();
			}

			if (clientScope != null) {
				clientScope.close();
			}
			throw ex;
		}

		try {
			if (isSampled || isVetoed || !ingressSpan.isPresent()) {
				response = new BufferedClientHttpResponse(response);
				logResponse(response, apiPath, traceMetaMap, serviceName, mdTraceInfo);
			}
		} catch (Exception ex) {
			LOGGER.error("Exception occured during logging response!", ex);
		}

		if (clientSpan != null) {
			clientSpan.finish();
		}

		if (clientScope != null) {
			clientScope.close();
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
		MultivaluedMap<String, String> traceMeta, String serviceName, MDTraceInfo mdTraceInfo)
		throws IOException {
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
