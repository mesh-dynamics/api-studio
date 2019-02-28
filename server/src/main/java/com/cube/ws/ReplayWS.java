/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.*;
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
import com.cube.drivers.Replay.ReplayStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author prasad
 * The replay service
 */
@Path("/rs")
public class ReplayWS {

    private static final Logger LOGGER = LogManager.getLogger(ReplayWS.class);

	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response health() {
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"Replay service status\": \"RS is healthy\"}").build();
	}


	@POST
	@Path("init/{customerid}/{app}/{collection}")
	@Consumes("application/x-www-form-urlencoded")
	public Response init(@Context UriInfo ui, 
			@PathParam("collection") String collection, 
			MultivaluedMap<String, String> formParams,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
		// TODO: move all these constant strings to a file so we can easily change them.
		boolean async = Optional.ofNullable(formParams.getFirst("async"))
				.map(v -> {return (v == "t") ? true : false;})
				.orElse(false);
		List<String> reqids = Optional.ofNullable(formParams.get("reqids")).orElse(new ArrayList<String>());
		Optional<String> endpoint = Optional.ofNullable(formParams.getFirst("endpoint"));
		Optional<String> instanceid = Optional.ofNullable(formParams.getFirst("instanceid"));
		List<String> paths = Optional.ofNullable(formParams.get("paths")).orElse(new ArrayList<String>());
		
		// TODO: add <user> who initiates the replay to the "key" in addition to customerid, app, instanceid
		Stream<Replay> replays = rrstore.getReplay(Optional.ofNullable(customerid), Optional.ofNullable(app), instanceid, ReplayStatus.Running);
		String s = replays.map(r -> r.replayid).reduce("", (res, x) -> res + "; " + x);
		if (!s.isEmpty()) {
			return Response.ok(String.format("{\"Force complete these replay ids: %s\"}", s)).build();
		}
		
		return endpoint
				.map(e -> {
					return instanceid.map(inst -> {
						// TODO: introduce response transforms as necessary
						return Replay.initReplay(e, customerid, app, inst, collection, reqids, rrstore, async, paths, null)
								.map(replay -> {
									String json;
									try {
										json = jsonmapper.writeValueAsString(replay);
										return Response.ok(json, MediaType.APPLICATION_JSON).build();
									} catch (JsonProcessingException ex) {							
										LOGGER.error(String.format("Error in converting Replay object to Json for replayid %s", replay.replayid), ex);
										return Response.serverError().build();
									}
								}).orElse(Response.serverError().build());
					}).orElse(Response.status(Status.BAD_REQUEST).entity("Instanceid not specified").build());
				}).orElse(Response.status(Status.BAD_REQUEST).entity("Endpoint not specified").build());		
	}

	
	@POST
	@Path("transforms/{customerid}/{app}/{collection}/{replayid}")
	@Consumes("application/x-www-form-urlencoded")
	public Response upsertTransforms(@Context UriInfo ui, 
			MultivaluedMap<String, String> formParams, 
			@PathParam("customerid") String customerid,
			@PathParam("app") String app, 
			@PathParam("collection") String collection,
			@PathParam("replayid") String replayid) {
		// {"requestTransforms" : [{"src_path_into_body: xyz, "tgt_path_into_body" : abc}*]}
		
		List<String> xfmsParam = Optional.ofNullable(formParams.get("requestTransforms")).orElse(new ArrayList<String>());
		
		if (xfmsParam.size() == 0) {
			LOGGER.info(String.format("No transformation strings found %s",  xfmsParam));
			return Response.ok("{}", MediaType.APPLICATION_JSON).build();
		}
		try {
			String xfms = "";
			// expect only one JSON String. 
			if (xfmsParam.size() > 1) {
				LOGGER.error("Expected only one json string but got multiple: " + xfmsParam.size() + "; Only considering the first.");
			} 
			xfms = xfmsParam.get(0);
			Optional<Replay> replay = Replay.getStatus(replayid, this.rrstore);
			if (replay.isPresent()) {
				replay.get().updateXfmsFromJSONString(xfms);
			}
			String replayJson = jsonmapper.writeValueAsString(replay);
			return Response.ok(replayJson, MediaType.APPLICATION_JSON).build();
		} catch (Exception e) {
			LOGGER.error(String.format("Error in updating transforms %s: %s", replayid, xfmsParam.toString()), e);
			return Response.serverError().build();
		}
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
	@Path("forcecomplete/{replayid}")
	public Response forceComplete(@Context UriInfo ui, 
								  @PathParam("replayid") String replayid) {
		Optional<Replay> replay = Replay.getStatus(replayid, this.rrstore);
		
		Response resp = replay.map(r -> {
			if (r.status != ReplayStatus.Running || r.status != ReplayStatus.Init) {
				return Response.ok(String.format("Replay id state is already terminal: %s", r.status.toString())).build();
			}
			String json;
			try {
				r.status = ReplayStatus.Error;
				json = jsonmapper.writeValueAsString(r);
			} catch (JsonProcessingException e) {
				LOGGER.error(String.format("Error in converting Replay object to Json for replayid %s", replayid), e);
				return Response.serverError().build();
			}
			if (!rrstore.saveReplay(r)) {
				return Response.serverError().build();
			}
			return Response.ok(json, MediaType.APPLICATION_JSON).build();
		}).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayid: " + replayid).build());
		return resp;
	}
	

	@POST
	@Path("start/{customerid}/{app}/{collection}/{replayid}")
	@Consumes("application/x-www-form-urlencoded")
	public Response start(@Context UriInfo ui, @PathParam("collection") String collection, 
			@PathParam("replayid") String replayid,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app, 
			MultivaluedMap<String, String> formParams) {
		
		/**
		// Block for testing -- we need to initialize the auth token to inject
		List<String> xfmsParam = Optional.ofNullable(formParams.get("requestTransforms")).orElse(new ArrayList<String>());
		Optional<Replay> replay = Optional.ofNullable(null);
		if (xfmsParam.size() == 0) {
			LOGGER.info(String.format("No transformation strings found %s",  xfmsParam));
			return Response.ok("{}", MediaType.APPLICATION_JSON).build();
		}
		try {
			String xfms = "";
			// expect only one JSON String. 
			if (xfmsParam.size() > 1) {
				LOGGER.error("Expected only one json string but got multiple: " + xfmsParam.size() + "; Only considering the first.");
			} 
			xfms = xfmsParam.get(0);
			replay = Replay.getStatus(replayid, this.rrstore);
			if (replay.isPresent()) {
				replay.get().updateXfmsFromJSONString(xfms);
			}
		} catch (Exception e) {
		
		}
		/// end block for testing
		 */
		 
		Optional<Replay> replay = Replay.getStatus(replayid, this.rrstore);
		Response resp = replay.map(r -> {
			boolean status = r.start();
			if (status) {
				return Response.ok().build();
			} 
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
