package com.cube.interceptor.spring.interceptor;

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

import com.cube.interceptor.utils.Utils;

@Component
public class OkHttp3Interceptor implements Interceptor {

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

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			requestBody);
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

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, responseBody);
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
