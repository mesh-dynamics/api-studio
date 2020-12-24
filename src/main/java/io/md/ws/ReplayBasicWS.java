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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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

import io.md.cache.ProtoDescriptorCache;
import io.md.constants.ReplayStatus;
import io.md.core.ReplayTypeEnum;
import io.md.core.Utils;
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
import io.md.utils.UtilException;


/**
 * @author prasad
 * The replay service with basic apis needed for client side replays
 */
//@Path("/rs")
public class ReplayBasicWS {

    private static final Logger LOGGER = LogManager.getLogger(io.md.ws.ReplayBasicWS.class);

	@GET
	@Path("status/{replayId}")
    public Response status(@Context HttpHeaders headers, @Context UriInfo ui, @PathParam("replayId") String replayId) {
	    beforeApi(headers);
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
    public Response start(@Context HttpHeaders headers, @Context UriInfo ui,
                          @PathParam("recordingId") String recordingId,
                          MultivaluedMap<String, String> formParams) {

        beforeApi(headers);
        Optional<Recording> recordingOpt = dataStore.getRecording(recordingId);

        if (recordingOpt.isEmpty()) {
            LOGGER.error(String
                    .format("Cannot init Replay since cannot find recording for id %s", recordingId));
            return Response.status(Status.NOT_FOUND)
                    .entity(String.format("cannot find recording for id %s", recordingId)).build();
        }

        return startReplay(headers, formParams, recordingOpt.get());
    }

    protected Response startReplay(HttpHeaders headers, MultivaluedMap<String, String> formParams, Recording recording) {
        String replayId = "NotInited";
        try {
            // boolean startReplay = Utils.strToBool(formParams.getFirst("startReplay")).orElse(true);
            boolean analyze = Utils.strToBool(formParams.getFirst(Constants.ANALYZE_FIELD)).orElse(true);

            Replay replay = createReplayObject(formParams, recording);
            replayId = replay.replayId;
            // check if recording or replay is ongoing for (customer, app, instanceid)
            Optional<Response> errResp = Utils
                    .checkActiveCollection(dataStore, recording.customerId,
                            recording.app, replay.instanceId,
                            Optional.ofNullable(replay.userId));
            if (errResp.isPresent()) {
                return errResp.get();
            }
            beforeReplay(headers, formParams, recording, replay)
                    .thenApply(v -> doStartReplay(replay))
                    .thenCompose(v -> afterReplay(headers, formParams, recording, replay,
                            analyze ? Optional.ofNullable(analyzer) : Optional.empty()));
            String json = jsonMapper.writeValueAsString(replay);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (ParameterException e) {
            return Response.status(Status.BAD_REQUEST)
                    .entity((new JSONObject(Map.of("Message", e.getMessage()))).toString())
                    .build();
        } catch (JsonProcessingException ex) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Error in converting Replay object to Json)) String",
                    Constants.REPLAY_ID_FIELD, replayId)), ex);
            return Response.serverError().build();
        }
    }

    private Replay createReplayObject(MultivaluedMap<String, String> formParams, Recording recording) throws ParameterException {
        // TODO: move all these constant strings to a file so we can easily change them.
        boolean async = Utils.strToBool(formParams.getFirst(Constants.ASYNC_FIELD)).orElse(false);
        boolean excludePaths = Utils.strToBool(formParams.getFirst(Constants.EXCLUDE_PATH_FIELD)).orElse(false);
        List<String> reqIds = Optional.ofNullable(formParams.get(Constants.REQ_IDS_FIELD))
            .orElse(new ArrayList<String>());
        String endpoint = formParams.getFirst(Constants.END_POINT_FIELD);
        List<String> paths = Optional.ofNullable(formParams.get(Constants.PATHS_FIELD))
            .orElse(new ArrayList<String>());
        Optional<Double> sampleRate = Optional.ofNullable(formParams.getFirst(Constants.SAMPLE_RATE_FIELD))
            .flatMap(v -> Utils.strToDouble(v));
        List<String> intermediateServices = Optional.ofNullable(formParams.get(Constants.INTERM_SERVICE_FIELD))
            .orElse(new ArrayList<>());
        List<String> service = Optional.ofNullable(formParams.get(Constants.SERVICE_FIELD))
            .orElse(Collections.emptyList());
        String userId = formParams.getFirst(Constants.USER_ID_FIELD);
        String instanceId = formParams.getFirst(Constants.INSTANCE_ID_FIELD);
        String replayType = formParams.getFirst(Constants.REPLAY_TYPE_FIELD);
        List<String> mockServices = Optional.ofNullable(formParams.get(Constants.MOCK_SERVICES_FIELD))
            .orElse(new ArrayList<String>());
        Optional<String> testConfigName = Optional.ofNullable(formParams.getFirst(Constants.TEST_CONFIG_NAME_FIELD));

        Optional<String> dynamicInjectionConfigVersion = Optional.ofNullable(formParams.getFirst(Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD)).or(()->recording.dynamicInjectionConfigVersion);
        Optional<String> staticInjectionMap = Optional.ofNullable(formParams.getFirst(Constants.STATIC_INJECTION_MAP_FIELD));

        // Request transformations - for injecting tokens and such
        Optional<String> xfms = Optional.ofNullable(formParams.getFirst(Constants.TRANSFORMS_FIELD));
        boolean tracePropagation = Utils.strToBool(formParams.getFirst(Constants.TRACE_PROPAGATION)).orElse(true);
        boolean storeToDatastore = Utils.strToBool(formParams.getFirst(Constants.STORE_TO_DATASTORE)).orElse(false);

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
            recording.customerId,
                recording.app, instanceId, recording.collection, userId)
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
        replayBuilder.withRunId(replayBuilder.getReplayId() + " " + Instant.now().toString());
        replayBuilder.withTracePropagation(tracePropagation);
        replayBuilder.withStoreToDatastore(storeToDatastore);

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

    private boolean doStartReplay(Replay replay) {

        return ReplayDriverFactory
                .initReplay(replay, dataStore, jsonMapper, protoDescriptorCacheOptional)
                .map(replayDriver -> {
                    boolean status = replayDriver.start();
                    if (!status) {
                        LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                                "Not able to start replay. It may be already running or completed", Constants.REPLAY_ID_FIELD
                                , replay.replayId)));
                    }
                    return status;
                }).orElseGet(() -> {
                    LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                            "Not able to create replay driver", Constants.REPLAY_ID_FIELD
                            , replay.replayId)));
                    return false;
                });
    }

    protected CompletableFuture<Void> beforeReplay(HttpHeaders headers, MultivaluedMap<String, String> formParams, Recording recording,
                                                   Replay replay) {
        // nothing to do
	    return CompletableFuture.completedFuture(null);
    }

    protected CompletableFuture<Void> afterReplay(HttpHeaders headers, MultivaluedMap<String, String> formParams, Recording recording,
                                                  Replay replay, Optional<Analyzer> analyzer) {
        // nothing to do
        return CompletableFuture.completedFuture(null);
    }

    protected void beforeApi(HttpHeaders headers) {
        // nothing to do
        return ;
    }

    protected void analyze(Replay replay, Optional<Analyzer> analyzerOpt) {
        ReplayStatus status = ReplayStatus.Running;
        while (status == ReplayStatus.Running) {
            try {
                Thread.sleep(5000);
                Optional<Replay> currentRunningReplay = dataStore
                        .getCurrentRecordOrReplay(replay.customerId,
                                replay.app, replay.instanceId)
                        .flatMap(runningRecordOrReplay -> runningRecordOrReplay.replay);
                status = currentRunningReplay.filter(runningReplay -> runningReplay.
                        replayId.equals(replay.replayId)).map(r -> r.status).orElse(replay.status);
            } catch (InterruptedException e) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                        "Exception while sleeping  the thread", Constants.REPLAY_ID_FIELD
                        , replay.replayId)));
            }
        }
        analyzerOpt.ifPresent(analyzer -> analyzer.analyze(replay.replayId , Optional.empty()));
    }



    /**
     * @param dataStore
     */
    public ReplayBasicWS(DataStore dataStore, Analyzer analyzer, Optional<ProtoDescriptorCache> protoDescriptorCacheOptional) {
        super();
        this.dataStore = dataStore;
        this.jsonMapper = CubeObjectMapperProvider.getInstance();
        this.analyzer = analyzer;
        this.protoDescriptorCacheOptional = protoDescriptorCacheOptional;
    }

	protected final DataStore dataStore;
    protected final Analyzer analyzer;
    ObjectMapper jsonMapper;
    protected final Optional<ProtoDescriptorCache>  protoDescriptorCacheOptional;
}
