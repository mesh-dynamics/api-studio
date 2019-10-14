/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.cache.ReplayResultCache;
import com.cube.core.Utils;
import com.cube.dao.Recording;
import com.cube.dao.Replay;
import com.cube.dao.Replay.ReplayStatus;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.ReplayDriver;
import org.json.JSONArray;
import org.json.JSONObject;

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
            LOGGER.info(String.format("No transformation strings found %s", xfmsParam));
            return Response.ok("{}", MediaType.APPLICATION_JSON).build();
        }
        try {
            String xfms = "";
            // expect only one JSON String.
            if (xfmsParam.size() > 1) {
                LOGGER.error("Expected only one json string but got multiple: " + xfmsParam.size() + "; Only considering the first.");
            }
            xfms = xfmsParam.get(0);
            Optional<Replay> replay = ReplayDriver.getStatus(replayid, this.rrstore);
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
        Optional<Replay> replay = ReplayDriver.getStatus(replayid, rrstore);
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
        Optional<Replay> replay = ReplayDriver.getStatus(replayid, this.rrstore);

        Response resp = replay.map(r -> {
            rrstore.invalidateCurrentCollectionCache(r.customerid, r.app, r.instanceid);
            if (r.status != ReplayStatus.Running && r.status != ReplayStatus.Init) {
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
            //replayResultCache.stopReplay(r.customerid, r.app, r.instanceid, replayid);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayid: " + replayid).build());
        return resp;
    }


    /**
     * This is used only for unit testing purposes to explicitly have a replay in start mode, so that mocking
     * can be tested
     * @param ui
     * @param replayid
     * @return
     */
    @POST
    @Path("forcestart/{replayid}")
    public Response forceStart(@Context UriInfo ui,
                               @PathParam("replayid") String replayid) {
        Optional<Replay> replay = ReplayDriver.getStatus(replayid, this.rrstore);

        Response resp = replay.map(r -> {
            if (r.status != ReplayStatus.Init) {
                return Response.ok(String.format("Replay id state is not Init: %s", r.status.toString())).build();
            }
            String json;
            try {
                r.status = ReplayStatus.Running;
                json = jsonmapper.writeValueAsString(r);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Replay object to Json for replayid %s", replayid), e);
                return Response.serverError().build();
            }
            if (!rrstore.saveReplay(r)) {
                return Response.serverError().build();
            }
            //replayResultCache.startReplay(r.customerid, r.app, r.instanceid, replayid);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayid: " + replayid).build());
        return resp;
    }


    @POST
    @Path("start/{recordingid}")
    @Consumes("application/x-www-form-urlencoded")
    public Response start(@Context UriInfo ui,
                          @PathParam("recordingid") String recordingid,
                          MultivaluedMap<String, String> formParams) {
        /**
         // Block for testing -- we need to initialize the auth token to inject
         List<String> xfmsParam = Optional.ofNullable(formParams.get("requestTransforms")).orElse(new ArrayList<String>());
         Optional<Replay> replay = Optional.ofNullable(null);
         if (xfmsParam.size() == 0) {
         LOGGER.sinfo(String.format("No transformation strings found %s",  xfmsParam));
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

        // TODO: move all these constant strings to a file so we can easily change them.
        boolean async = Optional.ofNullable(formParams.getFirst("async"))
            .map(v -> {
                return (v == "t") ? true : false;
            })
            .orElse(false);
        List<String> reqids = Optional.ofNullable(formParams.get("reqids")).orElse(new ArrayList<String>());
        Optional<String> endpoint = Optional.ofNullable(formParams.getFirst("endpoint"));
        List<String> paths = Optional.ofNullable(formParams.get("paths")).orElse(new ArrayList<String>());
        Optional<Double> samplerate = Optional.ofNullable(formParams.getFirst("samplerate")).flatMap(v -> Utils.strToDouble(v));
        List<String> intermediateServices = Optional.ofNullable(formParams.get("intermservice")).orElse(new ArrayList<>());

        if (!formParams.containsKey("userid")) {
            return Response.status(Status.BAD_REQUEST).entity((new JSONObject(Map.of("Message","userid Not Specified"))).toString()).build();
        }

        String userid = formParams.getFirst("userid");

        Optional<Recording> recordingOpt = rrstore.getRecording(recordingid);
        if (recordingOpt.isEmpty()) {
            LOGGER.error(String.format("Cannot init Replay since cannot find recording for id %s", recordingid));
            return Response.status(Status.NOT_FOUND).entity(String.format("cannot find recording for id %s", recordingid)).build();
        }

        Recording recording = recordingOpt.get();

        // TODO: add <user> who initiates the replay to the "key" in addition to customerid, app, instanceid
        List running_replays = rrstore.getReplay(Optional.ofNullable(recording.customerid), Optional.ofNullable(recording.app),
            Optional.ofNullable(recording.instanceid), ReplayStatus.Running).map(replay -> replay.replayid).collect(Collectors.toList()) ;
        if (!running_replays.isEmpty()) {
            return Response.status(Status.FORBIDDEN).entity((new JSONObject(Map.of("Force Complete", new JSONArray(running_replays)))).toString()).build();
        }

        // check if recording or replay is ongoing for (customer, app, instanceid)
        Optional<Response> errResp = WSUtils.checkActiveCollection(rrstore, Optional.ofNullable(recording.customerid), Optional.ofNullable(recording.app),
            Optional.ofNullable(recording.instanceid));
        if (errResp.isPresent()) {
            return errResp.get();
        }

            // TODO need to enforce template version specification later
        /*if (!formParams.containsKey("templateSetVer")) {
            return Response.status(Status.BAD_REQUEST).entity("{\"Cause\" : \"Template Set Version Not Specified\"}").build();
        }*/

        Optional<String> templateSetVersion = Optional.ofNullable(formParams
            .getFirst("templateSetVer"))/*.orElse(Recording.DEFAULT_TEMPLATE_VER)*/;

        return endpoint.map(e -> {
                        // TODO: introduce response transforms as necessary
                        return ReplayDriver.initReplay(e, recording.customerid, recording.app, recording.instanceid, recording.collection, userid,
                            reqids, rrstore, async, paths, null, samplerate, intermediateServices,templateSetVersion)
                            .map(replayDriver -> {
                                String json;
                                Replay replay = replayDriver.getReplay();
                                try {
                                    json = jsonmapper.writeValueAsString(replay);
                                    boolean status = replayDriver.start();
                                    if (status) {
                                        return Response.ok(json, MediaType.APPLICATION_JSON).build();
                                    }
                                    return Response.status(Response.Status.CONFLICT).entity("Not able to start replay. It may be already running or completed").build();
                                } catch (JsonProcessingException ex) {
                                    LOGGER.error(String.format("Error in converting Replay object to Json for replayid %s", replay.replayid), ex);
                                    return Response.serverError().build();
                                }
                            }).orElse(Response.serverError().build());
                }).orElse(Response.status(Status.BAD_REQUEST).entity("Endpoint not specified").build());

    }


	/**
	 * @param config
	 */
	@Inject
	public ReplayWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
		this.replayResultCache = config.replayResultCache;
		this.config = config;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	ReplayResultCache replayResultCache;
	private final Config config;
}
