/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.dao.ReqRespStore;
import com.cube.drivers.Replay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author prasad
 * The replay service
 */
@Path("/rs")
public class ReplayWS {

    private static final Logger LOGGER = LogManager.getLogger(ReplayWS.class);

	@POST
	@Path("init/{customerid}/{app}/{collection}")
	@Consumes("application/x-www-form-urlencoded")
	public Response init(@Context UriInfo ui, @PathParam("collection") String collection, 
			MultivaluedMap<String, String> formParams,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
		boolean async = Optional.ofNullable(formParams.get("async"))
				.flatMap(vals -> vals.stream().findFirst().map(v -> {return (v == "t") ? true : false;}))
				.orElse(false);
		List<String> reqids = Optional.ofNullable(formParams.get("reqids")).orElse(new ArrayList<String>());
		Optional<String> endpoint = Optional.ofNullable(formParams.get("endpoint"))
				.flatMap(vals -> vals.stream().findFirst());
		List<String> paths = Optional.ofNullable(formParams.get("paths")).orElse(new ArrayList<String>());

		Optional<Response> resp = endpoint
				.flatMap(e -> Replay.initReplay(e, customerid, app, collection, reqids, rrstore, async, paths))
				.map(replay -> {
					String json;
					try {
						json = jsonmapper.writeValueAsString(replay);
						return Response.ok(json, MediaType.APPLICATION_JSON).build();
					} catch (JsonProcessingException e) {
						LOGGER.error(String.format("Error in converting Replay object to Json for replayid %s", replay.replayid), e);
						return Response.serverError().build();
					}});
		
		return resp.orElse(endpoint.isPresent() ? 
				Response.serverError().build() : Response.status(Status.BAD_REQUEST).entity("Endpoint not specified").build());
		
	}


	@GET
	@Path("status/{customerid}/{app}/{collection}/{replayid}")
	public Response status(@Context UriInfo ui, @PathParam("collection") String collection, 
			@PathParam("replayid") String replayid,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
		Optional<Replay> replay = Replay.getStatus(replayid, rrstore);
		Response resp = replay.map(r -> {
			String json;
			try {
				json = jsonmapper.writeValueAsString(r);
				return Response.ok(json, MediaType.APPLICATION_JSON).build();
			} catch (JsonProcessingException e) {
				LOGGER.error(String.format("Error in converting Replay object to Json for replayid %s", replayid), e);
				return Response.serverError().build();
			}
		}).orElse(Response.status(Response.Status.NOT_FOUND).entity("Status not found for replayid: " + replayid).build());
		
		return resp;
	}

	@POST
	@Path("start/{customerid}/{app}/{collection}/{replayid}")
	public Response start(@Context UriInfo ui, @PathParam("collection") String collection, 
			@PathParam("replayid") String replayid,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
		Optional<Replay> replay = Replay.getStatus(replayid, rrstore);
		Response resp = replay.map(r -> {
			boolean status = r.start();
			if (status)
				return Response.ok().build();
			else 
				return Response.status(Response.Status.CONFLICT).entity("Not able to start replay. It may be already running or completed").build();
		}).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayid: " + replayid).build());
		
		return resp;
	}

	
	
	/**
	 * @param rrstore
	 */
	@Inject
	public ReplayWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
}
