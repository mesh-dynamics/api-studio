package com.cube.interceptor.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Span;

import com.cube.interceptor.utils.Utils;

@Provider
@Priority(3000)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

	public static final String EMPTY = "";
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

	@Override
	public ContainerRequest filter(ContainerRequest containerRequest) {
		try {
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

					URI uri = containerRequest.getRequestUri();

					//query params
					MultivaluedMap<String, String> queryParams = containerRequest.getQueryParameters();

					//path
					String apiPath = uri.getPath();

					MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);

					MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(containerRequest,
						mdTraceInfo);

					containerRequest.getProperties().put(Constants.MD_SAMPLE_REQUEST, true);
					containerRequest.getProperties().put(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
					containerRequest.getProperties().put(Constants.MD_API_PATH_PROP, apiPath);
					containerRequest.getProperties().put(Constants.MD_TRACE_INFO, mdTraceInfo);

					logRequest(containerRequest, apiPath, traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
						queryParams, mdTraceInfo);

					//Setting the ingress md span as parent span and re-injecting them into the headers.
					// This is done after the capture as this is the parent only for the subsequent capture
					span.setBaggageItem(Constants.MD_PARENT_SPAN, span.context().toSpanId());
				}
			}));
		} catch (Exception e) {
			LOGGER.error("Exception while running the logging filter ", e);
		}
		return containerRequest;
	}

	@Override
	public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
		try {
			if (request.getProperties().get(Constants.MD_SAMPLE_REQUEST) != null) {
				request.getProperties().put(Constants.MD_RESPONSE_HEADERS_PROP,
						transformHeaders(response.getHttpHeaders()));
				request.getProperties().put(Constants.MD_STATUS_PROP, response.getStatus());
				logResponse(request, getResponseBody(response));
			}
		} catch (Exception e) {
			LOGGER.error("Exception while running the logging filter ", e);
		}

		return response;
	}

	//this coverts the Object values in the Map to String values
	private MultivaluedMap<String, String> transformHeaders(MultivaluedMap<String, Object> httpHeaders) {
			MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
			for (Map.Entry<String, List<Object>> entry : httpHeaders.entrySet()) {
				for (Object value : entry.getValue()) {
					if (null != value) {
						if (value instanceof  String) {
							multivaluedMap.add(entry.getKey(), (String) value);
						} else {
							final HeaderDelegate hp = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
							multivaluedMap.add(entry.getKey(), (hp != null) ? hp.toString(value) : value.toString());
						}
					}
				}
			}
			return multivaluedMap;
	}


	private void logRequest(ContainerRequest containerRequest, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = containerRequest.getRequestHeaders();

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(containerRequest.getMethod(), cRequestId, Optional.empty());

		//body
		byte[] requestBody = getRequestBody(containerRequest);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private void logResponse(ContainerRequest request, byte[] respBody) {
		Object apiPathObj = request.getProperties().get(Constants.MD_API_PATH_PROP);
		Object traceMetaMapObj = request.getProperties().get(Constants.MD_TRACE_META_MAP_PROP);
		Object respHeadersObj = request.getProperties().get(Constants.MD_RESPONSE_HEADERS_PROP);
		Object statusObj = request.getProperties().get(Constants.MD_STATUS_PROP);
		Object traceInfo = request.getProperties().get(Constants.MD_TRACE_INFO);

		//hdrs
		MultivaluedMap<String, String> responseHeaders = respHeadersObj != null ?
				(MultivaluedMap <String, String>) respHeadersObj : Utils.createEmptyMultivaluedMap();

		MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ?
				(MultivaluedMap<String, String>)traceMetaMapObj : Utils.createEmptyMultivaluedMap();

		String apiPath = apiPathObj != null ? apiPathObj.toString() : EMPTY;
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, statusObj != null ? statusObj.toString() : EMPTY,
				Optional.empty());
		meta.putAll(traceMetaMap);

		MDTraceInfo mdTraceInfo = traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, respBody);

		removeSetContextProperty(request);
	}

	private void removeSetContextProperty(ContainerRequest request) {
		request.getProperties().remove(Constants.MD_API_PATH_PROP);
		request.getProperties().remove(Constants.MD_TRACE_META_MAP_PROP);
		request.getProperties().remove(Constants.MD_RESPONSE_HEADERS_PROP);
		request.getProperties().remove(Constants.MD_STATUS_PROP);
		request.getProperties().remove(Constants.MD_SAMPLE_REQUEST);
		request.getProperties().remove(Constants.MD_TRACE_INFO);
	}

	private MultivaluedMap<String, String> getTraceInfoMetaMap(ContainerRequest containerRequest,
		MDTraceInfo mdTraceInfo) {
		String xRequestId = containerRequest.getRequestHeaders().getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(mdTraceInfo, xRequestId);
	}

	private byte[] getRequestBody(ContainerRequest request) throws IOException {
		byte[] reqBytes = request.getEntityInputStream().readAllBytes();
		InputStream in = new ByteArrayInputStream(reqBytes);
		request.setEntityInputStream(in);
		return reqBytes;
	}

	private byte[] getResponseBody(ContainerResponse containerResponse) throws IOException {
		final MessageBodyWriter writer = containerResponse.getMessageBodyWorkers().getMessageBodyWriter(
				containerResponse.getEntity().getClass(), containerResponse.getEntityType(),
				containerResponse.getAnnotations(), containerResponse.getMediaType());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] responseBody;
		writer.writeTo(containerResponse.getEntity(), containerResponse.getEntity().getClass(),
				containerResponse.getEntityType(), containerResponse.getAnnotations(),
				containerResponse.getMediaType(), containerResponse.getHttpHeaders(), baos);
		responseBody = baos.toByteArray();
		baos.close();
		return responseBody;
	}
}