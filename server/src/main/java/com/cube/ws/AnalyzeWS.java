/**
 * Copyright Cube I O
 */
package com.cube.ws;

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

import com.cube.dao.MatchResultAggregate;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.Analysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
		
		Optional<Analysis> analysis = Analysis.analyze(replayid, tracefield, rrstore);
		
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
		
		Optional<Analysis> analysis = Analysis.getStatus(replayid, rrstore);
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
	 * @param rrstore
	 */
	@Inject
	public AnalyzeWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
}
