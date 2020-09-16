/**
 * Copyright Cube I O
 */
package io.md.core;


import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.md.dao.RecordOrReplay;
import io.md.services.DataStore;
import io.md.utils.Constants;


/**
 * @author prasad
 *
 */
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
                /*"referer",*/ "upgrade",
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
			LOGGER.error("Error while parsing int",e);
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


    public static Optional<Instant> msStrToTimeStamp(String val) {
	    try {
	        return strToLong(val).map(Instant::ofEpochMilli);
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


   public static <K,T> List<T> getFromMVMapAsOptional(MultivaluedMap<K, T> map, K key) {
	  return Optional.ofNullable(map.get(key)).orElse(Collections.emptyList());
  }

    static public Optional<Response> checkActiveCollection(DataStore dataStore, String customerId,
                                                           String app, String instanceId,
                                                           Optional<String> userId) {
        Optional<RecordOrReplay> recordOrReplay = dataStore.getCurrentRecordOrReplay(customerId, app,
            instanceId);
        Optional<String> rrcollection = recordOrReplay.flatMap(rr -> rr.getRecordingCollection());
        Optional<String> replayId = recordOrReplay.flatMap(rr -> rr.getReplayId());
        Optional<String> recordingId = recordOrReplay.flatMap(rr -> rr.getRecordingId());
        String runType = recordOrReplay.map(rr -> rr.isRecording() ? "Recording" : "Replay").orElse("None");

        return rrcollection.map(collection -> {
            // TODO: use constant strings from Ashok's PR once its merged
            Map<String, String> respObj = Map.of("message", runType + " ongoing",
                "customerId", customerId,
                "app", app,
                "instance", instanceId,
                "collection", collection,
                "replayId", replayId.orElse("None"),
	            "recordingId", recordingId.orElse("None"),
                "userId", userId.orElse("None"));
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(respObj)
                .build();
        });
    }

    public static class BadValueException extends Exception {
        public BadValueException(String message) {
            super(message);
        }
    }
}
