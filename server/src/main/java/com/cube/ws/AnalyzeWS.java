/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.cache.RequestComparatorCache;
import com.cube.cache.TemplateCache;
import com.cube.cache.TemplateKey;
import com.cube.core.CompareTemplate;
import com.cube.dao.Analysis;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.Analyzer;
import com.cube.exception.CacheException;

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
				.analyze(replayid, tracefield, rrstore, jsonmapper, templateCache , requestComparatorCache);
		
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
	@Path("registerTemplate/{type}/{appId}/{customerId}/{serviceName}/{path:.+}")
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
			templateCache.invalidateKey(key);
			requestComparatorCache.invalidateKey(key);
			//Analyzer.removeKey(key);
			return Response.ok().type(MediaType.TEXT_PLAIN).entity("Json String successfully stored in Solr").build();
		} catch (JsonProcessingException e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid JSON String sent").build();
    	} catch (IOException e) {
    		return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Error Occured " + e.getMessage()).build();
		} catch (CacheException e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Unable to invalidate cache entry " +
					"for corresponding template").build();
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
		this.templateCache = config.templateCache;
		this.requestComparatorCache = config.requestComparatorCache;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	// Template cache to retrieve analysis templates from solr
	TemplateCache templateCache;
	RequestComparatorCache requestComparatorCache;
}
