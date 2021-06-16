/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.interceptor.spring.egress;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.logging.log4j.message.ObjectMessage;
import org.eclipse.jetty.http.HttpFields;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.MDTraceInfo;
import org.slf4j.Logger;

public class Utils {

	private static final Logger LOGGER = LogMgr.getLogger(Utils.class);

	public static final long PAYLOAD_MAX_LIMIT = 25000000; //25 MB

	private static final WebClientConfig config;

	static {
		config = new WebClientConfig();
	}

	public static boolean isSampled(MultivaluedMap<String, String> requestHeaders) {
		return ((config.intentResolver.isIntentToRecord() && config.commonConfig.sampler
			.isSampled(requestHeaders)) || config.intentResolver.isIntentToMock());
	}

	public static MultivaluedMap<String, String> getRequestMeta(String method, String cRequestId,
		Optional<String> serviceName) {
		MultivaluedMap<String, String> metaMap = new MultivaluedHashMap<>();
		getCommonMeta(metaMap, serviceName);
		metaMap.add(Constants.TYPE_FIELD, Constants.REQUEST);
		metaMap.add(Constants.METHOD_FIELD, method);
		metaMap.add(Constants.DEFAULT_REQUEST_ID, cRequestId);
		return metaMap;
	}

	public static MultivaluedMap<String, String> getResponseMeta(String pathUri,
		String statusCode, Optional<String> serviceName) {
		MultivaluedMap<String, String> metaMap = new MultivaluedHashMap<>();
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

	public static MultivaluedMap<String, String> getMultiMap(
		Set<Entry<String, List<String>>> inputSet) {
		MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
		for (Entry<String, List<String>> entry : inputSet) {
			String key = entry.getKey();
			multivaluedMap.addAll(key, entry.getValue());
		}
		return multivaluedMap;
	}

	public static void createAndLogReqEvent(String apiPath,
		MultivaluedMap<String, String> queryParams, MultivaluedMap<String, String> requestHeaders,
		MultivaluedMap<String, String> meta, MDTraceInfo mdTraceInfo, byte[] requestBody) {
		try {
			Event requestEvent = io.md.utils.Utils
				.createHTTPRequestEvent(apiPath, queryParams,
					new MultivaluedHashMap<>(), meta, requestHeaders,
					mdTraceInfo, requestBody, Optional.empty(), config.jsonMapper, true);
			config.recorder.record(requestEvent);
		} catch (InvalidEventException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Invalid Event",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)).toString());
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Json Processing Exception. Unable to create event!",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)).toString());
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
					Constants.API_PATH_FIELD, apiPath)).toString());
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(
				Map.of(Constants.MESSAGE, "Json Processing Exception. Unable to create event!",
					Constants.ERROR, e.getMessage(),
					Constants.API_PATH_FIELD, apiPath)).toString());
		}
	}

	public static MultivaluedMap<String, String> getQueryParams(URI uri) {
		List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
		MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
		for (NameValuePair param : params) {
			queryParams.add(param.getName(), param.getValue());
		}

		return queryParams;
	}

	public static MultivaluedMap<String, String> getHeaders(HttpFields headers) {
		MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();
		headers.stream().forEach(header -> {
			String key = header.getName();
			for (String value : header.getValues()) {
				headerMap.add(key, value);
			}

		});
		return headerMap;
	}

}
