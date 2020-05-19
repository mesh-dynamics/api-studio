package io.cube.spring.ingress;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
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
		CachedBodyHttpServletRequest requestWrapper = null;
		ContentCachingResponseWrapper responseWrapper = null;
		boolean isSampled = false;
		boolean isVetoed = false;
		String apiPath = null;
		MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
		MDTraceInfo mdTraceInfo = new MDTraceInfo();
		Optional<Span> currentSpan = Optional.empty();
		try {
			currentSpan = CommonUtils.getCurrentSpan();
			if (currentSpan.isPresent()) {
				Span span = currentSpan.get();
				//Either baggage has sampling set to true or this service uses its veto power to sample.
				isSampled = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
				isVetoed = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

				if (isSampled || isVetoed) {
					requestWrapper = new CachedBodyHttpServletRequest(httpServletRequest);
					responseWrapper = new ContentCachingResponseWrapper(httpServletResponse);

					//path
					apiPath = requestWrapper.getRequestURI();

					//query params
					MultivaluedMap<String, String> queryParams = getQueryParameters(
						requestWrapper.getQueryString());

					MultivaluedMap<String, String> formParams = getMultiMap(
						requestWrapper.getParameterMap());

					mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);

					String xRequestId = requestWrapper.getHeader(Constants.X_REQUEST_ID);
					traceMetaMap = CommonUtils
						.buildTraceInfoMap(CommonConfig.getInstance().serviceName, mdTraceInfo,
							xRequestId);

					logRequest(requestWrapper, apiPath,
						traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams,
						formParams,
						mdTraceInfo);

					//Setting the ingress md span as parent span and re-injecting them into the headers.
					// This is done after the capture as this is the parent only for the subsequent capture
					span.setBaggageItem(Constants.MD_PARENT_SPAN, span.context().toSpanId());
				} else {
					LOGGER
						.debug("Sampling is false!");
				}
			}
		} catch (Exception ex) {
			LOGGER.error("Exception occured while capturing "
				+ "request/response in logging filter!", ex);
		}

		if (requestWrapper != null && responseWrapper != null) {
			filterChain.doFilter(requestWrapper, responseWrapper);
			logResponse(responseWrapper, apiPath, traceMetaMap, mdTraceInfo);
		} else {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}

		//Need to copy back the response body as it is consumed by responseWrapper.
		//first call to copyBodyToResponse() makes the content empty once it copies it
		// to the original response, so multiple calls do not have an impact.
		if (responseWrapper != null) {
			responseWrapper.copyBodyToResponse();
		}

	}


	private void logRequest(CachedBodyHttpServletRequest requestWrapper, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils.getHeaders(requestWrapper);

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestWrapper.getMethod(), cRequestId, Optional.empty());

		//body
		byte[] requestBody = IOUtils.toByteArray(requestWrapper.getInputStream());

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

	private static MultivaluedMap<String, String> getMultiMap(Map<String, String[]> parameterMap) {
		MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
		parameterMap.forEach((key, values) -> {
			for (String value : values) {
				multivaluedMap.add(key, value);
			}
		});
		return multivaluedMap;
	}

	private static MultivaluedMap<String, String> getQueryParameters(String queryString) {
		MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();

		if (StringUtils.isEmpty(queryString)) {
			return queryParameters;
		}

		String[] parameters = queryString.split("&");

		for (String parameter : parameters) {
			String[] keyValuePair = parameter.split("=");
			if (keyValuePair.length == 1) {
				queryParameters.add(keyValuePair[0], "");
			} else {
				queryParameters.add(keyValuePair[0], keyValuePair[1]);
			}
		}
		return queryParameters;
	}

}
