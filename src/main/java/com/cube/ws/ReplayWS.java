/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static io.md.core.Utils.buildErrorResponse;

import com.cube.learning.DynamicInjectionRulesLearner;
import com.cube.learning.InjectionExtractionMeta;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.constants.ReplayStatus;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.injection.DynamicInjectionConfig;
import io.md.services.Analyzer;
import io.md.ws.ReplayBasicWS;
import io.md.dao.ReplayBuilder;
import io.md.drivers.AbstractReplayDriver;
import io.md.dao.ReplayUpdate;
import io.md.dao.ReplayUpdate.ReplaySaveFailureException;
import io.md.utils.Constants;
import io.md.core.Utils;

import com.cube.core.ServerUtils;
import com.cube.core.TagConfig;
import com.cube.dao.ReplayQuery;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr.SolrStoreException;
import com.cube.dao.Result;
import com.cube.drivers.RealAnalyzer;

/**
 * @author prasad
 * The full replay service
 */
@Path("/rs")
public class ReplayWS extends ReplayBasicWS {

    private static final Logger LOGGER = LogManager.getLogger(ReplayWS.class);

	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map solrHealth = ServerUtils.solrHealthCheck(config.solr);
        Map respMap = new HashMap(solrHealth);
        respMap.put(Constants.SERVICE_HEALTH_STATUS, "RS is healthy");
        return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }


	@POST
	@Path("forcecomplete/{replayId}")
    public Response forceComplete(@Context UriInfo ui,
                                  @PathParam("replayId") String replayId) {
        Optional<Replay> replay = AbstractReplayDriver.getStatus(replayId, this.rrstore);

        Response resp = replay.map(r -> {
            if (r.status != ReplayStatus.Running && r.status != ReplayStatus.Init) {
                Optional<RecordOrReplay> fromCacheOrSolr =
                    rrstore.getCurrentRecordOrReplay(Optional.of(r.customerId)
                        , Optional.of(r.app), Optional.of(r.instanceId));
                return fromCacheOrSolr.map(recordOrReplay -> {
                    if (!recordOrReplay.isRecording()) {
                        Replay currentReplay = recordOrReplay.replay.get();
                        if (currentReplay.replayId.equals(r.replayId)) {
                            if (!rrstore.forceDeleteInCache(r)) {
                                return Response.serverError().build();
                            }
                            return Response.ok("Conflicting status in solr and cache"
                                + ", evicted from cache").build();
                        }
                    }
                    return null;
                }).orElse(Response.ok(String
                    .format("Replay id state is already terminal: %s", r.status.toString()))
                    .build());

            }
            String json;
            try {
                r.status = ReplayStatus.Error;
                json = jsonMapper.writeValueAsString(r);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Replay object to Json for replayId %s", replayId), e);
                return Response.serverError().build();
            }
            if (!rrstore.forceDeleteInCache(r)) {
                return Response.serverError().build();
            }
            if (!rrstore.saveReplay(r)) {
                return Response.serverError().build();
            }
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
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Replay not found for replayId: " + replayId).build());
        return resp;
    }


    @POST
    @Path("start/byGoldenName/{customerId}/{app}/{goldenName}")
    @Consumes("application/x-www-form-urlencoded")
    public Response startByGoldenName(@Context HttpHeaders headers,
                                      @Context UriInfo ui,
                                      @PathParam("app") String app,
                                      @PathParam("customerId") String customerId,
                                      @PathParam("goldenName") String goldenName,
                                      MultivaluedMap<String, String> formParams) {

        String label = formParams.getFirst("label");

        Optional<Recording> recordingOpt = rrstore.getRecordingByName(customerId, app, goldenName, Optional.ofNullable(label));

        if (recordingOpt.isEmpty()) {
            LOGGER.error(String
                .format("Cannot init Replay since cannot find recording for golden  name %s", goldenName));
            return Response.status(Status.NOT_FOUND)
                .entity(String.format("cannot find recording for golden  name %s", goldenName)).build();
        }

        return startReplay(headers, formParams, recordingOpt.get());
    }

    @POST
    @Path("saveDynamicInjectionConfig/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveDynamicInjectionConfig(@Context UriInfo uriInfo, DynamicInjectionConfig dynamicInjectionConfig) {
        try {
            String dynamicInjectionConfigId = rrstore.saveDynamicInjectionConfig(dynamicInjectionConfig);
            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully saved Dynamic Injection Config",
                "ID", dynamicInjectionConfigId,
                "dynamicInjectionConfigVersion", dynamicInjectionConfig.version))).toString()).build();
        } catch (SolrStoreException e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Unable to save Dynamic Injection Config",
                "dynamicInjectionConfig.version", dynamicInjectionConfig.version)), e);
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.SOLR_STORE_FAILED, "Unable to save Dynamic Injection Config: " +
                    e.getStackTrace()))).build();
        }
    }


    /**
     *
     * @param replayId
     * @return
     */
    @POST
    @Path("delete/{replayId}")
    public Response delete(@Context UriInfo ui, @PathParam("replayId") String replayId) {
        boolean hardDelete = Optional.ofNullable(ui.getQueryParameters().getFirst(io.md.constants.Constants.HARD_DELETE))
            .flatMap(Utils::strToBool).orElse(false);
        Optional<Replay> replay = rrstore.getReplay(replayId);
        Response response = replay.map(rep -> {
            try {
                String json;
                if(hardDelete) {
                    boolean deleteReplayMeta = rrstore.deleteAllReplayData(List.of(rep));
                    if(!deleteReplayMeta) {
                        LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR, "Replay Data is not deleted", "replayId", replayId)));
                        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                            buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                                "Replay Data is not deleted")).build();
                    }
                    json = "Replay is completely deleted";
                } else {
                    Replay deletedReplay = ReplayUpdate.softDeleteReplay(rrstore, rep);
                    LOGGER.info(new ObjectMessage(
                        Map.of(Constants.MESSAGE, "Soft deleting replay", "replayId", replayId)));
                    json = jsonMapper.writeValueAsString(deletedReplay);
                }
                return Response.ok(json, MediaType.APPLICATION_JSON).build();

            } catch (ReplaySaveFailureException ex) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR,
                    "Replay Save failure Exception for replayId", "ReplayId",
                    replayId,
                    Constants.REASON, ex)));
                return Response.serverError().entity(new JSONObject(Map.of(
                    Constants.ERROR, "Replay Save failure Exception for replayId",
                    "ReplayId", replayId))).build();
            } catch (JsonProcessingException ex) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR,
                    "Error in converting Replay object to Json for replayId", "ReplayId",
                    replayId,
                    Constants.REASON, ex)));
                return Response.serverError().entity(new JSONObject(Map.of(
                    Constants.ERROR, "Error in converting Replay object to Json for replayId",
                    "ReplayId", replayId))).build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity(new JSONObject(Map.of(
            Constants.MESSAGE, "Replay Not Found",
            "ReplayId", replayId))).build());
        return response;
    }

    @GET
    @Path("getReplays/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReplays(@Context UriInfo uriInfo, @PathParam("customerId") String customerId) {
        ReplayQuery replayQuery = new ReplayQuery(customerId, uriInfo.getQueryParameters());
        Result<Replay> result = rrstore.getReplay(replayQuery.customerId, replayQuery.app,
            replayQuery.instanceId, replayQuery.status, replayQuery.collection,
            replayQuery.numResults,  replayQuery.start, replayQuery.userId,  replayQuery.endDate,
            replayQuery.startDate, replayQuery.testConfigName, replayQuery.goldenName,  false);
        List<Replay> finalResult = result.getObjects().collect(Collectors.toList());
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(Map.of("response", finalResult)).build();
    }

    @POST
    @Path("saveReplay")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveReplay(@Context UriInfo uriInfo, Replay replay){
        boolean saveReplay = rrstore.saveReplay(replay);
        if(saveReplay) {
            return Response.ok().entity(replay).build();
        } else {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Unable to save Replay",
                Constants.REPLAY_ID_FIELD, replay.replayId)));
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.SOLR_STORE_FAILED, "Unable to save Replay for replayId:" + replay.replayId))
                .build();
        }
    }

    @POST
    @Path("getPotentialDynamicInjectionConfigs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPotentialDynamicInjectionConfigs(@Context UriInfo uriInfo,
        EventQuery eventQuery) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        Optional<List<String>> paths = Optional.ofNullable(queryParams.get("path"));
        Optional<Boolean> discardSingleValues = Optional.of(Boolean.valueOf(
            queryParams.getFirst("discardSingleValues")));

        DynamicInjectionRulesLearner diLearner = new DynamicInjectionRulesLearner(paths);

        Result<Event> events = rrstore.getEvents(eventQuery);
        diLearner.processEvents(events);

        try {

            List<InjectionExtractionMeta> finalMetaList = diLearner
                .generateRules(discardSingleValues);

            CsvSchema csvSchema = csvMapper.schemaFor(InjectionExtractionMeta.class).withHeader();
            String data = csvMapper.writer(csvSchema).writeValueAsString(finalMetaList);

            File file = new File("/tmp/di.csv");
            FileUtils.writeStringToFile(file, data, Charset.defaultCharset());
            Response.ResponseBuilder response = Response.ok((Object) file);
            response.header("Content-Disposition", "attachment; filename=\"di_configs.csv\"");
            return response.build();

        } catch (JsonProcessingException e) {
            LOGGER.error(
                String.format(
                    "Error in converting Event list to Json for customer %s, app %s, collections: %s",
                    eventQuery.getCustomerId(), eventQuery.getApp(), eventQuery.getCollections()),
                e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    String.format(
                        "Error in converting Event list to Json for customer %s, app %s, collections: %s",
                        eventQuery.getCustomerId(), eventQuery.getApp(),
                        eventQuery.getCollections()))).build();
        } catch (IOException e) {
            LOGGER.error(
                String.format("Error in file creation for customer %s, app %s, collections: %s",
                    eventQuery.getCustomerId(), eventQuery.getApp(), eventQuery.getCollections()),
                e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    String.format("Error in file creation for customer %s, app %s, collections: %s",
                        eventQuery.getCustomerId(), eventQuery.getApp(),
                        eventQuery.getCollections()))).build();
        }
    }

    @POST
    @Path("saveDynamicInjectionConfigFromCsv/{customerId}/{app}/{version}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveDynamicInjectionConfigFromCsv(@PathParam("customerId") String customerId,
        @PathParam("app") String app,
        @PathParam("version") String version,
        @FormDataParam("file") InputStream uploadedInputStream) {

        CsvSchema csvSchema = csvMapper.schemaFor(InjectionExtractionMeta.class)
            .withSkipFirstDataRow(true);
        List<InjectionExtractionMeta> injectionExtractionMetaList;

        try {
            MappingIterator<InjectionExtractionMeta> mi = csvMapper
                .readerFor(InjectionExtractionMeta.class).
                    with(csvSchema).readValues(uploadedInputStream);
            injectionExtractionMetaList = mi.readAll();
            String dynamicInjectionConfigId = rrstore
                .saveDynamicInjectionConfigFromCsv(customerId, app, version,
                    injectionExtractionMetaList);

            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully saved Dynamic Injection Config",
                "ID", dynamicInjectionConfigId,
                "dynamicInjectionConfigVersion", version))).toString()).build();
        } catch (SolrStoreException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to save Dynamic Injection Config",
                    "dynamicInjectionConfig.version", version)), e);
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.SOLR_STORE_FAILED,
                    "Unable to save Dynamic Injection Config: " +
                        e.getStackTrace()))).build();
        } catch (IOException e) {
            LOGGER.error(
                String.format("Error in reading CSV file for customer %s, app %s, version %s",
                    customerId, app, version), e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    String.format("Error in reading CSV file for customer %s, app %s, version: %s",
                        customerId, app, version))).build();
        }
    }

    @GET
    @Path("getDynamicInjectionConfig/{customerId}/{app}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDynamicInjectionConfig(@Context UriInfo uriInfo,
        @PathParam("customerId") String customerId, @PathParam("app") String app,
        @PathParam("version") String version) {
        Optional<DynamicInjectionConfig> dynamicInjectionConfig = rrstore.getDynamicInjectionConfig(
            customerId, app, version);
        Response resp = dynamicInjectionConfig.map(d -> {
            try{
                String json = jsonMapper.writeValueAsString(d);
                return Response.ok(json).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting DynamicInjectionConfig object to Json for customerId=%s, app=%s, version=%s",
                    customerId, app, version), e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Error in converting DynamicInjectionConfig object to Json")).build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                "DynamicInjectionConfig object not found")).build());
        return resp;
    }

    @POST
    @Path("replay/restart/{customerId}/{app}/{replayId}")
    public Response restartReplay(@Context UriInfo uriInfo, @PathParam("customerId") String customerId,
        @PathParam("app") String app, @PathParam("replayId") String replayId) {

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String instanceId = queryParams.getFirst(Constants.INSTANCE_ID_FIELD);
        String recordingCollection = queryParams.getFirst(Constants.COLLECTION_FIELD);
        String userId = queryParams.getFirst(Constants.USER_ID_FIELD);
        String endPoint = queryParams.getFirst(Constants.END_POINT_FIELD);
        if (instanceId == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("InstaceId needs to be given for a replay")
                .build();
        }
        if (recordingCollection == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("RecordingCollection needs to be given for a replay")
                .build();
        }
        if (userId == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("userId needs to be given for a replay")
                .build();
        }
        if (endPoint == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("endPoint needs to be given for a replay")
                .build();
        }
        Optional<Replay> optionalReplay = rrstore.getReplay(replayId);
        Replay replay = null;
        if(optionalReplay.isPresent()) {
            replay = optionalReplay.get();
            replay.collection = recordingCollection;
            if(!replay.customerId.equals(customerId)) {
                LOGGER.error(String.format("customerId is not matching the replay customerId for replayId %s", replayId));
                return Response.status(Status.BAD_REQUEST)
                    .entity("customerId is not matching the replay customerId")
                    .build();
            }
            if(!replay.app.equals(app)) {
                LOGGER.error(String.format("App is not matching the replay app for replayId %s", replayId));
                return Response.status(Status.BAD_REQUEST)
                    .entity("App is not matching the replay app")
                    .build();
            }
        } else {
            ReplayBuilder replayBuilder = new ReplayBuilder(endPoint, customerId, app, instanceId,
                recordingCollection, userId)
                .withReplayId(replayId);
            replay = replayBuilder.build();
        }
        replay.status = ReplayStatus.Running;
        replay.runId = replayId + " " + Instant.now().toString();
        rrstore.saveReplay(replay);
        try {
            String json = jsonMapper.writeValueAsString(replay);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }  catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Replay object to Json for replayId %s", replayId), e);
            return Response.serverError().build();
        }
    }

    @POST
    @Path("deferredDeleteReplay/{replayId}/{status}")
    public Response deferredDeleteReplay(@Context UriInfo uriInfo,
        @PathParam("replayId") String replayId, @PathParam("status") String status) {
        Optional<Replay> optionalReplay = rrstore.getReplay(replayId);
        Response resp = optionalReplay.map(replay -> {
            ReplayStatus statusToSet = Utils.valueOf(ReplayStatus.class, status).orElse(ReplayStatus.Completed);
            replay.status = statusToSet;
            boolean expireReplayInCache = rrstore.deferredDelete(replay);
            if(expireReplayInCache) {
                return Response.ok().entity(replay).build();
            } else {
                LOGGER.error(String.format("The Replay in cache is not expired for replayId %s", replayId));
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.REPLAY_ID_FIELD,
                        "The Replay in cache is not expired")).build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                "Replay object not found")).build());
        return resp;
    }

    @POST
    @Path("cache/flushall")
    public Response cacheFlushAll() {
        return ServerUtils.flushAll(config);
    }

    @Override
    protected CompletableFuture<Void> beforeReplay(@Context HttpHeaders headers,
                                                   MultivaluedMap<String, String> formParams, Recording recording,
                                                   Replay replay) {
        Optional<String> tagOpt = formParams == null ? Optional.empty()
            : Optional.ofNullable(formParams.getFirst(Constants.TAG_FIELD));

        return tagOpt.map(tag -> this.tagConfig.setTag(recording, replay.instanceId, tag))
            .orElse(CompletableFuture.completedFuture(null));
    }

    @Override
    protected CompletableFuture<Void> afterReplay(@Context HttpHeaders headers,
                                                  MultivaluedMap<String, String> formParams, Recording recording,
        Replay replay, Optional<Analyzer> analyzerOpt) {
        Optional<String> resetTagOpt = formParams == null ? Optional.empty()
            : Optional.ofNullable(formParams.getFirst(Constants.RESET_TAG_FIELD));

         return CompletableFuture.runAsync(() -> analyze(replay, analyzerOpt))
            .thenCompose(v ->
                 resetTagOpt.map(tag -> this.tagConfig.setTag(recording, replay.instanceId, tag))
                    .orElse(CompletableFuture.completedFuture(null)));
    }

    /**
	 * @param config
	 */
	@Inject
	public ReplayWS(Config config) {
        //super(new io.cube.agent.ProxyDataStore(), new ProxyAnalyzer());
		super(config.rrstore, new RealAnalyzer(config.rrstore), Optional.of(config.protoDescriptorCache));
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
		this.tagConfig = new TagConfig(config.rrstore);

		this.csvMapper = new CsvMapper();
        csvMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
    }


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	CsvMapper csvMapper;
	TagConfig tagConfig;
    private final Config config;
}
