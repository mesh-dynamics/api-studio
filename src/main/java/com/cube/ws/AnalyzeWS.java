/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.io.IOException;
import java.time.Instant;
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
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import redis.clients.jedis.Jedis;

import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplateRegistries;
import com.cube.core.Utils;
import com.cube.dao.Analysis;
import com.cube.dao.Event;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.Recording;
import com.cube.dao.RecordingOperationSetSP;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;
import com.cube.dao.Result;
import com.cube.drivers.Analyzer;
import com.cube.golden.RecordingUpdate;
import com.cube.golden.ReqRespUpdateOperation;
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


    @Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"Analysis service status\": \"AS is healthy\"}").build();
    }


	@POST
    @Path("analyze/{replayid}")
    @Consumes("application/x-www-form-urlencoded")
    public Response analyze(@Context UriInfo ui, @PathParam("replayid") String replayid,
                            MultivaluedMap<String, String> formParams) {
        String tracefield = Optional.ofNullable(formParams.get("tracefield"))
            .flatMap(vals -> vals.stream().findFirst())
            .orElse(Config.DEFAULT_TRACE_FIELD);
        // override templateSet specified in recording if it is passed in the param
        Optional<String> templateVersion =
            Optional.ofNullable(formParams.getFirst("templateSet"));


        Optional<Analysis> analysis = Analyzer
            .analyze(replayid, tracefield, rrstore
                , jsonmapper, requestComparatorCache, responseComparatorCache, templateVersion);

        return analysis.map(av -> {
            String json;
            try {
                json = jsonmapper.writeValueAsString(av);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Analysis object to Json for replayid %s", replayid), e);
                return Response.serverError().build();
            }
        }).orElse(Response.serverError().build());
    }


	@GET
    @Path("status/{replayid}")
    public Response status(@Context UriInfo ui,
                           @PathParam("replayid") String replayid) {
        Optional<Analysis> analysis = Analyzer.getStatus(replayid, rrstore);
        Response resp = analysis.map(av -> {
            String json;
            try {
                json = jsonmapper.writeValueAsString(av);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Analysis object to Json for replayid %s", replayid), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Analysis not found for replayid: " + replayid).build());
        return resp;
    }

	@GET
    @Path("aggrresult/{replayid}")
    public Response getResultAggregate(@Context UriInfo ui,
                                       @PathParam("replayid") String replayid) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> service = Optional.ofNullable(queryParams.getFirst("service"));
        boolean bypath = Optional.ofNullable(queryParams.getFirst("bypath"))
            .map(v -> v.equals("y")).orElse(false);

        Stream<MatchResultAggregate> resStream = rrstore.getResultAggregate(replayid, service, bypath);
        Collection<MatchResultAggregate> res = resStream.collect(Collectors.toList());

//        Collection<MatchResultAggregate> res = rrstore.computeResultAggregate(replayid, service, bypath);
        String json;
        try {
            json = jsonmapper.writeValueAsString(res);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting result aggregate object to Json for replayid %s", replayid), e);
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
            //jsonmapper.enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
            TemplateRegistries registries = jsonmapper.readValue(templateRegistryArray, TemplateRegistries.class);
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
                rrstore.saveCompareTemplate(key, jsonmapper.writeValueAsString(registry.getTemplate()));
                requestComparatorCache.invalidateKey(key);
                responseComparatorCache.invalidateKey(key);

            }));
            */
            Optional<String> templateVersion = version.equals("AUTO") ? Optional.empty() : Optional.of(version);
            TemplateSet templateSet = Utils.templateRegistriesToTemplateSet(registries, customerId, appId, templateVersion);
            Utils.invalidateCacheFromTemplateSet(templateSet, requestComparatorCache, responseComparatorCache);
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
            CompareTemplate template = jsonmapper.readValue(templateAsJson, CompareTemplate.class);
            TemplateKey key;
            if ("request".equalsIgnoreCase(type)) {
                key = new TemplateKey(Optional.empty(), customerId, appId, serviceName, path, TemplateKey.Type.Request);
            } else if ("response".equalsIgnoreCase(type)) {
                key = new TemplateKey(Optional.of("DEFAULT"), customerId, appId, serviceName, path,
                    TemplateKey.Type.Response);
            } else {
                return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid template type, should be " +
                    "either request or response :: " + type).build();
            }
            rrstore.saveCompareTemplate(key, templateAsJson);
            requestComparatorCache.invalidateKey(key);
            responseComparatorCache.invalidateKey(key);
            //Analyzer.removeKey(key);
            return Response.ok().type(MediaType.TEXT_PLAIN).entity("Json String successfully stored in Solr").build();
        } catch (JsonProcessingException e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid JSON String sent").build();
        } catch (IOException e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Error Occured " + e.getMessage()).build();
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
        Optional<String> apipath = Optional.ofNullable(queryParams.getFirst("apipath"));
        Optional<String> jsonpath = Optional.ofNullable(queryParams.getFirst("jsonpath"));

        if (apipath.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity("{\"Error\": \"apipath is mssing\"}").build();
        }
        if (jsonpath.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity("{\"Error\": \"jsonpath is mssing\"}").build();
        }

        TemplateKey tkey = new TemplateKey(Optional.of(templateVersion), customerId, appId, service, apipath.get(),
            ruleType);

        Optional<TemplateEntry> rule = rrstore.getCompareTemplate(tkey)
            .map(template -> template.getRule(jsonpath.get()));

        if (rule.isPresent()) {
            try {
                return Response.status(Response.Status.OK).type(MediaType.APPLICATION_JSON)
                    .entity(jsonmapper.writeValueAsString(rule.get())).build();
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
	private String getJsonArrayString(Stream<Analysis.ReqRespMatchResult> reqRespMatchResults) {
		return reqRespMatchResults.flatMap(result -> {
			try {
				return Stream.of(jsonmapper.writeValueAsString(result));
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
        Optional<Analysis.ReqRespMatchResult> matchResult =
            rrstore.getAnalysisMatchResult(recordReqId, replayId);
        return matchResult.map(mRes -> {
            Stream<Analysis.ReqRespMatchResult> recordMatchResultList = rrstore.
                expandOnTrace(mRes, true);
            Stream<Analysis.ReqRespMatchResult> replayMatchResultList = rrstore.
                expandOnTrace(mRes, false);
            String recordJsonArray = getJsonArrayString(recordMatchResultList);
            String replayJsonArray = getJsonArrayString(replayMatchResultList);
            String resultJson = "{\"record\" : " + recordJsonArray + " , \"replay\" : " + replayJsonArray + " }";
            return Response.ok().type(MediaType.
                APPLICATION_JSON).entity(resultJson).build();
        }).orElse(Response.serverError().type(MediaType.TEXT_PLAIN).entity("No Analysis Match Result Found for " +
            "recordReqId:replayId :: " + recordReqId + ":" + replayId).build());
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
        List<String> instanceId = Optional.ofNullable(queryParams.get("instanceId")).orElse(Collections.EMPTY_LIST);
        Optional<String> service = Optional.ofNullable(queryParams.getFirst("service"));
        Optional<String> collection = Optional.ofNullable(queryParams.getFirst("collection"));
        boolean bypath = Optional.ofNullable(queryParams.getFirst("bypath"))
            .map(v -> v.equals("y")).orElse(false);
        Optional<Integer> numResults = Optional.ofNullable(queryParams.getFirst("numresults")).
            map(Integer::valueOf).or(() -> Optional.of(20));
        Stream<Replay> replays = rrstore.getReplay(Optional.of(customer), Optional.of(app), instanceId,
            List.of(Replay.ReplayStatus.Completed, Replay.ReplayStatus.Error), numResults, collection);
        String finalJson = replays.map(replay -> {
            String replayid = replay.replayid;
            String creationTimeStamp = replay.creationTimeStamp;
            Optional<Recording> recordingOpt = rrstore.getRecordingByCollectionAndTemplateVer(replay.customerid, replay.app
                ,  replay.collection , replay.templateVersion);
            String recordingInfo = "";
            if (recordingOpt.isEmpty()) {
                LOGGER.error("Unable to find recording corresponding to given replay");
            } else {
                Recording recording = recordingOpt.get();
                recordingInfo = "\" , \"recordingid\" : \"" + recording.getId()
                    + "\" , \"collection\" : \"" + recording.collection
                    + recording.templateVersion.map(templatever -> "\" , \"templateVer\" : \"" + templatever).orElse("");
            }

            Stream<MatchResultAggregate> resStream = rrstore.getResultAggregate(replayid, service, bypath);
            Collection<MatchResultAggregate> res = resStream.collect(Collectors.toList());

//            Collection<MatchResultAggregate> res = rrstore.computeResultAggregate(replayid, service, bypath);
            StringBuilder jsonBuilder = new StringBuilder();
            String json;
            jsonBuilder.append("{ \"replayid\" : \"" + replayid + "\" , \"timestamp\" : \"" + creationTimeStamp
                + recordingInfo +  "\" , \"results\" : ");
            try {
                json = jsonmapper.writeValueAsString(res);
                jsonBuilder.append(json);
            } catch (JsonProcessingException e) {
                jsonBuilder.append("[]");
                LOGGER.error(String.format("Error in converting result aggregate object to Json for replayid %s",
                    replayid), e);
            }
            jsonBuilder.append("}");
            return jsonBuilder.toString();
        }).collect(Collectors.joining(" , ", "[", "]"));
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(finalJson).build();
    }


    /**
     *
     * @param ui
     * @return the results for reqids matching a path and other constraints
     */
    @GET
    @Path("analysisResByPath/{replayId}")
    public Response getResultsByPath(@Context UriInfo ui, @PathParam("replayId") String replayId) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> service = Optional.ofNullable(queryParams.getFirst("service"));
        Optional<String> path = Optional.ofNullable(queryParams.getFirst("path")); // the path to drill
        // down on
        Optional<Integer> start = Optional.ofNullable(queryParams.getFirst("start")).flatMap(Utils::strToInt); // for
        // paging
        Optional<Integer> nummatches =
            Optional.ofNullable(queryParams.getFirst("nummatches")).flatMap(Utils::strToInt).or(() -> Optional.of(20)); //
        // for paging
        Optional<Comparator.MatchType> reqmt = Optional.ofNullable(queryParams.getFirst("reqmt"))
                .flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
        Optional<Comparator.MatchType> respmt = Optional.ofNullable(queryParams.getFirst("respmt"))
            .flatMap(v -> Utils.valueOf(Comparator.MatchType.class, v));
        Optional<Boolean> includeDiff = Optional.ofNullable(queryParams.getFirst("includediff")).flatMap(Utils::strToBool);


        /* using array as container for value to be updated since lambda function cannot update outer variables */
        Long[] numFound = {0L};

        List<MatchRes> matchResList = rrstore.getReplay(replayId).map(replay -> {

            Result<Analysis.ReqRespMatchResult> result = rrstore.getAnalysisMatchResults(replayId, service, path,
                reqmt, respmt, start, nummatches);
            numFound[0] = result.numFound;
            List<Analysis.ReqRespMatchResult> res = result.getObjects().collect(Collectors.toList());
            List<String> reqids = res.stream().map(r -> r.recordreqid).flatMap(Optional::stream).collect(Collectors.toList());

            Map<String, com.cube.dao.Request> requestMap = new HashMap<>();
            if (!reqids.isEmpty()) {
                // empty reqid list would lead to returning of all requests, so check for it
                Result<com.cube.dao.Request> requestResult = rrstore.getRequests(replay.customerid, replay.app, replay.collection,
                    reqids, Collections.emptyList(), Event.RecordReplayType.Record);
                requestResult.getObjects().forEach(req -> req.reqid.ifPresent(reqidv -> requestMap.put(reqidv, req)));
            }

            return res.stream().map(matchRes -> {
                Optional<com.cube.dao.Request> request =
                    matchRes.recordreqid.flatMap(reqid -> Optional.ofNullable(requestMap.get(reqid)));

                Optional<String> diff = Optional.empty();
                Optional<com.cube.dao.Response> recordResponse = Optional.empty();
                Optional<com.cube.dao.Response> replayResponse = Optional.empty();

                if(includeDiff.orElse(false)) {
                    diff = Optional.of(matchRes.diff);
                    recordResponse = matchRes.recordreqid.flatMap(rrstore::getResponse);
                    replayResponse = matchRes.replayreqid.flatMap(rrstore::getResponse);
                }

                return new MatchRes(matchRes.recordreqid, matchRes.replayreqid, matchRes.reqmt, matchRes.nummatch,
                    matchRes.respmt, matchRes.path,
                    request.map(req -> req.qparams).orElse(new MultivaluedHashMap<>()),
                    request.map(req -> req.fparams).orElse(new MultivaluedHashMap<>()), request.map(req -> req.method),
                    diff, recordResponse, replayResponse);
            }).collect(Collectors.toList());
        }).orElse(Collections.emptyList());

        String json;
        try {
            json = jsonmapper.writeValueAsString(new MatchResults(matchResList, numFound[0]));
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Match results list to Json for replayid %s, app %s, " +
                    "collection %s.", replayId));
            return Response.serverError().build();
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
        Optional<String> recordReqId = Optional.ofNullable(queryParams.getFirst("recordReqId"));
        Optional<String> replayReqId = Optional.ofNullable(queryParams.getFirst("replayReqId"));

        Optional<Analysis.ReqRespMatchResult> matchResult =
            rrstore.getAnalysisMatchResult(recordReqId, replayReqId, replayId);
        Optional<com.cube.dao.Response> recordResponse = recordReqId.flatMap(rrstore::getResponse);
        Optional<com.cube.dao.Response> replayResponse = replayReqId.flatMap(rrstore::getResponse);


        String json;
        try {
            json = jsonmapper.writeValueAsString(new RespAndMatchResults(recordResponse, replayResponse, matchResult));
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting response and match results to Json for replayid %s, " +
                "recordreqid %s, replay reqid %s", replayId, recordReqId, replayReqId));
            return Response.serverError().build();
        }
    }

    @POST
    @Path("saveTemplateSet/{customer}/{app}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveTemplateSet(@Context UriInfo uriInfo, @PathParam("customer") String customer,
                                    @PathParam("app") String app, TemplateSet templateSet) {
        try {
            String templateSetId = rrstore.saveTemplateSet(templateSet);
            return Response.ok("{\"Message\" :  \"Successfully saved template set\" , \"ID\" : \"" +
                templateSetId + "\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("{\"Message\" :  \"Unable to save template set\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }

    @POST
    @Path("cache/flushall")
    public Response cacheFlushAll() {
        requestComparatorCache.invalidateAll();
        responseComparatorCache.invalidateAll();
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
            Map<TemplateKey, SingleTemplateUpdateOperation> updates = jsonmapper.readValue(templateUpdateOperations,
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
            // save the new template set (and return the new version as a part of the response)
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
     * Create a new golden set given a collection and template set id
     * @param collection Collection name/id
     * @param templateSetId Template set id
     * @return Appropriate response
     */
/*    @GET
    @Path("createGoldenSet/{collection}/{templateSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGoldenSet(@PathParam("collection") String collection,
                                    @PathParam("templateSetId") String templateSetId) {
        try {
            String goldenSetId = rrstore.createGoldenSet(collection, templateSetId, Optional.empty(), Optional.empty());
            return Response.ok().entity("{\"Message\" :  \"Golden set successfully created\" , \"ID\" : \"" +
                goldenSetId + "\"}").build();
        } catch (Exception e) {
            LOGGER.error("Error while creating golden set :: "  + e.getMessage());
            return Response.serverError().entity("{\"Message\" :  \"Error while creating golden set\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Path("goldenSet/getAll/{rootGoldenSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAllGoldenSetsWithRoot(@PathParam("rootGoldenSetId") String rootGoldenSetId) {
        try {
            List<GoldenSet> goldenSetList = rrstore.getAllDerivedGoldenSets(rootGoldenSetId).collect(Collectors.toList());
            String asJson = jsonmapper.writeValueAsString(goldenSetList);
            return Response.ok().entity(asJson).build();
        } catch (Exception e) {
            LOGGER.error("Error while retrieving golden sets for root :: "  + rootGoldenSetId + " :: " + e.getMessage());
            return Response.serverError().entity("{\"Message\" :  \"Error while retrieving golden sets with given root\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
        }
    }*/


    /**
     * Update an existing recording with the specified template update and collection
     * update operation set, and create a new golden set with the modified template set
     * and collection
     * @param recordingId Source Recording (combination of collection and template set)
     * @param collectionUpdateOpSetId The collection update operation set id
     * @param templateUpdOpSetId Template update operation set id
     * @return Appropriate response
     */
    @GET
    @Path("updateGoldenSet/{recordingId}/{replayId}/{collectionUpdOpSetId}/{templateUpdOpSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateGoldenSet(@PathParam("recordingId") String recordingId,
                                    @PathParam("replayId") String replayId,
                                    @PathParam("collectionUpdOpSetId") String collectionUpdateOpSetId,
                                    @PathParam("templateUpdOpSetId") String templateUpdOpSetId) {
        try{
            Recording originalRec = rrstore.getRecording(recordingId).orElseThrow(() ->
                new Exception("Unable to find recording object for the given id"));
            TemplateSet templateSet = rrstore.getTemplateSet(originalRec.customerid, originalRec.app, originalRec
                .templateVersion.orElse(Recording.DEFAULT_TEMPLATE_VER)).orElseThrow(() ->
                new Exception("Unable to find template set mentioned in the specified golden set"));
            TemplateUpdateOperationSet templateUpdateOperationSet = rrstore
                .getTemplateUpdateOperationSet(templateUpdOpSetId).orElseThrow(() ->
                    new Exception("Unable to find Template Update Operation Set of specified id"));
            TemplateSetTransformer setTransformer = new TemplateSetTransformer();
            TemplateSet updatedTemplateSet = setTransformer.updateTemplateSet(templateSet, templateUpdateOperationSet);
            String updatedTemplateSetId = rrstore.saveTemplateSet(updatedTemplateSet);
            // TODO With similar update logic find the updated collection id
            String newCollectionName = originalRec.collection.concat("-").concat(UUID.randomUUID().toString());
            boolean b = recordingUpdate.applyRecordingOperationSet(replayId, newCollectionName, collectionUpdateOpSetId, originalRec);
            if (!b) throw new Exception("Unable to create an updated collection from existing golden");

            Recording updatedRecording = new Recording(originalRec.customerid,
                originalRec.app, originalRec.instanceid, newCollectionName, Recording.RecordingStatus.Completed,
                Optional.of(Instant.now()), Optional.of(updatedTemplateSet.version), Optional.of(originalRec.getId()),
                originalRec.rootRecordingId.or(() -> Optional.of(originalRec.getId())));

            rrstore.saveRecording(updatedRecording);
            return Response.ok().entity("{\"Message\" :  \"Successfully created new recording with specified original recording " +
                "and set of operations\" , \"ID\" : \"" + updatedRecording.getId() + "\"}").build();
        } catch (Exception e) {
            LOGGER.error("Error while updating golden set :: "  + e.getMessage());
            return Response.serverError().entity("{\"Message\" :  \"Error while updating recording\" , \"Error\" : \"" +
                e.getMessage() + "\"}").build();
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
            json = jsonmapper.writeValueAsString(Map.of("operationSetId", operationSetId));
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
                        String json = jsonmapper.writeValueAsString(operationSet);
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
                response = jsonmapper
                    .writeValueAsString(Map.of("Message" , "Successfully updated Recording Update Operation Set"
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
		this.jsonmapper = config.jsonmapper;
		this.config = config;
		this.requestComparatorCache = config.requestComparatorCache;
		this.responseComparatorCache = config.responseComparatorCache;
		this.recordingUpdate = new RecordingUpdate((ReqRespStoreSolr) rrstore, jsonmapper);
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	Config config;
    private final RecordingUpdate recordingUpdate;
    // Template cache to retrieve analysis templates from solr
	final RequestComparatorCache requestComparatorCache;
	final ResponseComparatorCache responseComparatorCache;

    /**
     * some fields from ReqRespMatchResult and some from Request to be returned by some api calls
     */
	static class MatchRes {


        public MatchRes(Optional<String> recordreqid,
                        Optional<String> replayreqid,
                        Comparator.MatchType reqmt,
                        int nummatch,
                        Comparator.MatchType respmt,
                        String path,
                        MultivaluedMap<String, String> qparams,
                        MultivaluedMap<String, String> fparams,
                        Optional<String> method, Optional<String> diff,
                        Optional<com.cube.dao.Response> recordResponse,
                        Optional<com.cube.dao.Response> replayResponse) {
            this.recordreqid = recordreqid;
            this.replayreqid = replayreqid;
            this.reqmt = reqmt;
            this.nummatch = nummatch;
            this.respmt = respmt;
            this.path = path;
            this.qparams = qparams;
            this.fparams = fparams;
            this.method = method;
            this.diff = diff;
            this.recordResponse = recordResponse;
            this.replayResponse = replayResponse;
        }

        public final Optional<String> recordreqid;
        public final Optional<String> replayreqid;
        public final Comparator.MatchType reqmt;
        public final int nummatch;
        public final Comparator.MatchType respmt;
        public final String path;
        @JsonDeserialize(as=MultivaluedHashMap.class)
        public final MultivaluedMap<String, String> qparams; // query params
        @JsonDeserialize(as=MultivaluedHashMap.class)
        public final MultivaluedMap<String, String> fparams; // form params
        public final Optional<String> method;
        public final Optional<String> diff;
        public final Optional<com.cube.dao.Response> recordResponse;
        public final Optional<com.cube.dao.Response> replayResponse;


    }

    static class MatchResults {
        public MatchResults(List<MatchRes> res, long numFound) {
            this.res = res;
            this.numFound = numFound;
        }

        public final List<MatchRes> res;
	    public final long numFound;
    }

    static class RespAndMatchResults {


        public RespAndMatchResults(Optional<com.cube.dao.Response> recordResponse, Optional<com.cube.dao.Response> replayResponse,
                                   Optional<Analysis.ReqRespMatchResult> matchResult) {
            this.recordResponse = recordResponse;
            this.replayResponse = replayResponse;
            this.matchResult = matchResult;
        }

        public final Optional<com.cube.dao.Response> recordResponse;
	    public final Optional<com.cube.dao.Response> replayResponse;
	    public final Optional<Analysis.ReqRespMatchResult> matchResult;
    }
}
