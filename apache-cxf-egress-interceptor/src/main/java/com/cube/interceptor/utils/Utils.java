package com.cube.interceptor.utils;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.MDTraceInfo;

import com.cube.interceptor.config.Config;

public class Utils {

	private static final Logger LOGGER = LogManager.getLogger(Utils.class);

	public static final long PAYLOAD_MAX_LIMIT = 25000000; //25 MB

	private static final Config config;

	static {
		config = new Config();
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

	public static MultivaluedMap<String, String> buildTraceInfoMap(MDTraceInfo mdTraceInfo,
		String xRequestId) {
		String cRequestId = config.commonConfig.serviceName.concat("-")
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
		try {
			Event requestEvent = io.md.utils.Utils
				.createHTTPRequestEvent(apiPath, queryParams,
					Utils.createEmptyMultivaluedMap(), meta, requestHeaders, mdTraceInfo,
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
		MDTraceInfo mdTraceInfo, byte[] responseBody) {
		try {
			Event responseEvent = io.md.utils.Utils
				.createHTTPResponseEvent(apiPath, meta,
					responseHeaders, mdTraceInfo, responseBody, Optional.empty(), config.jsonMapper,
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
}
