package com.cube.interceptor.apachecxf.egress;

import static io.md.utils.Utils.getTraceInfo;

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
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.cxf.jaxrs.client.spec.ClientRequestContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.WriterInterceptorContextImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Span;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.utils.Utils;

/**
 * Priority is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Client filter to execute before Tracing Filter.
 **/
@Provider
@Priority(3500)
public class ClientFilter implements WriterInterceptor, ClientRequestFilter, ClientResponseFilter {

	private static final Logger LOGGER = LogManager.getLogger(ClientFilter.class);

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public void aroundWriteTo(WriterInterceptorContext context)
		throws IOException, WebApplicationException {

		ClientRequestContext reqContext = null;
		//get the request context
		if (context instanceof WriterInterceptorContextImpl) {
			Message message = JAXRSUtils.getCurrentMessage();
			reqContext = new ClientRequestContextImpl(message.getExchange().getOutMessage(), true);
		}

		if (reqContext != null) {
			recordRequest(context, reqContext);
		} else {
			LOGGER
				.debug(new ObjectMessage(
					Map.of(Constants.MESSAGE, "Unable to fetch request context!")));
			context.proceed();
		}
	}

	private void logRequest(WriterInterceptorContext writerInterceptorContext,
		ClientRequestContext clientRequestContext, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo,
		String serviceName)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = clientRequestContext.getStringHeaders();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(clientRequestContext.getMethod(), cRequestId,
				Optional.ofNullable(serviceName));

		//body
		byte[] requestBody = null;

		//we pass null for GET requests
		if (writerInterceptorContext != null) {
			requestBody = getRequestBody(writerInterceptorContext);
		}

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private byte[] getRequestBody(WriterInterceptorContext interceptorContext) throws IOException {
		OutputStream originalStream = interceptorContext.getOutputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] reqBody;
		interceptorContext.setOutputStream(baos);
		try {
			interceptorContext.proceed();
		} finally {
			reqBody = baos.toByteArray();
			baos.writeTo(originalStream);
			baos.close();
			interceptorContext.setOutputStream(originalStream);
		}
		return reqBody;
	}

	@Override
	public void filter(ClientRequestContext clientRequestContext,
		ClientResponseContext clientResponseContext) throws IOException {
		if (clientRequestContext.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
			Object apiPathObj = clientRequestContext.getProperty(Constants.MD_API_PATH_PROP);
			Object serviceNameObj = clientRequestContext.getProperty(Constants.MD_SERVICE_PROP);
			Object traceMetaMapObj = clientRequestContext
				.getProperty(Constants.MD_TRACE_META_MAP_PROP);
			Object traceInfo = clientRequestContext.getProperty(Constants.MD_TRACE_INFO);

			String apiPath = apiPathObj != null ? apiPathObj.toString() : Strings.EMPTY;
			String serviceName = serviceNameObj != null ? serviceNameObj.toString() : Strings.EMPTY;

			ObjectMapper mapper = new ObjectMapper();
			MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
				.convertValue(traceMetaMapObj, MetadataMap.class)
				: Utils.createEmptyMultivaluedMap();

			MDTraceInfo mdTraceInfo =
				traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();

			logResponse(clientResponseContext, apiPath, traceMetaMap, mdTraceInfo, serviceName);

			removeSetContextProperty(clientRequestContext);
		}
	}

	private void logResponse(ClientResponseContext responseContext, String apiPath,
		MultivaluedMap<String, String> traceMeta, MDTraceInfo mdTraceInfo, String serviceName)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> responseHeaders = responseContext.getHeaders();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(responseContext.getStatus()),
				Optional.ofNullable(serviceName));
		meta.putAll(traceMeta);

		//body
		byte[] responseBody = getResponseBody(responseContext);

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);
	}

	private byte[] getResponseBody(ClientResponseContext respContext) {
		try (InputStream entityStream = respContext.getEntityStream()) {
			if (entityStream != null) {
				byte[] respBody = IOUtils
					.toByteArray(entityStream);
				InputStream in = new ByteArrayInputStream(respBody);
				respContext.setEntityStream(in);
				return respBody;
			}
		} catch (IOException ex) {
			LOGGER.error(new ObjectMessage(
				Map.of(
					Constants.MESSAGE, "Failure during reading the response body",
					Constants.REASON, ex.getMessage()
				)));
			respContext.setEntityStream(new ByteArrayInputStream(new byte[0]));
		}

		return null;
	}

	private void removeSetContextProperty(ClientRequestContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_SERVICE_PROP);
		context.removeProperty(Constants.MD_SAMPLE_REQUEST);
		context.removeProperty(Constants.MD_TRACE_INFO);
	}

	@Override
	public void filter(ClientRequestContext clientRequestContext) throws IOException {
		if (clientRequestContext.getMethod().equalsIgnoreCase("GET")) {
			//aroundWriteTo will not be called, as there will be no body to write.
			//hence have to log the request here.
			recordRequest(null, clientRequestContext);
		}
	}

	private void recordRequest(WriterInterceptorContext writerInterceptorContext,
		ClientRequestContext clientRequestContext) throws IOException {
		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		if (currentSpan.isPresent()) {
			Span span = currentSpan.get();
			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));
			boolean isVetoed = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_VETOED));

			if (isSampled || isVetoed) {
				span.setBaggageItem(Constants.MD_IS_VETOED, null);

				URI uri = clientRequestContext.getUri();

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

				String xRequestId = clientRequestContext.getStringHeaders()
					.getFirst(Constants.X_REQUEST_ID);
				MultivaluedMap<String, String> traceMetaMap = Utils
					.buildTraceInfoMap(mdTraceInfo, xRequestId);

				clientRequestContext.setProperty(Constants.MD_SERVICE_PROP, serviceName);
				clientRequestContext
					.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
				clientRequestContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);
				clientRequestContext.setProperty(Constants.MD_SAMPLE_REQUEST, true);
				clientRequestContext.setProperty(Constants.MD_TRACE_INFO, mdTraceInfo);

				logRequest(writerInterceptorContext, clientRequestContext, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams,
					mdTraceInfo, serviceName);
			} else {
				LOGGER.debug(new ObjectMessage(
					Map.of(Constants.MESSAGE, "Sampling is false!")));
				if (writerInterceptorContext != null) {
					writerInterceptorContext.proceed();
				}
			}
		} else {
			LOGGER
				.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Current Span is empty!")));
			if (writerInterceptorContext != null) {
				writerInterceptorContext.proceed();
			}
		}
	}
}