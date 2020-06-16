package io.md.utils;

import static io.md.dao.FnReqRespPayload.RetStatus.Success;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.core.Comparator;
import io.md.dao.DataObj;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.RunType;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.MDTraceInfo;
import io.md.dao.RawPayload.RawPayloadEmptyException;
import io.md.dao.RawPayload.RawPayloadProcessingException;
import io.md.services.FnResponse;
import io.md.services.MockResponse;
import io.md.services.Mocker.MockerException;
import io.opentracing.Span;

public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

	// Assumes name is not null
	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
		return EnumSet.allOf(clazz).stream()
			.filter(v -> v.name().toLowerCase().equals(name.toLowerCase()))
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
			"referer", "upgrade",
			"via", "warning", "transfer-encoding")));
		DISALLOWED_HEADERS_SET = Collections.unmodifiableSet(treeSet);
	}

	public static final Predicate<String>
		ALLOWED_HEADERS = (header) -> !DISALLOWED_HEADERS_SET.contains(header);

	/**
	 * @param intStr
	 * @return
	 */
	public static Optional<Integer> strToInt(String intStr) {
		try {
			return Optional.ofNullable(intStr).map(Integer::valueOf);
		} catch (Exception e) {
			return Optional.empty();
		}
	}


	public static Optional<Double> strToDouble(String dblStr) {
		try {
			return Optional.ofNullable(dblStr).map(Double::valueOf);
		} catch (Exception e) {
			return Optional.empty();
		}
	}


	public static Optional<Long> strToLong(String longStr) {
		try {
			return Optional.ofNullable(longStr).map(Long::valueOf);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static Optional<Instant> strToTimeStamp(String val) {
		try {
			return Optional.of(Instant.parse(val)); // parse cannot return null
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static Optional<Boolean> strToBool(String boolStr) {
		try {
			return Optional.ofNullable(boolStr).map(BooleanUtils::toBoolean);
		} catch (Exception e) {
			return Optional.empty();
		}
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

	// TODO: jsonMapper can be removed. This will need change in all the interceptors
	public static Event createHTTPRequestEvent(String apiPath,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs, MDTraceInfo mdTraceInfo, byte[] body,
		Optional<String> collection,
		ObjectMapper jsonMapper, boolean dontCheckCollection)
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

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && (dontCheckCollection
			|| collection
			.isPresent()) && runType.isPresent() && method.isPresent()) {

			HTTPRequestPayload httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams,
				formParams, method.get(), body , apiPath);

			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse(Constants.NOT_APPLICABLE), dontCheckCollection ? Constants.NOT_APPLICABLE : collection.get(),
				mdTraceInfo, runType.get(), timestamp,
				reqId.orElse(Constants.NOT_APPLICABLE),
				apiPath, Event.EventType.HTTPRequest);
			eventBuilder.setPayload(httpRequestPayload);
			Event event = eventBuilder.createEvent();

			return event;
		} else {
			throw new Event.EventBuilder.InvalidEventException();
		}

	}


	public static Event createHTTPRequestEvent(String apiPath,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> hdrs,
		MDTraceInfo mdTraceInfo, byte[] body,
		String customerId, String app, String service, String instance,
		RunType runType, String method, String reqId)
		throws Event.EventBuilder.InvalidEventException {

			HTTPRequestPayload httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams,
				formParams, method, body , apiPath);

			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId, app,
				service, instance, Constants.NOT_APPLICABLE,
				mdTraceInfo, runType, Optional.empty(),
				reqId, apiPath, Event.EventType.HTTPRequest);
			eventBuilder.setPayload(httpRequestPayload);
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
		Comparator comparator)
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
				apiPath, Event.EventType.HTTPRequest);
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
		String payload = event.getPayloadAsJsonString();
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

		Optional<String> customerId = getFirst(meta, Constants.CUSTOMER_ID_FIELD);
		Optional<String> app = getFirst(meta, Constants.APP_FIELD);
		Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
		Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
		//Optional<String> traceId = getFirst(meta, Constants.DEFAULT_TRACE_FIELD);
		Optional<Event.RunType> runType = getFirst(meta, Constants.RUN_TYPE_FIELD)
			.flatMap(type -> Utils.valueOf(Event.RunType.class, type));
		Optional<String> reqId = getFirst(meta, Constants.DEFAULT_REQUEST_ID);
		Optional<Instant> timestamp = getFirst(meta, Constants.TIMESTAMP_FIELD)
			.flatMap(Utils::strToTimeStamp);
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
				apiPath, Event.EventType.HTTPResponse);
			eventBuilder.setPayload(httpResponsePayload);
			Event event = eventBuilder.createEvent();
			return event;
		} else {
			throw new Event.EventBuilder.InvalidEventException();
		}

	}

	public static HTTPResponsePayload getResponsePayload(Event event, ObjectMapper jsonMapper)
		throws IOException, RawPayloadEmptyException, RawPayloadProcessingException {
		String payload = event.getPayloadAsJsonString();
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
		/*, "Content-type", "Content-Type", "content-Type"*/);

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
			.findFirst().map(x -> x.toLowerCase().trim());
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
		String method, String body,
		MultivaluedMap<String, String> headers, MultivaluedMap<String, String> queryParams) throws EventBuilder.InvalidEventException, JsonProcessingException {
		// At the time of mock, our lua filters don't get deployed, hence no request id is generated
		// we can generate a new request id here in the mock service
		String requestId = service.concat("-mock-").concat(String.valueOf(UUID.randomUUID()));

		MultivaluedMap<String, String> meta = new MultivaluedHashMap<>();

		setSpanTraceIDParentSpanInMeta(meta, headers, app);
		Optional<String> traceId = getFirst(meta, Constants.DEFAULT_TRACE_FIELD);
		Optional<String> spanId = getFirst(meta, Constants.DEFAULT_SPAN_FIELD);
		Optional<String> parentSpanId = getFirst(meta, Constants.DEFAULT_PARENT_SPAN_FIELD);
		MDTraceInfo mdTraceInfo = new MDTraceInfo(traceId.orElse(generateTraceId()) ,
			spanId.orElse("NA") , parentSpanId.orElse("NA"));

		byte[] bodyBytes = (body != null && (!body.isEmpty())) ?
			body.getBytes(StandardCharsets.UTF_8) : null;

		return createHTTPRequestEvent(path, queryParams, formParams, headers, mdTraceInfo,
			bodyBytes, customerId, app, service, instanceId, RunType.Replay, method, requestId);
	}


	static private void setSpanTraceIDParentSpanInMeta(MultivaluedMap<String, String> meta, MultivaluedMap<String, String> headers,
			String app) {
	    String mdTrace = headers.getFirst(CommonUtils.getDFSuffixBasedOnApp(Constants.MD_TRACE_FIELD, app));
	    if (mdTrace != null && !mdTrace.equals("")) {
	        String[] parts = decodedValue(mdTrace).split(":");
	        if (parts.length != 4) {
	            LOGGER.warn("trace id should have 4 parts but found: " + parts.length);
	            return;
	        } else {
	            String traceId = parts[0];
	            if (traceId.length() <= 32 && traceId.length() >= 1) {
	                meta.putSingle(Constants.DEFAULT_SPAN_FIELD, Long.toHexString((new BigInteger(parts[1], 16)).longValue()));
	                meta.putSingle(Constants.DEFAULT_TRACE_FIELD,
		                convertTraceId(high(parts[0]), (new BigInteger(parts[0], 16)).longValue()));
	            } else {
	                LOGGER.error("Trace id [" + traceId + "] length is not within 1 and 32");
	            }
	        }
	    } else if ( headers.getFirst(Constants.DEFAULT_TRACE_FIELD) != null ) {
	        meta.putSingle(Constants.DEFAULT_TRACE_FIELD, headers.getFirst(Constants.DEFAULT_TRACE_FIELD));
	        if ( headers.getFirst(Constants.DEFAULT_SPAN_FIELD) != null) {
	            meta.putSingle(Constants.DEFAULT_SPAN_FIELD, decodedValue(headers.getFirst(Constants.DEFAULT_SPAN_FIELD)));
	        }
	    } else {
	        LOGGER.warn("Neither default not md trace id header found to the mock sever request");
	    }

	    String mdParentSpanHdr = CommonUtils.getDFSuffixBasedOnApp(Constants.MD_BAGGAGE_PARENT_SPAN, app);
	    if (headers.getFirst(mdParentSpanHdr) != null ) {
	        meta.putSingle(Constants.DEFAULT_PARENT_SPAN_FIELD, decodedValue(headers.getFirst(mdParentSpanHdr)));
	    } else if (headers.getFirst(Constants.DEFAULT_BAGGAGE_PARENT_SPAN) != null ) {
	        meta.putSingle(Constants.DEFAULT_PARENT_SPAN_FIELD, decodedValue(headers.getFirst(Constants.DEFAULT_BAGGAGE_PARENT_SPAN)));
	    } else {
	        LOGGER.warn("Neither default not md baggage parent span id header found to the mock sever request");
	    }
	}

	static private String convertTraceId(long traceIdHigh, long traceIdLow) {
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

	static private long high(String hexString) {
	    if (hexString.length() > 16) {
	        int highLength = hexString.length() - 16;
	        String highString = hexString.substring(0, highLength);
	        return (new BigInteger(highString, 16)).longValue();
	    } else {
	        return 0L;
	    }
	}

	static private String decodedValue(String value) {
	    try {
	        return URLDecoder.decode(value, "UTF-8");
	    } catch (UnsupportedEncodingException var3) {
	        return value;
	    }
	}
}
