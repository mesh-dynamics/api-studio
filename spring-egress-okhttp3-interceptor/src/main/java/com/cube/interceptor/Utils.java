package com.cube.interceptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.http.HttpRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder.InvalidEventException;

public class Utils {

	private static final Logger LOGGER = LogManager.getLogger(Utils.class);

	public static final long PAYLOAD_MAX_LIMIT = 25000000; //25 MB

	private static final Config config;

	static {
		config = new Config();
	}

	public static boolean isSampled(MultivaluedMap<String, String> requestHeaders) {
		return ((config.intentResolver.isIntentToRecord() && config.commonConfig.sampler
			.isSampled(requestHeaders)) || config.intentResolver.isIntentToMock());
	}

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
		metaMap.add(Constants.CUSTOMER_ID_FIELD, config.commonConfig.customerId);
		metaMap.add(Constants.APP_FIELD, config.commonConfig.app);
		metaMap.add(Constants.INSTANCE_ID_FIELD, config.commonConfig.instance);
		metaMap.add(Constants.SERVICE_FIELD, serviceName.orElse(config.commonConfig.serviceName));
	}

	public static MultivaluedMap<String, String> buildTraceInfoMap(String traceId, String spanId,
		String parentSpanId, String xRequestId) {
		String cRequestId = config.commonConfig.serviceName.concat("-")
			.concat(traceId == null ? "" : traceId).concat("-").concat(
				UUID.randomUUID().toString());

		MultivaluedMap<String, String> metaMap = Utils.createEmptyMultivaluedMap();
		metaMap.add(Constants.DEFAULT_REQUEST_ID, cRequestId);
		if (traceId != null) {
			metaMap.add(Constants.DEFAULT_TRACE_FIELD, traceId);
		}
		if (spanId != null) {
			metaMap.add(Constants.DEFAULT_SPAN_FIELD, spanId);
		}
		if (parentSpanId != null) {
			metaMap.add(Constants.DEFAULT_PARENT_SPAN_FIELD, parentSpanId);
		}
		if (xRequestId != null) {
			metaMap.add(Constants.X_REQUEST_ID, xRequestId);
		}
		return metaMap;
	}

	public static MultivaluedMap<String, String> getMultiMap(
		Set<Entry<String, List<String>>> inputSet) {
		MultivaluedMap<String, String> multivaluedMap = Utils.createEmptyMultivaluedMap();
		for (Entry<String, List<String>> entry : inputSet) {
			String key = entry.getKey();
			multivaluedMap.addAll(key, entry.getValue());
		}
		return multivaluedMap;
	}

	public static void createAndLogReqEvent(String apiPath,
		MultivaluedMap<String, String> queryParams, MultivaluedMap<String, String> requestHeaders,
		MultivaluedMap<String, String> meta, String requestBody) {
		try {
			Event requestEvent = io.md.utils.Utils
				.createHTTPRequestEvent(apiPath, queryParams,
					Utils.createEmptyMultivaluedMap(), meta, requestHeaders,
					requestBody, Optional.empty(), config.jsonMapper, true);
			config.recorder.record(requestEvent);
		} catch (InvalidEventException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Invalid Event",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Json Processing Exception. Unable to create event!",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)));
		}
	}

	public static void createAndLogRespEvent(String apiPath,
		MultivaluedMap<String, String> responseHeaders, MultivaluedMap<String, String> meta,
		String responseBody) {
		try {
			Event responseEvent = io.md.utils.Utils
				.createHTTPResponseEvent(apiPath, meta,
					responseHeaders, responseBody, Optional.empty(), config.jsonMapper,
					true);
			config.recorder.record(responseEvent);
		} catch (InvalidEventException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Invalid Event",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Json Processing Exception. Unable to create event!",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)));
		}
	}

	public static MultivaluedMap<String, String> getQueryParams(URI uri) {
		List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
		MultivaluedMap<String, String> queryParams = Utils.createEmptyMultivaluedMap();
		for (NameValuePair param : params) {
			queryParams.add(param.getName(), param.getValue());
		}

		return queryParams;
	}

	public static MultivaluedMap<String, String> getTraceInfoMetaMap(HttpRequest request) {
		//Expecting the trace info to be single valued.
		List<String> traceId = request.getHeaders().get(Constants.DEFAULT_TRACE_FIELD);
		List<String> spanId = request.getHeaders().get(Constants.DEFAULT_SPAN_FIELD);
		List<String> parentSpanId = request.getHeaders().get(Constants.DEFAULT_PARENT_SPAN_FIELD);
		List<String> xRequestId = request.getHeaders().get(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(
			traceId == null ? null : traceId.get(0),
			spanId == null ? null : spanId.get(0),
			parentSpanId == null ? null : parentSpanId.get(0),
			xRequestId == null ? null : xRequestId.get(0)
		);
	}

	public static MultivaluedMap<String, String> createEmptyMultivaluedMap() {
		return new MultivaluedHashMap<>();
	}

}
