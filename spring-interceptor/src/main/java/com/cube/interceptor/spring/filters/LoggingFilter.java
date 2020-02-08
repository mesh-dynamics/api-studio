package com.cube.interceptor.spring.filters;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.md.constants.Constants;

import com.cube.interceptor.utils.Utils;

/**
 * Reference : https://stackoverflow.com/a/44497698/2761431
 */

@Component
public class LoggingFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LogManager.getLogger(LoggingFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws ServletException, IOException {

		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(
			httpServletRequest);
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(
			httpServletResponse);

		filterChain.doFilter(requestWrapper, responseWrapper);

		//hdrs
		MultivaluedMap<String, String> requestHeaders = getHeaders(requestWrapper);
		boolean isSampled = Utils.isSampled(requestHeaders);

		if (isSampled) {

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

			MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(requestWrapper);

			logRequest(requestWrapper, apiPath,
				traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams);
			logResponse(responseWrapper, apiPath, traceMetaMap);
		}

		//Need to copy back the response body as it is consumed by responseWrapper.
		responseWrapper.copyBodyToResponse();

	}

	private void logRequest(ContentCachingRequestWrapper requestWrapper, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams) throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = getHeaders(requestWrapper);

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestWrapper.getMethod(), cRequestId, Optional.empty());

		//body
		String requestBody = IOUtils.toString(requestWrapper.getContentAsByteArray(),
			requestWrapper.getCharacterEncoding());

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			requestBody);

	}

	private void logResponse(ContentCachingResponseWrapper responseWrapper, String apiPath,
		MultivaluedMap<String, String> traceMeta)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(responseWrapper.getStatus()),
				Optional.empty());
		meta.putAll(traceMeta);

		//hdrs
		MultivaluedMap<String, String> responseHeaders = getHeaders(responseWrapper);

		//body
		String responseBody = IOUtils.toString(responseWrapper.getContentInputStream(),
			responseWrapper.getCharacterEncoding());

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, responseBody);
	}

	private MultivaluedMap<String, String> getHeaders(
		ContentCachingRequestWrapper requestWrapper) {
		MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();
		Collections.list(requestWrapper.getHeaderNames()).stream()
			.forEach(headerName -> {
				Enumeration<String> headerValues = requestWrapper.getHeaders(headerName);
				while(headerValues.hasMoreElements()) {
					headerMap.add(headerName, headerValues.nextElement());
				}
			});

		return headerMap;
	}

	private MultivaluedMap<String, String> getHeaders(
		ContentCachingResponseWrapper responseWrapper) {
		MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();
		responseWrapper.getHeaderNames().stream()
			.forEach(headerName -> {
				for(String headerValue : responseWrapper.getHeaders(headerName)) {
					headerMap.add(headerName, headerValue);
				}
			});
		return headerMap;
	}

	@NotNull
	public static MultivaluedMap<String, String> getTraceInfoMetaMap(
		ContentCachingRequestWrapper requestWrapper) {
		String traceId = requestWrapper.getHeader(Constants.DEFAULT_TRACE_FIELD);
		String spanId = requestWrapper.getHeader(Constants.DEFAULT_SPAN_FIELD);
		String parentSpanId = requestWrapper.getHeader(Constants.DEFAULT_PARENT_SPAN_FIELD);
		String xRequestId = requestWrapper.getHeader(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(traceId, spanId, parentSpanId, xRequestId);
	}

}
