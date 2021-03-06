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

package io.md.utils;

import static io.md.dao.FnReqRespPayload.RetStatus.Success;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;
import io.md.cache.ProtoDescriptorCache;
import io.md.cache.ProtoDescriptorCache.ProtoDescriptorKey;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.dao.*;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Recording.RecordingStatus;
import io.md.logger.LogMgr;
import io.md.services.DSResult;
import io.md.services.DataStore;
import io.md.services.DataStore.TemplateNotFoundException;
import io.md.tracer.TracerMgr;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.core.Comparator;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.RunType;
import io.md.dao.Recording.RecordingType;
import io.md.dao.RawPayload.RawPayloadEmptyException;
import io.md.dao.RawPayload.RawPayloadProcessingException;
import io.md.services.FnResponse;
import io.md.services.MockResponse;
import io.md.services.Mocker.MockerException;
import io.opentracing.Span;
import kotlin.Result;

public class Utils {

	private static final Logger LOGGER = LogMgr.getLogger(Utils.class);

	// Assumes name is not null
	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
		return EnumSet.allOf(clazz).stream()
			.filter(v -> v.name().equalsIgnoreCase(name))
			.findAny();
	}

	// copied from jdk.internal.net.http.common.Utils, since it is private there and we
	// need this list
	// TODO: Always keep this in sync
	private static final Set<String> DISALLOWED_HEADERS_SET;

	static {
		// A case insensitive TreeSet of strings.
		TreeSet<String> treeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		treeSet.addAll(new HashSet<>(Arrays.asList("connection", "content-length",
			"date", "expect", "from", "host", "origin",
			/*"referer",*/ "upgrade",
			"via", "warning", "transfer-encoding" , "content-encoding")));
		DISALLOWED_HEADERS_SET = Collections.unmodifiableSet(treeSet);
	}

	public static final Predicate<String>
		ALLOWED_HEADERS = (header) -> !DISALLOWED_HEADERS_SET.contains(header);

	public static<T> Optional<T> safeGet(Supplier<T> supplier){
		try{
			return Optional.ofNullable(supplier.get());
		}catch(Exception e){
			return Optional.empty();
		}
	}

	public static<T,U> Optional<U> safeFnExecute(T val , Function<T , U> mapperFn){
		try{
			return Optional.ofNullable(val).map(mapperFn);
		}catch(Exception e){
			return Optional.empty();
		}
	}

	/**
	 * @param intStr
	 * @return
	 */
	public static Optional<Integer> strToInt(String intStr) {
		return safeFnExecute(intStr , Integer::valueOf);
	}

	public static Optional<Float> strToFloat(String floatStr) {
		return safeFnExecute(floatStr , Float::valueOf);
	}

	public static Optional<Double> strToDouble(String dblStr) {
		return safeFnExecute(dblStr , Double::valueOf);
	}

	public static Optional<Long> strToLong(String longStr) {
		return safeFnExecute(longStr , Long::valueOf);
	}

	public static Optional<Instant> strToTimeStamp(String val) {
		return safeFnExecute(val , Instant::parse);
	}

	public static Optional<Boolean> strToBool(String boolStr) {
		return safeFnExecute(boolStr , BooleanUtils::toBoolean);
	}


	public static IntNode intToJson(Integer val) {
		return IntNode.valueOf(val);
	}

	public static TextNode strToJson(String val) {
		return TextNode.valueOf(val);
	}

	public static Pattern analysisTimestampPattern = Pattern
		.compile("\\\\\"timestamp\\\\\":\\d{13},");
	public static Pattern recordingTimestampPattern = Pattern
		.compile(",\"timestamp_dt\":\\{\"name\":\"timestamp_dt\",\"value\":\".+\"\\}");

	public static Pattern replayMetaIdPattern = Pattern
		.compile("\"id\":\\{\"name\":\"id\",\"value\":\"(.+?)\"},");
	public static Pattern replayIdPattern = Pattern
		.compile("\"replayid_s\":\\{\"name\":\"replayid_s\",\"value\":\"(.+?)\"},");
	public static Pattern timestampIdPattern = Pattern.compile(
		",\"creationtimestamp_s\":\\{\"name\":\"creationtimestamp_s\",\"value\":\"(.+?)\"}");
	public static Pattern versionPattern = Pattern
		.compile(",\"version_s\":\\{\"name\":\"version_s\"}");

	public static String removePatternFromString(String val, Pattern pattern) {
		Matcher matcher = pattern.matcher(val);
		return matcher.replaceAll("");
	}


	/**
	 * https://stackoverflow.com/questions/7498030/append-relative-url-to-java-net-url
	 *
	 * @param baseUrl Base Url
	 * @param suffix  Relative path to append to the base url
	 * @return Concatenated Normalized Path (// are converted to /)
	 * @throws Exception Exception if Any
	 */
	static public String appendUrlPath(String baseUrl, String suffix) throws Exception {
		URIBuilder uriBuilder = new URIBuilder(baseUrl);
		return uriBuilder.setPath(uriBuilder.getPath() + "/" + suffix)
			.build().normalize().toString();
	}


	public static Optional<String> getFirst(MultivaluedMap<String, String> fieldMap,
		String fieldname) {
		return Optional.ofNullable(fieldMap.getFirst(fieldname));
	}

	public static Event createHTTPRequestEvent(String apiPath,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs, MDTraceInfo mdTraceInfo, byte[] body,
		Optional<String> collection,
		ObjectMapper jsonMapper, boolean dontCheckCollection)
		throws JsonProcessingException, Event.EventBuilder.InvalidEventException {
		return createHTTPRequestEvent(apiPath, queryParams, formParams, meta, hdrs, mdTraceInfo, body,
			collection, jsonMapper, dontCheckCollection, Collections.EMPTY_MAP);
	}

	// TODO: jsonMapper can be removed. This will need change in all the interceptors
	public static Event createHTTPRequestEvent(String apiPath,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs, MDTraceInfo mdTraceInfo, byte[] body,
		Optional<String> collection,
		ObjectMapper jsonMapper, boolean dontCheckCollection, Map eventMetaData)
		throws JsonProcessingException, Event.EventBuilder.InvalidEventException {
		Optional<String> customerId = getFirst(meta, Constants.CUSTOMER_ID_FIELD);
		Optional<String> app = getFirst(meta, Constants.APP_FIELD);
		Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
		Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
		//Optional<String> traceId = getFirst(hdrs, Constants.DEFAULT_TRACE_FIELD);
		Optional<Event.RunType> runType = getFirst(meta, Constants.RUN_TYPE_FIELD)
			.flatMap(type -> Utils.valueOf(Event.RunType.class, type));
		Optional<Instant> timestamp = getFirst(meta, Constants.TIMESTAMP_FIELD)
			.flatMap(Utils::strToTimeStamp);
		Optional<String> method = getFirst(meta, Constants.METHOD_FIELD);
		Optional<String> reqId = getFirst(meta, Constants.DEFAULT_REQUEST_ID);
		RecordingType recordingType = getFirst(meta, Constants.RECORDING_TYPE_FIELD)
						.flatMap(r -> Utils.valueOf(RecordingType.class, r))
						.orElse(RecordingType.Golden);
		String runId = getFirst(meta, Constants.RUN_ID_FIELD).orElse(mdTraceInfo.traceId);

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && (dontCheckCollection
			|| collection
			.isPresent()) && runType.isPresent() && method.isPresent()) {

			HTTPRequestPayload httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams,
				formParams, method.get(), body , apiPath);
			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse(Constants.NOT_APPLICABLE), dontCheckCollection ? Constants.NOT_APPLICABLE : collection.get(),
				mdTraceInfo, runType.get(), timestamp,
				reqId.orElse(Constants.NOT_APPLICABLE),
				apiPath, Event.EventType.HTTPRequest, recordingType).withRunId(runId).withMetaData(eventMetaData);
			eventBuilder.setPayload(httpRequestPayload);
			Event event = eventBuilder.createEvent();

			return event;
		} else {
			LOGGER.info(
				Constants.CUSTOMER_ID_FIELD + customerId.orElse("NOT PRESENT") + "\n" +
					Constants.APP_FIELD + app.orElse("NOT PRESENT") + "\n" +
					Constants.SERVICE_FIELD + service.orElse("NOT PRESENT") + "\n" +
					Constants.COLLECTION_FIELD + collection.orElse("NOT PRESENT") + "\n" +
					"DontCheckCollection" + dontCheckCollection + "\n" +
					Constants.RUN_TYPE_FIELD + runType.map(rt -> rt.toString())
					.orElse("NOT PRESENT") + "\n" +
					Constants.METHOD_FIELD + method.orElse("NOT PRESENT") + "\n");

			throw new Event.EventBuilder.InvalidEventException();
		}

	}


	public static Event createMockedRequestEvent(String apiPath,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> hdrs,
		MDTraceInfo mdTraceInfo, byte[] body,
		String customerId, String app, String service, String instance,
		RunType runType, String method, String reqId, RecordingType recordingType, Optional<Instant> timestamp)
		throws Event.EventBuilder.InvalidEventException {

		Payload mockedRequestPayload = null;
		if (getMimeType(hdrs).orElseGet(()->{
			LOGGER.info("Did not find the Mime-type header in request. Giving default "+MediaType.TEXT_PLAIN);
			return MediaType.TEXT_PLAIN;
		}).toLowerCase()
			.startsWith(Constants.APPLICATION_GRPC)) {
			GRPCRequestPayload grpcRequestPayload = new GRPCRequestPayload(hdrs, body, apiPath, method);
			mockedRequestPayload = grpcRequestPayload;

		} else {
			HTTPRequestPayload httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams,
				formParams, method, body, apiPath);
			mockedRequestPayload = httpRequestPayload;
		}

		Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId, app,
			service, instance, Constants.NOT_APPLICABLE,
			mdTraceInfo, runType, timestamp,
			reqId, apiPath, Event.EventType.HTTPRequest, recordingType);
		eventBuilder.setPayload(mockedRequestPayload);
		Event event = eventBuilder.createEvent();
		return event;
	}

	// TODO: This will go away once we remove storeSingleReqResp from CubeStore
	// this is server side version, used by RealMocker
	public static Event createHTTPRequestEvent(String apiPath, Optional<String> reqId,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs, String method, String body,
		Optional<String> collection, Instant timestamp,
		Optional<Event.RunType> runType, Optional<String> customerId,
		Optional<String> app,
		Comparator comparator, String runId, RecordingType recordingType)
		throws EventBuilder.InvalidEventException {



		HTTPRequestPayload httpRequestPayload;
		// We treat empty body ("") as null
		if (body != null && (!body.isEmpty())) {
			httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams, formParams, method,
				body.getBytes(StandardCharsets.UTF_8), apiPath);
		} else {
			httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams, formParams, method,
				null, apiPath);
		}

		//httpRequestPayload.postParse();

		Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
		Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
		Optional<String> traceId = getFirst(meta, Constants.DEFAULT_TRACE_FIELD);
		Optional<String> spanId = getFirst(meta, Constants.DEFAULT_SPAN_FIELD);
		Optional<String> parentSpanId = getFirst(meta, Constants.DEFAULT_PARENT_SPAN_FIELD);

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && collection.isPresent() && runType.isPresent()) {
			EventBuilder eventBuilder = new EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse("NA"), collection.get(),
				new MDTraceInfo(traceId.orElse(generateTraceId()) , spanId.orElse("NA") , parentSpanId.orElse("NA"))
				, runType.get(), Optional.of(timestamp),
				reqId.orElse("NA"),
				apiPath, Event.EventType.HTTPRequest, recordingType).withRunId(runId);
			eventBuilder.setPayload(httpRequestPayload);
			Event event = eventBuilder.createEvent();
			event.parseAndSetKey(comparator.getCompareTemplate());

			return event;
		} else {
			throw new EventBuilder.InvalidEventException();
		}

	}

	public static MultivaluedMap<String, String> setLowerCaseKeys(
		MultivaluedMap<String, String> mvMap) {
		if (mvMap == null) {
			return null;
		}
		MultivaluedMap<String, String> lowerCaseMVMap = new MultivaluedHashMap<>();
		for (String key : new ArrayList<>(mvMap.keySet())) {
			String lowerCase = key.toLowerCase();
			for (String value : mvMap.get(key)) {
				lowerCaseMVMap.add(lowerCase, value);
			}
		}
		return lowerCaseMVMap;
	}

	public static HTTPRequestPayload getRequestPayload(Event event, ObjectMapper jsonMapper)
		throws IOException, RawPayloadEmptyException, RawPayloadProcessingException {
		String payload = event.payload.getPayloadAsJsonString();
		return jsonMapper.readValue(payload, HTTPRequestPayload.class);
	}

	public static Event createHTTPResponseEvent(String apiPath,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs,
		MDTraceInfo mdTraceInfo,
		byte[] body,
		Optional<String> collection,
		ObjectMapper jsonMapper, boolean isRecordedAtSource)
		throws JsonProcessingException, Event.EventBuilder.InvalidEventException {
		return createHTTPResponseEvent(apiPath, meta, hdrs, mdTraceInfo, body,
			collection, jsonMapper, isRecordedAtSource, Collections.EMPTY_MAP);
	}

	public static Event createHTTPResponseEvent(String apiPath,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs,
		MDTraceInfo mdTraceInfo,
		byte[] body,
		Optional<String> collection,
		ObjectMapper jsonMapper, boolean isRecordedAtSource, Map eventMetaData)
		throws JsonProcessingException, Event.EventBuilder.InvalidEventException {

		Optional<String> customerId = getFirst(meta, Constants.CUSTOMER_ID_FIELD);
		Optional<String> app = getFirst(meta, Constants.APP_FIELD);
		Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
		Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
		//Optional<String> traceId = getFirst(meta, Constants.DEFAULT_TRACE_FIELD);
		Optional<Event.RunType> runType = getFirst(meta, Constants.RUN_TYPE_FIELD)
			.flatMap(type -> Utils.valueOf(Event.RunType.class, type));
		RecordingType recordingType =getFirst(meta, Constants.RECORDING_TYPE_FIELD)
				.flatMap(r -> Utils.valueOf(RecordingType.class, r))
				.orElse(RecordingType.Golden);
		Optional<String> reqId = getFirst(meta, Constants.DEFAULT_REQUEST_ID);
		Optional<Instant> timestamp = getFirst(meta, Constants.TIMESTAMP_FIELD)
			.flatMap(Utils::strToTimeStamp);
		String runId = getFirst(meta, Constants.RUN_ID_FIELD).orElse(mdTraceInfo.traceId);
		Optional<Integer> status = getFirst(meta, Constants.STATUS).flatMap(sval -> {
			try {
				return Optional.of(Integer.valueOf(sval));
			} catch (Exception e) {
				LOGGER.error(String.format("Expecting integer status, got %s", sval));
				return Optional.empty();
			}
		});

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && (isRecordedAtSource
			|| collection
			.isPresent()) && runType.isPresent() && status.isPresent()) {
			HTTPResponsePayload httpResponsePayload = new HTTPResponsePayload(hdrs, status.get(),
				body);
			/*String payloadStr = null;
			final Span span = CommonUtils.startClientSpan("respPayload");
			try (Scope scope = CommonUtils.activateSpan(span)) {
				payloadStr = jsonMapper.writeValueAsString(httpResponsePayload);
			} finally {
				span.finish();
			}*/
			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse(Constants.NOT_APPLICABLE), isRecordedAtSource ? Constants.NOT_APPLICABLE : collection.get(),
				mdTraceInfo, runType.get(), timestamp,
				reqId.orElse(Constants.NOT_APPLICABLE),
				apiPath, Event.EventType.HTTPResponse, recordingType).withRunId(runId).withMetaData(eventMetaData);
			eventBuilder.setPayload(httpResponsePayload);
			Event event = eventBuilder.createEvent();
			return event;
		} else {
			LOGGER.info(
				Constants.CUSTOMER_ID_FIELD + customerId.orElse("NOT PRESENT") + "\n" +
					Constants.APP_FIELD + app.orElse("NOT PRESENT") + "\n" +
					Constants.SERVICE_FIELD + service.orElse("NOT PRESENT") + "\n" +
					Constants.COLLECTION_FIELD + collection.orElse("NOT PRESENT") + "\n" +
					"isRecordedAtSource" + isRecordedAtSource + "\n" +
					Constants.RUN_TYPE_FIELD + runType.map(rt -> {
					return rt.toString();
				}).orElse("NOT PRESENT") + "\n" +
					Constants.STATUS + status.map(s -> String.valueOf(s)).orElse("NOT PRESENT")
					+ "\n");

			throw new Event.EventBuilder.InvalidEventException();
		}

	}

	public static HTTPResponsePayload getResponsePayload(Event event, ObjectMapper jsonMapper)
		throws IOException, RawPayloadEmptyException, RawPayloadProcessingException {
		String payload = event.payload.getPayloadAsJsonString();
		return jsonMapper.readValue(payload, HTTPResponsePayload.class);
	}

	public static MDTraceInfo getTraceInfo(Span currentSpan) {
		JaegerSpanContext spanContext = (JaegerSpanContext) currentSpan.context();

		String traceId = spanContext.getTraceId();
		String spanId = Long.toHexString(spanContext.getSpanId());
		String parentSpanId = Long.toHexString(spanContext.getParentId());
		MDTraceInfo mdTraceInfo = new MDTraceInfo(traceId, spanId, parentSpanId, spanContext.baggageItems());
		return mdTraceInfo;
	}

	private static final List<String> HTTP_CONTENT_TYPE_HEADERS = Arrays.asList("content-type"
		, "Content-type", "Content-Type", "content-Type");

	public static boolean isJsonMimeType(MultivaluedMap<String, String> headers) {
		return HTTP_CONTENT_TYPE_HEADERS.stream()
			.map(headers::getFirst).findFirst().filter(x ->
				x.toLowerCase().trim().startsWith(MediaType.APPLICATION_JSON)).isPresent();
	}

	public  static  Optional<String> getMimeType(MultivaluedMap<String, String> headers) {
		if (headers == null)
			return Optional.empty();
		return HTTP_CONTENT_TYPE_HEADERS.stream()
			.map(headers::getFirst).filter(Objects::nonNull)
			.findFirst().map(x -> x.trim());
	}

	public static boolean startsWithIgnoreCase(String str, String prefix)
	{
		return str.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	public static boolean endsWithIgnoreCase(String str, String suffix)
	{
		int suffixLength = suffix.length();
		return str.regionMatches(true, str.length() - suffixLength, suffix, 0, suffixLength);
	}

	//Referred from io.jaegertracing.internal.propagation
	public static String generateTraceId() {
		long high = random.nextLong();
		long low = random.nextLong();
		char[] result = new char[32];
		int pos = 0;
		writeHexLong(result, pos, high);
		pos += 16;

		writeHexLong(result, pos, low);
		return new String(result);
	}

	// Taken from io.jaegertracing.internal.propagation
	/**
	 * Inspired by {@code okio.Buffer.writeLong}
	 */
	static void writeHexLong(char[] data, int pos, long v) {
		writeHexByte(data, pos + 0, (byte) ((v >>> 56L) & 0xff));
		writeHexByte(data, pos + 2, (byte) ((v >>> 48L) & 0xff));
		writeHexByte(data, pos + 4, (byte) ((v >>> 40L) & 0xff));
		writeHexByte(data, pos + 6, (byte) ((v >>> 32L) & 0xff));
		writeHexByte(data, pos + 8, (byte) ((v >>> 24L) & 0xff));
		writeHexByte(data, pos + 10, (byte) ((v >>> 16L) & 0xff));
		writeHexByte(data, pos + 12, (byte) ((v >>> 8L) & 0xff));
		writeHexByte(data, pos + 14, (byte) (v & 0xff));
	}
	static final char[] HEX_DIGITS =
		{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	static void writeHexByte(char[] data, int pos, byte b) {
		data[pos + 0] = HEX_DIGITS[(b >> 4) & 0xf];
		data[pos + 1] = HEX_DIGITS[b & 0xf];
	}


	private static final long traceIdRandomSeed = System.currentTimeMillis();

	private static final Random random = new Random(traceIdRandomSeed);

	public static String createLogMessasge(Object ...objects) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("{");
	    for (int i=1; i<objects.length; i+=2) {
	        String key = objects[i-1].toString();
	        String val = objects[i].toString();
	        sb.append("\"").append(key).append("\":");
	        sb.append("\"").append(val).append("\"");
	        if (i+2 < objects.length) {
	            sb.append(", ");
	        }
	    }
	    sb.append("}");
	    return sb.toString();
	}

	static public Optional<FnResponse> mockResponseToFnResponse(MockResponse mockResponse)
		throws MockerException {
		return mockResponse.response.map(UtilException.rethrowFunction(retEvent -> {
			try {
				return new FnResponse(
					retEvent.payload.getValAsString(Constants.FN_RESPONSE_PATH),
					Optional.of(retEvent.timestamp),
					Success, Optional.empty(),
					mockResponse.numResults > 1);
			} catch (DataObj.PathNotFoundException e) {
				LOGGER.error(Utils.createLogMessasge(
					Constants.API_PATH_FIELD, retEvent.apiPath), e);
				throw new MockerException(Constants.JSON_PARSING_EXCEPTION,
					"Unable to find response path in json ");
			}
		}));
	}

	public static Event createRequestMockNew(String path, MultivaluedMap<String, String> formParams,
		String customerId, String app, String instanceId, String service,
		String method, byte[] body,
		MultivaluedMap<String, String> headers,
		MultivaluedMap<String, String> queryParams,
		Optional<String> traceIdValue , TracerMgr tracerMgr, Optional<Instant> timestamp) throws EventBuilder.InvalidEventException, JsonProcessingException {
		// At the time of mock, our lua filters don't get deployed, hence no request id is generated
		// we can generate a new request id here in the mock service
		String requestId = service.concat("-mock-").concat(String.valueOf(UUID.randomUUID()));

		MDTraceInfo traceInfo  = tracerMgr.getTraceInfo(headers, customerId, app).orElse(new MDTraceInfo());
		Optional<String> traceId = Optional.ofNullable(traceIdValue.orElse(traceInfo.traceId));
		Optional<String> spanId = Optional.ofNullable(traceInfo.spanId);
		Optional<String> parentSpanId = Optional.ofNullable(traceInfo.parentSpanId);

		RecordingType recordingType = RecordingType.Replay;
		String traceIdData = traceId.orElse(generateTraceId());
		// Special character to be set for parentSpanId
		MDTraceInfo mdTraceInfo = new MDTraceInfo(traceIdData ,
			spanId.orElse("NA") , parentSpanId.orElse(Constants.PARENTSPANID_SPECIAL_CHARACTERS));

		/*byte[] bodyBytes = (body != null && (!body.isEmpty())) ?
			body.getBytes(StandardCharsets.UTF_8) : null;*/

		return createMockedRequestEvent(path, queryParams, formParams, headers, mdTraceInfo,
			body, customerId, app, service, instanceId, RunType.Mock , method, requestId,
			recordingType, timestamp);
	}

	static public String convertTraceId(long traceIdHigh, long traceIdLow) {
	    if (traceIdHigh == 0L) {
	        return Long.toHexString(traceIdLow);
	    }
	    final String hexStringHigh = Long.toHexString(traceIdHigh);
	    final String hexStringLow = Long.toHexString(traceIdLow);
	    if (hexStringLow.length() < 16) {
	        // left pad low trace id with '0'.
	        // In theory, only 12.5% of all possible long values will be padded.
	        // In practice, using Random.nextLong(), only 6% will need padding
	        return hexStringHigh + "0000000000000000".substring(hexStringLow.length()) + hexStringLow;
	    }
	    return hexStringHigh + hexStringLow;
	}

	static public long high(String hexString) {
	    if (hexString.length() > 16) {
	        int highLength = hexString.length() - 16;
	        String highString = hexString.substring(0, highLength);
	        return (new BigInteger(highString, 16)).longValue();
	    } else {
	        return 0L;
	    }
	}

	static public String decodedValue(String value) {
	    try {
	        return URLDecoder.decode(value, "UTF-8");
	    } catch (UnsupportedEncodingException var3) {
	        return value;
	    }
	}

	public static String generateRequestId(String service, String traceId) {
		return service.concat("-").concat(traceId)
				.concat("-").concat(UUID.randomUUID().toString());
	}

	public static Optional<String> extractMethod(Event event) {
		if (event.payload instanceof HTTPRequestPayload) return
				Optional.ofNullable(((HTTPRequestPayload) event.payload).getMethod());
		return Optional.empty();
	}

	public static Optional<MockWithCollection> getMockCollection(DataStore dataStore, String customerId , String app , String instanceId , boolean devtool){
		return dataStore.getCurrentRecordOrReplay(customerId, app, instanceId).map(recordOrReplay->{
			Optional<Replay> runningReplay = recordOrReplay.replay;
			if(!runningReplay.isPresent()){
				LOGGER.error("Could not get replayCollection / recording collection from MockWithCollection " +recordOrReplay);
				return null;
			}
			Replay replay = runningReplay.get();
			String replayCollection = replay.replayId;
			String collection = replay.getCurrentRecording();
			String templateVersion = recordOrReplay.getTemplateVersion();
			String optionalRunId = runningReplay.get().runId;
			Optional<String> dynamicInjectionCfgVersion = recordOrReplay.getDynamicInjectionConfigVersion();

			return new MockWithCollection(replayCollection, collection, templateVersion, optionalRunId, dynamicInjectionCfgVersion, devtool , Optional.of(replay));
		});
	}

	public static void setProtoDescriptorGrpcEvent(Event e, ProtoDescriptorCache protoDescriptorCache) {
		if(!(e.payload instanceof GRPCPayload)) {
			LOGGER.error(Utils.createLogMessasge(
				Constants.MESSAGE, "Payload type not Grpc for event.Cannot set protodescriptor",
				Constants.REQ_ID_FIELD, e.reqId));
			return;
		}
		GRPCPayload ge = (GRPCPayload) e.payload;
		// If run from devtool then set collection as runId to always miss cache and fetch from DB.
		// This is done to ensure consistency in case a new proto has been uploaded
		if(e.getRunType() == RunType.DevTool || e.getRunType() == RunType.DevToolProxy) {
			ge.setProtoDescriptor(protoDescriptorCache.get(
				new ProtoDescriptorKey(e.customerId, e.app, e.runId)));
		}
		else {
			ge.setProtoDescriptor(protoDescriptorCache.get(
				new ProtoDescriptorKey(e.customerId, e.app, e.getCollection())));
		}
	}

	static public String getHttpMethod(Event event) {
		String requestHttpMethod;
		try {
			requestHttpMethod = event.payload.getValAsString(Constants.METHOD_PATH);
		} catch (DataObj.PathNotFoundException e) {
			LOGGER
					.error("Cannot find /method in request" + event.reqId + " No extraction", e);
			requestHttpMethod = "";
		}
		return requestHttpMethod;
	}
	public static JsonNode convertStringToNode(String value, ObjectMapper jsonMapper)
	{
		try {
			return  jsonMapper.readTree(value);
		} catch (IOException e) {
			return new TextNode(value);
		}
	}

	public static <T> void ifPresentOrElse(Optional<T> val , Consumer<? super T> action, Runnable emptyAction ){
		if (val.isPresent()) {
			action.accept(val.get());
		} else {
			emptyAction.run();
		}
	}

	/*public static String constructTemplateSetVersion(String templateSetName, Optional<String> templateSetLabel) {
		return templateSetName + templateSetLabel.map(l -> l.isEmpty() ? "" : "::" + l).orElse("");
	}*/

	public static String createTemplateSetVersion(String templateSetName, String templateSetLabel) {
		return templateSetName + (!templateSetLabel.isEmpty() ? "::" + templateSetLabel : "");
	}

	public static Pair<String, String> extractTemplateSetNameAndLabel(String templateSetVersion) {
		String[] splits = templateSetVersion.split("::");
		if (splits.length == 1 ) return Pair.of(templateSetVersion, "");
		return Pair.of(splits[0] , splits[1]);
	}


	public static final DateTimeFormatter templateLabelFormatter =  DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss_SSS");

	public static TemplateKey getTemplateKey(Event e , String templateVersion , Optional<Type> type){
		Type t = type.orElse(e.payload instanceof RequestPayload ? Type.RequestCompare : Type.ResponseCompare);
		return  new TemplateKey(templateVersion, e.customerId,  e.app, e.service, e.apiPath, t,  Utils.extractMethod(e), e.getCollection());
	}

	/*
	public static CompareTemplate getCompareTemplate(Event e, String templateVersion, DataStore rrstore , String recordingId)
		throws TemplateNotFoundException {
		Type t = e.payload instanceof RequestPayload ? Type.RequestCompare : Type.ResponseCompare;
		return rrstore.getTemplate(e.customerId , e.app , e.service , e.apiPath ,  templateVersion , t , Optional.of(e.eventType)  , Utils.extractMethod(e) , e.getCollection() );
	}
	*/

}
