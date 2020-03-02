package com.cube.interceptor.jersey.egress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.util.Strings;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.spi.MessageBodyWorkers;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.md.utils.UtilException;
import io.opentracing.Span;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.jersey.egress.utils.Utils;

public class ClientLoggingFilter extends ClientFilter {

	private static final Config config;
	private MessageBodyWorkers workers;
	private final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

	static {
		config = new Config();
	}

	public ClientLoggingFilter(MessageBodyWorkers workers) {
		this.workers = workers;
	}

	@Override
	public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
		// Modify the request
		try {
			clientRequest = filter(clientRequest);
		} catch (Exception e) {
			e.printStackTrace();
		}


		// Call the next client handler in the filter chain
		ClientResponse resp = getNext().handle(clientRequest);

		// Modify the response
		try {
			return filter(clientRequest, resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resp;
	}

	public ClientRequest filter(ClientRequest clientRequest) throws IOException {
		//hdrs
		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		currentSpan.ifPresent(UtilException.rethrowConsumer(span ->
		{
			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
				.toBoolean(span.getBaggageItem(Constants.MD_IS_SAMPLED));

			MultivaluedMap<String, String> headersMap = Utils.transformHeaders(clientRequest.getHeaders());
			//check veto if upstream service decides to NOT SAMPLE
			if (!isSampled && config.commonConfig.samplerVeto) {
				isSampled = Utils.isSampled(headersMap);
			}

			if (isSampled) {
				URI uri = clientRequest.getURI();

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

				MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(headersMap, mdTraceInfo);

				clientRequest.getProperties().put(Constants.MD_SERVICE_PROP, serviceName);
				clientRequest.getProperties().put(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
				clientRequest.getProperties().put(Constants.MD_API_PATH_PROP, apiPath);
				clientRequest.getProperties().put(Constants.MD_SAMPLE_REQUEST, isSampled);
				clientRequest.getProperties().put(Constants.MD_TRACE_INFO, mdTraceInfo);

				logRequest(clientRequest, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
					queryParams, mdTraceInfo, serviceName);
			}
		}));

		return clientRequest;
	}

	public ClientResponse filter(ClientRequest clientRequest, ClientResponse clientResponse) throws IOException {
		if (clientRequest.getProperties().get(Constants.MD_SAMPLE_REQUEST) != null) {
			clientRequest.getProperties()
				.put(Constants.MD_RESPONSE_HEADERS_PROP,
					clientResponse.getHeaders());
			clientRequest.getProperties()
				.put(Constants.MD_STATUS_PROP, clientResponse.getStatus());
			clientRequest.getProperties().put(Constants.MD_BODY_PROP, getResponseBody(clientResponse));
			try {
				logResponse(clientRequest, getResponseBody(clientResponse));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return clientResponse;
	}

	private void logRequest(ClientRequest clientRequest, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo,
		String serviceName)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils.transformHeaders(clientRequest.getHeaders());

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(clientRequest.getMethod(), cRequestId,
				Optional.ofNullable(serviceName));

		//body
		String requestBody = getRequestBody(clientRequest);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private String getRequestBody(ClientRequest clientRequest) throws  IOException {
		Object entity = clientRequest.getEntity();
		if (entity != null) {
			Type entityType = null;
			if (entity instanceof GenericEntity) {
				GenericEntity ge = (GenericEntity) entity;
				entityType = ge.getType();
				entity = ge.getEntity();
			} else {
				entityType = entity.getClass();
			}

			Class entityClass = entity.getClass();
			MultivaluedMap<String, Object> headers = clientRequest.getHeaders();
			MediaType mediaType = getMediaType(entityClass, (Type) entityType, headers);
			MessageBodyWriter bw = this.workers
					.getMessageBodyWriter(entityClass, (Type) entityType, EMPTY_ANNOTATIONS, mediaType);

			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			bw.writeTo(entity, entityClass, entityType, EMPTY_ANNOTATIONS, mediaType, headers, baos);
			String requestBody = baos.toString("UTF-8");
			baos.close();
			return requestBody;
		}
		return Strings.EMPTY;
	}


	private MediaType getMediaType(Class entityClass, Type entityType, MultivaluedMap<String, Object> headers) {
		Object mediaTypeHeader = headers.getFirst("Content-Type");
		if (mediaTypeHeader instanceof MediaType) {
			return (MediaType)mediaTypeHeader;
		} else if (mediaTypeHeader != null) {
			return MediaType.valueOf(mediaTypeHeader.toString());
		} else {
			List<MediaType> mediaTypes = this.workers.getMessageBodyWriterMediaTypes(entityClass, entityType, EMPTY_ANNOTATIONS);
			MediaType mediaType = this.getMediaType(mediaTypes);
			headers.putSingle("Content-Type", mediaType);
			return mediaType;
		}
	}


	private MediaType getMediaType(List<MediaType> mediaTypes) {
		if (mediaTypes.isEmpty()) {
			return MediaType.APPLICATION_OCTET_STREAM_TYPE;
		} else {
			MediaType mediaType = (MediaType)mediaTypes.get(0);
			if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
				mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
			}

			return mediaType;
		}
	}

	private void logResponse(ClientRequest clientRequest, String respBody)
		throws IOException {
		Object apiPathObj = clientRequest.getProperties().get(Constants.MD_API_PATH_PROP);
		Object traceMetaMapObj = clientRequest.getProperties().get(Constants.MD_TRACE_META_MAP_PROP);
		Object respHeadersObj = clientRequest.getProperties().get(Constants.MD_RESPONSE_HEADERS_PROP);
		Object statusObj = clientRequest.getProperties().get(Constants.MD_STATUS_PROP);
		Object serviceNameObj = clientRequest.getProperties().get(Constants.MD_SERVICE_PROP);
		Object respBodyObj = clientRequest.getProperties().get(Constants.MD_BODY_PROP);
		Object traceInfo = clientRequest.getProperties().get(Constants.MD_TRACE_INFO);

		String apiPath = apiPathObj != null ? apiPathObj.toString() : Strings.EMPTY;
		String serviceName = serviceNameObj != null ? serviceNameObj.toString() : Strings.EMPTY;

		MultivaluedMap<String, String> responseHeaders = respHeadersObj != null ?
				(MultivaluedMap<String, String>) respHeadersObj
				: Utils.createEmptyMultivaluedMap();
		MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ?
				(MultivaluedMap<String, String>) traceMetaMapObj
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

		removeSetContextProperty(clientRequest);
	}

	private void removeSetContextProperty(ClientRequest clientRequest) {
		clientRequest.getProperties().remove(Constants.MD_API_PATH_PROP);
		clientRequest.getProperties().remove(Constants.MD_TRACE_META_MAP_PROP);
		clientRequest.getProperties().remove(Constants.MD_RESPONSE_HEADERS_PROP);
		clientRequest.getProperties().remove(Constants.MD_STATUS_PROP);
		clientRequest.getProperties().remove(Constants.MD_SERVICE_PROP);
		clientRequest.getProperties().remove(Constants.MD_BODY_PROP);
		clientRequest.getProperties().remove(Constants.MD_LOG_STREAM_PROP);
		clientRequest.getProperties().remove(Constants.MD_SAMPLE_REQUEST);
		clientRequest.getProperties().remove(Constants.MD_TRACE_INFO);
	}

	private MDTraceInfo getTraceInfo(Span currentSpan) {
		JaegerSpanContext spanContext = (JaegerSpanContext) currentSpan.context();

		String traceId = spanContext.getTraceId();
		String spanId = String.valueOf(spanContext.getSpanId());
		String parentSpanId = String.valueOf(spanContext.getParentId());
		MDTraceInfo mdTraceInfo = new MDTraceInfo(traceId, spanId, parentSpanId);
		return mdTraceInfo;
	}

	private MultivaluedMap<String, String> getTraceInfoMetaMap(
		MultivaluedMap<String, String> headersMap, MDTraceInfo mdTraceInfo) {
		String xRequestId = headersMap.getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(mdTraceInfo, xRequestId);
	}

	private String getResponseBody(ClientResponse clientResponse) throws IOException {
		String json = IOUtils.toString(clientResponse.getEntityInputStream(), StandardCharsets.UTF_8);
		InputStream in = IOUtils.toInputStream(json, StandardCharsets.UTF_8);
		clientResponse.setEntityInputStream(in);

		return json;
	}
}
