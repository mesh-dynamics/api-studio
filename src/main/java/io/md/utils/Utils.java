package io.md.utils;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.MDTraceInfo;
import io.opentracing.Scope;
import io.opentracing.Span;

public class Utils {

	private static final Logger LOGGER = LogManager.getLogger(Utils.class);

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
		treeSet.addAll(Set.of("connection", "content-length",
			"date", "expect", "from", "host", "origin",
			"referer", "upgrade",
			"via", "warning", "transfer-encoding"));
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


	public static Map<String, Object> extractThriftParams(String thriftApiPath) {
		Map<String, Object> params = new HashMap<>();
		if (thriftApiPath != null) {
			String[] splitResult = thriftApiPath.split("::");
			String methodName = splitResult[0];
			String argsClassName = splitResult[1];
			params.put(Constants.THRIFT_METHOD_NAME, methodName);
			params.put(Constants.THRIFT_CLASS_NAME, argsClassName);
		}
		return params;
	}

	public static Optional<String> getFirst(MultivaluedMap<String, String> fieldMap,
		String fieldname) {
		return Optional.ofNullable(fieldMap.getFirst(fieldname));
	}

	public static Event createHTTPRequestEvent(String apiPath,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs, MDTraceInfo mdTraceInfo, String body,
		Optional<String> collection,
		ObjectMapper jsonMapper, boolean isRecordedAtSource)
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

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && (isRecordedAtSource
			|| collection
			.isPresent()) && runType.isPresent() && method.isPresent()) {

			HTTPRequestPayload httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams,
				formParams, method.get(), body);

			String payloadStr = null;
			final Span span = CommonUtils.startClientSpan("reqPayload");
			try (Scope scope = CommonUtils.activateSpan(span)) {
				payloadStr = jsonMapper.writeValueAsString(httpRequestPayload);
			} finally {
				span.finish();
			}

			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse("NA"), isRecordedAtSource ? "NA" : collection.get(),
				mdTraceInfo, runType.get(), timestamp,
				reqId.orElse("NA"),
				apiPath, Event.EventType.HTTPRequest);
			eventBuilder.setRawPayloadString(payloadStr);
			Event event = eventBuilder.createEvent();
			//TODO keep this logic on cube end
			//event.parseAndSetKey(config, comparator.getCompareTemplate());

			return event;
		} else {
			throw new Event.EventBuilder.InvalidEventException();
		}

	}

	public static MultivaluedMap<String, String> setLowerCaseKeys(MultivaluedMap<String, String> mvMap) {
		MultivaluedMap<String, String> lowerCaseMVMap = new MultivaluedHashMap<>();
		for (String key : new ArrayList<String>(mvMap.keySet())) {
			String lowerCase = key.toLowerCase();
			for (String value : mvMap.remove(key))
				lowerCaseMVMap.add(lowerCase, value);
		}
		return lowerCaseMVMap;
	}

	public static HTTPRequestPayload getRequestPayload(Event event, ObjectMapper jsonMapper)
		throws IOException {
		String payload = event.getPayloadAsJsonString(Map.of(Constants.OBJECT_MAPPER, jsonMapper));
		return jsonMapper.readValue(payload, HTTPRequestPayload.class);
	}

	public static Event createHTTPResponseEvent(String apiPath,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs,
		MDTraceInfo mdTraceInfo,
		String body,
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
			String payloadStr = null;
			final Span span = CommonUtils.startClientSpan("respPayload");
			try (Scope scope = CommonUtils.activateSpan(span)) {
				payloadStr = jsonMapper.writeValueAsString(httpResponsePayload);
			} finally {
				span.finish();
			}
			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse("NA"), isRecordedAtSource ? "NA" : collection.get(),
				mdTraceInfo, runType.get(), timestamp,
				reqId.orElse("NA"),
				apiPath, Event.EventType.HTTPResponse);
			eventBuilder.setRawPayloadString(payloadStr);
			Event event = eventBuilder.createEvent();
			return event;
		} else {
			throw new Event.EventBuilder.InvalidEventException();
		}

	}

	public static HTTPResponsePayload getResponsePayload(Event event, ObjectMapper jsonMapper)
		throws IOException {
		String payload = event.getPayloadAsJsonString(Map.of(Constants.OBJECT_MAPPER, jsonMapper));
		return jsonMapper.readValue(payload, HTTPResponsePayload.class);
	}

}
