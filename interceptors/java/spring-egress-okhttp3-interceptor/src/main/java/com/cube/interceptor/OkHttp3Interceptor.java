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

package com.cube.interceptor;

import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Span;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import io.md.constants.Constants;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSource;

@Component
public class OkHttp3Interceptor implements Interceptor {

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request request = chain.request();

		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils.getMultiMap(
			request.headers().toMultimap().entrySet());
		boolean isSampled = Utils.isSampled(requestHeaders);

		String apiPath = Strings.EMPTY, serviceName = Strings.EMPTY;
		MultivaluedMap<String, String> traceMetaMap = new MultivaluedHashMap<>();
		if (isSampled) {
			//queryParams
			HttpUrl url = request.url();
			Map<String, List<String>> queryParamsMap =
				request.url().queryParameterNames().stream()
					.collect(
						Collectors.toMap(name -> name, name -> url.queryParameterValues(name)));
			MultivaluedMap<String, String> queryParams = Utils
				.getMultiMap(queryParamsMap.entrySet());

			//path
			apiPath = "/".concat(String.join("/", url.pathSegments()));

			//serviceName to be host+port for outgoing calls
			serviceName = String.join(":", url.host(), String.valueOf(url.port()));

			traceMetaMap = getTraceInfoMetaMap(request);

			logRequest(request, requestHeaders, apiPath,
				traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID),
				serviceName, queryParams);
		}
		Response response = chain.proceed(request);
		if (isSampled) {
			logResponse(response, apiPath, traceMetaMap, serviceName);
		}
		return response;
	}

	private void logRequest(Request request, MultivaluedMap<String, String> requestHeaders,
		String apiPath, String cRequestId, String serviceName,
		MultivaluedMap<String, String> queryParams)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(request.method(), cRequestId, Optional
				.of(serviceName));

		//body
		String requestBody = bodyToString(request);
		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		currentSpan.ifPresent(span -> {
			MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);
			Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
					requestBody, mdTraceInfo);
		});

	}

	private void logResponse(Response response, String apiPath,
		MultivaluedMap<String, String> traceMeta,
		String serviceName)
		throws IOException {
		//hdrs
		MultivaluedMap<String, String> responseHeaders = Utils.getMultiMap(
			response.headers().toMultimap().entrySet());

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(response.code()),
				Optional.of(serviceName));
		meta.putAll(traceMeta);

		//body
		String responseBody = bodyToString(response, Utils.PAYLOAD_MAX_LIMIT);
		Optional<Span> currentSpan = CommonUtils.getCurrentSpan();
		currentSpan.ifPresent(span -> {
			MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(span);
			Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, responseBody, mdTraceInfo);
		});

	}

	private MultivaluedMap<String, String> getTraceInfoMetaMap(Request request) {
		String traceId = request.header(Constants.DEFAULT_TRACE_FIELD);
		String spanId = request.header(Constants.DEFAULT_SPAN_FIELD);
		String parentSpanId = request.header(Constants.DEFAULT_PARENT_SPAN_FIELD);
		String xRequestId = request.header(Constants.X_REQUEST_ID);
		return Utils.buildTraceInfoMap(traceId, spanId, parentSpanId, xRequestId);
	}

	private static String bodyToString(final Request request) {

		if (request.body() != null) {
			try {
				final Request copy = request.newBuilder().build();
				final Buffer buffer = new Buffer();
				copy.body().writeTo(buffer);
				return buffer.readUtf8();
			} catch (final IOException e) {
				return "Failed to stringify request body";
			}
		}

		return "";
	}

	private static String bodyToString(final Response response, long limit) throws IOException {
		if (response.body() != null) {
			final BufferedSource source = response.body().source();
			if (source.request(limit)) {
				throw new IOException("Body too long!");
			}
			final String responseBody = source.buffer().snapshot().utf8();

			return responseBody;
		}

		return "";
	}
}
