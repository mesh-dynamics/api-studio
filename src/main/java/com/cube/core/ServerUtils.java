/**
 * Copyright Cube I O
 */
package com.cube.core;


import io.md.core.Comparator.Diff;
import io.md.core.CompareTemplate.DataType;
import io.md.dao.Recording.RecordingType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.cube.agent.FnReqResponse;
import io.cube.agent.UtilException;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;
import io.md.core.ValidateCompareTemplate;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording;
import io.md.utils.Constants;
import io.md.utils.Utils;

import com.cube.dao.ReqRespStore;
import com.cube.golden.TemplateSet;
import com.cube.ws.Config;
import redis.clients.jedis.Jedis;


/**
 * @author prasad
 *
 */
public class ServerUtils {

    private static final Logger LOGGER = LogManager.getLogger(ServerUtils.class);

    // TODO: Keep in refactoring
    public static ValidateCompareTemplate validateTemplateSet(TemplateSet templateSet) {
        return templateSet.templates.stream().map(CompareTemplateVersioned::validate)
            .filter(v -> !v.isValid())
            .findFirst()
            .orElseGet(() -> new ValidateCompareTemplate(true, Optional.of("")));
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

    public static JsonNode
    convertArrayToObject(JsonNode node, CompareTemplate template, String path, String newPath,
                         Set<String> pathsToBeReconstructed){
        if (node.isArray()) {
            TemplateEntry arrayRule = template.getRule(path);
            ArrayNode nodeAsArray = (ArrayNode) node;
            ObjectNode equivalentObjNode = JsonNodeFactory.instance.objectNode();
            pathsToBeReconstructed.add(newPath);
            if (arrayRule.dt == DataType.Set) {
                Optional<JsonPointer> pathPointer =
                    arrayRule.arrayComparisionKeyPath.map(JsonPointer::compile);
                for (int i = 0 ; i < nodeAsArray.size() ; i++) {
                    JsonNode elem = nodeAsArray.get(i);
                    String key = pathPointer.map(pathPtr ->
                        elem.at(pathPtr).toString()).orElse(elem.toString());
                    equivalentObjNode.set(key
                        , convertArrayToObject(elem, template, path.concat("/").concat(String.valueOf(i)),
                            newPath.concat("/").concat(key)  , pathsToBeReconstructed));
                }
            } else {
                for (int i = 0 ; i < nodeAsArray.size() ; i++){
                    equivalentObjNode.set(String.valueOf(i), convertArrayToObject(nodeAsArray.get(i)
                        , template, path.concat("/").concat(String.valueOf(i))
                        , newPath.concat("/").concat(String.valueOf(i)) , pathsToBeReconstructed));
                }
            }
            return equivalentObjNode;
        } else if (node.isObject()) {
            ObjectNode nodeAsObject = (ObjectNode) node;
            ObjectNode equivalentObjNode = JsonNodeFactory.instance.objectNode();
            Iterator<String> fieldNames = nodeAsObject.fieldNames();
            while(fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                equivalentObjNode.set(fieldName, convertArrayToObject(nodeAsObject.get(fieldName)
                    ,template , path.concat("/").concat(fieldName) , newPath.concat("/").concat(fieldName) , pathsToBeReconstructed));
            }
            return equivalentObjNode;
        }
        return node;
    }

    /**
     * 	https://stackoverflow.com/questions/13530999/fastest-way-to-get-all-values-from-a-map-where-the-key-starts-with-a-certain-exp#13531376
     */
    private static SortedMap<String, Diff> getByPrefix(
        NavigableMap<String, Diff> myMap, String prefix ) {
        return myMap.subMap( prefix, prefix + Character.MAX_VALUE );
    }

    private static Map<String, JsonNode> convertObjectToMap(ObjectNode node, ObjectMapper jsonMapper) {
        return  jsonMapper.convertValue(node, new TypeReference<Map<String, JsonNode>>(){});
    }

    private static  void transformIndexInDiff(TreeMap<String, Diff> diffMap,
                                              String arrayPath, String oldIndex, String newIndex) {
        String oldPrefix = arrayPath.concat("/").concat(oldIndex);
        SortedMap<String , Diff> diffByPrefix =
            getByPrefix(diffMap, oldPrefix);
        diffByPrefix.values().forEach(diff -> {
            String oldPath = diff.path;
            diff.path = oldPath.replace(oldPrefix, arrayPath.concat("/").concat(newIndex));
        });
    }



    public static void reconstructArray(JsonNode leftRoot, JsonNode rightRoot
        , String arrayPath, TreeMap<String, Diff> diffMap, ObjectMapper jsonMapper) {

        JsonPointer jsonPointer = JsonPointer.compile(arrayPath);
        JsonNode leftNode =  leftRoot.at(jsonPointer);
        Map<String, JsonNode> leftArrayMap = new HashMap<>();
        if (leftNode != null && ! leftNode.isMissingNode() && leftNode.isObject()) {
            leftArrayMap = convertObjectToMap((ObjectNode) leftNode, jsonMapper);
        }

        JsonNode rightNode = rightRoot.at(jsonPointer);
        Map<String, JsonNode> rightArrayMap = new HashMap<>();
        if (rightNode != null && ! rightNode.isMissingNode() && rightNode.isObject()) {
            rightArrayMap = convertObjectToMap((ObjectNode) rightNode, jsonMapper);
        }

        Set<String> leftKeys =  new HashSet<>(leftArrayMap.keySet());
        Set<String> rightKeys = new HashSet<>(rightArrayMap.keySet());

        Set<String> intersection  = new HashSet<>(leftKeys);
        intersection.retainAll(rightKeys);
        leftKeys.removeAll(intersection);
        rightKeys.removeAll(intersection);

        ArrayNode leftArrayNode = JsonNodeFactory.instance.arrayNode();
        ArrayNode rightArrayNode = JsonNodeFactory.instance.arrayNode();

        int index = 0;
        for (String key : intersection) {
            leftArrayNode.add(leftArrayMap.get(key));
            rightArrayNode.add(rightArrayMap.get(key));
            transformIndexInDiff(diffMap, arrayPath,  key , String.valueOf(index));
            index++;
        }

        int leftIndex = index;
        for (String key : leftKeys) {
            leftArrayNode.add(leftArrayMap.get(key));
            transformIndexInDiff(diffMap, arrayPath,  key , String.valueOf(leftIndex));
            leftIndex++;
        }

        int rightIndex = index;
        for (String key : rightKeys) {
            rightArrayNode.add(rightArrayMap.get(key));
            transformIndexInDiff(diffMap, arrayPath,  key , String.valueOf(rightIndex));
            rightIndex++;
        }

        if (leftNode != null && ! leftNode.isMissingNode()) {
            JsonNode leftNodeParent = leftRoot.at(jsonPointer.head());
            ((ObjectNode) leftNodeParent).set(jsonPointer.last().getMatchingProperty()
                , leftArrayNode);
        }

        if (rightNode != null && ! rightNode.isMissingNode()) {
            JsonNode rightNodeParent = rightRoot.at(jsonPointer.head());
            ((ObjectNode) rightNodeParent).set(jsonPointer.last().getMatchingProperty()
                , rightArrayNode);
        }

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


    public static Event createHTTPResponseEvent(String apiPath, Optional<String> reqId,
                                                Integer status,
                                                MultivaluedMap<String, String> meta,
                                                MultivaluedMap<String, String> hdrs,
                                                String body,
                                                Optional<String> collection, Instant timestamp,
                                                Optional<Event.RunType> runType, Optional<String> customerId,
                                                Optional<String> app,
                                                ReqRespStore rrstore, Optional<String> runId, RecordingType recordingType) throws EventBuilder.InvalidEventException {
	    HTTPResponsePayload httpResponsePayload;
	    // We treat empty body ("") as null
	    if (body != null && (!body.isEmpty())) {
		    httpResponsePayload = new HTTPResponsePayload(hdrs, status, body.getBytes(StandardCharsets.UTF_8));
	    } else {
		    httpResponsePayload = new HTTPResponsePayload(hdrs, status, null);
	    }

	    Optional<String> service = Utils.getFirst(meta, Constants.SERVICE_FIELD);
        Optional<String> instance = Utils.getFirst(meta, Constants.INSTANCE_ID_FIELD);
        Optional<String> traceId = Utils.getFirst(meta, Constants.DEFAULT_TRACE_FIELD);

        if (customerId.isPresent() && app.isPresent() && service.isPresent() && collection.isPresent() && runType.isPresent()) {
            EventBuilder eventBuilder = new EventBuilder(customerId.get(), app.get(),
                service.get(), instance.orElse("NA"), collection.get(),
                new MDTraceInfo(traceId.orElse(reqId.flatMap(rrstore::getRequestEvent)
	                .map(Event::getTraceId).orElse("NA")), null, null),
                runType.get(), Optional.of(timestamp),
                reqId.orElse("NA"),
                apiPath, Event.EventType.HTTPResponse, recordingType);
            eventBuilder.setPayload(httpResponsePayload);
            eventBuilder.withRunId(runId);
            Event event = eventBuilder.createEvent();
            return event;
        } else {
            throw new EventBuilder.InvalidEventException();
        }

    }

	public static Map<String, TemplateEntry> getAllPathRules(Event event, Recording recording, TemplateKey.Type templateKeyType,
                                                             String service, String apiPath, ReqRespStore rrstore, Config config) {
		TemplateKey tkey = new TemplateKey(recording.templateVersion, recording.customerId, recording.app, service, apiPath,
			templateKeyType);

		Optional<CompareTemplate> templateOptional = rrstore.getCompareTemplate(tkey);
		Map<String, TemplateEntry> pathRules = new HashMap<>();
		templateOptional.ifPresent(UtilException.rethrowConsumer(template -> {
			event.getPayload().getPathRules(template, pathRules);
		}));

		return pathRules;
    }

  public static Response flushAll(Config config) {
    config.rrstore.invalidateCache();
    try (Jedis jedis = config.jedisPool.getResource()) {
      jedis.flushAll();
      return Response.ok().build();
    } catch (Exception e) {
      return Response.serverError().entity("Exception occured while flushing :: " + e.getMessage()).build();
    }
  }

    public static Map solrHealthCheck (SolrClient solr) {
	    try {
		    SolrPing solrPing = new SolrPing();
		    SolrPingResponse solrPingResponse = solrPing.process(solr);
		    int status = solrPingResponse.getStatus();
		    String solrStatusMessage = status==0 ? "Solr server up" : "Solr server not working";
		    return Map.of(Constants.SOLR_STATUS_CODE, status, Constants.SOLR_STATUS_MESSAGE, solrStatusMessage);
	    }
	    catch (IOException ioe) {
		    return Map.of(Constants.SOLR_STATUS_CODE, -1, Constants.SOLR_STATUS_MESSAGE, "Unable to reach Solr server", Constants.ERROR, ioe.getMessage());
	    }
	    catch (SolrServerException sse) {
		    return Map.of(Constants.SOLR_STATUS_CODE, -1, Constants.SOLR_STATUS_MESSAGE, "Unable to reach Solr server", Constants.ERROR, sse.getMessage());
	    }
	}
}
