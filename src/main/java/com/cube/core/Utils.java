/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.JsonNode;

import io.cube.agent.FnReqResponse;
import io.cube.agent.UtilException;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.ValidateCompareTemplate;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.MDTraceInfo;

import com.cube.cache.ComparatorCache;
import com.cube.cache.ComparatorCache.TemplateNotFoundException;
import com.cube.cache.TemplateKey;
import com.cube.cache.TemplateKey.Type;
import com.cube.dao.Recording;
import com.cube.dao.ReqRespStore;
import com.cube.golden.TemplateSet;
import com.cube.utils.Constants;
import com.cube.ws.Config;


/**
 * @author prasad
 *
 */
public class Utils {

    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    private static final long traceIdRandomSeed = System.currentTimeMillis();

    private static final Random random = new Random(traceIdRandomSeed);

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

	public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
		CompletableFuture<Void> allDoneFuture =
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
		return allDoneFuture.thenApply(v ->
				futures.stream().
						map(future -> future.join()).
						collect(Collectors.<T>toList())
		);
	}

	public static ValidateCompareTemplate validateTemplateSet(TemplateSet templateSet) {
        return templateSet.templates.stream().map(CompareTemplateVersioned::validate)
            .filter(v -> !v.isValid())
            .findFirst()
            .orElseGet(() -> new ValidateCompareTemplate(true, Optional.of("")));
	}


    public static IntNode intToJson(Integer val) {
		return IntNode.valueOf(val);
    }

	public static TextNode strToJson(String val) {
		return TextNode.valueOf(val);
	}

    public static Pattern analysisTimestampPattern = Pattern.compile("\\\\\"timestamp\\\\\":\\d{13},");
	public static Pattern recordingTimestampPattern = Pattern.compile(",\"timestamp_dt\":\\{\"name\":\"timestamp_dt\",\"value\":\".+\"\\}");

    public static Pattern replayMetaIdPattern = Pattern.compile("\"id\":\\{\"name\":\"id\",\"value\":\"(.+?)\"},");
    public static Pattern replayIdPattern = Pattern.compile("\"replayid_s\":\\{\"name\":\"replayid_s\",\"value\":\"(.+?)\"},");
    public static Pattern timestampIdPattern = Pattern.compile(",\"creationtimestamp_s\":\\{\"name\":\"creationtimestamp_s\",\"value\":\"(.+?)\"}");
    public static Pattern versionPattern = Pattern.compile(",\"version_s\":\\{\"name\":\"version_s\"}");

    public static String removePatternFromString(String val, Pattern pattern) {
	    Matcher matcher = pattern.matcher(val);
	    return matcher.replaceAll("");
    }

    public static void preProcess(FnReqResponse fnReqResponse) {
	    try {
            if (fnReqResponse.name.equals("add")
                && fnReqResponse.argVals.length > 0) {
                if (fnReqResponse.argVals[0].contains("\"type_s\":{\"name\":\"type_s\",\"value\":\"Analysis\"}")) {
                    String newVal = removePatternFromString(fnReqResponse.argVals[0], analysisTimestampPattern);
                    fnReqResponse.argVals[0] = newVal;
                    fnReqResponse.argsHash[0] = newVal.hashCode();
                } else if (fnReqResponse.argVals[0].contains("{\"type_s\":{\"name\":\"type_s\",\"value\":\"Recording\"}")) {
                    String newVal = removePatternFromString(fnReqResponse.argVals[0], recordingTimestampPattern);
                    fnReqResponse.argVals[0] = newVal;
                    fnReqResponse.argsHash[0] = newVal.hashCode();
                } else if (fnReqResponse.argVals[0].startsWith("{\"id\":{\"name\":\"id\",\"value\":\"ReplayMeta-")) {
                    String newVal = removePatternFromString(fnReqResponse.argVals[0], replayMetaIdPattern);
                    newVal = removePatternFromString(newVal, replayIdPattern);
                    newVal = removePatternFromString(newVal, timestampIdPattern);
                    newVal = removePatternFromString(newVal, versionPattern);
                    fnReqResponse.argVals[0] = newVal;
                    fnReqResponse.argsHash[0] = newVal.hashCode();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while preprocessing fn req resp object :: " + e.getMessage());
        }
    }

    public static JsonNode convertArrayToObject(JsonNode node){
        if (node.isArray()) {
            ArrayNode nodeAsArray = (ArrayNode) node;
            ObjectNode equivalentObjNode = JsonNodeFactory.instance.objectNode();
            for (int i = 0 ; i < nodeAsArray.size() ; i++){
                equivalentObjNode.set(String.valueOf(i), convertArrayToObject(nodeAsArray.get(i)));
            }
            return equivalentObjNode;
        } else if (node.isObject()) {
            ObjectNode nodeAsObject = (ObjectNode) node;
            ObjectNode equivalentObjNode = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = nodeAsObject.fieldNames();
            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                equivalentObjNode.set(fieldName, convertArrayToObject(nodeAsObject.get(fieldName)));
            }
            return equivalentObjNode;
        }
        return node;
    }


    static public TemplateSet templateRegistriesToTemplateSet(TemplateRegistries registries,
                                                              String customerId, String appId,
                                                              Optional<String> templateVersion) {
        List<TemplateRegistry> templateRegistries = registries.getTemplateRegistryList();

        List<CompareTemplateVersioned> compareTemplateVersionedList =
            templateRegistries
                .stream()
                .map(registry -> new CompareTemplateVersioned(Optional.of(registry.getService()),
            Optional.of(registry.getPath()), registry.getType(), registry.getTemplate()))
                .collect(Collectors.toList());

        // pass null for version if version is empty and timestamp so that new version number is created automatically
        TemplateSet templateSet = new TemplateSet(templateVersion.orElse(null), customerId, appId, null,
            compareTemplateVersionedList , Optional.empty());

        return templateSet;

    }

    static Pattern templateKeyPattern = Pattern.compile("TemplateKey\\{customerId=(.+?),"
	    + " appId=(.+?), serviceId=(.+?), path=(.+?), version=(.+?), type=(.+?)}");

    /**
     * https://stackoverflow.com/questions/7498030/append-relative-url-to-java-net-url
     * @param baseUrl Base Url
     * @param suffix Relative path to append to the base url
     * @return Concatenated Normalized Path (// are converted to /)
     * @throws Exception Exception if Any
     */
    static public String appendUrlPath(String baseUrl, String suffix) throws Exception {
        URIBuilder uriBuilder = new URIBuilder(baseUrl);
        return uriBuilder.setPath(uriBuilder.getPath() + "/" + suffix)
            .build().normalize().toString();
    }

    public static String buildSuccessResponse(String status, JSONObject data) {
        JSONObject successResponse = new JSONObject();
        successResponse.put(Constants.STATUS, status);
        successResponse.put(Constants.DATA, data);

        return successResponse.toString();
    }

    public static String buildErrorResponse(String status, String msgId, String msg) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put(Constants.STATUS, status);

        JSONObject data = new JSONObject();
        data.put(Constants.MESSAGE_ID, msgId);
        data.put(Constants.MESSAGE, msg);

        errorResponse.put(Constants.DATA, data);

        return errorResponse.toString();
    }

    public static Map<String,Object> extractThriftParams(String thriftApiPath) {
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

    public static Optional<String> getFirst(MultivaluedMap<String, String> fieldMap, String fieldname) {
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
                                               Comparator comparator)
	    throws JsonProcessingException, EventBuilder.InvalidEventException {

	    HTTPRequestPayload httpRequestPayload;
	    // We treat empty body ("") as null
	    if (body != null && (!body.isEmpty())) {
		    httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams, formParams, method,
			    body.getBytes(StandardCharsets.UTF_8));
	    } else {
		    httpRequestPayload = new HTTPRequestPayload(hdrs, queryParams, formParams, method,
			    null);
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

    /*public static HTTPRequestPayload getRequestPayload(Event event, Config config)
	    throws IOException, RawPayloadEmptyException, RawPayloadProcessingException {
        String payload = event.getPayloadAsJsonString();
        return config.jsonMapper.readValue(payload, HTTPRequestPayload.class);
    }*/


    public static Event createHTTPResponseEvent(String apiPath, Optional<String> reqId,
                                                Integer status,
                                                MultivaluedMap<String, String> meta,
                                                MultivaluedMap<String, String> hdrs,
                                                String body,
                                                Optional<String> collection, Instant timestamp,
                                                Optional<Event.RunType> runType, Optional<String> customerId,
                                                Optional<String> app,
                                                Config config) throws JsonProcessingException, EventBuilder.InvalidEventException {
	    HTTPResponsePayload httpResponsePayload;
	    // We treat empty body ("") as null
	    if (body != null && (!body.isEmpty())) {
		    httpResponsePayload = new HTTPResponsePayload(hdrs, status, body.getBytes(StandardCharsets.UTF_8));
	    } else {
		    httpResponsePayload = new HTTPResponsePayload(hdrs, status, null);
	    }

	    Optional<String> service = getFirst(meta, Constants.SERVICE_FIELD);
        Optional<String> instance = getFirst(meta, Constants.INSTANCE_ID_FIELD);
        Optional<String> traceId = getFirst(meta, Constants.DEFAULT_TRACE_FIELD);

        if (customerId.isPresent() && app.isPresent() && service.isPresent() && collection.isPresent() && runType.isPresent()) {
            EventBuilder eventBuilder = new EventBuilder(customerId.get(), app.get(),
                service.get(), instance.orElse("NA"), collection.get(),
                new MDTraceInfo(traceId.orElse(reqId.flatMap(config.rrstore::getRequestEvent)
	                .map(Event::getTraceId).orElse("NA")), null, null),
                runType.get(), Optional.of(timestamp),
                reqId.orElse("NA"),
                apiPath, Event.EventType.HTTPResponse);
            eventBuilder.setPayload(httpResponsePayload);
            Event event = eventBuilder.createEvent();
            return event;
        } else {
            throw new EventBuilder.InvalidEventException();
        }

    }

    /*public static HTTPResponsePayload getResponsePayload(Event event, Config config)
	    throws IOException, RawPayloadEmptyException, RawPayloadProcessingException {
    	return (HTTPResponsePayload) event.payload;
    }*/

	public static Map<String, TemplateEntry> getAllPathRules(Event event, Recording recording, TemplateKey.Type templateKeyType,
		String service, String apiPath, ReqRespStore rrstore, Config config) {
		TemplateKey tkey = new TemplateKey(recording.templateVersion, recording.customerId, recording.app, service, apiPath,
			templateKeyType);

		Optional<CompareTemplate> templateOptional = rrstore.getCompareTemplate(tkey);
		Map<String, TemplateEntry> pathRules = new HashMap<>();
		templateOptional.ifPresent(UtilException.rethrowConsumer(template -> {
			event.payload.getPathRules(template, pathRules);
		}));

		return pathRules;
    }

    //Referred from io.jaegertracing.internal.propagation

    private static String generateTraceId() {
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
}
