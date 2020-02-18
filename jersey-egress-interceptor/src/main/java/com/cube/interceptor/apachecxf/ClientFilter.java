package com.cube.interceptor.apachecxf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.util.Strings;
import org.glassfish.jersey.message.MessageUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Span;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.utils.Utils;

public class ClientFilter implements ClientRequestFilter, ClientResponseFilter, WriterInterceptor {

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		//hdrs
		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		currentSpan.ifPresent(UtilException.rethrowConsumer(span ->
		{
			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED))
				|| config.commonConfig.samplerVeto;
			if (isSampled) {
				URI uri = requestContext.getUri();

				//query params
				MultivaluedMap<String, String> queryParams = Utils.getQueryParams(uri);

				//path
				String apiPath = uri.getPath();

				//serviceName to be host+port for outgoing calls
				String serviceName =
					uri.getPort() != -1
						? String.join(":", uri.getHost(), String.valueOf(uri.getPort()))
						: uri.getHost();

				MDTraceInfo mdTraceInfo = getTraceInfo(span);

				MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(requestContext,
					mdTraceInfo);

				requestContext.setProperty(Constants.MD_SERVICE_PROP, serviceName);
				requestContext.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
				requestContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);
				requestContext.setProperty(Constants.MD_SAMPLE_REQUEST, isSampled);
				requestContext.setProperty(Constants.MD_TRACE_INFO, mdTraceInfo);

				final OutputStream stream = new ClientLoggingStream(
					requestContext.getEntityStream());
				requestContext.setEntityStream(stream);
				requestContext.setProperty(Constants.MD_LOG_STREAM_PROP, stream);

				logRequest(requestContext, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
					queryParams, mdTraceInfo, serviceName);
			}
		}));
	}

	@Override
	public void filter(ClientRequestContext requestContext,
		ClientResponseContext respContext) throws IOException {
		if (requestContext.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
			requestContext
				.setProperty(Constants.MD_RESPONSE_HEADERS_PROP,
					respContext.getHeaders());
			requestContext
				.setProperty(Constants.MD_STATUS_PROP, respContext.getStatus());
			requestContext.setProperty(Constants.MD_BODY_PROP, getResponseBody(respContext));
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

	private void logRequest(ClientRequestContext requestContext, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo,
		String serviceName)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = requestContext.getStringHeaders();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestContext.getMethod(), cRequestId,
				Optional.ofNullable(serviceName));

		//body
		String requestBody = getRequestBody(requestContext);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private String getRequestBody(ClientRequestContext requestContext) {
		if (requestContext.hasEntity()) {
			final ClientLoggingStream stream = (ClientLoggingStream) requestContext
				.getProperty(Constants.MD_LOG_STREAM_PROP);
			return stream.getString(MessageUtils.getCharset(requestContext.getMediaType()));
		}
		return Strings.EMPTY;
	}

	private void logResponse(WriterInterceptorContext context)
		throws IOException {
		Object apiPathObj = context.getProperty(Constants.MD_API_PATH_PROP);
		Object traceMetaMapObj = context.getProperty(Constants.MD_TRACE_META_MAP_PROP);
		Object respHeadersObj = context.getProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		Object statusObj = context.getProperty(Constants.MD_STATUS_PROP);
		Object serviceNameObj = context.getProperty(Constants.MD_SERVICE_PROP);
		Object respBodyObj = context.getProperty(Constants.MD_BODY_PROP);
		Object traceInfo = context.getProperty(Constants.MD_TRACE_INFO);

		String apiPath = apiPathObj != null ? apiPathObj.toString() : Strings.EMPTY;
		String serviceName = serviceNameObj != null ? serviceNameObj.toString() : Strings.EMPTY;

		ObjectMapper mapper = new ObjectMapper();
		MultivaluedMap<String, String> responseHeaders = respHeadersObj != null ? mapper
			.convertValue(respHeadersObj, MultivaluedMap.class) : Utils.createEmptyMultivaluedMap();
		MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
			.convertValue(traceMetaMapObj, MultivaluedMap.class)
			: Utils.createEmptyMultivaluedMap();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(statusObj != null ? (Integer) statusObj : 500),
				Optional.ofNullable(serviceName));
		meta.putAll(traceMetaMap);

		MDTraceInfo mdTraceInfo = traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();

		//body
		String responseBody = respBodyObj != null ? respBodyObj.toString() : Strings.EMPTY;

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);

		removeSetContextProperty(context);
	}

	private void removeSetContextProperty(WriterInterceptorContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		context.removeProperty(Constants.MD_STATUS_PROP);
		context.removeProperty(Constants.MD_SERVICE_PROP);
		context.removeProperty(Constants.MD_BODY_PROP);
		context.removeProperty(Constants.MD_LOG_STREAM_PROP);
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

	private MultivaluedMap<String, String> getTraceInfoMetaMap(ClientRequestContext reqContext,
		MDTraceInfo mdTraceInfo) {
		String xRequestId = reqContext.getStringHeaders().getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(mdTraceInfo, xRequestId);
	}

	private String getResponseBody(ClientResponseContext respContext) throws IOException {
		String json = IOUtils.toString(respContext.getEntityStream(), StandardCharsets.UTF_8);
		InputStream in = IOUtils.toInputStream(json, StandardCharsets.UTF_8);
		respContext.setEntityStream(in);

		return json;
	}
}
