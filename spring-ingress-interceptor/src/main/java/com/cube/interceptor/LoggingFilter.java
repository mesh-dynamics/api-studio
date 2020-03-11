package com.cube.interceptor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Span;

/**
 * Reference : https://stackoverflow.com/a/44497698/2761431
 */

@Component
@Order(3001)
public class LoggingFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LogManager.getLogger(LoggingFilter.class);

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws ServletException, IOException {

		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(
			httpServletRequest);
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(
			httpServletResponse);

		filterChain.doFilter(requestWrapper, responseWrapper);

		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		currentSpan.ifPresent(UtilException.rethrowConsumer(span ->
		{
			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
			boolean isVetoed = BooleanUtils.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

			if (isSampled || isVetoed) {
				//this is local baggage item
				span.setBaggageItem(Constants.MD_IS_VETOED, null);

				//path
				String apiPath = requestWrapper.getContextPath();

				MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
				try {
					queryParams = Utils
						.getQueryParams(new URI(requestWrapper.getRequestURL().toString()));
				} catch (URISyntaxException e) {
					LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "URI formation failed,  query params ignored!")));
				}

				MDTraceInfo mdTraceInfo = CommonUtils.mdTraceInfoFromContext();

				String xRequestId = requestWrapper.getHeader(Constants.X_REQUEST_ID);
				MultivaluedMap<String, String> traceMetaMap = CommonUtils
					.buildTraceInfoMap(config.commonConfig.serviceName, mdTraceInfo, xRequestId);

				logRequest(requestWrapper, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams, mdTraceInfo);
				logResponse(responseWrapper, apiPath, traceMetaMap, mdTraceInfo);
			}
		}));

		//Need to copy back the response body as it is consumed by responseWrapper.
		responseWrapper.copyBodyToResponse();
	}

	private void logRequest(ContentCachingRequestWrapper requestWrapper, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils.getHeaders(requestWrapper);

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestWrapper.getMethod(), cRequestId, Optional.empty());

		//body
		byte[] requestBody = requestWrapper.getContentAsByteArray();

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			mdTraceInfo, requestBody);

	}

	private void logResponse(ContentCachingResponseWrapper responseWrapper, String apiPath,
		MultivaluedMap<String, String> traceMeta, MDTraceInfo mdTraceInfo)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(responseWrapper.getStatus()),
				Optional.empty());
		meta.putAll(traceMeta);

		//hdrs
		MultivaluedMap<String, String> responseHeaders = getHeaders(responseWrapper);

		//body
		byte[] responseBody = responseWrapper.getContentAsByteArray();

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);
	}

	private MultivaluedMap<String, String> getHeaders(
		ContentCachingResponseWrapper responseWrapper) {
		MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();
		responseWrapper.getHeaderNames().stream()
			.forEach(headerName -> {
				for (String headerValue : responseWrapper.getHeaders(headerName)) {
					headerMap.add(headerName, headerValue);
				}
			});
		return headerMap;
	}

}
