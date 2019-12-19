package io.md.utils;

import java.io.IOException;
import java.time.Instant;
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
import io.md.core.Comparator;
import io.md.dao.Event;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;

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

	public static Event createHTTPRequestEvent(String apiPath, Optional<String> reqId,
		MultivaluedMap<String, String> queryParams,
		MultivaluedMap<String, String> formParams,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs, String method, String body,
		Optional<String> collection, Instant timestamp,
		Optional<Event.RunType> runType, Optional<String> customerId,
		Optional<String> app,
		ObjectMapper jsonMapper,
		Comparator comparator)
		throws JsonProcessingException, Event.EventBuilder.InvalidEventException {
		HTTPRequestPayload httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams,
			formParams, method, body);

		String payloadStr = jsonMapper.writeValueAsString(httpRequestPayload);

		Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
		Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
		Optional<String> traceId = getFirst(hdrs, Constants.DEFAULT_TRACE_FIELD);

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && collection
			.isPresent() && runType.isPresent()) {
			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse("NA"), collection.get(),
				traceId.orElse("NA"), runType.get(), timestamp,
				reqId.orElse("NA"),
				apiPath, Event.EventType.HTTPRequest);
			eventBuilder.setRawPayloadString(payloadStr);
			Event event = eventBuilder.createEvent();
			//event.parseAndSetKey(config, comparator.getCompareTemplate());

			return event;
		} else {
			throw new Event.EventBuilder.InvalidEventException();
		}

	}

	public static HTTPRequestPayload getRequestPayload(Event event, ObjectMapper jsonMapper)
		throws IOException {
		String payload = event.getPayloadAsJsonString(Map.of(Constants.OBJECT_MAPPER, jsonMapper));
		return jsonMapper.readValue(payload, HTTPRequestPayload.class);
	}


	public static Event createHTTPResponseEvent(String apiPath, Optional<String> reqId,
		Integer status,
		MultivaluedMap<String, String> meta,
		MultivaluedMap<String, String> hdrs,
		String body,
		Optional<String> collection, Instant timestamp,
		Optional<Event.RunType> runType, Optional<String> customerId,
		Optional<String> app,
		ObjectMapper jsonMapper)
		throws JsonProcessingException, Event.EventBuilder.InvalidEventException {
		HTTPResponsePayload httpResponsePayload = new HTTPResponsePayload(hdrs, status, body);

		String payloadStr = jsonMapper.writeValueAsString(httpResponsePayload);

		Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
		Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
		Optional<String> traceId = getFirst(meta, Constants.DEFAULT_TRACE_FIELD);

		if (customerId.isPresent() && app.isPresent() && service.isPresent() && collection
			.isPresent() && runType.isPresent()) {
			Event.EventBuilder eventBuilder = new Event.EventBuilder(customerId.get(), app.get(),
				service.get(), instance.orElse("NA"), collection.get(),
				traceId.orElse("NA"), runType.get(), timestamp,
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
