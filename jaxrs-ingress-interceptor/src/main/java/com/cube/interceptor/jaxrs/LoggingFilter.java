package com.cube.interceptor.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Span;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.utils.Utils;

@Provider
//@Priority(Priorities.HEADER_DECORATOR + 10)
@Priority(3000)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter,
	WriterInterceptor {

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public void filter(ContainerRequestContext reqContext) throws IOException {
		//hdrs
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

				URI uri = reqContext.getUriInfo().getRequestUri();

				//query params
				MultivaluedMap<String, String> queryParams = reqContext.getUriInfo()
					.getQueryParameters();

				//path
				String apiPath = uri.getPath();

				MDTraceInfo mdTraceInfo = getTraceInfo(span);

				MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(reqContext,
					mdTraceInfo);

				reqContext.setProperty(Constants.MD_SAMPLE_REQUEST, true);
				reqContext.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
				reqContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);
				reqContext.setProperty(Constants.MD_TRACE_INFO, mdTraceInfo);

				logRequest(reqContext, apiPath, traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
					queryParams, mdTraceInfo);
			}
		}));
	}

	@Override
	public void filter(ContainerRequestContext containerRequestContext,
		ContainerResponseContext containerResponseContext) throws IOException {
		if (containerRequestContext.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
			containerRequestContext
				.setProperty(Constants.MD_RESPONSE_HEADERS_PROP,
					containerResponseContext.getStringHeaders());
			containerRequestContext
				.setProperty(Constants.MD_STATUS_PROP, containerResponseContext.getStatus());
		}
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context)
		throws IOException, WebApplicationException {
		if (context.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
			logResponse(context);
		} else {
			context.proceed();
		}
	}

	private void logRequest(ContainerRequestContext reqContext, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(reqContext.getMethod(), cRequestId, Optional.empty());

		//body
		String requestBody = getRequestBody(reqContext);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private void logResponse(WriterInterceptorContext context)
		throws IOException {
		Object apiPathObj = context.getProperty(Constants.MD_API_PATH_PROP);
		Object traceMetaMapObj = context.getProperty(Constants.MD_TRACE_META_MAP_PROP);
		Object respHeadersObj = context.getProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		Object statusObj = context.getProperty(Constants.MD_STATUS_PROP);
		Object traceInfo = context.getProperty(Constants.MD_TRACE_INFO);

		ObjectMapper mapper = new ObjectMapper();
		//hdrs
		MultivaluedMap<String, String> responseHeaders = respHeadersObj != null ? mapper
			.convertValue(respHeadersObj, MultivaluedMap.class) : Utils.createEmptyMultivaluedMap();

		MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
			.convertValue(traceMetaMapObj, MultivaluedMap.class)
			: Utils.createEmptyMultivaluedMap();
		String apiPath = apiPathObj != null ? apiPathObj.toString() : Strings.EMPTY;
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath,
				String.valueOf(statusObj != null ? statusObj.toString() : Strings.EMPTY),
				Optional.empty());
		meta.putAll(traceMetaMap);

		MDTraceInfo mdTraceInfo = traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();

		//body
		String responseBody = getResponseBody(context);

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);

		removeSetContextProperty(context);
	}

	private void removeSetContextProperty(WriterInterceptorContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		context.removeProperty(Constants.MD_STATUS_PROP);
		context.removeProperty(Constants.MD_SAMPLE_REQUEST);
		context.removeProperty(Constants.MD_TRACE_INFO);
	}

	private MDTraceInfo getTraceInfo(Span currentSpan) {
		JaegerSpanContext spanContext = (JaegerSpanContext) currentSpan.context();

		String traceId = spanContext.getTraceId();
		String spanId = String.valueOf(spanContext.getSpanId());
		String parentSpanId = String.valueOf(spanContext.getParentId());
		MDTraceInfo mdTraceInfo = new MDTraceInfo(traceId, spanId, parentSpanId);
		return mdTraceInfo;
	}

	private MultivaluedMap<String, String> getTraceInfoMetaMap(ContainerRequestContext reqContext,
		MDTraceInfo mdTraceInfo) {
		String xRequestId = reqContext.getHeaders().getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(mdTraceInfo, xRequestId);
	}

	private String getRequestBody(ContainerRequestContext reqContext) throws IOException {
		String json = IOUtils.toString(reqContext.getEntityStream(), StandardCharsets.UTF_8);
		InputStream in = IOUtils.toInputStream(json, StandardCharsets.UTF_8);
		reqContext.setEntityStream(in);

		return json;
	}

	private String getResponseBody(WriterInterceptorContext context) throws IOException {
		OutputStream originalStream = context.getOutputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String responseBody;
		context.setOutputStream(baos);
		try {
			context.proceed();
		} finally {
			responseBody = baos.toString("UTF-8");
			baos.writeTo(originalStream);
			baos.close();
			context.setOutputStream(originalStream);
		}

		return responseBody;
	}
}