/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static com.cube.core.Utils.buildErrorResponse;
import static com.cube.core.Utils.buildSuccessResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import redis.clients.jedis.Jedis;

import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplateRegistries;
import com.cube.core.Utils;
import com.cube.core.ValidateCompareTemplate;
import com.cube.dao.Analysis;
import com.cube.dao.CubeMetaInfo;
import com.cube.dao.Event;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.Recording;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.RecordingBuilder;
import com.cube.dao.RecordingOperationSetSP;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespMatchResult;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Result;
import com.cube.drivers.Analyzer;
import com.cube.golden.RecordingUpdate;
import com.cube.golden.ReqRespUpdateOperation;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;
import com.cube.golden.transform.TemplateSetTransformer;
import com.cube.golden.transform.TemplateUpdateOperationSetTransformer;
import com.cube.utils.Constants;

/**
 * @author prasad
 * The replay service
 */
@Path("/as")
public class AnalyzeWS {

    private static final Logger LOGGER = LogManager.getLogger(AnalyzeWS.class);


    @Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
    	Map solrHealth = WSUtils.solrHealthCheck(config.solr);
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

        try {
            Optional<Analysis> analysis = Analyzer.analyze(replayId, tracefield, config);

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
        } catch (ComparatorCache.TemplateNotFoundException e) {
            return Response.serverError().entity((
                buildErrorResponse(Constants.ERROR, Constants.TEMPLATE_NOT_FOUND,
                    "Cannot analyze since template does not exist : " +
                    e.getMessage()))).build();
        }
    }


	@GET
    @Path("status/{replayId}")
    public Response status(@Context UriInfo ui,
        @PathParam("replayId") String replayId) {
		Optional<Analysis> analysis = Analyzer.getStatus(replayId, rrstore);
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

	/**
	 * Api to get replay results for a given customer/app/virtual(mock) service and replay combination.
	 * The result would contain request match / not match counts for all the paths covered by
	 * the service during replay.
	 * @param uriInfo
	 * @param customerId
	 * @param app
	 * @param service
	 * @param replayId
	 * @return
	 */
	@GET
    @Path("replayRes/{customerId}/{app}/{service}/{replayId}")
    public Response replayResult(@Context UriInfo uriInfo, @PathParam("customerId") String customerId,
                                 @PathParam("app") String app, @PathParam("service") String service,
                                 @PathParam("replayId") String replayId) {
        List<String> replayRequestCountResults = rrstore.getReplayRequestCounts(customerId, app, service, replayId);
        String resultJson = replayRequestCountResults.stream().collect(Collectors.joining(",", "[", "]"));
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(resultJson).build();
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
            TemplateSet templateSet = Utils.templateRegistriesToTemplateSet(registries, customerId, appId, templateVersion);
            //String templateSetJSON = jsonmapper.writeValueAsString(templateSet);

            ValidateCompareTemplate validTemplate = Utils.validateTemplateSet(templateSet);
            if(!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message", validTemplate.getMessage() ))).toString()).build();
            }

            Utils.invalidateCacheFromTemplateSet(templateSet, comparatorCache);
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
            TemplateKey key;
            if (Constants.REQUEST.equalsIgnoreCase(type)) {
                key = new TemplateKey(Constants.DEFAULT_TEMPLATE_VER, customerId, appId,
                    serviceName, path, TemplateKey.Type.Request);
            } else if (Constants.RESPONSE.equalsIgnoreCase(type)) {
                key = new TemplateKey(Constants.DEFAULT_TEMPLATE_VER, customerId, appId,
                    serviceName, path,
                    TemplateKey.Type.Response);
            } else {
                return Response.serverError().type(MediaType.TEXT_PLAIN)
                    .entity("Invalid template type, should be " +
                        "either request or response :: " + type).build();
            }

            ValidateCompareTemplate validTemplate = template.validate();
            if (!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(
                    (new JSONObject(Map.of("Message", validTemplate.getMessage()))).toString())
                    .build();
            }
            rrstore.saveCompareTemplate(key, templateAsJson);
            comparatorCache.invalidateKey(key);
            return Response.ok().type(MediaType.TEXT_PLAIN).entity("Json String successfully stored in Solr").build();
        } catch (JsonProcessingException e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN)
                .entity("Invalid JSON String sent").build();
        } catch (IOException e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN)
                .entity("Error Occured " + e.getMessage()).build();
        }
        catch (CompareTemplate.CompareTemplateStoreException e) {
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.TEMPLATE_STORE_FAILED, "Unable to save template set: " +
                    e.getMessage()))).build();
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
    @Path("getRespTemplate/{customerId}/{appId}/{templateVersion}/{service}")
    public Response getRespTemplate(@Context UriInfo urlInfo, @PathParam("appId") String appId,
                                     @PathParam("customerId") String customerId,
                                     @PathParam("templateVersion") String templateVersion,
                                     @PathParam("service") String service) {

        return getCompareTemplate(urlInfo, appId, customerId, templateVersion, service, TemplateKey.Type.Response);
    }

    /**
     * Endpoint to get registered request template
     * @param urlInfo UrlInfo object
     * @param appId Application Id
     * @param customerId Customer Id
     * @param templateVersion Template version
     * @param service The service id
     * @return
     */
    @GET
    @Path("getReqTemplate/{customerId}/{appId}/{templateVersion}/{service}")
    public Response getReqTemplate(@Context UriInfo urlInfo, @PathParam("appId") String appId,
                                    @PathParam("customerId") String customerId,
                                    @PathParam("templateVersion") String templateVersion,
                                    @PathParam("service") String service) {

        return getCompareTemplate(urlInfo, appId, customerId, templateVersion, service, TemplateKey.Type.Request);
    }


    public Response getCompareTemplate(UriInfo urlInfo, String appId,
                                       String customerId,
                                       String templateVersion,
                                       String service,
                                       TemplateKey.Type ruleType) {

        MultivaluedMap<String, String> queryParams = urlInfo.getQueryParameters();
        Optional<String> apipath = Optional.ofNullable(queryParams.getFirst(Constants.API_PATH_FIELD));
        Optional<String> jsonpath = Optional.ofNullable(queryParams.getFirst(Constants.JSON_PATH_FIELD));

        if (apipath.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity("{\"Error\": \"apiPath is mssing\"}").build();
        }
        if (jsonpath.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity("{\"Error\": \"jsonpath is mssing\"}").build();
        }

        TemplateKey tkey = new TemplateKey(templateVersion, customerId, appId, service, apipath.get(),
            ruleType);

        Optional<TemplateEntry> rule = rrstore.getCompareTemplate(tkey)
            .map(template -> template.getRule(jsonpath.get()));

        if (rule.isPresent()) {
            try {
                return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
                    .entity(jsonMapper.writeValueAsString(rule.get())).build();
            } catch (Exception e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON)
                    .entity("{\"Error\":\"Not able to convert rule to json\"}").build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON)
            .entity("{\"Error\":\"Assertion rule not found\"}").build();
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
			    .map(event -> event.getPayloadAsJsonString(config));
		    Optional<String> recordedResponse = rrstore.getResponseEvent(recordReqId)
			    .map(event -> event.getPayloadAsJsonString(config));

		    Optional<String> replayedRequest = matchRes.replayReqId
			    .flatMap(rrstore::getRequestEvent)
			    .map(event -> event.getPayloadAsJsonString(config));

		    Optional<String> replayedResponse = matchRes.replayReqId.flatMap(rrstore::getResponseEvent)
			    .map(event -> event.getPayloadAsJsonString(config));

		    Optional<String> diff = Optional.empty();
		    Optional<String> reqCompDiff = Optional.empty();
		    try {
			    diff = Optional.of(jsonMapper.writeValueAsString(matchRes.respCompareRes.diffs));
			    reqCompDiff = matchRes.reqCompareRes.map(UtilException.rethrowFunction(
			    	reqCompRes -> jsonMapper.writeValueAsString(reqCompRes.diffs)));

		    } catch (JsonProcessingException e) {
			   LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				   "Unable to convert diff list to json string")),e);
		    }
		    MatchRes matchResFinal = new MatchRes(matchRes.recordReqId, matchRes.replayReqId,
			    matchRes.reqMatchRes, matchRes.numMatch,
			    matchRes.respCompareRes.mt, matchRes.service, matchRes.path,
			    matchRes.reqCompareRes.map(reqComRes -> reqComRes.mt),
			    diff, reqCompDiff, request, replayedRequest, recordedResponse, replayedResponse);

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

        Optional<Instant> endDateTS = Optional.empty();
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

        boolean byPath = Optional.ofNullable(queryParams.getFirst("byPath"))
            .map(v -> v.equals("y")).orElse(false);
        Optional<Integer> start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD)).flatMap(Utils::strToInt);
        Optional<Integer> numResults = Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD)).map(Integer::valueOf).or(() -> Optional.of(20));

        Result<Replay> replaysResult = rrstore.getReplay(Optional.of(customer), Optional.of(app), instanceId,
            List.of(Replay.ReplayStatus.Completed, Replay.ReplayStatus.Error), collection, numResults, start, userId, endDateTS);
        long numFound = replaysResult.numFound;
        Stream<Replay> replays = replaysResult.getObjects();
        String finalJson = replays.map(replay -> {
            String replayId = replay.replayId;
            Instant creationTimeStamp = replay.creationTimeStamp;
            Optional<Recording> recordingOpt = rrstore.getRecordingByCollectionAndTemplateVer(replay.customerId, replay.app,
                replay.collection , replay.templateVersion);
            String recordingInfo = "";
            if (recordingOpt.isEmpty()) {
                LOGGER.error("Unable to find recording corresponding to given replay");
            } else {
                Recording recording = recordingOpt.get();
                recordingInfo = "\" , \"recordingid\" : \"" + recording.getId()
                    + "\" , \"collection\" : \"" + recording.collection
                    + "\" , \"templateVer\" : \"" + recording.templateVersion;
            }

            Stream<MatchResultAggregate> resStream = rrstore.getResultAggregate(replayId, service, byPath);
            Collection<MatchResultAggregate> res = resStream.collect(Collectors.toList());

//            Collection<MatchResultAggregate> res = rrstore.computeResultAggregate(replayId, service, bypath);
            StringBuilder jsonBuilder = new StringBuilder();
            String json;
            jsonBuilder.append("{ \"replayId\" : \"" + replayId + "\" , \"timestamp\" : \"" + creationTimeStamp.toString()
                + recordingInfo +  "\" , \"results\" : ");
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

    /**
     *
     * @param ui
     * @return the results for reqids matching a path and other constraints
     */
    @GET
    @Path("analysisResByPath/{replayId}")
    public Response getAnalysisResultsByPath(@Context UriInfo ui,
        @PathParam("replayId") String replayId) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> service = Optional
            .ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
        Optional<String> path = Optional
            .ofNullable(queryParams.getFirst(Constants.PATH_FIELD)); // the path to drill down on
        Optional<Integer> start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD))
            .flatMap(Utils::strToInt); // for paging
        Optional<Integer> numResults =
            Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD))
                .flatMap(Utils::strToInt).or(() -> Optional.of(20)); // for paging
        Optional<Comparator.MatchType> reqMatchType = Optional
            .ofNullable(queryParams.getFirst(Constants.REQ_MATCH_TYPE))
            .flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
        Optional<Comparator.MatchType> respMatchType = Optional
            .ofNullable(queryParams.getFirst(Constants.RESP_MATCH_TYPE))
            .flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
        Optional<Boolean> includeDiff = Optional
            .ofNullable(queryParams.getFirst(Constants.INCLUDE_DIFF)).flatMap(Utils::strToBool);
		Optional<String> resolution = Optional.ofNullable(queryParams
			.getFirst(Constants.DIFF_RESOLUTION_FIELD));

        /* using array as container for value to be updated since lambda function cannot update outer variables */
        Long[] numFound = {0L};
        String[] app = {"", ""};

        List<MatchRes> matchResList = rrstore.getReplay(replayId).map(replay -> {

            Result<ReqRespMatchResult> result = rrstore
                .getAnalysisMatchResults(replayId, service, path,
                    reqMatchType, respMatchType, start, numResults, resolution);
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
                        reqIds, Collections.emptyList(), Event.RunType.Record);
                requestResult.getObjects().forEach(req -> requestMap.put(req.reqId, req));
            }

            return res.stream().map(matchRes -> {
                Optional<String> request =
                    matchRes.recordReqId
                        .flatMap(reqId -> Optional.ofNullable(requestMap.get(reqId)))
                        .map(event -> event.getPayloadAsJsonString(config));

                Optional<String> recordedRequest = Optional.empty();
                Optional<String> replayedRequest = Optional.empty();
                Optional<String> diff = Optional.empty();
                Optional<String> recordResponse = Optional.empty();
                Optional<String> replayResponse = Optional.empty();
				Optional<String> reqCompDiff = Optional.empty();
				Optional<Comparator.MatchType> reqCompResType =  matchRes.reqCompareRes.map(reqCompRes ->
					reqCompRes.mt);
                if (includeDiff.orElse(false)) {
                    recordedRequest = request;
                    replayedRequest = matchRes.replayReqId
                        .flatMap(rrstore::getRequestEvent)
                        .map(event -> event.getPayloadAsJsonString(config));
	                try {
		                diff = Optional.of(jsonMapper.writeValueAsString(matchRes.respCompareRes.diffs));
		                reqCompDiff = matchRes.reqCompareRes.map(UtilException.rethrowFunction(reqCompRes ->
			                 jsonMapper.writeValueAsString(reqCompRes.diffs)));
	                } catch (JsonProcessingException e) {
		                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
			                "Unable to convert diff to json string")), e);
	                }
	                recordResponse = matchRes.recordReqId.flatMap(rrstore::getResponseEvent)
                        .map(event -> event.getPayloadAsJsonString(config));
                    replayResponse = matchRes.replayReqId.flatMap(rrstore::getResponseEvent)
                        .map(event -> event.getPayloadAsJsonString(config));
                }

                return new MatchRes(matchRes.recordReqId, matchRes.replayReqId,
                    matchRes.reqMatchRes, matchRes.numMatch,
                    matchRes.respCompareRes.mt, matchRes.service, matchRes.path, reqCompResType,
                    diff, reqCompDiff, recordedRequest, replayedRequest, recordResponse, replayResponse);
            }).collect(Collectors.toList());
        }).orElse(Collections.emptyList());

        String json;
        try {
            json = jsonMapper
                .writeValueAsString(new MatchResults(matchResList, numFound[0], app[0], app[1]));
            return Response.ok().type(MediaType.APPLICATION_JSON)
                .entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject(json))).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Error in converting Match results list to Json",
                Constants.REPLAY_ID_FIELD, replayId,
                Constants.APP_FIELD, app,
                Constants.SERVICE_FIELD, service,
                Constants.PATH_FIELD, path
            )
            ));
            return Response.serverError().entity(
                buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    e.getMessage())).build();
        }
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
            .map(event -> event.getPayloadAsJsonString(config));
        Optional<String> replayResponse = replayReqId.flatMap(rrstore::getResponseEvent)
            .map(event -> event.getPayloadAsJsonString(config));

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
            ValidateCompareTemplate validTemplate = Utils.validateTemplateSet(templateSet);
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
        comparatorCache.invalidateAll();
        try (Jedis jedis = config.jedisPool.getResource()) {
            jedis.flushAll();
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().entity("Exception occured while flushing :: " + e.getMessage()).build();
        }
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
            TemplateSet updated = templateSetOpt.flatMap(templateSet -> updateOperationSetOpt.map(updateOperationSet ->
                transformer.updateTemplateSet(templateSet, updateOperationSet)))
                .orElseThrow(() -> new Exception("Missing template set or template update operation set"));

            // Validate updated template set
            ValidateCompareTemplate validTemplate = Utils.validateTemplateSet(updated);
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
            if (name==null) {
                throw new Exception("Name not specified for golden");
            }

            String userId = formParams.getFirst("userId");
            if (userId==null) {
                throw new Exception("userId not specified for golden");
            }

            // Ensure name is unique for a customer and app
            Optional<Recording> recWithSameName = rrstore.getRecordingByName(originalRec.customerId, originalRec.app, name);
            if (recWithSameName.isPresent()) {
                throw new Exception("Golden already present for name - " + name + " .Specify unique name");
            }

            TemplateSet templateSet = rrstore.getTemplateSet(originalRec.customerId, originalRec.app, originalRec
                .templateVersion).orElseThrow(() ->
                new Exception("Unable to find template set mentioned in the specified golden set"));
            TemplateUpdateOperationSet templateUpdateOperationSet = rrstore
                .getTemplateUpdateOperationSet(templateUpdOpSetId).orElseThrow(() ->
                    new Exception("Unable to find Template Update Operation Set of specified id"));
            TemplateSetTransformer setTransformer = new TemplateSetTransformer();
            TemplateSet updatedTemplateSet = setTransformer.updateTemplateSet(templateSet, templateUpdateOperationSet);

	        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully updated template set",
		        Constants.OLD_TEMPLATE_SET_VERSION, templateSet.version, Constants.NEW_TEMPLATE_SET_VERSION, updatedTemplateSet.version,
		        Constants.CUSTOMER_ID_FIELD, updatedTemplateSet.customer, Constants.APP_FIELD
		        , updatedTemplateSet.app, Constants.TEMPLATE_UPD_OP_SET_ID_FIELD,
		        templateUpdOpSetId , Constants.RECORDING_ID, recordingId, Constants.REPLAY_ID_FIELD, replayId)));

            // Validate updated template set
            ValidateCompareTemplate validTemplate = Utils.validateTemplateSet(updatedTemplateSet);
            if(!validTemplate.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message", validTemplate.getMessage() ))).toString()).build();
            }

            String updatedTemplateSetId = rrstore.saveTemplateSet(updatedTemplateSet);
            // TODO With similar update logic find the updated collection id
            String newCollectionName = UUID.randomUUID().toString();
            boolean b = recordingUpdate.applyRecordingOperationSet(replayId, newCollectionName, collectionUpdateOpSetId, originalRec);
            if (!b) throw new Exception("Unable to create an updated collection from existing golden");


            Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst("codeVersion"));
            Optional<String> branch = Optional.ofNullable(formParams.getFirst("branch"));
            Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst("gitCommitId"));
            List<String> tags = Optional.ofNullable(formParams.get("tags")).orElse(new ArrayList<String>());
            Optional<String> comment = Optional.ofNullable(formParams.getFirst("comment"));

            RecordingBuilder recordingBuilder = new RecordingBuilder(new CubeMetaInfo(
            	originalRec.customerId, originalRec.app, originalRec.instanceId), newCollectionName)
                .withStatus(RecordingStatus.Completed).withTemplateSetVersion(updatedTemplateSet.version)
	            .withParentRecordingId(originalRec.getId()).withRootRecordingId(originalRec.rootRecordingId)
                .withName(name).withTags(tags).withCollectionUpdateOpSetId(collectionUpdateOpSetId)
	            .withTemplateUpdateOpSetId(templateUpdOpSetId).withUserId(userId);
	        originalRec.codeVersion.ifPresent(recordingBuilder::withCodeVersion);
	        originalRec.branch.ifPresent(recordingBuilder::withBranch);
	        originalRec.gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
	        originalRec.comment.ifPresent(recordingBuilder::withComment);
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

            RecordingBuilder recordingBuilder = new RecordingBuilder(new CubeMetaInfo(
            	originalRec.customerId, originalRec.app, originalRec.instanceId), newCollectionName)
	            .withStatus(RecordingStatus.Completed).withTemplateSetVersion(templateSet.version)
	            .withParentRecordingId(originalRec.getId()).withRootRecordingId(originalRec.rootRecordingId)
	            .withName(originalRec.name).withTags(originalRec.tags).withArchived(originalRec.archived)
	            .withUserId(originalRec.userId);
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
         * @param config
         */
	@Inject
	public AnalyzeWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
		this.comparatorCache = config.comparatorCache;
		this.recordingUpdate = new RecordingUpdate(config);
	}


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	Config config;
    private final RecordingUpdate recordingUpdate;
    // Template cache to retrieve analysis templates from solr
    final ComparatorCache comparatorCache;

    /**
     * some fields from ReqRespMatchResult and some from Request to be returned by some api calls
     */
	static class MatchRes {


        public MatchRes(Optional<String> recordReqId,
                        Optional<String> replayReqId,
                        Comparator.MatchType reqmt,
                        int numMatch,
                        Comparator.MatchType respmt,
                        String service,
                        String path,
                        Optional<Comparator.MatchType> reqCompResType,
                        Optional<String> diff,
                        Optional<String> reqCompDiff,
                        Optional<String> recordRequest,
                        Optional<String> replayRequest,
                        Optional<String> recordResponse,
                        Optional<String> replayResponse) {
            this.recordReqId = recordReqId;
            this.replayReqId = replayReqId;
            this.reqmt = reqmt;
            this.numMatch = numMatch;
            this.respmt = respmt;
            this.service = service;
            this.path = path;
            this.diff = diff;
            this.reqCompDiff = reqCompDiff;
            this.reqCompResType = reqCompResType;
            this.recordRequest = recordRequest;
            this.replayRequest = replayRequest;
            this.recordResponse = recordResponse;
            this.replayResponse = replayResponse;
        }

        public final Optional<String> recordReqId;
        public final Optional<String> replayReqId;
        public final Comparator.MatchType reqmt;
        public final int numMatch;
        public final Comparator.MatchType respmt;
        public final String service;
        public final String path;
	    //Using JsonRawValue on <Optional> field results in Jackson serialization failure.
	    //Hence getMethods() are used to fetch the value.
        @JsonIgnore
        public final Optional<String> diff;
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
		@JsonIgnore
		public final Optional<Comparator.MatchType> reqCompResType;
	    //JsonRawValue is to avoid Jackson escaping the String while using writeValueAsString
	    @JsonRawValue
	    public String getDiff() {
		    return diff.orElse(null);
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

	    @JsonRawValue Comparator.MatchType getReqCompResType(){return reqCompResType.orElse(null);}
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
