/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static io.md.core.Utils.buildErrorResponse;
import static io.md.core.Utils.buildSuccessResponse;
import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;
import static io.md.core.Comparator.MatchType.DontCare;
import static io.md.core.TemplateKey.Type;
import static io.md.dao.Recording.RecordingStatus;
import static io.md.services.DataStore.TemplateNotFoundException;

import com.cube.core.ServerUtils;
import com.cube.dao.ApiTraceFacetQuery;
import io.md.dao.ApiTraceResponse;
import io.md.dao.ApiTraceResponse.ServiceReqRes;
import io.md.constants.ReplayStatus;
import io.md.core.Comparator.Match;
import io.md.dao.ConvertEventPayloadResponse;
import io.md.dao.Event.EventType;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.Payload;
import io.md.dao.RecordingOperationSetSP;
import io.md.dao.ResponsePayload;
import io.md.dao.Analysis.ReqRespMatchWithEvent;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.solr.common.util.Pair;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import io.md.core.Comparator;
import io.md.core.Comparator.MatchType;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.CompareTemplateStoreException;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;
import io.md.core.ValidateCompareTemplate;
import io.md.dao.Event;
import io.md.dao.Event.RunType;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.dao.Analysis;
import io.md.dao.ReqRespMatchResult;
import io.md.dao.ReqRespUpdateOperation;
import io.md.services.Analyzer;
import io.md.utils.Constants;
import io.md.core.Utils;

import com.cube.core.TemplateRegistries;
import com.cube.dao.AnalysisMatchResultQuery;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.RecordingBuilder;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr.ReqRespResultsWithFacets;
import com.cube.dao.Result;
import com.cube.drivers.RealAnalyzer;
import com.cube.golden.RecordingUpdate;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;
import com.cube.golden.transform.TemplateSetTransformer;
import com.cube.golden.transform.TemplateUpdateOperationSetTransformer;

/**
 * @author prasad
 * The replay service
 */
@Path("/as")
public class AnalyzeWS {

    private static final Logger LOGGER = LogManager.getLogger(AnalyzeWS.class);

    private final Analyzer analyzer;


    @Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
    	Map solrHealth = ServerUtils.solrHealthCheck(config.solr);
    	Map respMap = new HashMap(solrHealth);
    	respMap.put(Constants.SERVICE_HEALTH_STATUS, "AS is healthy");
	    return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }


	@POST
    @Path("analyze/{replayId}")
    @Consumes("application/x-www-form-urlencoded")
    public Response analyze(@Context UriInfo ui, @PathParam("replayId") String replayId,
                            MultivaluedMap<String, String> formParams) {
        String tracefield = Optional.ofNullable(formParams.get("tracefield"))
            .flatMap(vals -> vals.stream().findFirst())
            .orElse(Constants.DEFAULT_TRACE_FIELD);

        Optional<io.md.dao.Analysis> analysis = analyzer.analyze(replayId);

        return analysis.map(av -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(av);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Analysis object to Json for replayid %s", replayId), e);
                return Response.serverError().build();
            }
        }).orElse(Response.serverError().build());
    }


	@GET
    @Path("status/{replayId}")
    public Response status(@Context UriInfo ui,
        @PathParam("replayId") String replayId) {
		Optional<Analysis> analysis = RealAnalyzer.getStatus(replayId, rrstore);
		Response resp = analysis.map(av -> {
			String json;
			try {
				json = jsonMapper.writeValueAsString(av);
				return Response.ok().type(MediaType.APPLICATION_JSON)
					.entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject(json))).build();
			} catch (JsonProcessingException e) {
				LOGGER.error(new ObjectMessage(Map.of(
					Constants.MESSAGE, "Error in converting Analysis object to Json "
						+ e.getMessage(),
					Constants.REPLAY_ID_FIELD, replayId
				)));
				return Response.serverError().entity(
					buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
						e.getMessage())).build();
			}
		}).orElse(
			Response.status(Status.NOT_FOUND).entity(
				buildErrorResponse(Constants.FAIL, Constants.ANALYSIS_NOT_FOUND,
					"Analysis not found for replayId: " + replayId)).build());
		return resp;
	}

	@GET
    @Path("aggrresult/{replayId}")
    public Response getResultAggregate(@Context UriInfo ui,
                                       @PathParam("replayId") String replayId) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> service = Optional.ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
        boolean bypath = Optional.ofNullable(queryParams.getFirst("bypath"))
            .map(v -> v.equals("y")).orElse(false);

        Stream<MatchResultAggregate> resStream = rrstore.getResultAggregate(replayId, service, bypath);
        Collection<MatchResultAggregate> res = resStream.collect(Collectors.toList());

//        Collection<MatchResultAggregate> res = rrstore.computeResultAggregate(replayId, service, bypath);
        String json;
        try {
            json = jsonMapper.writeValueAsString(res);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting result aggregate object to Json for replayId %s", replayId), e);
            return Response.serverError().build();
        }
    }


	@POST
    @Path("registerTemplateApp/{customerId}/{appId}/{version}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerTemplateApp(@Context UriInfo uriInfo,
                                        @PathParam("customerId") String customerId,
                                        @PathParam("appId") String appId,
                                        @PathParam("version") String version,
                                        String templateRegistryArray) {
        try {
            //TODO study the impact of enabling this flag in other deserialization methods
            //jsonMapper.enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            TemplateRegistries registries = jsonMapper.readValue(templateRegistryArray, TemplateRegistries.class);
            /*
            List<TemplateRegistry> templateRegistries = registries.getTemplateRegistryList();
            TemplateKey.Type templateKeyType;
            if ("request".equalsIgnoreCase(type)) {
                templateKeyType = TemplateKey.Type.Request;
            } else if ("response".equalsIgnoreCase(type)) {
                templateKeyType = TemplateKey.Type.Response;
            } else {
                return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid template type, should be " +
                    "either request or response :: " + type).build();
            }
            */
            /*
            templateRegistries.forEach(UtilException.rethrowConsumer(registry -> {
                TemplateKey key = new TemplateKey(customerId, appId, registry.getService()
                    , registry.getPath(), templateKeyType);
                rrstore.saveCompareTemplate(key, jsonMapper.writeValueAsString(registry.getTemplate()));
                requestComparatorCache.invalidateKey(key);
                responseComparatorCache.invalidateKey(key);

            }));
            */
            Optional<String> templateVersion = version.equals("AUTO") ? Optional.empty() : Optional.of(version);
            TemplateSet templateSet = ServerUtils.templateRegistriesToTemplateSet(registries, customerId, appId,
                templateVersion);
            //String templateSetJSON = jsonmapper.writeValueAsString(templateSet);

            ValidateCompareTemplate validTemplate = ServerUtils.validateTemplateSet(templateSet);
            if(!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message", validTemplate.getMessage() ))).toString()).build();
            }

            rrstore.saveTemplateSet(templateSet);

            return Response.ok().type(MediaType.APPLICATION_JSON).entity(String.format(
                "{\"status\":\"success\", \"customer\":\"%s\", \"app\":\"%s\", \"version\":\"%s\"}",
                templateSet.customer, templateSet.app, templateSet.version)).build();
        } catch (JsonProcessingException e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(String.format(
                "{\"status\":\"fail\", \"message\":\"Invalid JSON: %s\"}",
                e.getMessage())).build();
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(String.format(
                "{\"status\":\"fail\", \"message\":\"%s\"}",
                e.getMessage())).build();
        }
    }


	/**
	 * Endpoint to save an analysis template as json in solr
	 * Will send appropriate Error Messages as response if unable to save
	 * Will overwrite any existing template against the same key
	 * @param urlInfo UrlInfo object
	 * @param appId Application Id
	 * @param customerId Customer Id
	 * @param serviceName Service Name
	 * @param templateAsJson Template As Json
	 * @return
	 */
	@POST
    @Path("registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path:.+}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response registerTemplate(@Context UriInfo urlInfo, @PathParam("appId") String appId,
                                     @PathParam("customerId") String customerId,
                                     @PathParam("serviceName") String serviceName,
                                     @PathParam("path") String path,
                                     @PathParam("type") String type,
                                     String templateAsJson) {
        try {
            //This is just to see the template is not invalid, and can be parsed according
            // to our class definition , otherwise send error response
            CompareTemplate template = jsonMapper.readValue(templateAsJson, CompareTemplate.class);
            Type templateType = Utils.valueOf(Type.class, type).orElseThrow(
	            () -> new CompareTemplateStoreException("Invalid Template Type, should be "
		            + "either RequestMatch, RequestCompare or ResponseCompare"));
	        TemplateKey key = new TemplateKey(DEFAULT_TEMPLATE_VER, customerId, appId,
		        serviceName, path, templateType);
            ValidateCompareTemplate validTemplate = template.validate();
            if (!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(
                    (new JSONObject(Map.of("Message", validTemplate.getMessage())))
	                    .toString()).build();
            }
            rrstore.saveCompareTemplate(key, templateAsJson);
            return Response.ok().type(MediaType.TEXT_PLAIN)
	            .entity("Json String successfully stored in Solr").build();
        } catch (JsonProcessingException e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN)
                .entity("Invalid JSON String sent").build();
        } catch (IOException e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN)
                .entity("Error Occured " + e.getMessage()).build();
        }
        catch (CompareTemplate.CompareTemplateStoreException e) {
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.TEMPLATE_STORE_FAILED
	                , "Unable to save template set: " + e.getMessage()))).build();
        }
    }

    /**
     * Endpoint to get registered response template
     * @param urlInfo UrlInfo object
     * @param appId Application Id
     * @param customerId Customer Id
     * @param templateVersion Template version
     * @param service The service id
     * @return
     */
    @GET
    @Path("getTemplate/{customerId}/{appId}/{templateVersion}/{service}/{type}")
    public Response getTemplate(@Context UriInfo urlInfo, @PathParam("appId") String appId,
	    @PathParam("customerId") String customerId, @PathParam("templateVersion") String templateVersion,
	    @PathParam("service") String service, @PathParam("type") String type) {
    	return Utils.valueOf(Type.class, type).map(templateType ->
		     getCompareTemplate(urlInfo, appId, customerId, templateVersion, service
			     , templateType))
		    .orElse(Response.serverError().entity(new JSONObject(Map.of(Constants.MESSAGE
			    , "Template type not specified correctly"))).build());
    }


    public Response getCompareTemplate(UriInfo urlInfo, String appId,
                                       String customerId,
                                       String templateVersion,
                                       String service,
                                       Type ruleType) {

        MultivaluedMap<String, String> queryParams = urlInfo.getQueryParameters();
        Optional<String> apipath = Optional.ofNullable(queryParams.getFirst(Constants.API_PATH_FIELD));
        Optional<String> jsonpath = Optional.ofNullable(queryParams.getFirst(Constants.JSON_PATH_FIELD));
        Optional<EventType> eventType = Optional.ofNullable(queryParams.getFirst(Constants.EVENT_TYPE_FIELD))
            .flatMap(v -> Utils.valueOf(EventType.class, v));

        if (apipath.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity(Map.of(Constants.ERROR, "Api Path not Specified")).build();
        }

        TemplateKey tkey = new TemplateKey(templateVersion, customerId, appId, service, apipath.get(),
            ruleType);

        try {
          CompareTemplate compareTemplate = rrstore.getComparator(tkey, eventType).getCompareTemplate();
          String resp = "";
          if (jsonpath.isEmpty()) {
            resp = jsonMapper.writeValueAsString(compareTemplate);
          } else {
            TemplateEntry rule = compareTemplate.getRule(jsonpath.get());
            resp = jsonMapper.writeValueAsString(rule);
          }
	        return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
		        .entity(resp).build();

        } catch (JsonProcessingException e) {
	        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
		        .type(MediaType.APPLICATION_JSON)
		        .entity(Map.of(Constants.ERROR
			        , "Not able to convert rule to json")).build();
        } catch (TemplateNotFoundException e) {
	        return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON)
		        .entity(Map.of(Constants.ERROR, "Assertion rule not found")).build();
        }

    }

    /**
	 * Given a stream of ReqRespMatchResult objects convert them to serialized json array
	 * @param reqRespMatchResults
	 * @return
	 */
	private String getJsonArrayString(Stream<ReqRespMatchResult> reqRespMatchResults) {
		return reqRespMatchResults.flatMap(result -> {
			try {
				return Stream.of(jsonMapper.writeValueAsString(result));
			} catch (JsonProcessingException e) {
				return Stream.empty();
			}
		}).collect(Collectors.joining("," , "[" , "]"));
	}

	/**
	 * Api to access analysis result for a given recorded request and related replay.
	 * The function:
	 * a) First finds the req resp match result for the given recorded request and replay id.
	 * b) Expands the recorded request and replayed request on traceid's
	 * c) returns a json array of all the match results for each request in recorded and replayed trace graphs.
	 * Note that this api returns the results in a flat format. The reconstruction of the trace graphs
	 * will happen at the UI end, since the UI already has the graph structure. We just have to super-impose
	 * the requests on the graph template matching nodes based on service names. (on a second though we only
	 * have service calling service edges and not path calling path edges at the UI end - need to expand on that)
	 * (Maybe the graph can be constructed by using span id's)
	 * @param urlInfo
	 * @param recordReqId
	 * @param replayId
	 * @return
	 */
	@GET
    @Path("analysisRes/{replayId}/{recordReqId}")
    public Response getAnalysisResult(@Context UriInfo urlInfo, @PathParam("recordReqId") String recordReqId,
                                      @PathParam("replayId") String replayId) {
        Optional<ReqRespMatchResult> matchResult =
            rrstore.getAnalysisMatchResult(recordReqId, replayId);
        return matchResult.map(mRes -> {
            Stream<ReqRespMatchResult> recordMatchResultList = rrstore.
                expandOnTrace(mRes, true);
            Stream<ReqRespMatchResult> replayMatchResultList = rrstore.
                expandOnTrace(mRes, false);
            String recordJsonArray = getJsonArrayString(recordMatchResultList);
            String replayJsonArray = getJsonArrayString(replayMatchResultList);
            String resultJson = "{\"record\" : " + recordJsonArray + " , \"replay\" : " + replayJsonArray + " }";
            return Response.ok().type(MediaType.
                APPLICATION_JSON).entity(resultJson).build();
        }).orElse(Response.serverError().type(MediaType.TEXT_PLAIN).entity("No Analysis Match Result Found for " +
            "recordReqId:replayId :: " + recordReqId + ":" + replayId).build());
    }

    @GET
    @Path("analysisResNoTrace/{replayId}/{recordReqId}")
    public Response getAnalysisResultWithoutTrace(@Context UriInfo urlInfo,
	    @PathParam("recordReqId") String recordReqId,
	    @PathParam("replayId") String replayId) {
	    Optional<ReqRespMatchResult> matchResult =
		    rrstore.getAnalysisMatchResult(recordReqId, replayId);
	    return matchResult.map(matchRes -> {
		    Optional<String> request = rrstore.getRequestEvent(recordReqId)
			    .map(event -> event.payload.getPayloadAsJsonString());

		    Optional<String> recordedResponse = extractPayload(matchRes.respCompareRes.recordedResponse ,
			    rrstore.getResponseEvent(recordReqId)).map(Payload::getPayloadAsJsonString);

		    Optional<String> replayedRequest = matchRes.replayReqId
			    .flatMap(rrstore::getRequestEvent)
			    .map(event -> event.payload.getPayloadAsJsonString());

		    Optional<String> replayedResponse = extractPayload(matchRes.respCompareRes.replayedResponse,
			    matchRes.replayReqId.flatMap(rrstore::getResponseEvent))
			    .map(Payload::getPayloadAsJsonString);


		    Optional<String> respCompDiff = Optional.empty();
		    Optional<String> reqCompDiff = Optional.empty();;
		    try {
			    respCompDiff = Optional.of(jsonMapper.writeValueAsString(matchRes.respCompareRes.diffs));
			    reqCompDiff = Optional.of(jsonMapper.writeValueAsString(matchRes.reqCompareRes.diffs));

		    } catch (JsonProcessingException e) {
			   LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				   "Unable to convert diff list to json string")),e);
		    }
		    MatchRes matchResFinal = new MatchRes(matchRes.recordReqId, matchRes.replayReqId,
			    matchRes.reqMatchRes, matchRes.numMatch,
			    matchRes.respCompareRes.mt, matchRes.service, matchRes.path,
			    matchRes.reqCompareRes.mt,
			    respCompDiff, reqCompDiff, request, replayedRequest, recordedResponse
			    , replayedResponse, matchRes.recordTraceId, matchRes.replayTraceId,
                matchRes.recordedSpanId, matchRes.recordedParentSpanId,
                matchRes.replayedSpanId, matchRes.replayedParentSpanId,
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());

		    String resultJson = null;
		    try {
			    resultJson = jsonMapper.writeValueAsString(matchResFinal);
		    } catch (JsonProcessingException e) {
			    return Response.serverError()
				    .entity((new JSONObject(Map.of(
				    	Constants.MESSAGE, "Json Processing Exception",
					    Constants.REASON, e.getMessage(),
					    Constants.RECORD_REQ_ID_FIELD, recordReqId,
					    Constants.REPLAY_ID_FIELD, replayId
					    ))).toString())
				    .build();
		    }
		    return Response.ok().type(MediaType.
			    APPLICATION_JSON).entity(resultJson).build();
	    }).orElse(Response.serverError()
		    .entity((new JSONObject(Map.of(
		    	Constants.MESSAGE, "No Analysis Match Result Found"
		    ))).toString())
		    .build());
    }


    /**
     * Return Time Line results for a given customer id , app combo
     * Optional Parameters include restriction on <i>collection</i> id (later we should be able to specify
     * a range of collection ids or dates)
     * Includes optional restriction on instance id which can be a list(multiple instanceId's allowed)
     * Return results segragated by path if <i>bypath</i> variable is set y in query params
     * We can also restrict the results to a particular gateway service (which is what should
     * be done ideally) using <i>service</i> query param
     * Note the replays are sorted in descending order of date/time once all the above filters are applied,
     * and stats for only <i>numresults</i> number of results/replays are returned
     * @param urlInfo
     * @param customer
     * @param app
     * @return
     */
    @GET
    @Path("timelineres/{customer}/{app}")
    public Response getTimelineResults(@Context UriInfo urlInfo, @PathParam("customer") String customer,
                                       @PathParam("app") String app) {
        MultivaluedMap<String, String> queryParams = urlInfo.getQueryParameters();
        List<String> instanceId = Optional.ofNullable(queryParams.get(Constants.INSTANCE_ID_FIELD)).orElse(Collections.EMPTY_LIST);
        Optional<String> service = Optional.ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
        Optional<String> collection = Optional.ofNullable(queryParams.getFirst(Constants.COLLECTION_FIELD));
        Optional<String> userId = Optional.ofNullable(queryParams.getFirst(Constants.USER_ID_FIELD));
        Optional<String> endDate = Optional.ofNullable(queryParams.getFirst(Constants.END_DATE_FIELD));
        Optional<String> startDate = Optional.ofNullable(queryParams.getFirst(Constants.START_DATE_FIELD));
        Optional<String> testConfigName = Optional.ofNullable(queryParams.getFirst(Constants.TEST_CONFIG_NAME_FIELD));
        Optional<String> goldenName = Optional.ofNullable(queryParams.getFirst(Constants.GOLDEN_NAME_FIELD));

        Optional<Instant> endDateTS = Optional.empty();
        Optional<Instant> startDateTS =  Optional.empty();
        // For checking correct date format
        if(endDate.isPresent()) {
            try {
                // Accepted format for date as per ISO-8601 are -
                // yyyy-MM-ddTHH:MM:SSZ UTC, with an offset of +00:00
                endDateTS = Optional.of(Instant.parse(endDate.get()));
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(
                    Map.of("Message", "Date format should be yyyy-MM-ddTHH:MM:SSZ",
                        "Error", e.getMessage())).toString())).build();
            }
        }
        if (startDate.isPresent()) {
          try {
            startDateTS = Optional.of(Instant.parse(startDate.get()));
          } catch (DateTimeParseException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(
                Map.of("Message", "start Date format should be yyyy-MM-ddTHH:MM:SSZ",
                    "Error", e.getMessage())).toString())).build();
          }
        }

        boolean byPath = Optional.ofNullable(queryParams.getFirst("byPath"))
            .map(v -> v.equals("y")).orElse(false);
        Optional<Integer> start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD)).flatMap(Utils::strToInt);
        Optional<Integer> numResults = Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD)).map(Integer::valueOf).or(() -> Optional.of(20));

        Result<Replay> replaysResult = rrstore.getReplay(Optional.of(customer), Optional.of(app), instanceId,
            List.of(ReplayStatus.Completed, ReplayStatus.Error), collection, numResults, start, userId, endDateTS, startDateTS, testConfigName, goldenName, false);
        long numFound = replaysResult.numFound;
        Stream<Replay> replays = replaysResult.getObjects();
        String finalJson = replays.map(replay -> {
            String replayId = replay.replayId;
            String testConfigNameValue = replay.testConfigName.orElse("");
          /**
           * TODO
           * once all replays will be having the updateTimestamp, we can directy set updationTimestamp
           */
            Instant timeStamp = replay.analysisCompleteTimestamp != Instant.EPOCH ? replay.analysisCompleteTimestamp : replay.creationTimeStamp;
            Optional<Recording> recordingOpt = rrstore.getRecordingByCollectionAndTemplateVer(replay.customerId, replay.app,
                replay.collection , Optional.of(replay.templateVersion));
            String recordingInfo = "";
            if (recordingOpt.isEmpty()) {
                LOGGER.error("Unable to find recording corresponding to given replay");
            } else {
                Recording recording = recordingOpt.get();
                recordingInfo = "\" , \"recordingid\" : \"" + recording.getId()
                    + "\" , \"collection\" : \"" + recording.collection
                    + "\" , \"templateVer\" : \"" + recording.templateVersion
                    + "\", \"goldenName\" : \"" + recording.name
                    + "\", \"userName\" : \"" + recording.userId
                    + "\", \"goldenLabel\" : \"" + recording.label;
            }

            Stream<MatchResultAggregate> resStream = rrstore.getResultAggregate(replayId, service, byPath);
            Collection<MatchResultAggregate> res = resStream.collect(Collectors.toList());

//            Collection<MatchResultAggregate> res = rrstore.computeResultAggregate(replayId, service, bypath);
            StringBuilder jsonBuilder = new StringBuilder();
            String json;
            jsonBuilder.append("{ \"replayId\" : \"" + replayId + "\" , \"timestamp\" : \"" + timeStamp.toString()
                + "\" , \"testConfigName\" : \"" +  testConfigNameValue + recordingInfo +  "\" , \"results\" : ");
            try {
                json = jsonMapper.writeValueAsString(res);
                jsonBuilder.append(json);
            } catch (JsonProcessingException e) {
                jsonBuilder.append("[]");
                LOGGER.error(String.format("Error in converting result aggregate object to Json for replayId %s",
                    replayId), e);
            }
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        }).collect(Collectors.joining(" , ", "", ""));
        finalJson = "{" + "\"numFound\" : " + numFound + "," +
            "\"timelineResults\" : [" + finalJson + "]}";
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(finalJson).build();
    }

    private Optional<HTTPResponsePayload> extractPayload(Optional<JsonNode> payload
	    , Optional<Event> event) {
    	return payload.map(HTTPResponsePayload::new).or(() ->
		    event.map(e -> (HTTPResponsePayload)e.payload));
    }

    /**
     *
     * @param ui
     * @return the results for reqids matching a path and other constraints
     */
    @GET
    @Path("analysisResByPath/{replayId}")
    public Response getAnalysisResultsByPath(@Context UriInfo ui,
        @PathParam("replayId") String replayId) {
      long size = config.getResponseSize();
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        AnalysisMatchResultQuery analysisMatchResultQuery = new AnalysisMatchResultQuery(replayId
		    , queryParams);
        Optional<Boolean> includeDiff = Optional
            .ofNullable(queryParams.getFirst(Constants.INCLUDE_DIFF)).flatMap(Utils::strToBool);

        /* using array as container for value to be updated since lambda function cannot update outer variables */
        Long[] numFound = {0L};
        String[] app = {"", ""};
	    Map facetMap = new HashMap();
	    List<MatchRes> matchResList = rrstore.getReplay(replayId).map(replay -> {

		    ReqRespResultsWithFacets resultWithFacets = rrstore
			    .getAnalysisMatchResults(analysisMatchResultQuery);

		    Result<ReqRespMatchResult> result = resultWithFacets.result;
		    ArrayList diffResFacets = resultWithFacets.diffResolFacets;
	        ArrayList serviceFacets =  resultWithFacets.serviceFacets;
		    ArrayList pathFacets =  resultWithFacets.pathFacets;

		    facetMap.put(Constants.DIFF_RES_FACET, diffResFacets);
		    facetMap.put(Constants.SERVICE_FACET, serviceFacets);
		    facetMap.put(Constants.PATH_FACET, pathFacets);

		    numFound[0] = result.numFound;
            app[0] = replay.app;
            app[1] = replay.templateVersion;
            List<ReqRespMatchResult> res = result.getObjects()
                .collect(Collectors.toList());
            List<String> reqIds = res.stream().map(r -> r.recordReqId).flatMap(Optional::stream)
                .collect(Collectors.toList());

            Map<String, Event> requestMap = new HashMap<>();
            if (!reqIds.isEmpty()) {
                // empty reqId list would lead to returning of all requests, so check for it
                Result<Event> requestResult = rrstore
                    .getRequests(replay.customerId, replay.app, replay.collection,
                        reqIds, Collections.emptyList(), Collections.emptyList(), Optional.of(
		                    Event.RunType.Record));
                requestResult.getObjects().forEach(req -> requestMap.put(req.reqId, req));
            }

		    return res.stream().map(matchRes -> {
			    Optional<Event> reqEvent = matchRes.recordReqId
				    .flatMap(reqId -> Optional.ofNullable(requestMap.get(reqId)));
			    Optional<String> request = reqEvent
				    .map(e -> e.payload.getPayloadAsJsonString(true));
			    Optional<Long> recordReqTime = reqEvent.map(e -> e.timestamp.toEpochMilli());

			    Optional<String> recordedRequest = Optional.empty();
			    Optional<String> replayedRequest = Optional.empty();
			    Optional<String> respCompDiff = Optional.empty();
			    Optional<String> recordResponse = Optional.empty();
			    Optional<Boolean> recordResponseTruncated = Optional.of(false);
			    Optional<String> replayResponse = Optional.empty();
			    Optional<Boolean> replayResponseTruncated = Optional.of(false);
			    Optional<String> reqCompDiff = Optional.empty();
			    Optional<Long> replayReqTime = Optional.empty();
			    Optional<Long> recordRespTime = Optional.empty();
			    Optional<Long> replayRespTime = Optional.empty();
			    MatchType reqCompResType = matchRes.reqCompareRes.mt;
			    if (includeDiff.orElse(false)) {
				    recordedRequest = request;
				    Optional<Event> replayedRequestEvent = matchRes.replayReqId
					    .flatMap(rrstore::getRequestEvent);
				    replayedRequest = replayedRequestEvent
					    .map(e -> e.payload.getPayloadAsJsonString(true));
				    replayReqTime = replayedRequestEvent.map(e -> e.timestamp.toEpochMilli());
				    List<Comparator.Diff> responseCompDiffList =
					    matchRes.respCompareRes.diffs.size() > config.getPathsToKeepLimit()
						    ? matchRes.respCompareRes.diffs
						    .subList(0, (int) config.getPathsToKeepLimit())
						    : matchRes.respCompareRes.diffs;

				    try {
					    respCompDiff = Optional
						    .of(jsonMapper.writeValueAsString(responseCompDiffList));
					    reqCompDiff = Optional.of(jsonMapper.writeValueAsString(matchRes
						    .reqCompareRes.diffs));
				    } catch (JsonProcessingException e) {
					    LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
						    "Unable to convert diff to json string")), e);
				    }
				    List<String> pathsToKeep = getPathsToKeep(responseCompDiffList);

				    Optional<Event> recordResponseEvent = matchRes.recordReqId
					    .flatMap(rrstore::getResponseEvent);

				    Optional<ConvertEventPayloadResponse> convertRecordResponse =
					    extractPayload(matchRes.respCompareRes.recordedResponse, recordResponseEvent)
						    .map(payload -> payload.checkAndConvertResponseToString(true, pathsToKeep,
						    size, "/body"));
				    recordResponse = convertRecordResponse.map(resp -> resp.getResponse());
				    recordResponseTruncated = convertRecordResponse.map(resp -> resp.isTruncated());
				    recordRespTime = recordResponseEvent.map(e -> e.timestamp.toEpochMilli());

				    Optional<Event> replayResponseEvent = matchRes.replayReqId
					    .flatMap(rrstore::getResponseEvent);

				    Optional<ConvertEventPayloadResponse> convertReplayResponse =
					    extractPayload(matchRes.respCompareRes.replayedResponse, replayResponseEvent)
						    .map(payload -> payload.checkAndConvertResponseToString(true, pathsToKeep,
							    size, "/body"));
				    replayResponse = convertReplayResponse.map(resp -> resp.getResponse());
				    replayResponseTruncated = convertReplayResponse.map(resp -> resp.isTruncated());
				    replayRespTime = replayResponseEvent.map(e -> e.timestamp.toEpochMilli());
			    }

                return new MatchRes(matchRes.recordReqId, matchRes.replayReqId,
                    matchRes.reqMatchRes, matchRes.numMatch,
                    matchRes.respCompareRes.mt, matchRes.service, matchRes.path, reqCompResType
	                , respCompDiff, reqCompDiff, recordedRequest, replayedRequest, recordResponse
	                , replayResponse, matchRes.recordTraceId, matchRes.replayTraceId,
                    matchRes.recordedSpanId, matchRes.recordedParentSpanId,
                    matchRes.replayedSpanId, matchRes.replayedParentSpanId,
	                recordReqTime, recordRespTime,
	                replayReqTime, replayRespTime, Optional.of(replay.instanceId)
                    ,recordResponseTruncated, replayResponseTruncated);
            }).collect(Collectors.toList());
        }).orElse(Collections.emptyList());

        String json;
        try {
            json = jsonMapper
                .writeValueAsString(new MatchResults(matchResList, numFound[0], app[0], app[1]));
	        JSONObject jsonObject = new JSONObject(json);
	        jsonObject.put(Constants.FACETS, facetMap);

            return Response.ok().type(MediaType.APPLICATION_JSON)
                .entity(buildSuccessResponse(Constants.SUCCESS, jsonObject)).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Error in converting Match results list to Json",
                Constants.REPLAY_ID_FIELD, replayId,
                Constants.APP_FIELD, app/*,
                Constants.SERVICE_FIELD, service,
                Constants.PATH_FIELD, path*/
            )
            ));
            return Response.serverError().entity(
                buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    e.getMessage())).build();
        }
    }

    private List<String> getPathsToKeep(List<Comparator.Diff> diffs) {
      List<String> pathsToKeep = new ArrayList<>();
      for(Comparator.Diff diff: diffs) {
        if(diff.path.contains("body")) {
          pathsToKeep.add(diff.path);
        }
      }
      return pathsToKeep;
    }


    /**
     * Api to access analysis result for a given recorded request and related replay.
     * Returns the responses for the requests as well
     * @param uriInfo
     * @param replayId
     * @return
     */
    @GET
    @Path("analysisResByReq/{replayId}")
    public Response getResultByReq(@Context UriInfo uriInfo,
        @PathParam("replayId") String replayId) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        Optional<String> recordReqId = Optional
            .ofNullable(queryParams.getFirst(Constants.RECORD_REQ_ID_FIELD));
        Optional<String> replayReqId = Optional
            .ofNullable(queryParams.getFirst(Constants.REPLAY_REQ_ID_FIELD));

        Optional<ReqRespMatchResult> matchResult =
            rrstore.getAnalysisMatchResult(recordReqId, replayReqId, replayId);
        Optional<String> recordResponse = recordReqId.flatMap(rrstore::getResponseEvent)
            .map(event -> event.payload.getPayloadAsJsonString());
        Optional<String> replayResponse = replayReqId.flatMap(rrstore::getResponseEvent)
            .map(event -> event.payload.getPayloadAsJsonString());

        String json;
        try {
            json = jsonMapper.writeValueAsString(
                new RespAndMatchResults(recordResponse, replayResponse, matchResult));
            return Response.ok().type(MediaType.APPLICATION_JSON)
                .entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject(json))).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Error in converting response and match results to Json",
                Constants.REPLAY_ID_FIELD, replayId,
                Constants.RECORD_REQ_ID_FIELD, recordReqId,
                Constants.REPLAY_REQ_ID_FIELD, replayReqId
            )
            ));

            return Response.serverError()
                .entity(buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    e.getMessage())).build();
        }
    }

    @POST
    @Path("saveTemplateSet/{customer}/{app}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveTemplateSet(@Context UriInfo uriInfo, @PathParam("customer") String customer,
                                    @PathParam("app") String app, TemplateSet templateSet) {
        try {
	        templateSet.templates.forEach(compareTemplateVersioned -> {
		        String normalisedAPIPath= CompareTemplate.normaliseAPIPath(compareTemplateVersioned.requestPath);
		        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Normalizing APIPath before storing template ",
			        "Original APIPath", compareTemplateVersioned.requestPath,
			        "Normalised APIPath", normalisedAPIPath)));
		        compareTemplateVersioned.requestPath = normalisedAPIPath;
	        });
            ValidateCompareTemplate validTemplate = ServerUtils.validateTemplateSet(templateSet);
            if(!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message", validTemplate.getMessage() ))).toString()).build();
            }
            String templateSetId = rrstore.saveTemplateSet(templateSet);
            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully saved template set",
                "ID", templateSetId,
                "templateSetVersion", templateSet.version))).toString()).build();
        } catch (CompareTemplate.CompareTemplateStoreException e) {
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.TEMPLATE_STORE_FAILED, "Unable to save template set: " +
                    e.getMessage()))).build();
        }

        catch (TemplateSet.TemplateSetMetaStoreException e) {
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.TEMPLATE_META_STORE_FAILED, "Unable to save template meta: " +
                    e.getMessage()))).build();
        }

        catch (Exception e) {
            return Response.serverError().entity((new JSONObject(Map.of(
                "Message", "Unable to save template set",
                "Error", e.getMessage()))).toString()).build();
        }
    }

    @POST
    @Path("cache/flushall")
    public Response cacheFlushAll() {
      return ServerUtils.flushAll(config);
    }
    /**
     * Initiate recording of template set update operations
     * @param uriInfo Context
     * @param customer Customer
     * @param app App
     * @param sourceVersion Version of the source template set
     * @return Appropriate Response
     */
    @POST
    @Path("initTemplateOperationSet/{customer}/{app}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initTemplateOperationSet(@Context UriInfo uriInfo, @PathParam("customer") String customer,
                                             @PathParam("app") String app,  @PathParam("version") String sourceVersion) {
        // delegate to backend store
        try {
            String operationSetID = rrstore.createTemplateUpdateOperationSet(customer, app, sourceVersion);
	        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully created new template "
		        +  "rules update op set", Constants.TEMPLATE_UPD_OP_SET_ID_FIELD, operationSetID)));
            return Response.ok().entity("{\"Message\" :  \"Template Update Operation Set successfully created\" , \"ID\" : \"" +
                operationSetID + "\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"Message\" :  \"Unable to initiate template update operation set\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }

    /**
     * Update operation set for modification of a template set (add new rules)
     * @param uriInfo Context
     * @param operationSetId The id of the existing update operation set
     * @param templateUpdateOperations The new operations to be added to the set
     *                                 (a map of template key, vs update operations for the particular template)
     * @return Appropriate Response
     */
    @POST
    @Path("updateTemplateOperationSet/{operationSetId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTemplateOperationSet(@Context UriInfo uriInfo, @PathParam("operationSetId") String operationSetId
        , String templateUpdateOperations) {
        TypeReference<HashMap<TemplateKey, SingleTemplateUpdateOperation>> typeReference =
            new TypeReference<>() {};
        try {
            // deserialize new operations to be added
            Map<TemplateKey, SingleTemplateUpdateOperation> updates = jsonMapper.readValue(templateUpdateOperations,
                typeReference);
            // get existing operation set against the id specified
            Optional<TemplateUpdateOperationSet> updateOperationSetOpt = rrstore.getTemplateUpdateOperationSet(operationSetId);
            TemplateUpdateOperationSetTransformer transformer = new TemplateUpdateOperationSetTransformer();
            // merge operations
            TemplateUpdateOperationSet transformed = updateOperationSetOpt.flatMap(updateOperationSet -> Optional.of
                (transformer.updateTemplateOperationSet(updateOperationSet , updates)))
                .orElseThrow(() -> new Exception("Missing template update operation set for given id"));
            // save the merged operation set
            rrstore.saveTemplateUpdateOperationSet(transformed);
            LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully updated template "
	            + "rules update op set", Constants.TEMPLATE_UPD_OP_SET_ID_FIELD, operationSetId)));
            return Response.ok().entity("{\"Message\" :  \"Successfully updated Template update operation set\" , \"ID\" : \"" +
                operationSetId + "\"}").build();
        } catch (Exception e) {
            LOGGER.error("Error while reading template update operation list from json string :: " + e.getMessage());
            return Response.serverError().entity("{\"Message\" :  \"Unable to update template update operation set\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }

    /**
     * Update an existing template set, based on the operations specified in an update set
     * @param templateSetId Id of the exising template set (part of a golden set)
     * @param templateUpdateOperationSetId Id of the update operations set
     * @return Appropriate Response
     */
    @GET
    @Path("updateTemplateSet/{templateSetId}/{operationSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTemplateSet(@PathParam("templateSetId") String templateSetId, @PathParam("operationSetId")
        String templateUpdateOperationSetId) {
        try{
            // Get template set and update operation set from solr
            Optional<TemplateSet> templateSetOpt = rrstore.getTemplateSet(templateSetId);
            Optional<TemplateUpdateOperationSet> updateOperationSetOpt = rrstore.getTemplateUpdateOperationSet(templateUpdateOperationSetId);
            TemplateSetTransformer transformer = new TemplateSetTransformer();
            // transform the template set based on the operations specified
	        TemplateSet updated = templateSetOpt.flatMap(UtilException.rethrowFunction(
		        templateSet -> updateOperationSetOpt.map(UtilException.rethrowFunction(
			        updateOperationSet ->
				        transformer.updateTemplateSet(templateSet, updateOperationSet,
					        rrstore)))))
		        .orElseThrow(
			        () -> new Exception("Missing template set or template update operation set"));
            // Validate updated template set
            ValidateCompareTemplate validTemplate = ServerUtils.validateTemplateSet(updated);
            if(!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message", validTemplate.getMessage() ))).toString()).build();
            }
            // save the new template set (and return the new version as a part of the response)
	        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully updated template set",
		        Constants.OLD_TEMPLATE_SET_ID, templateSetId, Constants.NEW_TEMPLATE_SET_VERSION, updated.version,
		        Constants.CUSTOMER_ID_FIELD, updated.customer, Constants.APP_FIELD
		        , updated.app, Constants.TEMPLATE_UPD_OP_SET_ID_FIELD, templateUpdateOperationSetId)));
            rrstore.saveTemplateSet(updated);
            return Response.ok().entity("{\"Message\" :  \"Template Set successfully updated\" , \"ID\" : \"" +
                updated.version + "\"}").build();
        } catch (Exception e) {
            LOGGER.error("Error while updating template set :: " + templateSetId + " :: with operation set id :: "
                + templateUpdateOperationSetId);
            return Response.serverError().entity("{\"Message\" :  \"Unable to update template set\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }


    /**
     * Update an existing recording with the specified template update and collection
     * update operation set, and create a new golden set with the modified template set
     * and collection
     * @param recordingId Source Recording (combination of collection and template set)
     * @param collectionUpdateOpSetId The collection update operation set id
     * @param templateUpdOpSetId Template update operation set id
     * @return Appropriate response
     */
    @POST
    @Path("updateGoldenSet/{recordingId}/{replayId}/{collectionUpdOpSetId}/{templateUpdOpSetId}")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGoldenSet(@PathParam("recordingId") String recordingId,
                                    @PathParam("replayId") String replayId,
                                    @PathParam("collectionUpdOpSetId") String collectionUpdateOpSetId,
                                    @PathParam("templateUpdOpSetId") String templateUpdOpSetId,
                                    MultivaluedMap<String, String> formParams) {
        try {
            Recording originalRec = rrstore.getRecording(recordingId).orElseThrow(() ->
                new Exception("Unable to find recording object for the given id"));

            String name = formParams.getFirst("name");
            String label = formParams.getFirst("label");

            if (name==null || label==null) {
                throw new Exception("Name or label not specified for golden");
            }

            String userId = formParams.getFirst("userId");


            if (userId==null ) {
                throw new Exception("userId not specified for golden");
            }

            // Ensure name is unique for a customer and app
            Optional<Recording> recWithSameName = rrstore.getRecordingByName(originalRec.customerId, originalRec.app, name, Optional.ofNullable(label));
            if (recWithSameName.isPresent()) {
                throw new Exception("Golden already present for name - " + name + "/" + label + ".Specify unique name/label");
            }

            // creating a new temporary empty template set against the old version
	        // (if one doesn't exist already)
            TemplateSet templateSet = rrstore.getTemplateSet(originalRec.customerId, originalRec.app, originalRec
                .templateVersion)
                .orElse(new TemplateSet(originalRec.templateVersion, originalRec.customerId,
	                originalRec.app, Instant.now(), Collections.emptyList(), Optional.empty()));
            TemplateUpdateOperationSet templateUpdateOperationSet = rrstore
                .getTemplateUpdateOperationSet(templateUpdOpSetId).orElseThrow(() ->
                    new Exception("Unable to find Template Update Operation Set of specified id"));
            TemplateSetTransformer setTransformer = new TemplateSetTransformer();
            TemplateSet updatedTemplateSet = setTransformer.updateTemplateSet(
            	templateSet, templateUpdateOperationSet, config.rrstore);

	        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully updated template set",
		        Constants.OLD_TEMPLATE_SET_VERSION, templateSet.version, Constants.NEW_TEMPLATE_SET_VERSION, updatedTemplateSet.version,
		        Constants.CUSTOMER_ID_FIELD, updatedTemplateSet.customer, Constants.APP_FIELD
		        , updatedTemplateSet.app, Constants.TEMPLATE_UPD_OP_SET_ID_FIELD,
		        templateUpdOpSetId , Constants.RECORDING_ID, recordingId, Constants.REPLAY_ID_FIELD, replayId)));

            // Validate updated template set
            ValidateCompareTemplate validTemplate = ServerUtils.validateTemplateSet(updatedTemplateSet);
            if(!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message", validTemplate.getMessage() ))).toString()).build();
            }

            String updatedTemplateSetId = rrstore.saveTemplateSet(updatedTemplateSet);
            // TODO With similar update logic find the updated collection id
            String newCollectionName = UUID.randomUUID().toString();
            boolean b = recordingUpdate.applyRecordingOperationSet(replayId, newCollectionName
	            , collectionUpdateOpSetId, originalRec, updatedTemplateSet.version);
            if (!b) throw new Exception("Unable to create an updated collection from existing golden");


            Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst("codeVersion"));
            Optional<String> branch = Optional.ofNullable(formParams.getFirst("branch"));
            Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst("gitCommitId"));
            List<String> tags = Optional.ofNullable(formParams.get("tags")).orElse(new ArrayList<String>());
            Optional<String> comment = Optional.ofNullable(formParams.getFirst("comment"));

            RecordingBuilder recordingBuilder = new RecordingBuilder(
            	originalRec.customerId, originalRec.app, originalRec.instanceId, newCollectionName)
                .withStatus(RecordingStatus.Completed).withTemplateSetVersion(updatedTemplateSet.version)
	            .withParentRecordingId(originalRec.getId()).withRootRecordingId(originalRec.rootRecordingId)
                .withName(name).withLabel(label).withTags(tags).withCollectionUpdateOpSetId(collectionUpdateOpSetId)
	            .withTemplateUpdateOpSetId(templateUpdOpSetId).withUserId(userId).withRecordingType(originalRec.recordingType);
            codeVersion.ifPresent(recordingBuilder::withCodeVersion);
            branch.ifPresent(recordingBuilder::withBranch);
            gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
            comment.ifPresent(recordingBuilder::withComment);
	        originalRec.generatedClassJarPath.ifPresent(UtilException
		        .rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));

            Recording updatedRecording = recordingBuilder.build();

            rrstore.saveRecording(updatedRecording);
            return Response.ok().entity("{\"Message\" :  \"Successfully created new recording with specified original recording " +
                "and set of operations\" , \"ID\" : \"" + updatedRecording.getId() + "\"}").build();
        } catch (Exception e) {
            LOGGER.error("Error while updating golden set :: "  + e.getMessage());
            return Response.serverError().entity("{\"Message\" :  \"Error while updating recording\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("sanitizeGoldenSet")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sanitizeRecording (@QueryParam("recordingId") String recordingId,
                                       @QueryParam("replayId") String replayId)   {

        try {
            Recording originalRec = rrstore.getRecording(recordingId).orElseThrow(() ->
                new Exception("Unable to find recording object for the given id"));
            TemplateSet templateSet = rrstore.getTemplateSet(originalRec.customerId, originalRec.app, originalRec
                .templateVersion).orElseThrow(() ->
                new Exception("Unable to find template set mentioned in the specified golden set"));

            String newCollectionName = originalRec.collection + "-" + UUID.randomUUID().toString();
            boolean created = recordingUpdate.createSanitizedCollection(replayId, newCollectionName, originalRec);

            if (!created) throw new Exception("Unable to create an updated collection from existing golden");

            RecordingBuilder recordingBuilder = new RecordingBuilder(
            	originalRec.customerId, originalRec.app, originalRec.instanceId, newCollectionName)
	            .withStatus(RecordingStatus.Completed).withTemplateSetVersion(templateSet.version)
	            .withParentRecordingId(originalRec.getId()).withRootRecordingId(originalRec.rootRecordingId)
	            .withName(originalRec.name).withLabel(originalRec.label).withTags(originalRec.tags).withArchived(originalRec.archived)
	            .withUserId(originalRec.userId).withRecordingType(originalRec.recordingType);
	        originalRec.codeVersion.ifPresent(recordingBuilder::withCodeVersion);
	        originalRec.branch.ifPresent(recordingBuilder::withBranch);
	        originalRec.gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
	        originalRec.comment.ifPresent(recordingBuilder::withComment);
	        originalRec.generatedClassJarPath.ifPresent(UtilException
		        .rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));

            Recording updatedRecording = recordingBuilder.build();

            rrstore.saveRecording(updatedRecording);
            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully created new recording by sanitizing the specified original recording",
                "ID", updatedRecording.getId()))).toString()).build();
        }  catch (Exception e) {
            LOGGER.error("Error while creating sanitized golden set :: "  + e.getMessage(), e);
            return Response.serverError().entity(new JSONObject(Map.of(
                "Message", "Error while creating sanitized golden set",
                "Error", e.getMessage())).toString()).build();
        }
    }


    /**
    * API to create a new recording operation set for a customer and app. This creates a new RecordingOperationSetMeta
    * entry in Solr and returns the id. This Meta entry ties together the RecordingOperationSets of different
    * services and paths, linked
    * via the id of the Meta entry, which can be used to add or update operations in the operation sets and later apply
    * them to a replay.
    * @param customer
    * @param app
    *
     */
    @POST
    @Path("goldenUpdate/recordingOperationSet/create/")
    public Response createRecordingOperationSet(@QueryParam("customer") String customer,
                                    @QueryParam("app") String app) {
        LOGGER.debug("Received request to create new recording operation set for customer: " + customer + " app: " + app);
        String operationSetId;
        try {
            operationSetId = recordingUpdate.createRecordingOperationSet(customer, app);
            LOGGER.info("Created recording operation set with id: " + operationSetId);
        } catch (Exception e) {
            LOGGER.error("exception while creating recording operation set", e);
            return Response.serverError().build();
        }

        String json = null;
        try {
            json = jsonMapper.writeValueAsString(Map.of("operationSetId", operationSetId));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }

    /**
     * API to get the RecordingOperationSetSP for an operationSetId, service and path
     */
    @GET
    @Path("goldenUpdate/recordingOperationSet/get/")
    public Response getRecordingOperationSet(@QueryParam("operationSetId") String operationSetId,
                                             @QueryParam("service") String service,
                                             @QueryParam("path") String path) {
        LOGGER.debug(String.format("Received request for fetching recording operation set with operationSetId %s, " +
            "service %s, path %s", operationSetId, service, path));
        try {
            Optional<RecordingOperationSetSP> recordingOperationSet = recordingUpdate.getRecordingOperationSet(
                operationSetId, service, path);
            return recordingOperationSet
                .map(operationSet -> {
                    try {
                        String json = jsonMapper.writeValueAsString(operationSet);
                        return Response.ok(json, MediaType.APPLICATION_JSON).build();
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Error converting JSON to String: " + e);
                        return Response.serverError().build();
                    }
                })
                .orElseGet(() -> {
                    LOGGER.error("recording operation set not found");
                    return Response.status(Response.Status.NOT_FOUND).build();
                });

        } catch (Exception e) {
            LOGGER.error("exception while getting recording operation set", e);
            return Response.serverError().build();
        }
    }

    /**
     * API to update operations for a operationSetId, service and path
     */
    @POST
    @Path("goldenUpdate/recordingOperationSet/updateMultiPath/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRecordingOperationSet(List<RecordingOperationSetSP> requests) {
        List<String> recordingOperationSetIds = new ArrayList<>();
        try {
            requests.forEach(UtilException.rethrowConsumer(request -> {
                request.generateId();
                String recordingOperationSetId = request.operationSetId;
                String service = request.service;
                String path = request.path;
                List<ReqRespUpdateOperation> newOperationList = request.operationsList;

                LOGGER.debug(String.format("Received request for updating operation set, id: %s, service: %s, path: %s, new " +
                    "operation list: %s", recordingOperationSetId, service, path, newOperationList));

                boolean b = recordingUpdate.updateRecordingOperationSet(request);

                if(b) {
                    recordingOperationSetIds.add(recordingOperationSetId);
                } else {
                    throw new Exception("Error updating operation set for id " +  recordingOperationSetId);
                }
            }));

            return Response.ok().type(MediaType.APPLICATION_JSON)
                .entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject(Map.of("recordingOperationSetIds",recordingOperationSetIds)))).build();

        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.FAIL, Constants.UPDATE_RECORDING_OPERATION_FAILED,
                    "Update failed. Exception message - " + e.getMessage())).build();
        }
    }


    // Todo : This API can go away once the UI is stable with "goldenUpdate/recordingOperationSet/updateMultiPath/"
    /**
     * API to update operations for a operationSetId, service and path
     */
    @POST
    @Path("goldenUpdate/recordingOperationSet/update/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRecordingOperationSet(RecordingOperationSetSP request) {
        request.generateId();
        String recordingOperationSetId = request.operationSetId;
        String service = request.service;
        String path = request.path;
        List<ReqRespUpdateOperation> newOperationList = request.operationsList;

        LOGGER.debug(String.format("Received request for updating operation set, id: %s, service: %s, path: %s, new " +
            "operation list: %s", recordingOperationSetId, service, path, newOperationList));

        boolean b = recordingUpdate.updateRecordingOperationSet(request);
        if(b) {
            String response = "Success";
            String type = MediaType.TEXT_PLAIN;
            try {
                response = jsonMapper
                    .writeValueAsString(Map.of(
                    	Constants.MESSAGE , "Successfully updated Recording Update Operation Set"
                        , "ID" , recordingOperationSetId));
                type = MediaType.APPLICATION_JSON;
            } catch (JsonProcessingException e) {
                LOGGER.error("Error while constructing json response :: " + e.getMessage());
            }
            return Response.ok().entity(response).type(type).build();
        } else {
            LOGGER.error("error updating operation set");
            return Response.serverError().build();
        }
    }


    /**
     * API to transform a replay collection by applying an operation set to a it
     * @param operationSetId
     * @param replayId
     * @param collectionName name of the transformed collection
     * @return
     */
    @POST
    @Path("goldenUpdate/recordingOperationSet/apply/")
    public Response applyRecordingOperationSet(@QueryParam("operationSetId") String operationSetId,
                                             @QueryParam("replayId") String replayId,
                                             @QueryParam("collectionName") String collectionName) {

        LOGGER.debug(String.format("Received request to apply operation set %s to replay %s, with collection name %s",
            operationSetId, replayId, collectionName));
        boolean b =  true;//recordingUpdate.applyRecordingOperationSet(replayId, collectionName, operationSetId);

        if(b) {
            return Response.ok().build();
        } else {
            LOGGER.error("error applying operation set");
            return Response.serverError().build();
        }
    }

	/**
	 * API to return Golden insights for a given golden Id, service and path
	 * @param recordingId
	 * @return
	 */
	@GET
    @Path("goldenInsights/{recordingId}")
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	public Response goldenInsights(@Context UriInfo urlInfo,
		@PathParam("recordingId") String recordingId) {

		try {
			Recording recording = rrstore.getRecording(recordingId).orElseThrow(() ->
				new Exception("Unable to find recording object for the given id"));

			MultivaluedMap<String, String> queryParams = urlInfo.getQueryParameters();
			String service = queryParams.getFirst(Constants.SERVICE_FIELD);
			if (service == null) {
				throw new Exception("Service not specified for golden");
			}

			String apiPath = queryParams.getFirst(Constants.API_PATH_FIELD);
			if (apiPath == null) {
				throw new Exception("ApiPath not specified for golden");
			}
			String normalisedApiPath = CompareTemplate.normaliseAPIPath(apiPath);

			JSONObject jsonObject = new JSONObject();
			jsonObject.put(Constants.RECORDING_ID, recordingId);

			Optional<Event> responseOptional = rrstore
				.getSingleResponseEvent(recording.customerId, recording.app, recording.collection,
					List.of(service), List.of(normalisedApiPath), Optional.empty());

			responseOptional.ifPresentOrElse(UtilException.rethrowConsumer(response -> {

				Map<String, TemplateEntry> responseCompareRules = ServerUtils
					.getAllPathRules(response, recording, Type.ResponseCompare,
						service, normalisedApiPath, rrstore, config);

				jsonObject.put(Constants.RESPONSE, response.payload.getPayloadAsJsonString());
				jsonObject.put(Constants.RESPONSE_COMPARE_RULES,
					jsonMapper.writeValueAsString(responseCompareRules));

				Optional<Event> requestOptional = rrstore.getRequestEvent(response.getReqId());

				requestOptional.ifPresentOrElse(UtilException.rethrowConsumer(request -> {
					setRequestAndRules(recording, service, normalisedApiPath, jsonObject, request);

				}), () -> {
					jsonObject.put(Constants.REQUEST, JSONObject.NULL);
					jsonObject.put(Constants.REQUEST_MATCH_RULES, JSONObject.NULL);
					jsonObject.put(Constants.REQUEST_COMPARE_RULES, JSONObject.NULL);
				});
			}), () -> {
				jsonObject.put(Constants.RESPONSE, JSONObject.NULL);
				jsonObject.put(Constants.RESPONSE_COMPARE_RULES, JSONObject.NULL);
				jsonObject.put(Constants.REQUEST, JSONObject.NULL);
				jsonObject.put(Constants.REQUEST_MATCH_RULES, JSONObject.NULL);
				jsonObject.put(Constants.REQUEST_COMPARE_RULES, JSONObject.NULL);

			});

			return Response.ok().entity(jsonObject.toString()).build();

		} catch (Exception e) {
			LOGGER.error(
				new ObjectMessage(Map.of(Constants.MESSAGE, "Error while returning golden insights",
					Constants.RECORDING_ID, recordingId)), e);
			return Response.serverError().entity(
				buildErrorResponse(Constants.ERROR, "Error while returning golden insights",
					e.getMessage())).build();
		}

	}


	/**
	 * API to return Golden meta data for a given golden Id
	 * @param recordingId
	 * @return
	 */
	@GET
	@Path("getGoldenMetaData/{recordingId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGoldenMetaData(@Context UriInfo urlInfo,
		@PathParam("recordingId") String recordingId) {
		try {
			Recording recording = rrstore.getRecording(recordingId).orElseThrow(() ->
				new Exception("Unable to find recording object for the given id"));

			ArrayList servicePathFacets = rrstore
				.getServicePathHierarchicalFacets(recording.collection, RunType.Record);

			Map jsonMap = jsonMapper.convertValue(recording, Map.class);
			jsonMap.put(Constants.SERVICE_FACET, servicePathFacets);

			return Response.ok().entity(jsonMapper.writeValueAsString(jsonMap)).build();
		} catch (Exception e) {
			LOGGER.error(
				new ObjectMessage(Map.of(Constants.MESSAGE, "Error while returning golden meta info",
					Constants.RECORDING_ID, recordingId)), e);
			return Response.serverError().entity(
				buildErrorResponse(Constants.ERROR, "Error while returning golden meta info",
					e.getMessage())).build();
		}

	}

	@GET
  @Path("getApiFacets/{customerId}/{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApiFacets(@Context UriInfo uriInfo,
      @PathParam("customerId") String customerId,
      @PathParam("appId") String appId) {
	  ApiTraceFacetQuery apiTraceFacetQuery = new ApiTraceFacetQuery(customerId, appId, uriInfo.getQueryParameters());
	  ArrayList servicePathFacets =  rrstore.getApiFacets(apiTraceFacetQuery);
	  Map jsonMap = new HashMap();
	  jsonMap.put(Constants.SERVICE_FACET, servicePathFacets);
	  return Response.ok().entity(jsonMap).build();
  }

  @GET
  @Path("getApiTrace/{customerId}/{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getApiTrace(@Context UriInfo uriInfo,
      @PathParam("customerId") String customerId,
      @PathParam("appId") String appId) {

	  MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
	  ApiTraceFacetQuery apiTraceFacetQuery = new ApiTraceFacetQuery(customerId, appId, queryParams);
	  Integer depth = Optional.ofNullable(uriInfo.getQueryParameters().getFirst("depth"))
        .flatMap(val -> Utils.strToInt(val).filter(v -> v >0)).orElse(1);
	  Integer numResults =
        Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD)).flatMap(Utils::strToInt).orElse(20);
	  Optional<Integer> start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD)).flatMap(Utils::strToInt);
	  long numFound = 0;
	  Result<Event> result = rrstore.getApiTrace(apiTraceFacetQuery, start,
        Optional.of(numResults),
        Arrays.asList(EventType.HTTPRequest), true);
	  numFound = result.getNumFound();
	  Set<String> traceIds = new HashSet<>();
	  Set<String> collections = new HashSet<>();
	  Set<String> runIds = new HashSet<>();
    Set<String> reqIds = new HashSet<>();
	  result.getObjects().forEach(event -> {
	    traceIds.add(event.getTraceId());
	    collections.add(event.getCollection());
	    reqIds.add(event.getReqId());
	  });
	  apiTraceFacetQuery.withTraceIds(traceIds);
	  apiTraceFacetQuery.withCollections(collections);
	  apiTraceFacetQuery.withRunIds(runIds);

	  ArrayList<ApiTraceResponse> response = new ArrayList<>();
	  if(!apiTraceFacetQuery.traceIds.isEmpty()) {
	    /**TODO: we need to update the trace for other event types
       *currently we are supporting only HTTPRequest and HTTPResponse
       * we need to change the logic to support other eventTypes
       */
	    result = rrstore
          .getApiTrace(apiTraceFacetQuery, start, Optional.empty(),
              Arrays.asList(EventType.HTTPRequest, EventType.HTTPResponse), false);

	    MultivaluedMap<String, Event> mapForEventsTraceIds = new MultivaluedHashMap<>();
	    MultivaluedMap<String, Event> traceCollectionMap = new MultivaluedHashMap<>();
	    result.getObjects().forEach(
	        res -> {
	          if (res.eventType == EventType.HTTPRequest) {
	            traceCollectionMap.add(getTraceKeyFromEvent(res), res);
	          }
	          mapForEventsTraceIds.add(getTraceKeyFromEvent(res), res);
	        });

	    traceCollectionMap.forEach((traceCollectionKey, events) -> {
	      List<Event> parentRequestEvents = apiTraceFacetQuery.apiPath.map(path -> {
	        return events.stream()
              .filter(e -> e.apiPath.equals(path))
              .limit(numResults)
              .collect(Collectors.toList());
	      }).orElseGet(() -> {
	        // find event such that there is no event having span id equal to its parent span id
          Map<String, Event> requestEventsBySpanId = new HashMap<>();
          events.forEach(e -> requestEventsBySpanId.put(e.spanId, e));
          return events.stream()
              .filter(e -> e.parentSpanId.equals("NA") || requestEventsBySpanId.get(e.parentSpanId) == null)
              .limit(numResults)
              .collect(Collectors.toList());
	      });
	      if (parentRequestEvents.isEmpty()) {
	        LOGGER.error(
	            new ObjectMessage(Map.of(Constants.MESSAGE, "No request events found",
                  Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, appId,
                  Constants.TRACE_ID_FIELD + " " + Constants.COLLECTION_FIELD,
                  traceCollectionKey)));
	        return;
	      }
	      for (Event parent : parentRequestEvents) {
	        if(reqIds.contains(parent.getReqId())) {
            response.add(getApiTraceResponse(parent, depth,
                Utils.getFromMVMapAsOptional(mapForEventsTraceIds, traceCollectionKey)));
          }
	      }
	    });
	  }
    response.sort(new java.util.Comparator<ApiTraceResponse>() {
      @Override
      public int compare(ApiTraceResponse o1, ApiTraceResponse o2) {
        return o2.reqTimestamp.compareTo(o1.reqTimestamp);
      }
    });
	  Map jsonMap = new HashMap();

	  jsonMap.put("response", response);
	  jsonMap.put("numFound", numFound);
	  return Response.ok().entity(jsonMap).build();
  }

  private String getTraceKeyFromEvent(Event event) {
      return  event.getTraceId() + " " +  event.getCollection() + " " + event.runId.orElse("NA");
  }

  private ApiTraceResponse getApiTraceResponse(Event parentRequestEvent, int depth, List<Event> eventsForTraceId) {
    final ApiTraceResponse apiTraceResponse = new ApiTraceResponse(parentRequestEvent.getTraceId(),
        parentRequestEvent.getCollection(), parentRequestEvent.timestamp);

    MultivaluedMap<String, Event> requestEventsByParentSpanId = new MultivaluedHashMap<>();
    Map<String, Event> responseEventsByReqId = new HashMap<>();
    eventsForTraceId.forEach(e -> {
        if(e.isRequestType()) {
          requestEventsByParentSpanId.add(e.parentSpanId, e);
        } else {
          responseEventsByReqId.put(e.reqId, e);
        }
    });

    levelOrderTraversal(parentRequestEvent,  depth, apiTraceResponse, responseEventsByReqId,
        requestEventsByParentSpanId);
    apiTraceResponse.res.sort(new java.util.Comparator<ServiceReqRes>() {
      @Override
      public int compare(ServiceReqRes o1, ServiceReqRes o2) {
        return o2.reqTimestamp.compareTo(o1.reqTimestamp);
      }
    });
    return apiTraceResponse;
  }

  private void levelOrderTraversal(Event e, int level, final ApiTraceResponse apiTraceResponse, Map<String, Event> responseEventsByReqId,
      MultivaluedMap<String, Event> requestEventsByParentSpanId) {
	  if(level == 0) return;

	  Event responseEvent = responseEventsByReqId.get(e.reqId);
    HTTPRequestPayload payload = (HTTPRequestPayload) e.payload;

    String status = responseEvent != null ? ((ResponsePayload) responseEvent.payload).getStatusCode() : "";
    ServiceReqRes serviceReqRes = new ServiceReqRes(e.service, e.apiPath,
        e.reqId, e.timestamp, e.spanId, e.parentSpanId, status, payload.getMethod()
	    , (MultivaluedHashMap<String, String>) payload.getQueryParams());
    apiTraceResponse.res.add(serviceReqRes);
    List<Event> eventList = requestEventsByParentSpanId.get(e.spanId);
    if (eventList == null) return;
    List<Event> children = eventList.stream()
        .filter(child -> child.reqId != null && !child.reqId.equals(e.reqId)).collect(Collectors.toList());
    for(Event child: children) {
      levelOrderTraversal(child, level-1, apiTraceResponse, responseEventsByReqId,
          requestEventsByParentSpanId);
    }
  }

	private void setRequestAndRules(Recording recording, String service, String apiPath,
		JSONObject jsonObject, Event request) throws JsonProcessingException {
		jsonObject.put(Constants.REQUEST, request.payload.getPayloadAsJsonString());

		Map<String, TemplateEntry> requestMatchRules = ServerUtils
			.getAllPathRules(request, recording, Type.RequestMatch,
				service, apiPath, rrstore, config);

		Map<String, TemplateEntry> requestCompareRules = ServerUtils
			.getAllPathRules(request, recording, Type.RequestCompare,
				service, apiPath, rrstore, config);
		jsonObject.put(Constants.REQUEST_MATCH_RULES,
			jsonMapper.writeValueAsString(requestMatchRules));
		jsonObject.put(Constants.REQUEST_COMPARE_RULES,
			jsonMapper.writeValueAsString(requestCompareRules));
	}


	@GET
	@Path("getReqRespMatchResult")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReqRespMatchResult(@Context UriInfo uriInfo) {
		MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
		if(queryParams==null) {
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
				.entity(Map.of(Constants.ERROR, "No queryParams are specified for lhsReqId and rhsReqId")).build();
		}

		// lhsReqId should be from recording collection and rhsReqId from replay
		Optional<String> lhsReqId = Optional.ofNullable(queryParams.getFirst("lhsReqId"));
		Optional<String> rhsReqId = Optional.ofNullable(queryParams.getFirst("rhsReqId"));

		if (lhsReqId.isEmpty() || rhsReqId.isEmpty()) {
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
				.entity(Map.of(Constants.ERROR, "lhsReqId or rhsReqId not Specified")).build();
		}

		Optional<Event> lhsRequestEventOpt = rrstore.getRequestEvent(lhsReqId.get());
		Optional<Event> rhsRequestEventOpt = rrstore.getRequestEvent(rhsReqId.get());

		if (lhsRequestEventOpt.isEmpty() || rhsRequestEventOpt.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
				.entity(Map.of(Constants.ERROR, "lhsReqEvent or rhsReqEvent not found in solr"))
				.build();
		}
		Event lhsRequestEvent = lhsRequestEventOpt.get();
		Event rhsRequestEvent = rhsRequestEventOpt.get();

		Optional<Event> lhsResponseEventOpt = rrstore.getResponseEvent(lhsReqId.get());
		Optional<Event> rhsResponseEventOpt = rrstore.getResponseEvent(rhsReqId.get());

		Optional<Recording> recordingOpt = rrstore
			.getRecordingByCollectionAndTemplateVer(lhsRequestEvent.customerId, lhsRequestEvent.app,
				lhsRequestEvent.getCollection(), Optional.empty());

		if (recordingOpt.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
				.entity(Map.of(Constants.ERROR,
					"Recording not found in solr. lhsReqId should be from recording collection and rhsReqId from replay"))
				.build();
		}
		Recording recording = recordingOpt.get();

		Comparator.Match reqCompareRes = Match.NOMATCH;
		Comparator.Match respCompareRes = Match.NOMATCH;
		try {
			TemplateKey reqCompareKey = new TemplateKey(recording.templateVersion,
				lhsRequestEvent.customerId,
				lhsRequestEvent.app, lhsRequestEvent.service, lhsRequestEvent.apiPath,
				Type.RequestCompare, io.md.utils.Utils.extractMethod(lhsRequestEvent)
				, Optional.of(recording.collection));
			Comparator reqComparator = rrstore
				.getComparator(reqCompareKey, lhsRequestEvent.eventType);
				reqCompareRes = reqComparator.compare(lhsRequestEvent.payload, rhsRequestEvent.payload);
			TemplateKey respCompareKey = new TemplateKey(recording.templateVersion,
				lhsRequestEvent.customerId,
				lhsRequestEvent.app, lhsRequestEvent.service, lhsRequestEvent.apiPath,
				Type.ResponseCompare, io.md.utils.Utils.extractMethod(lhsRequestEvent)
				, Optional.of(recording.collection));

			if (lhsResponseEventOpt.isPresent() && rhsResponseEventOpt.isPresent()) {
				Event lhsResponseEvent = lhsResponseEventOpt.get();
				Event rhsResponseEvent = rhsResponseEventOpt.get();
				Comparator respComparator = rrstore
					.getComparator(respCompareKey, lhsResponseEvent.eventType);
				respCompareRes = respComparator
					.compare(lhsResponseEvent.payload, rhsResponseEvent.payload);
			}
		} catch (Exception e) {
			LOGGER.error(new ObjectMessage(Map.of(
				Constants.MESSAGE, "Exception while comparing request")), e);
			return Response.serverError().entity(
				buildErrorResponse(Constants.ERROR, "Error while comparing requests",
					e.getMessage())).build();
		}

		ReqRespMatchWithEvent reqRespMatchWithEvent = new ReqRespMatchWithEvent(lhsRequestEvent,
			Optional.of(rhsRequestEvent),
			respCompareRes, lhsResponseEventOpt, rhsResponseEventOpt, reqCompareRes);

		ReqRespMatchResult res = Analysis.createReqRespMatchResult(reqRespMatchWithEvent, DontCare,
			1, "NA");

		Optional<String> respCompDiff = Optional.empty();
		Optional<String> reqCompDiff = Optional.empty();

		try {
			respCompDiff = Optional.of(jsonMapper.writeValueAsString(res.respCompareRes.diffs));
			reqCompDiff = Optional.of(jsonMapper.writeValueAsString(
				res.reqCompareRes.diffs));
		} catch (JsonProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Unable to convert diff to json string")), e);
		}

		MatchRes matchRes = new MatchRes(res.recordReqId, res.replayReqId,
			res.reqMatchRes, res.numMatch,
			res.respCompareRes.mt, res.service, res.path, res.reqCompareRes.mt
			, respCompDiff, reqCompDiff, Optional.of(lhsRequestEvent.payload.getPayloadAsJsonString(true)),
			Optional.of(rhsRequestEvent.payload.getPayloadAsJsonString(true)),
			lhsResponseEventOpt.map(e -> e.payload.getPayloadAsJsonString(true))
			, rhsResponseEventOpt.map(e -> e.payload.getPayloadAsJsonString(true)), res.recordTraceId,
			res.replayTraceId,
			res.recordedSpanId, res.recordedParentSpanId,
			res.replayedSpanId, res.replayedParentSpanId,
			Optional.empty(), Optional.empty(),
			Optional.empty(), Optional.empty(), Optional.empty()
			, Optional.empty(), Optional.empty());


		Map jsonMap = new HashMap();
		jsonMap.put("res", matchRes);
		return Response.ok().entity(jsonMap).build();
	}


	/**
	 * @param config
	 */
	@Inject
	public AnalyzeWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
		this.recordingUpdate = new RecordingUpdate(config);
        analyzer = new RealAnalyzer(rrstore);
    }


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	Config config;
    private final RecordingUpdate recordingUpdate;

    /**
     * some fields from ReqRespMatchResult and some from Request to be returned by some api calls
     */
	static class MatchRes {

	    public MatchRes(Optional<String> recordReqId,
                        Optional<String> replayReqId,
                        Comparator.MatchType reqMatchResType,
                        int numMatch,
                        Comparator.MatchType respCompResType,
                        String service,
                        String path,
                        Comparator.MatchType reqCompResType,
                        Optional<String> diff,
                        Optional<String> reqCompDiff,
                        Optional<String> recordRequest,
                        Optional<String> replayRequest,
                        Optional<String> recordResponse,
                        Optional<String> replayResponse,
	                    Optional<String> recordTraceId,
	                    Optional<String> replayTraceId,
                        Optional<String> recordedSpanId,
                        Optional<String> recordedParentSpanId,
                        Optional<String> replayedSpanId,
                        Optional<String> replayedParentSpanId,
	                    Optional<Long> recordReqTime,
					    Optional<Long> recordRespTime,
		                Optional<Long> replayReqTime,
		                Optional<Long> replayRespTime,
                    Optional<String> instanceId,
          Optional<Boolean> recordResponseTruncated,
          Optional<Boolean> replayResponseTruncated
	        ) {
            this.recordReqId = recordReqId;
            this.replayReqId = replayReqId;
            this.reqMatchResType = reqMatchResType;
            this.numMatch = numMatch;
            this.respCompResType = respCompResType;
            this.service = service;
            this.path = path;
            this.respCompDiff = diff;
            this.reqCompDiff = reqCompDiff;
            this.reqCompResType = reqCompResType;
            this.recordRequest = recordRequest;
            this.replayRequest = replayRequest;
            this.recordResponse = recordResponse;
            this.replayResponse = replayResponse;
            this.recordTraceId = recordTraceId;
            this.replayTraceId = replayTraceId;
            this.recordedSpanId = recordedSpanId;
            this.recordedParentSpanId = recordedParentSpanId;
            this.replayedSpanId = replayedSpanId;
            this.replayedParentSpanId = replayedParentSpanId;
            this.recordReqTime = recordReqTime;
		    this.recordRespTime = recordReqTime;
		    this.replayReqTime = recordReqTime;
		    this.replayRespTime = recordReqTime;
		    this.instanceId = instanceId;
		    this.recordResponseTruncated = recordResponseTruncated;
		    this.replayResponseTruncated = replayResponseTruncated;
	    }

        public final Optional<String> recordReqId;
        public final Optional<String> replayReqId;
        public final Comparator.MatchType reqMatchResType;
        public final int numMatch;
        public final Comparator.MatchType respCompResType;
	    public final Comparator.MatchType reqCompResType;
	    public final String service;
        public final String path;
        public final Optional<String> recordTraceId;
        public final Optional<String> replayTraceId;
        public final Optional<String> recordedSpanId;
        public final Optional<String> recordedParentSpanId;
        public final Optional<String> replayedSpanId;
        public final Optional<String> replayedParentSpanId;
	    public final Optional<Long> recordReqTime;
	    public final Optional<Long> recordRespTime;
	    public final Optional<Long> replayReqTime;
	    public final Optional<Long> replayRespTime;
	    public final Optional<String> instanceId;
	    public final Optional<Boolean> recordResponseTruncated;
	    public final Optional<Boolean> replayResponseTruncated;

	    //Using JsonRawValue on <Optional> field results in Jackson serialization failure.
	    //Hence getMethods() are used to fetch the value.
        @JsonIgnore
        public final Optional<String> respCompDiff;
        @JsonIgnore
        public final Optional<String> recordRequest;
        @JsonIgnore
        public final Optional<String> replayRequest;
        @JsonIgnore
        public final Optional<String> recordResponse;
        @JsonIgnore
        public final Optional<String> replayResponse;
        @JsonIgnore
        public final Optional<String> reqCompDiff;
	    //JsonRawValue is to avoid Jackson escaping the String while using writeValueAsString
	    @JsonRawValue
	    public String getRespCompDiff() {
		    return respCompDiff.orElse(null);
	    }

	    @JsonRawValue
	    public String getRecordRequest() {
		    return recordRequest.orElse(null);
	    }

	    @JsonRawValue
	    public String getReplayRequest() {
		    return replayRequest.orElse(null);
	    }

	    @JsonRawValue
	    public String getRecordResponse() {
		    return recordResponse.orElse(null);
	    }

	    @JsonRawValue
	    public String getReplayResponse() {
		    return replayResponse.orElse(null);
	    }

	    @JsonRawValue
	    public String getReqCompDiff() {return reqCompDiff.orElse(null);}
	}

    static class MatchResults {
        public MatchResults(List<MatchRes> res, long numFound, String app, String templateVersion) {
            this.res = res;
            this.numFound = numFound;
            this.app = app;
            this.templateVersion = templateVersion;
        }

        public final List<MatchRes> res;
	    public final long numFound;
	    public String app;
	    public String templateVersion;
    }

    static class RespAndMatchResults {


        public RespAndMatchResults(Optional<String> recordResponse, Optional<String> replayResponse,
                                   Optional<ReqRespMatchResult> matchResult) {
            this.recordResponse = recordResponse;
            this.replayResponse = replayResponse;
            this.matchResult = matchResult;
        }

        //Using JsonRawValue on <Optional> field results in Jackson serialization failure.
	    //Hence getMethods() are used to fetch the value.
        @JsonIgnore
        public final Optional<String> recordResponse;
        @JsonIgnore
	    public final Optional<String> replayResponse;
	    public final Optional<ReqRespMatchResult> matchResult;

	    //JsonRawValue is to avoid Jackson escaping the String while using writeValueAsString
	    @JsonRawValue
	    public String getRecordResponse() {
		    return recordResponse.orElse(null);
	    }

	    @JsonRawValue
	    public String getReplayResponse() {
		    return replayResponse.orElse(null);
	    }
    }
}
