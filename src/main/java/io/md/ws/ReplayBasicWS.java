/**
 * Copyright Cube I O
 */
package io.md.ws;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
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

import io.md.utils.UtilException;
import io.md.core.ReplayTypeEnum;
import io.md.core.Utils;
import io.md.dao.CubeMetaInfo;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.dao.ReplayBuilder;
import io.md.drivers.AbstractReplayDriver;
import io.md.drivers.ReplayDriverFactory;
import io.md.exception.ParameterException;
import io.md.services.Analyzer;
import io.md.services.DataStore;
import io.md.utils.Constants;
import io.md.utils.CubeObjectMapperProvider;

/**
 * @author prasad
 * The replay service with basic apis needed for client side replays
 */
//@Path("/rs")
public class ReplayBasicWS {

    private static final Logger LOGGER = LogManager.getLogger(io.md.ws.ReplayBasicWS.class);

    private final Analyzer analyzer;



	@GET
	@Path("status/{replayId}")
    public Response status(@Context UriInfo ui, @PathParam("replayId") String replayId) {
        Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, dataStore);
        Response resp = replay.map(r -> {
            String json;
            try {
                // check if there's a current running replay in cache , which matches the replay whose
                // status is required. If there's a current running replay, set the status of the replay
                // to Running before sending the status back.
                Optional<Replay> currentRunningReplay = dataStore.getCurrentRecordOrReplay(r.customerId,
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
    public void start(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui,
                      @PathParam("recordingId") String recordingId,
                      MultivaluedMap<String, String> formParams) {
        Optional<Recording> recordingOpt = dataStore.getRecording(recordingId);


        if (recordingOpt.isEmpty()) {
            LOGGER.error(String
                .format("Cannot init Replay since cannot find recording for id %s", recordingId));
            asyncResponse.resume(Response.status(Status.NOT_FOUND)
                .entity(String.format("cannot find recording for id %s", recordingId)).build());
        }

        startReplay(formParams, recordingOpt.get())
        .thenApply(response -> asyncResponse.resume(response))
        .exceptionally(e -> asyncResponse.resume(
            Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(String.format("Server error: " + e.getMessage())).build()));
    }

    protected CompletableFuture<Response> startReplay(MultivaluedMap<String, String> formParams, Recording recording) {
        try {
            boolean startReplay = Utils.strToBool(formParams.getFirst("startReplay")).orElse(true);
            boolean analyze = Utils.strToBool(formParams.getFirst("analyze")).orElse(true);

            Replay replay = createReplayObject(formParams, recording);
            // check if recording or replay is ongoing for (customer, app, instanceid)
            Optional<Response> errResp = WSUtils
                .checkActiveCollection(dataStore, recording.customerId,
                    recording.app, replay.instanceId,
                    Optional.ofNullable(replay.userId));
            if (errResp.isPresent()) {
                return CompletableFuture.completedFuture(errResp.get());
            }
            return beforeReplay(formParams, recording, replay)
                .thenApply(v -> doStartReplay(replay, startReplay))
                .thenCompose(response ->
                     afterReplay(formParams, recording, replay,
                            analyze ? Optional.ofNullable(analyzer) : Optional.empty())
                        .thenApply(v ->  response));
        } catch (ParameterException e) {
            return CompletableFuture.completedFuture(Response.status(Status.BAD_REQUEST)
                .entity((new JSONObject(Map.of("Message", e.getMessage()))).toString())
                .build());
        }

    }

    private Replay createReplayObject(MultivaluedMap<String, String> formParams, Recording recording) throws ParameterException {
        // TODO: move all these constant strings to a file so we can easily change them.
        boolean async = Utils.strToBool(formParams.getFirst("async")).orElse(false);
        boolean excludePaths = Utils.strToBool(formParams.getFirst("excludePaths")).orElse(false);
        List<String> reqIds = Optional.ofNullable(formParams.get("reqIds"))
            .orElse(new ArrayList<String>());
        String endpoint = formParams.getFirst("endPoint");
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
        Optional<String> testConfigName = Optional.ofNullable(formParams.getFirst("testConfigName"));

        Optional<String> dynamicInjectionConfigVersion = Optional.ofNullable(formParams.getFirst("dynamicInjectionConfigVersion"));
        Optional<String> staticInjectionMap = Optional.ofNullable(formParams.getFirst("staticInjectionMap"));

        // Request transformations - for injecting tokens and such
        Optional<String> xfms = Optional.ofNullable(formParams.getFirst("transforms"));

        if (userId == null) {
            throw new ParameterException("userId Not Specified");
        }

        if (instanceId == null) {
            throw new ParameterException("instanceId Not Specified");
        }

        if (endpoint == null) {
            throw new ParameterException("endpoint Not Specified");
        }

        ReplayBuilder replayBuilder = new ReplayBuilder(endpoint,
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
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Error while constructing class loader from the specified jar path",
                Constants.ERROR, ex.getMessage()
            )), ex);
            throw new ParameterException("Error while constructing class loader from the specified jar path", ex);
        }
        return replayBuilder.build();
    }

    private Response doStartReplay(Replay replay, boolean startReplay) {

        return ReplayDriverFactory
            .initReplay(replay, dataStore, jsonMapper)
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
    }

    protected CompletableFuture<Void> beforeReplay(MultivaluedMap<String, String> formParams, Recording recording,
                                                   Replay replay) {
        // nothing to do
	    return CompletableFuture.completedFuture(null);
    }

    protected CompletableFuture<Void> afterReplay(MultivaluedMap<String, String> formParams, Recording recording,
                                                  Replay replay, Optional<Analyzer> analyzer) {
        // nothing to do
        return CompletableFuture.completedFuture(null);
    }


    /**
     * @param dataStore
     */
    public ReplayBasicWS(DataStore dataStore, Analyzer analyzer) {
        super();
        this.dataStore = dataStore;
        this.jsonMapper = CubeObjectMapperProvider.getInstance();
        this.analyzer = analyzer;
    }

	DataStore dataStore;
	ObjectMapper jsonMapper;
}
