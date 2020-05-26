package io.cube.jaxrs.ingress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Scope;
import io.opentracing.Span;

@Provider
@Priority(3000)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter,
	WriterInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public void filter(ContainerRequestContext reqContext) throws IOException {
		try {
			//hdrs
			Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
			currentSpan.ifPresent(UtilException.rethrowConsumer(span ->
			{
				//Either baggage has sampling set to true or this service uses its veto power to sample.
				boolean isSampled = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
				boolean isVetoed = BooleanUtils
					.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

				if (isSampled || isVetoed) {
					//hdrs
					MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();

					URI uri = reqContext.getUriInfo().getRequestUri();

					//query params
					MultivaluedMap<String, String> queryParams = reqContext.getUriInfo()
						.getQueryParameters();

					//path
					String apiPath = uri.getPath();

					MDTraceInfo mdTraceInfo = CommonUtils.mdTraceInfoFromContext();

					String xRequestId = requestHeaders.getFirst(Constants.X_REQUEST_ID);
					MultivaluedMap<String, String> traceMetaMap = CommonUtils
						.buildTraceInfoMap(CommonConfig.getInstance().serviceName, mdTraceInfo,
							xRequestId);

					reqContext.setProperty(Constants.MD_SAMPLE_REQUEST, true);
					reqContext.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
					reqContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);
					reqContext.setProperty(Constants.MD_TRACE_INFO, mdTraceInfo);

					logRequest(reqContext, apiPath,
						traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
						requestHeaders, queryParams, mdTraceInfo);
				}
			}));
		} catch (Exception ex) {
			LOGGER.error(String.valueOf(Map.of(Constants.MESSAGE,
				"Exception occured during logging, proceeding to the application!")), ex);
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
		String cRequestId, MultivaluedMap<String, String> requestHeaders,
		MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		final Span span = io.cube.agent.Utils.createPerformanceSpan("reqProcess");
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			//meta
			MultivaluedMap<String, String> meta = Utils
				.getRequestMeta(reqContext.getMethod(), cRequestId, Optional.empty());

			//body
			byte[] requestBody = getRequestBody(reqContext);

			Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
				requestBody);
		} finally {
			span.finish();
		}
	}

	private void logResponse(WriterInterceptorContext context)
		throws IOException {
		Object parentSpanObj = context.getProperty(TracingFilter.spanKey);
		Span span = null;
		if (parentSpanObj != null) {
			span = io.cube.agent.Utils.createPerformanceSpan("respProcess",
				((Span) parentSpanObj).context());
		} else {
			span = io.cube.agent.Utils.createPerformanceSpan("respProcess");
		}

		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			String apiPath;
			MultivaluedMap<String, String> meta;
			MultivaluedMap<String, String> responseHeaders;
			MDTraceInfo mdTraceInfo;
			try {
				Object apiPathObj = context.getProperty(Constants.MD_API_PATH_PROP);
				Object traceMetaMapObj = context.getProperty(Constants.MD_TRACE_META_MAP_PROP);
				Object respHeadersObj = context.getProperty(Constants.MD_RESPONSE_HEADERS_PROP);
				Object statusObj = context.getProperty(Constants.MD_STATUS_PROP);
				Object traceInfo = context.getProperty(Constants.MD_TRACE_INFO);

				ObjectMapper mapper = new ObjectMapper();
				//hdrs
				responseHeaders = respHeadersObj != null ? mapper
					.convertValue(respHeadersObj, MultivaluedMap.class)
					: new MultivaluedHashMap<>();

				MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
					.convertValue(traceMetaMapObj, MultivaluedMap.class)
					: new MultivaluedHashMap<>();
				apiPath = apiPathObj != null ? apiPathObj.toString() : "";
				//meta
				meta = Utils
					.getResponseMeta(apiPath,
						String.valueOf(statusObj != null ? statusObj.toString() : ""),
						Optional.empty());
				meta.putAll(traceMetaMap);

				mdTraceInfo =
					traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();
			} catch (Exception ex) {
				LOGGER.error(String.valueOf(Map.of(Constants.MESSAGE,
					"Exception occured during logging response, proceeding to the application!")),
					ex);
				context.proceed();
				return;
			}

			//body
			byte[] responseBody = getResponseBody(context);

			Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);

			removeSetContextProperty(context);
		} finally {
			span.finish();
		}
	}

	private void removeSetContextProperty(WriterInterceptorContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		context.removeProperty(Constants.MD_STATUS_PROP);
		context.removeProperty(Constants.MD_SAMPLE_REQUEST);
		context.removeProperty(Constants.MD_TRACE_INFO);
	}

	private byte[] getRequestBody(ContainerRequestContext reqContext) throws IOException {
		final Span span = io.cube.agent.Utils.createPerformanceSpan("reqBody");
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			byte[] reqBytes = reqContext.getEntityStream().readAllBytes();
			new ByteArrayInputStream(reqBytes);
			InputStream in = new ByteArrayInputStream(reqBytes);
			reqContext.setEntityStream(in);

			return reqBytes;
		} finally {
			span.finish();
		}
	}

	private byte[] getResponseBody(WriterInterceptorContext context) throws IOException {
		final Span span = io.cube.agent.Utils.createPerformanceSpan("respBody");
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			OutputStream originalStream = context.getOutputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			context.setOutputStream(baos);
			try {
				context.proceed();
			} finally {
				baos.writeTo(originalStream);
				baos.close();
				context.setOutputStream(originalStream);
			}
			return baos.toByteArray();
		} finally {
			span.finish();
		}
	}
}