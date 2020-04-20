package com.cube.interceptor.apachecxf.ingress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.WriterInterceptorContextImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Scope;
import io.opentracing.Span;

import com.cube.interceptor.apachecxf.ingress.config.Config;
import com.cube.interceptor.apachecxf.ingress.utils.Utils;

/**
 * Priority is to specify in which order the filters are to be executed.
 * Lower the order, early the filter is executed.
 * We want Logging filter to execute after Tracing Filter during Ingress
 **/
@Provider
@Priority(3000)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter,
	WriterInterceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

	@Override
	public void filter(ContainerRequestContext reqContext) {
		//hdrs
		try {
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

					MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);

					MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(reqContext,
						mdTraceInfo);

					reqContext.setProperty(Constants.MD_SAMPLE_REQUEST, true);
					reqContext.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
					reqContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);
					reqContext.setProperty(Constants.MD_TRACE_INFO, mdTraceInfo);

					logRequest(reqContext, apiPath, traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
						queryParams, mdTraceInfo);

					// Setting parent span
					span.setBaggageItem(Constants.MD_PARENT_SPAN, span.context().toSpanId());

				}
			}));
		} catch (Exception e) {
			LOGGER.error(String.valueOf(
				Map.of(
					io.md.constants.Constants.MESSAGE, "Error occurred in intercepting request",
					io.md.constants.Constants.REASON, e.getMessage()
				)));
		}
	}

	@Override
	public void filter(ContainerRequestContext containerRequestContext,
		ContainerResponseContext containerResponseContext) {
		try {
			if (containerRequestContext.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
				containerRequestContext
					.setProperty(Constants.MD_RESPONSE_HEADERS_PROP,
						containerResponseContext.getStringHeaders());
				containerRequestContext
					.setProperty(Constants.MD_STATUS_PROP, containerResponseContext.getStatus());
				// aroundWriteTo will not be called for empty body
				if(containerResponseContext.getEntity()==null) {
					logResponse(null, containerRequestContext, new MutableBoolean(false));
				}
			}
		} catch (Exception e) {
			LOGGER.error(String.valueOf(
				Map.of(
					io.md.constants.Constants.MESSAGE, "Error occurred in intercepting response",
					io.md.constants.Constants.REASON, e.getMessage()
				)));
		}
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException {

		MutableBoolean didContextProceed = new MutableBoolean(false);

		try {
			ContainerRequestContext reqContext = null;
			//for Apache cxf
			if (context instanceof WriterInterceptorContextImpl) {
				Message message = JAXRSUtils.getCurrentMessage();
				reqContext = new ContainerRequestContextImpl(message.getExchange().getInMessage(),
					false, true);
			}

			if (reqContext != null
				&& reqContext.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
				logResponse(context, reqContext, didContextProceed);
			}
		} catch (Exception e) {
			LOGGER.error(String.valueOf(
				Map.of(
					io.md.constants.Constants.MESSAGE, "Error occurred in Mocking filter",
					io.md.constants.Constants.REASON, e.getMessage()
				)));
		} finally {
			if(didContextProceed.isFalse()) {
				LOGGER.info(String.valueOf(
					Map.of(
						io.md.constants.Constants.MESSAGE, "Proceeding context in aroundWriteTo finally"
					)));
				context.proceed();
			}
		}
	}

	private void logRequest(ContainerRequestContext reqContext, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		Span span = io.cube.agent.Utils.createPerformanceSpan(Constants.PROCESS_REQUEST_INGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			//hdrs
			MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();

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

	private void logResponse(WriterInterceptorContext context, ContainerRequestContext reqContext, MutableBoolean didContextProceed)
		throws IOException {
		Span span = io.cube.agent.Utils.createPerformanceSpan(Constants.PROCESS_RESPONSE_INGRESS);
		didContextProceed.setFalse();
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			Object apiPathObj = reqContext.getProperty(Constants.MD_API_PATH_PROP);
			Object traceMetaMapObj = reqContext.getProperty(Constants.MD_TRACE_META_MAP_PROP);
			Object respHeadersObj = reqContext.getProperty(Constants.MD_RESPONSE_HEADERS_PROP);
			Object statusObj = reqContext.getProperty(Constants.MD_STATUS_PROP);
			Object traceInfo = reqContext.getProperty(Constants.MD_TRACE_INFO);

			ObjectMapper mapper = new ObjectMapper();
			//hdrs
			MultivaluedMap<String, String> responseHeaders = respHeadersObj != null ? mapper
				.convertValue(respHeadersObj, MetadataMap.class)
				: Utils.createEmptyMultivaluedMap();

			MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
				.convertValue(traceMetaMapObj, MetadataMap.class)
				: Utils.createEmptyMultivaluedMap();
			String apiPath = apiPathObj != null ? apiPathObj.toString() : "";
			//meta
			MultivaluedMap<String, String> meta = Utils
				.getResponseMeta(apiPath,
					String.valueOf(statusObj != null ? statusObj.toString() : ""),
					Optional.empty());
			meta.putAll(traceMetaMap);

			MDTraceInfo mdTraceInfo =
				traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();

			byte[] responseBody = new byte[0];

			//we pass null for empty body
			if (context != null) {
				responseBody = getResponseBody(context, didContextProceed);
			}

			Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);

			removeSetContextProperty(reqContext);
		} finally {
			span.finish();
		}
	}

	private void removeSetContextProperty(ContainerRequestContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_RESPONSE_HEADERS_PROP);
		context.removeProperty(Constants.MD_STATUS_PROP);
		context.removeProperty(Constants.MD_SAMPLE_REQUEST);
		context.removeProperty(Constants.MD_TRACE_INFO);
	}

	private MultivaluedMap<String, String> getTraceInfoMetaMap(ContainerRequestContext reqContext,
		MDTraceInfo mdTraceInfo) {
		String xRequestId = reqContext.getHeaders().getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(mdTraceInfo, xRequestId);
	}

	private byte[] getRequestBody(ContainerRequestContext reqContext) throws IOException {
		final Span span = io.cube.agent.Utils.createPerformanceSpan(
			Constants.COPY_REQUEST_BODY_INGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			byte[] reqBody = IOUtils.toByteArray(reqContext.getEntityStream());
			InputStream in = new ByteArrayInputStream(reqBody);
			reqContext.setEntityStream(in);

			return reqBody;
		} finally {
			span.finish();
		}
	}

	private byte[] getResponseBody(WriterInterceptorContext context, MutableBoolean didContextProceed) throws IOException {
		Span span = io.cube.agent.Utils.createPerformanceSpan(Constants.COPY_RESPONSE_BODY_INGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			OutputStream originalStream = context.getOutputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] responseBody;
			context.setOutputStream(baos);
			try {
				didContextProceed.setTrue();
				context.proceed();
			} catch (Exception e) {
				LOGGER.error(String.valueOf(
					Map.of(
						io.md.constants.Constants.MESSAGE, "Error occurred in context proceed",
						io.md.constants.Constants.REASON, e.getMessage()
					)));
			} finally {
				responseBody = baos.toByteArray();
				baos.writeTo(originalStream);
				baos.close();
				context.setOutputStream(originalStream);
			}

			return responseBody;
		} finally {
			span.finish();
		}
	}
}