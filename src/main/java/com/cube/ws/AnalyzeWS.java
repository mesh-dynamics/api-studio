/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT;

import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.CompareTemplate;
import com.cube.core.TemplateRegistries;
import com.cube.core.TemplateRegistry;
import com.cube.core.UtilException;
import com.cube.dao.Analysis;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.Analyzer;

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
		
		Optional<Analysis> analysis = Analyzer
				.analyze(replayid, tracefield, rrstore
						, jsonmapper , requestComparatorCache , responseComparatorCache );
		
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

	    Collection<MatchResultAggregate> res = rrstore.getResultAggregate(replayid, service, bypath);
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
		List<String> replayRequestCountResults = rrstore.getReplayRequestCounts(customerId,app,service,replayId);
		String resultJson = replayRequestCountResults.stream().collect(Collectors.joining("," , "[" , "]"));
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(resultJson).build();
	}


	@POST
	@Path("registerTemplateApp/{type}/{customerId}/{appId}")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response registerTemplateApp(@Context UriInfo uriInfo , @PathParam("type") String type,
										@PathParam("customerId") String customerId , @PathParam("appId") String appId,
										String templateRegistryArray) {
		try {
			//TODO study the impact of enabling this flag in other deserialization methods
			//jsonmapper.enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
			TemplateRegistries registries = jsonmapper.readValue(templateRegistryArray , TemplateRegistries.class);
			List<TemplateRegistry> templateRegistries = registries.getTemplateRegistryList();
			TemplateKey.Type templateKeyType;
			if ("request".equalsIgnoreCase(type)) {
				templateKeyType = TemplateKey.Type.Request;
			} else if ("response".equalsIgnoreCase(type)) {
				templateKeyType = TemplateKey.Type.Response;
			} else {
				return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid template type, should be " +
						"either request or response :: "+ type).build();
			}
			templateRegistries.forEach(UtilException.rethrowConsumer(registry -> {
				TemplateKey key = new TemplateKey(customerId , appId , registry.getService()
						, registry.getPath() , templateKeyType);
				rrstore.saveCompareTemplate(key , jsonmapper.writeValueAsString(registry.getTemplate()));
				requestComparatorCache.invalidateKey(key);
				responseComparatorCache.invalidateKey(key);
			}));
			return Response.ok().type(MediaType.TEXT_PLAIN).entity(type.concat(" Compare Templates Registered for :: ")
					.concat(customerId).concat(" :: ").concat(appId)).build();
		} catch (JsonProcessingException e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid JSON String sent " + e.getMessage()).build();
		} catch (Exception e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Error Occured " + e.getMessage()).build();
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
    		CompareTemplate  template = jsonmapper.readValue(templateAsJson , CompareTemplate.class);
			TemplateKey key;
    		if ("request".equalsIgnoreCase(type)) {
				key = new TemplateKey(customerId, appId, serviceName, path , TemplateKey.Type.Request);
			} else if ("response".equalsIgnoreCase(type)) {
				key = new TemplateKey(customerId, appId, serviceName, path , TemplateKey.Type.Response);
			} else {
    			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid template type, should be " +
						"either request or response :: "+ type).build();
			}
			rrstore.saveCompareTemplate(key , templateAsJson);
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
			String recordedRequestId = mRes.recordreqid;
			String replayRequestId = mRes.replayreqid;
			Stream<Analysis.ReqRespMatchResult> recordMatchResultList = rrstore.
					expandOnTrace(recordedRequestId, replayId, true);
			Stream<Analysis.ReqRespMatchResult> replayMatchResultList = rrstore.
					expandOnTrace(replayRequestId, replayId, false);
			String recordJsonArray = getJsonArrayString(recordMatchResultList);
			String replayJsonArray = getJsonArrayString(replayMatchResultList);
			String resultJson = "{\"record\" : " + recordJsonArray + " , \"replay\" : " + replayJsonArray + " }";
			return Response.ok().type(MediaType.
					APPLICATION_JSON).entity(resultJson).build();
		}).orElse(Response.serverError().type(MediaType.TEXT_PLAIN).entity("No Analysis Match Result Found for " +
				"recordReqId:replayId :: " + recordReqId + ":" + replayId).build());
	}

    /**
     * Return Time Line results for a given customer id , app , instance id combo
     * Optional Parameters include restriction on <i>collection</i> id (later we should be able to specify
     * a range of collection ids or dates)
     * Return results segragated by path if <i>bypath</i> variable is set y in query params
     * We can also restrict the results to a particular gateway service (which is what should
     * be done ideally) using <i>service</i> query param
     * Note the replays are sorted in descending order of date/time once all the above filters are applied,
     * and stats for only <i>numresults</i> number of results/replays are returned
     * @param urlInfo
     * @param customer
     * @param app
     * @param instanceId
     * @return
     */
    @GET
	@Path("timelineres/{customer}/{app}/{instanceId}")
	public Response getTimelineResults(@Context UriInfo urlInfo, @PathParam("customer") String customer,
                                       @PathParam("app") String app, @PathParam("instanceId") String instanceId) {
        MultivaluedMap<String, String> queryParams = urlInfo.getQueryParameters();
        Optional<String> service = Optional.ofNullable(queryParams.getFirst("service"));
        Optional<String> collection = Optional.ofNullable(queryParams.getFirst("collection"));
        boolean bypath = Optional.ofNullable(queryParams.getFirst("bypath"))
            .map(v -> v.equals("y")).orElse(false);
        Optional<Integer> numResults = Optional.ofNullable(queryParams.getFirst("numresults")).
            map(Integer::valueOf).or(() -> Optional.of(20));
        Stream<Replay> replays = rrstore.getReplay(Optional.of(customer),Optional.of(app),Optional.of(instanceId),
            Replay.ReplayStatus.Completed , numResults, collection);
        String finalJson = replays.map(replay -> {
            String replayid = replay.replayid;
            String creationTimeStamp = replay.creationTimeStamp;
            Collection<MatchResultAggregate> res = rrstore.getResultAggregate(replayid, service, bypath);
            StringBuilder jsonBuilder = new StringBuilder();
            String json;
            jsonBuilder.append("{ \"replayid\" : \"" + replayid + "\" , \"timestamp\" : \"" +  creationTimeStamp
                + "\" , \"results\" : " );
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
        }).collect(Collectors.joining(" , " , "[" , "]"));
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(finalJson).build();
    }

	/**
	 * @param config
	 */
	@Inject
	public AnalyzeWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
		this.requestComparatorCache = config.requestComparatorCache;
		this.responseComparatorCache = config.responseComparatorCache;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	// Template cache to retrieve analysis templates from solr
	final RequestComparatorCache requestComparatorCache;
	final ResponseComparatorCache responseComparatorCache;
}
