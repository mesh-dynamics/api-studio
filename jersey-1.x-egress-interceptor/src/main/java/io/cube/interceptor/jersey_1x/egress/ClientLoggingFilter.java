package io.cube.interceptor.jersey_1x.egress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.spi.MessageBodyWorkers;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

public class ClientLoggingFilter extends ClientFilter {

	private static Config config;
	private MessageBodyWorkers workers;
	private final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientLoggingFilter.class);
	private static final String EMPTY = "";
	private static final List<String> HTTP_CONTENT_TYPE_HEADERS = Arrays
			.asList(new String[]{"content-type",
					"Content-type", "Content-Type", "content-Type"});

	static {
			config = new Config();
	}

	public ClientLoggingFilter(MessageBodyWorkers workers) {
		this.workers = workers;
	}

	@Override
	public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
		if (config == null) {
			LOGGER.error("Skipping the filter as the config is null");
			return getNext().handle(clientRequest);
		}
		Span clientSpan = null;
		Scope clientScope = null;
		String serviceName = null;

		try {
			serviceName = CommonUtils.getEgressServiceName(clientRequest.getURI());
			//hdrs
			Optional<Span> ingressSpan = CommonUtils.getCurrentSpan();

			clientSpan = CommonUtils.startClientSpan(Constants.MD_CHILD_SPAN,
					ingressSpan.map(Span::context).orElse(null), false);

			clientScope = CommonUtils.activateSpan(clientSpan);

			clientRequest = filter(clientRequest, clientSpan, !ingressSpan.isPresent(), serviceName);
		} catch (Exception e) {
			LOGGER.error("Exception occured during logging request, proceeding to the application!", e);
		}

		ClientResponse resp = null;
		try {
			// Call the next client handler in the filter chain
			resp = getNext().handle(clientRequest);
		} catch (Exception e) {
			if (clientSpan != null)
				clientSpan.finish();
			if (clientScope != null)
				clientScope.close();
			throw e;
		}


		// Modify the response
		try {
			CommonConfig commonConfig = config.commonConfig;
			//If egress to be mocked then skip data capture
			if (serviceName != null && !commonConfig.shouldMockService(serviceName)) {
				return filter(clientRequest, resp);
			}
		} catch (Exception e) {
			LOGGER.error(
					"Exception occured during logging response, proceeding to the application!", e);
		} finally {
			if (clientSpan != null)
				clientSpan.finish();
			if (clientScope != null)
				clientScope.close();
		}

		return resp;
	}

	public ClientRequest filter(ClientRequest clientRequest, Span clientSpan, boolean ingressEmpty,
			String service) {
		try {
			// Do not log request in case the egress serivce is to be mocked
			CommonConfig commonConfig = CommonConfig.getInstance();

			if (service != null && commonConfig.shouldMockService(service)) {
				return clientRequest;
			}

			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
					.toBoolean(clientSpan.getBaggageItem(Constants.MD_IS_SAMPLED));

			boolean isVetoed = BooleanUtils
					.toBoolean(clientSpan.getBaggageItem(Constants.MD_IS_VETOED));

			//Empty ingress span pertains to application initiated calls without ingress
			//So need to record all calls as these will not be driven by replay driver.
			if (isSampled || isVetoed || ingressEmpty) {
				//this is local baggage item
				clientSpan.setBaggageItem(Constants.MD_IS_VETOED, null);

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

				MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(clientSpan);

				MultivaluedMap<String, String>  headersMap = Utils.transformHeaders(clientRequest.getHeaders());

				MultivaluedMap<String, String> traceMetaMap = getTraceInfoMetaMap(headersMap, mdTraceInfo);

				clientRequest.getProperties().put(Constants.MD_SERVICE_PROP, serviceName);
				clientRequest.getProperties().put(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
				clientRequest.getProperties().put(Constants.MD_API_PATH_PROP, apiPath);
				clientRequest.getProperties().put(Constants.MD_SAMPLE_REQUEST, isSampled);
				clientRequest.getProperties().put(Constants.MD_TRACE_INFO, mdTraceInfo);

				logRequest(clientRequest, apiPath,
						traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
						queryParams, mdTraceInfo, serviceName);

				//Setting the current span id as parent span id value
				//This is intentionally set after the capture as it should be parent for subsequent capture
				clientSpan.setBaggageItem(Constants.MD_PARENT_SPAN, clientSpan.context().toSpanId());
			}
		} catch (Exception e ) {
			LOGGER.error(
					"Exception occured during logging, proceeding to the application!", e);
		}

		return clientRequest;
	}

	public ClientResponse filter(ClientRequest clientRequest, ClientResponse clientResponse) throws IOException {
		if (clientRequest.getProperties().get(Constants.MD_SAMPLE_REQUEST) != null) {
			clientRequest.getProperties()
				.put(Constants.MD_RESPONSE_HEADERS_PROP,
					clientResponse.getHeaders());
			clientRequest.getProperties()
				.put(Constants.MD_STATUS_PROP, clientResponse.getStatus());
			try {
				logResponse(clientRequest, getResponseBody(clientResponse));
			} catch (IOException ioe) {
				LOGGER.error("Exception while logging response", ioe);
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
		byte[] requestBody = getRequestBody(clientRequest);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private byte[] getRequestBody(ClientRequest clientRequest) throws  IOException {
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
			byte[] requestBody = baos.toByteArray();
			baos.close();
			return requestBody;
		}
		return new byte[0];
	}


	private MediaType getMediaType(Class entityClass, Type entityType, MultivaluedMap<String, Object> headers) {
		Optional<Object> mimeTypeObj = getMimeType(headers);
		if (mimeTypeObj.isPresent() && mimeTypeObj.get() instanceof MediaType) {
			return (MediaType)mimeTypeObj.get();
		} else if (mimeTypeObj.isPresent()) {
			return MediaType.valueOf(mimeTypeObj.get().toString());
		} else {
			List<MediaType> mediaTypes = this.workers.getMessageBodyWriterMediaTypes(entityClass, entityType, EMPTY_ANNOTATIONS);
			MediaType mediaType = this.getMediaType(mediaTypes);
			headers.putSingle("Content-Type", mediaType);
			return mediaType;
		}
	}

	private Optional<Object> getMimeType(MultivaluedMap<String, Object> headers) {
		if (headers == null)
			return Optional.empty();
		return HTTP_CONTENT_TYPE_HEADERS.stream()
				.map(headers::getFirst).filter(Objects::nonNull)
				.findFirst();
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

	private void logResponse(ClientRequest clientRequest, byte[] responseBody)
		throws IOException {
		Object apiPathObj = clientRequest.getProperties().get(Constants.MD_API_PATH_PROP);
		Object traceMetaMapObj = clientRequest.getProperties().get(Constants.MD_TRACE_META_MAP_PROP);
		Object respHeadersObj = clientRequest.getProperties().get(Constants.MD_RESPONSE_HEADERS_PROP);
		Object statusObj = clientRequest.getProperties().get(Constants.MD_STATUS_PROP);
		Object serviceNameObj = clientRequest.getProperties().get(Constants.MD_SERVICE_PROP);
		Object traceInfo = clientRequest.getProperties().get(Constants.MD_TRACE_INFO);

		String apiPath = apiPathObj != null ? apiPathObj.toString() : EMPTY;
		String serviceName = serviceNameObj != null ? serviceNameObj.toString() : EMPTY;

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

	private MultivaluedMap<String, String> getTraceInfoMetaMap(
		MultivaluedMap<String, String> headersMap, MDTraceInfo mdTraceInfo) {
		String xRequestId = headersMap.getFirst(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(mdTraceInfo, xRequestId);
	}

	private byte[] getResponseBody(ClientResponse clientResponse) throws IOException {
		byte[] respBytes = IOUtils.toByteArray(clientResponse.getEntityInputStream());
		InputStream in = new ByteArrayInputStream(respBytes);
		clientResponse.setEntityInputStream(in);
		return respBytes;
	}
}
