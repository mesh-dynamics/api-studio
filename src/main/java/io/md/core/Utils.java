/**
 * Copyright Cube I O
 */
package io.md.core;


import static io.md.utils.Utils.ALLOWED_HEADERS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.swing.text.html.Option;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.uri.UriComponent;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.md.dao.Event;
import io.md.dao.RecordOrReplay;
import io.md.dao.Replay;
import io.md.dao.RequestDetails;
import io.md.dao.RequestPayload;
import io.md.services.DataStore;
import io.md.utils.Constants;
import io.md.utils.UtilException;


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

	/**
	 * @param intStr
	 * @return
	 */
	public static Optional<Integer> strToInt(String intStr) {
		return io.md.utils.Utils.strToInt(intStr);
	}


	public static Optional<Double> strToDouble(String dblStr) {
        return io.md.utils.Utils.strToDouble(dblStr);
	}


    public static Optional<Long> strToLong(String longStr) {
        return io.md.utils.Utils.strToLong(longStr);
    }

    public static Optional<Instant> strToTimeStamp(String val) {
        return io.md.utils.Utils.strToTimeStamp(val);
    }


    public static Optional<Instant> msStrToTimeStamp(String val) {
	    return io.md.utils.Utils.strToTimeStamp(val);
    }


    public static Optional<Boolean> strToBool(String boolStr) {
        return io.md.utils.Utils.strToBool(boolStr);
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

        return recordOrReplay.map(rr -> {
            // TODO: use constant strings from Ashok's PR once its merged
            Map<String, Object> respObj = Map.of("message", rr.isRecording() ? "Recording" : "Replay" + " ongoing",
                "customerId", customerId,
                "app", app,
                "instance", instanceId,
                "collection", rr.getCollection(),
                 "recordOrReplay", rr,
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

    public static Optional<byte[]> decodeResponseBody(byte[] originalBody , String encoding){

    	try{
		    switch (encoding){
			    case "":
				    return Optional.of(originalBody);
			    case "gzip":
				    byte[] body =  new GZIPInputStream(new ByteArrayInputStream(originalBody)).readAllBytes();
				    return Optional.of(body);
			    default:
				    throw new UnsupportedOperationException("Unexpected Content-Encoding: " + encoding);
		    }
	    }catch (Exception e){
		    LOGGER.error("Stream reading error",e);
		    return Optional.empty();
	    }
    }

	public static RequestDetails buildRequestDetails(Replay replay, Event reqEvent, RequestPayload httpRequest){

		List<String> pathSegments = httpRequest
			.getValAsObject(Constants.PATH_SEGMENTS_PATH, List.class)
			.orElse(Collections.EMPTY_LIST);

		if (!pathSegments.isEmpty()) {
			try {
				reqEvent.apiPath = pathSegments.stream().collect(Collectors.joining("/"));
			} catch (Exception e) {
				LOGGER
					.error("Cannot form apiPath from pathSegments. Resolving to event apiPath",
						e);
			}
		} else {
			LOGGER.error("pathSegments not found. Resolving to event apiPath");
		}
		String apiPath = reqEvent.apiPath;
		UriBuilder uribuilder = UriBuilder.fromUri(replay.endpoint)
			.path(apiPath);

		MultivaluedHashMap<String, String> queryParams = httpRequest
			.getValAsObject(Constants.QUERY_PARAMS_PATH, MultivaluedHashMap.class)
			.orElse(new MultivaluedHashMap<String, String>());

		queryParams.forEach(UtilException.rethrowBiConsumer((k, vlist) -> {
			String[] params = vlist.stream().map(UtilException.rethrowFunction(v -> {
				return UriComponent
					.encode(v, UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);
				// return URLEncoder.encode(v, "UTF-8"); // this had a problem of
				// encoding space as +, which further gets encoded as %2B
			})).toArray(String[]::new);
			uribuilder.queryParam(k, (Object[]) params);
		}));


		byte[] requestBody = httpRequest.getBody();
		URI uri = uribuilder.build();

		LOGGER.debug("PATH :: " + uri.toString() + " OUTGOING REQUEST BODY :: " + new String(requestBody,
			StandardCharsets.UTF_8));

		// Fetch headers/queryParams and path etc from payload since injected value
		// would be present in dataObj instead of payload fields

		//TODO - HTTPHeaders and queryParams don't support types
		// they have to be string so add validation for that,
		// also add validation to be an array even if a singleton
		// because of jackson serialisation to Multivalued map

		// NOTE - HEADERS SHOULD BE READ AND SET AFTER SETTING THE BODY BECAUSE WHILE DOING GETBODY()
		// THE HEADERS MIGHT GET UPDATED ESPECIALLY IN CASE OF MULTIPART DATA WHERE WE SET NEW CONTENT-TYPE
		// HEADER WHILE WRAPPING THE BODY

		MultivaluedHashMap<String, String> headers = httpRequest
			.getValAsObject(Constants.HDR_PATH, MultivaluedHashMap.class)
			.orElse(new MultivaluedHashMap<String, String>());

		for(String k : headers.keySet()){
			// some headers are restricted and cannot be set on the request
			// lua adds ':' to some headers which we filter as they are invalid
			// and not needed for our requests.
			if (!(ALLOWED_HEADERS.test(k) && !k.startsWith(":"))) {
				headers.remove(k);
			}
		}

		//Adding additional headers during Replay, This will help identify the case where the request is retried
		// by the platform for some reason, which leads to multiple identical events during the replay run.
		headers.putSingle(Constants.CUBE_HEADER_PREFIX + Constants.SRC_REQUEST_ID, reqEvent.reqId);


		//This will help to catch if the same request is replayed multiple times by Replay Driver
		headers.putSingle(Constants.CUBE_HEADER_PREFIX + Constants.REQUEST_ID, UUID.randomUUID().toString());


		return new RequestDetails(uri , requestBody , httpRequest.getMethod() , headers);
	}

}
