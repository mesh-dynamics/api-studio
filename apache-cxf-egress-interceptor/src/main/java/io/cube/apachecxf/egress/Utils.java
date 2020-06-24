package io.cube.apachecxf.egress;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	public static final long PAYLOAD_MAX_LIMIT = 25000000; //25 MB

	private static final Config config = new Config();

	public static MultivaluedMap<String, String> getRequestMeta(String method, String cRequestId,
		Optional<String> serviceName) {
		MultivaluedMap<String, String> metaMap = Utils.createEmptyMultivaluedMap();
		getCommonMeta(metaMap, serviceName);
		metaMap.add(Constants.TYPE_FIELD, Constants.REQUEST);
		metaMap.add(Constants.METHOD_FIELD, method);
		metaMap.add(Constants.DEFAULT_REQUEST_ID, cRequestId);
		return metaMap;
	}

	public static MultivaluedMap<String, String> getResponseMeta(String pathUri,
		String statusCode, Optional<String> serviceName) {
		MultivaluedMap<String, String> metaMap = Utils.createEmptyMultivaluedMap();
		getCommonMeta(metaMap, serviceName);
		metaMap.add(Constants.TYPE_FIELD, Constants.RESPONSE);
		metaMap.add(Constants.STATUS, statusCode);
		metaMap.add(Constants.API_PATH_FIELD, pathUri);

		return metaMap;
	}

	public static void getCommonMeta(MultivaluedMap<String, String> metaMap,
		Optional<String> serviceName) {
		metaMap.add(Constants.TIMESTAMP_FIELD, Instant.now().toString());
		if (config.intentResolver.isIntentToRecord()) {
			metaMap.add(Constants.RUN_TYPE_FIELD, Constants.INTENT_RECORD);
		} else if (config.intentResolver.isIntentToMock()) {
			metaMap.add(Constants.RUN_TYPE_FIELD, Constants.REPLAY);
		}

		CommonConfig commonConfig = CommonConfig.getInstance();
		metaMap.add(Constants.CUSTOMER_ID_FIELD, commonConfig.customerId);
		metaMap.add(Constants.APP_FIELD, commonConfig.app);
		metaMap.add(Constants.INSTANCE_ID_FIELD, commonConfig.instance);
		metaMap.add(Constants.SERVICE_FIELD, serviceName.orElse(commonConfig.serviceName));
	}

	public static MultivaluedMap<String, String> buildTraceInfoMap(MDTraceInfo mdTraceInfo,
		String xRequestId) {
		String cRequestId = CommonConfig.getInstance().serviceName.concat("-")
			.concat(mdTraceInfo.traceId == null ? "" : mdTraceInfo.traceId).concat("-").concat(
				UUID.randomUUID().toString());

		MultivaluedMap<String, String> metaMap = Utils.createEmptyMultivaluedMap();
		metaMap.add(Constants.DEFAULT_REQUEST_ID, cRequestId);
		if (mdTraceInfo.traceId != null) {
			metaMap.add(Constants.DEFAULT_TRACE_FIELD, mdTraceInfo.traceId);
		}
		if (mdTraceInfo.spanId != null) {
			metaMap.add(Constants.DEFAULT_SPAN_FIELD, mdTraceInfo.spanId);
		}
		if (mdTraceInfo.parentSpanId != null) {
			metaMap.add(Constants.DEFAULT_PARENT_SPAN_FIELD, mdTraceInfo.parentSpanId);
		}
		if (xRequestId != null) {
			metaMap.add(Constants.X_REQUEST_ID, xRequestId);
		}
		return metaMap;
	}

	public static void createAndLogReqEvent(String apiPath,
		MultivaluedMap<String, String> queryParams, MultivaluedMap<String, String> requestHeaders,
		MultivaluedMap<String, String> meta, MDTraceInfo mdTraceInfo, byte[] requestBody) {
		Event requestEvent = null;
		final Span span = io.cube.agent.Utils.createPerformanceSpan(
			Constants.CREATE_REQUEST_EVENT_EGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)){
			requestEvent = io.md.utils.Utils
				.createHTTPRequestEvent(apiPath, queryParams,
					Utils.createEmptyMultivaluedMap(), meta, requestHeaders, mdTraceInfo,
					requestBody, Optional.empty(), config.jsonMapper, true, CommonConfig.getInstance().clientMetaDataMap);
		} catch (InvalidEventException e) {
			LOGGER.error( "Invalid Event", e);
		} catch (JsonProcessingException e) {
			LOGGER.error("Json Processing Exception. Unable to create event!", e);
		} finally {
			span.finish();
		}

		if (requestEvent != null) {
			final Span reqLog = io.cube.agent.Utils.createPerformanceSpan(
				Constants.LOG_REQUEST_EVENT_EGRESS);
			try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(reqLog)){
				config.recorder.record(requestEvent);
			} finally {
				reqLog.finish();
			}
		}
	}

	public static void createAndLogRespEvent(String apiPath,
		MultivaluedMap<String, String> responseHeaders, MultivaluedMap<String, String> meta,
		MDTraceInfo mdTraceInfo, byte[] responseBody) {
		Event responseEvent = null;
		final Span span = io.cube.agent.Utils.createPerformanceSpan(Constants
			.CREATE_RESPONSE_EVENT_EGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)){
			responseEvent = io.md.utils.Utils
				.createHTTPResponseEvent(apiPath, meta,
					responseHeaders, mdTraceInfo, responseBody, Optional.empty(), config.jsonMapper,
					true, CommonConfig.getInstance().clientMetaDataMap);
		} catch (InvalidEventException e) {
			LOGGER.error("Invalid Event", e);
		} catch (JsonProcessingException e) {
			LOGGER.error("Json Processing Exception. Unable to create event!", e);
		} finally {
			span.finish();
		}

		if (responseEvent != null) {
			final Span respLog = io.cube.agent.Utils.createPerformanceSpan(Constants
				.LOG_RESPONSE_EVENT_EGRESS);
			try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(respLog)){
				config.recorder.record(responseEvent);
			} finally {
				respLog.finish();
			}

		}
	}

	public static MultivaluedMap<String, String> getQueryParams(URI uri) {
		List<NameValuePair> params = URLEncodedUtils.parse(uri, "UTF-8");
		MultivaluedMap<String, String> queryParams = Utils.createEmptyMultivaluedMap();
		for (NameValuePair param : params) {
			queryParams.add(param.getName(), param.getValue());
		}

		return queryParams;
	}

	public static MultivaluedMap<String, String> createEmptyMultivaluedMap() {
		return new MultivaluedHashMap<>();
	}

	public static void closeSpanAndScope(Span span, Scope scope) {
		if (span != null) {
			span.finish();
		}

		if (scope != null) {
			scope.close();
		}
	}

	public static URI getMockingURI(URI originalUri) {

		CommonConfig commonConfig = CommonConfig.getInstance();
		String serviceName = CommonUtils.getEgressServiceName(originalUri);
		Optional<URI> mockingURI = Optional.empty();

		try {
			mockingURI = commonConfig.getMockingURI(originalUri, serviceName);
		} catch (URISyntaxException e) {
			LOGGER.error("Exception during forming Mocking URI");
		}

		return mockingURI.orElse(originalUri);
	}

	public static URL getMockingURL(URL originalUrl) {

		CommonConfig commonConfig = CommonConfig.getInstance();
		String serviceName = null;
		Optional<URL> mockingUrl = Optional.empty();
		try {
			URI originalUri = originalUrl.toURI();
			serviceName = CommonUtils.getEgressServiceName(originalUri);
			Optional<URI> mockingURI = commonConfig.getMockingURI(originalUri, serviceName);
			if (mockingURI.isPresent()) {
				mockingUrl = Optional.of(new URL(mockingURI.toString()));
			}
		} catch (URISyntaxException | MalformedURLException e) {
			LOGGER.error("URI syntax exception : ", e);
		}

		return mockingUrl.orElse(originalUrl);
	}

	public static String getMockingURI(String originalUrl) {
		return getMockingURI(URI.create(originalUrl)).toString();
	}
}
