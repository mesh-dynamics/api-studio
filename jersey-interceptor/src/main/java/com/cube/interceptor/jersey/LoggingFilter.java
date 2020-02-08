package com.cube.interceptor.jersey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;

import com.cube.interceptor.utils.Utils;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter,
	WriterInterceptor {

	@Override
	public void filter(ContainerRequestContext reqContext) throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();
		boolean isSampled = Utils.isSampled(requestHeaders);
		if (isSampled) {
			URI uri = reqContext.getUriInfo().getRequestUri();

			//query params
			MultivaluedMap<String, String> queryParams = reqContext.getUriInfo()
				.getQueryParameters();

			//path
			String apiPath = uri.getPath();

			MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(reqContext);

			reqContext.setProperty(Constants.MD_SAMPLE_REQUEST, isSampled);
			reqContext.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
			reqContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);

			logRequest(reqContext, apiPath, traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
				queryParams);
		}
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
		String cRequestId, MultivaluedMap<String, String> queryParams) throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(reqContext.getMethod(), cRequestId, Optional.empty());

		//body
		String requestBody = getRequestBody(reqContext);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			requestBody);
	}

	private void logResponse(WriterInterceptorContext context)
		throws IOException {
		Object apiPathObj = context.getProperty(Constants.MD_API_PATH_PROP);
		Object traceMetaMapObj = context.getProperty(Constants.MD_TRACE_META_MAP_PROP);
		Object respHeadersObj = context.getProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		Object statusObj = context.getProperty(Constants.MD_STATUS_PROP);

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

		//body
		String responseBody = getResponseBody(context);

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, responseBody);

		removeSetContextProperty(context);
	}

	private void removeSetContextProperty(WriterInterceptorContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		context.removeProperty(Constants.MD_STATUS_PROP);
		context.removeProperty(Constants.MD_SAMPLE_REQUEST);
	}

	private MultivaluedMap<String, String> getTraceInfoMetaMap(ContainerRequestContext reqContext) {
		//Assuming single value for the trace id keys.
		String traceId = reqContext.getHeaders().getFirst(Constants.DEFAULT_TRACE_FIELD);
		String spanId = reqContext.getHeaders().getFirst(Constants.DEFAULT_SPAN_FIELD);
		String parentSpanId = reqContext.getHeaders().getFirst(Constants.DEFAULT_PARENT_SPAN_FIELD);
		String xRequestId = reqContext.getHeaders().getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(traceId, spanId, parentSpanId, xRequestId);
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