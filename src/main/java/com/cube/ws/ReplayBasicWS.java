/**
 * Copyright Cube I O
 */
package com.cube.ws;

import com.cube.dao.Result;
import io.md.dao.agent.config.AgentConfigTagInfo;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.concurrent.TimeUnit;
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
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import io.md.core.ReplayTypeEnum;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.services.Analyzer;
import io.md.utils.CubeObjectMapperProvider;

import com.cube.core.Utils;
import com.cube.dao.CubeMetaInfo;
import com.cube.dao.ReplayBuilder;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.AbstractReplayDriver;
import com.cube.drivers.ReplayDriverFactory;
import com.cube.utils.Constants;

/**
 * @author prasad
 * The replay service with basic apis needed for client side replays
 */
//@Path("/rs")
public class ReplayBasicWS {

    private static final Logger LOGGER = LogManager.getLogger(ReplayBasicWS.class);

    private final Analyzer analyzer;



	@GET
	@Path("status/{replayId}")
    public Response status(@Context UriInfo ui, @PathParam("replayId") String replayId) {
        Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, reqRespStore);
        Response resp = replay.map(r -> {
            String json;
            try {
                // check if there's a current running replay in cache , which matches the replay whose
                // status is required. If there's a current running replay, set the status of the replay
                // to Running before sending the status back.
                Optional<Replay> currentRunningReplay = reqRespStore.getCurrentRecordOrReplay(r.customerId,
                    r.app, r.instanceId)
                    .flatMap(runningRecordOrReplay -> runningRecordOrReplay.replay);
                r.status = currentRunningReplay.map(runningReplay -> runningReplay.
                    replayId.equals(r.replayId)).orElse(false) ? currentRunningReplay.get().status : r.status;
                json = jsonMapper.writeValueAsString(r);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Replay object to Json for replayId %s", replayId), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Status.NOT_FOUND).entity("Status not found for replayId: " + replayId).build());

        return resp;
    }


    @POST
    @Path("start/{recordingId}")
    @Consumes("application/x-www-form-urlencoded")
    public Response start(@Context UriInfo ui,
        @PathParam("recordingId") String recordingId,
        MultivaluedMap<String, String> formParams) {
        Optional<Recording> recordingOpt = reqRespStore.getRecording(recordingId);

        if (recordingOpt.isEmpty()) {
            LOGGER.error(String
                .format("Cannot init Replay since cannot find recording for id %s", recordingId));
            return Response.status(Status.NOT_FOUND)
                .entity(String.format("cannot find recording for id %s", recordingId)).build();
        }

        return  startReplay(formParams, recordingOpt.get());
    }

    protected Response startReplay( MultivaluedMap<String, String> formParams, Recording recording) {
        // TODO: move all these constant strings to a file so we can easily change them.
        boolean async = Utils.strToBool(formParams.getFirst("async")).orElse(false);
        boolean excludePaths = Utils.strToBool(formParams.getFirst("excludePaths")).orElse(false);
        List<String> reqIds = Optional.ofNullable(formParams.get("reqIds"))
            .orElse(new ArrayList<String>());
        Optional<String> endpoint = Optional.ofNullable(formParams.getFirst("endPoint"));
        List<String> paths = Optional.ofNullable(formParams.get("paths"))
            .orElse(new ArrayList<String>());
        Optional<Double> sampleRate = Optional.ofNullable(formParams.getFirst("sampleRate"))
            .flatMap(v -> Utils.strToDouble(v));
        List<String> intermediateServices = Optional.ofNullable(formParams.get("intermService"))
            .orElse(new ArrayList<>());
        List<String> service = Optional.ofNullable(formParams.get("service"))
            .orElse(Collections.emptyList());
        String userId = formParams.getFirst("userId");
        String instanceId = formParams.getFirst("instanceId");
        String replayType = formParams.getFirst("replayType");
        List<String> mockServices = Optional.ofNullable(formParams.get("mockServices"))
            .orElse(new ArrayList<String>());
        boolean startReplay = Utils.strToBool(formParams.getFirst("startReplay")).orElse(true);
        boolean analyze = Utils.strToBool(formParams.getFirst("analyze")).orElse(true);
        Optional<String> testConfigName = Optional.ofNullable(formParams.getFirst("testConfigName"));

        Optional<String> dynamicInjectionConfigVersion = Optional.ofNullable(formParams.getFirst("dynamicInjectionConfigVersion"));
        Optional<String> staticInjectionMap = Optional.ofNullable(formParams.getFirst("staticInjectionMap"));
        Optional<String> tag = Optional.ofNullable(formParams.getFirst(Constants.TAG_FIELD));

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

        // check if recording or replay is ongoing for (customer, app, instanceid)
        Optional<Response> errResp = WSUtils
            .checkActiveCollection(reqRespStore, recording.customerId,
                recording.app, instanceId,
                Optional.ofNullable(userId));
        if (errResp.isPresent()) {
            return errResp.get();
        }

        if(tag.isPresent()) {
            String tagValue = tag.get();
            Result<AgentConfigTagInfo> response = reqRespStore.getAgentConfigTagInfoResults(
                recording.customerId, recording.app, Optional.empty(), instanceId);
            response.getObjects().forEach(agentconfig -> {
                AgentConfigTagInfo agentConfigTagInfo = new AgentConfigTagInfo(
                    agentconfig.customerId, agentconfig.app, agentconfig.service, agentconfig.instanceId, tagValue);
                reqRespStore.updateAgentConfigTag(agentConfigTagInfo);
            });
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException ex) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Exception while sleeping the thread",
                    Constants.TAG_FIELD, tagValue)), ex);
            }
        }

        return endpoint.map(e -> {
            // TODO: introduce response transforms as necessary

            ReplayBuilder replayBuilder = new ReplayBuilder(e,
                new CubeMetaInfo(recording.customerId,
                    recording.app, instanceId), recording.collection, userId)
                .withTemplateSetVersion(recording.templateVersion)
                .withReqIds(reqIds).withAsync(async).withPaths(paths)
                .withExcludePaths(excludePaths)
                .withIntermediateServices(intermediateServices)
                .withReplayType((replayType != null) ? Utils.valueOf(ReplayTypeEnum.class, replayType)
                    .orElse(ReplayTypeEnum.HTTP) : ReplayTypeEnum.HTTP)
                .withMockServices(mockServices)
                .withRecordingId(recording.id)
                .withGoldenName(recording.name);
            sampleRate.ifPresent(replayBuilder::withSampleRate);
            replayBuilder.withServiceToReplay(service);
            testConfigName.ifPresent(replayBuilder::withTestConfigName);
            xfms.ifPresent(replayBuilder::withXfms);
            dynamicInjectionConfigVersion.ifPresent(replayBuilder::withDynamicInjectionConfigVersion);
            staticInjectionMap.ifPresent(replayBuilder::withStaticInjectionMap);
            replayBuilder.withRunId(Optional.of(replayBuilder.getReplayId() + " " + Instant.now().toString()));

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
                .initReplay(replay, reqRespStore)
                .map(replayDriver -> {
                    String json;
                    Replay replayFromDriver = replayDriver.getReplay();
                    try {
                        json = jsonMapper.writeValueAsString(replayFromDriver);
                        if (startReplay) {
                            boolean status = replayDriver.start(analyze ? Optional.ofNullable(analyzer) : Optional.empty());
                            if (status) {
                                return Response.ok(json, MediaType.APPLICATION_JSON).build();
                            }
                            return Response.status(Status.CONFLICT).entity(
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
     * @param reqRespStore
     */
    @Inject
    public ReplayBasicWS(ReqRespStore reqRespStore, Analyzer analyzer) {
        super();
        this.reqRespStore = reqRespStore;
        this.jsonMapper = CubeObjectMapperProvider.getInstance();
        this.analyzer = analyzer;
    }

	ReqRespStore reqRespStore;
	ObjectMapper jsonMapper;
}
