/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.spring.ingress;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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

	private static final Logger LOGGER = LogMgr.getLogger(DataFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws ServletException, IOException {
		CachedBodyHttpServletRequest requestWrapper = null;

		String apiPath = null;
		MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
		MDTraceInfo mdTraceInfo = new MDTraceInfo();
		Optional<Span> currentSpan = Optional.empty();
		try {
			currentSpan = CommonUtils.getCurrentSpan();
			if (currentSpan.isPresent()) {
				Span span = currentSpan.get();
				//Either baggage has sampling set to true or this service uses its veto power to sample.
				boolean isSampled = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
				boolean isVetoed = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

				if (isSampled || isVetoed) {
					requestWrapper = new CachedBodyHttpServletRequest(httpServletRequest);

					//path
					apiPath = requestWrapper.getRequestURI();

					//query params
					MultivaluedMap<String, String> queryParams = Utils.getQueryParameters(
						requestWrapper.getQueryString());

					mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);

					String xRequestId = requestWrapper.getHeader(Constants.X_REQUEST_ID);
					traceMetaMap = CommonUtils
						.buildTraceInfoMap(CommonConfig.getInstance().serviceName, mdTraceInfo,
							xRequestId);

					logRequest(requestWrapper, apiPath,
						traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams,
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

		if (requestWrapper != null) {
			ResponseWrapper responseWrapper = new ResponseWrapper(httpServletResponse);
			filterChain.doFilter(requestWrapper, responseWrapper);
			logResponse(responseWrapper, httpServletResponse, apiPath, traceMetaMap, mdTraceInfo);
		} else {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}

	}

	private class ResponseWrapper extends  HttpServletResponseWrapper {

		ServletOutputStream originalStream = null;
		PrintWriter writer = null;
		FilterServletOutputStream filterStream = null;

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (writer != null) {
				throw new IllegalStateException("getWriter() has already been called on this response!");
			}

			if (originalStream == null) {
				originalStream = getResponse().getOutputStream();
				filterStream = new FilterServletOutputStream(originalStream);
			}

			return filterStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (originalStream != null) {
				throw new IllegalStateException("getOutputStream() has already been called on this response!");
			}

			if (writer == null) {
				filterStream = new FilterServletOutputStream(getResponse().getOutputStream());
				writer = new PrintWriter(
					new OutputStreamWriter(filterStream, getResponse().getCharacterEncoding()),
					true);
			}

			return writer;
		}

		@Override
		public void flushBuffer() throws IOException {
			if (writer != null) {
				writer.flush();
			} else if (originalStream != null) {
				filterStream.flush();
			}
		}

		public byte[] getResponseBody() {
			if (filterStream != null) {
				return filterStream.getBodyAsByteArray();
			} else {
				return new byte[0];
			}
		}
	}


	private void logRequest(CachedBodyHttpServletRequest requestWrapper, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams,
		MDTraceInfo mdTraceInfo)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils.getHeaders(requestWrapper);

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestWrapper.getMethod(), cRequestId, Optional.empty());

		//body
		byte[] requestBody = IOUtils.toByteArray(requestWrapper.getInputStream());

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			mdTraceInfo, requestBody);

	}

	private void logResponse(ResponseWrapper responseWrapper, HttpServletResponse httpServletResponse, String apiPath,
		MultivaluedMap<String, String> traceMeta, MDTraceInfo mdTraceInfo)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(responseWrapper.getStatus()),
				Optional.empty());
		meta.putAll(traceMeta);

		//body
		byte[] responseBody = responseWrapper.getResponseBody();
		int size = responseBody.length;
		httpServletResponse.flushBuffer();

		//hdrs
		MultivaluedMap<String, String> responseHeaders = getHeaders(responseWrapper);

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);
	}

	private MultivaluedMap<String, String> getHeaders(
		ResponseWrapper responseWrapper) {
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
