package io.cube.spring.ingress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Span;

/**
 * Reference : https://stackoverflow.com/a/44497698/2761431
 */

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute before Logging Filter.
 **/
@Component
@Order(3001)
public class DataFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataFilter.class);

	private static final Config config = new Config();

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws ServletException, IOException {
		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(
			httpServletRequest);
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(
			httpServletResponse);

		filterChain.doFilter(requestWrapper, responseWrapper);

		try {

			Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
			currentSpan.ifPresent(UtilException.rethrowConsumer(span ->
			{
				//Either baggage has sampling set to true or this service uses its veto power to sample.
				boolean isSampled = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
				boolean isVetoed = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

				if (isSampled || isVetoed) {
					//path
					String apiPath = requestWrapper.getRequestURI();

					//query params
					MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
					try {
						queryParams = Utils
							.getQueryParams(new URI(requestWrapper.getRequestURL().toString()));
					} catch (URISyntaxException e) {
						LOGGER.error("URI formation failed,  query params ignored!", e);
					}

					MultivaluedMap<String, String> formParams = getMultiMap(requestWrapper);

					MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);

					String xRequestId = requestWrapper.getHeader(Constants.X_REQUEST_ID);
					MultivaluedMap<String, String> traceMetaMap = CommonUtils
						.buildTraceInfoMap(CommonConfig.getInstance().serviceName, mdTraceInfo,
							xRequestId);

					logRequest(requestWrapper, apiPath,
						traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams, formParams,
						mdTraceInfo);
					logResponse(responseWrapper, apiPath, traceMetaMap, mdTraceInfo);

					//Setting the ingress md span as parent span and re-injecting them into the headers.
					// This is done after the capture as this is the parent only for the subsequent capture
					span.setBaggageItem(Constants.MD_PARENT_SPAN, span.context().toSpanId());
				} else {
					LOGGER
						.debug("Sampling is false!");
				}
			}));
		} catch (Exception ex) {
			LOGGER.error("Exception occured while capturing "
				+ "request/response in logging filter!", ex);
		}

		//Need to copy back the response body as it is consumed by responseWrapper.
		//first call to copyBodyToResponse() makes the content empty once it copies it
		// to the original response, so multiple calls do not have an impact.
		responseWrapper.copyBodyToResponse();

	}


	private void logRequest(ContentCachingRequestWrapper requestWrapper, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MultivaluedMap<String, String> formParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils.getHeaders(requestWrapper);

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestWrapper.getMethod(), cRequestId, Optional.empty());

		//body
		byte[] requestBody = requestWrapper.getContentAsByteArray();

		Utils.createAndLogReqEvent(apiPath, queryParams, formParams, requestHeaders, meta,
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

		//body
		byte[] responseBody = responseWrapper.getContentAsByteArray();

		//Need to do this before reading the headers. This call cannot be done
		//before reading the response body.
		responseWrapper.copyBodyToResponse();

		//hdrs
		MultivaluedMap<String, String> responseHeaders = getHeaders(responseWrapper);


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

	private static MultivaluedMap<String, String> getMultiMap(ContentCachingRequestWrapper requestWrapper) {
		MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
		requestWrapper.getParameterMap().entrySet().forEach(key -> {
			String k = key.getKey();
			String[] values = key.getValue();
			for (int i=0; i<values.length; i++) {
				multivaluedMap.add(k, values[i]);
			}
		});
		return multivaluedMap;
	}

}
