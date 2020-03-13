/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import io.md.core.ReplayTypeEnum;

import com.cube.cache.ReplayResultCache;
import com.cube.core.Utils;
import com.cube.dao.CubeMetaInfo;
import com.cube.dao.Recording;
import com.cube.dao.Replay;
import com.cube.dao.Replay.ReplayStatus;
import com.cube.dao.ReplayBuilder;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.AbstractReplayDriver;
import com.cube.drivers.ReplayDriverFactory;
import com.cube.utils.Constants;

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
        Map solrHealth = WSUtils.solrHealthCheck(config.solr);
        Map respMap = new HashMap(solrHealth);
        respMap.put(Constants.SERVICE_HEALTH_STATUS, "RS is healthy");
        return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }


    /*
	@POST
    @Path("transforms/{customerId}/{app}/{collection}/{replayId}")
    @Consumes("application/x-www-form-urlencoded")
    public Response upsertTransforms(@Context UriInfo ui,
                                     MultivaluedMap<String, String> formParams,
                                     @PathParam("customerId") String customerId,
                                     @PathParam("app") String app,
                                     @PathParam("collection") String collection,
                                     @PathParam("replayId") String replayId) {
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
            Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, this.rrstore);
            if (replay.isPresent()) {
                replay.get().updateXfmsFromJSONString(xfms);
            }
            String replayJson = jsonMapper.writeValueAsString(replay);
            return Response.ok(replayJson, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            LOGGER.error(String.format("Error in updating transforms %s: %s", replayId, xfmsParam.toString()), e);
            return Response.serverError().build();
        }

    }
    */

	@GET
	@Path("status/{replayId}")
    public Response status(@Context UriInfo ui, @PathParam("replayId") String replayId) {
        Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, rrstore);
        Response resp = replay.map(r -> {
            String json;
            try {
                // check if there's a current running replay in cache , which matches the replay whose
                // status is required. If there's a current running replay, set the status of the replay
                // to Running before sending the status back.
                Optional<Replay> currentRunningReplay = rrstore.getCurrentRecordOrReplay(Optional.of(r.customerId),
                    Optional.of(r.app), Optional.of(r.instanceId))
                    .flatMap(runningRecordOrReplay -> runningRecordOrReplay.replay);
                r.status = currentRunningReplay.map(runningReplay -> runningReplay.
                    replayId.equals(r.replayId)).orElse(false) ? currentRunningReplay.get().status : r.status;
                json = jsonMapper.writeValueAsString(r);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Replay object to Json for replayId %s", replayId), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Status not found for replayId: " + replayId).build());

        return resp;
    }


	@POST
	@Path("forcecomplete/{replayId}")
    public Response forceComplete(@Context UriInfo ui,
                                  @PathParam("replayId") String replayId) {
        Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, this.rrstore);

        Response resp = replay.map(r -> {
            rrstore.invalidateCurrentCollectionCache(r.customerId, r.app, r.instanceId);
            if (r.status != ReplayStatus.Running && r.status != ReplayStatus.Init) {
                return Response.ok(String.format("Replay id state is already terminal: %s", r.status.toString())).build();
            }
            String json;
            try {
                r.status = ReplayStatus.Error;
                json = jsonMapper.writeValueAsString(r);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Replay object to Json for replayId %s", replayId), e);
                return Response.serverError().build();
            }
            if (!rrstore.saveReplay(r)) {
                return Response.serverError().build();
            }
            //replayResultCache.stopReplay(r.customerId, r.app, r.instanceid, replayId);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayId: " + replayId).build());
        return resp;
    }


    /**
     * This is used only for unit testing purposes to explicitly have a replay in start mode, so that mocking
     * can be tested
     * @param ui
     * @param replayId
     * @return
     */
    @POST
    @Path("forcestart/{replayId}")
    public Response forceStart(@Context UriInfo ui,
                               @PathParam("replayId") String replayId) {
        Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, this.rrstore);

        Response resp = replay.map(r -> {
            String json;
            try {
                r.status = ReplayStatus.Running;
                json = jsonMapper.writeValueAsString(r);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Replay object to Json for replayId %s", replayId), e);
                return Response.serverError().build();
            }
            if (!rrstore.saveReplay(r)) {
                return Response.serverError().build();
            }
            //replayResultCache.startReplay(r.customerId, r.app, r.instanceid, replayId);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayId: " + replayId).build());
        return resp;
    }


    @POST
    @Path("start/{recordingId}")
    @Consumes("application/x-www-form-urlencoded")
    public Response start(@Context UriInfo ui,
        @PathParam("recordingId") String recordingId,
        MultivaluedMap<String, String> formParams) {

        // TODO: move all these constant strings to a file so we can easily change them.
        boolean async = Optional.ofNullable(formParams.getFirst("async"))
            .map(v -> {
                return (v == "t") ? true : false;
            })
            .orElse(false);
        List<String> reqIds = Optional.ofNullable(formParams.get("reqIds"))
            .orElse(new ArrayList<String>());
        Optional<String> endpoint = Optional.ofNullable(formParams.getFirst("endPoint"));
        List<String> paths = Optional.ofNullable(formParams.get("paths"))
            .orElse(new ArrayList<String>());
        Optional<Double> sampleRate = Optional.ofNullable(formParams.getFirst("sampleRate"))
            .flatMap(v -> Utils.strToDouble(v));
        List<String> intermediateServices = Optional.ofNullable(formParams.get("intermService"))
            .orElse(new ArrayList<>());
        Optional<String> service = Optional.ofNullable(formParams.getFirst("service"));
        String userId = formParams.getFirst("userId");
        String instanceId = formParams.getFirst("instanceId");
        String replayType = formParams.getFirst("replayType");
        boolean startReplay = Utils.strToBool(formParams.getFirst("startReplay")).orElse(true);

        // Request transformations - for injecting tokens and such
        Optional<String> xfms = Optional.ofNullable(formParams.getFirst("transforms"));

        if (userId == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity((new JSONObject(Map.of("Message", "userId Not Specified"))).toString())
                .build();
        }

        if (instanceId == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity((new JSONObject(Map.of("Message", "instanceId Not Specified"))).toString())
                .build();
        }

        Optional<Recording> recordingOpt = rrstore.getRecording(recordingId);
        if (recordingOpt.isEmpty()) {
            LOGGER.error(String
                .format("Cannot init Replay since cannot find recording for id %s", recordingId));
            return Response.status(Status.NOT_FOUND)
                .entity(String.format("cannot find recording for id %s", recordingId)).build();
        }

        Recording recording = recordingOpt.get();
        // check if recording or replay is ongoing for (customer, app, instanceid)
        Optional<Response> errResp = WSUtils
            .checkActiveCollection(rrstore, Optional.ofNullable(recording.customerId),
                Optional.ofNullable(recording.app), Optional.ofNullable(instanceId),
                Optional.ofNullable(userId));
        if (errResp.isPresent()) {
            return errResp.get();
        }

        return endpoint.map(e -> {
            // TODO: introduce response transforms as necessary

            ReplayBuilder replayBuilder = new ReplayBuilder(e,
                new CubeMetaInfo(recording.customerId,
                    recording.app, instanceId), recording.collection, userId)
                .withTemplateSetVersion(recording.templateVersion)
                .withReqIds(reqIds).withAsync(async).withPaths(paths)
                .withIntermediateServices(intermediateServices)
                .withReplayType((replayType != null) ? Utils.valueOf(ReplayTypeEnum.class, replayType)
                    .orElse(ReplayTypeEnum.HTTP) : ReplayTypeEnum.HTTP);
            sampleRate.ifPresent(replayBuilder::withSampleRate);
            service.ifPresent(replayBuilder::withServiceToReplay);
            xfms.ifPresent(replayBuilder::withXfms);
            try {
                recording.generatedClassJarPath
                    .ifPresent(UtilException.rethrowConsumer(replayBuilder::withGeneratedClassJar));
            } catch (Exception ex) {
                return Response.serverError().entity((new JSONObject(Map.of(
                    Constants.MESSAGE, "Error while constructing class loader from the specified jar path"
                    , Constants.ERROR, ex.getMessage()
                ))).toString()).build();
            }
            Replay replay = replayBuilder.build();
            return ReplayDriverFactory
                .initReplay(replay, config)
                .map(replayDriver -> {
                    String json;
                    Replay replayFromDriver = replayDriver.getReplay();
                    try {
                        json = jsonMapper.writeValueAsString(replayFromDriver);
                        if (startReplay) {
                            boolean status = replayDriver.start();
                            if (status) {
                                return Response.ok(json, MediaType.APPLICATION_JSON).build();
                            }
                            return Response.status(Response.Status.CONFLICT).entity(
                                "Not able to start replay. It may be already running or completed")
                                .build();
                        } else {
                            return Response.ok(json, MediaType.APPLICATION_JSON).build();
                        }
                    } catch (JsonProcessingException ex) {
                        LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                            "Error in converting Replay object to Json)) String",
                            Constants.REPLAY_ID_FIELD, replay.replayId)), ex);
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
		this.jsonMapper = config.jsonMapper;
		this.replayResultCache = config.replayResultCache;
		this.config = config;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	ReplayResultCache replayResultCache;
	private final Config config;
}
