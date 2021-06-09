/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static io.md.core.Utils.buildErrorResponse;

import com.cube.dao.AnalysisMatchResultQuery;
import com.cube.dao.ReqRespStoreSolr.ReqRespResultsWithFacets;
import com.cube.learning.DynamicInjectionGeneratedToActualConvertor;
import com.cube.learning.DynamicInjectionRulesLearner;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.ReqRespMatchResult;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.ExternalInjectionExtraction;
import io.md.injection.ExternalInjectionExtraction.ExternalExtraction;
import io.md.injection.ExternalInjectionExtraction.ExternalInjection;
import io.md.injection.ExternalInjectionExtraction.ExternalNamedInjectionExtraction;
import io.md.injection.StaticInjection;
import io.md.injection.StaticInjection.StaticInjectionMeta;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

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

        return startReplay(headers, formParams, List.of(recordingOpt.get()));
    }

    @POST
    @Path("saveDynamicInjectionConfigFromJson/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveDynamicInjectionConfigFromJson(@Context UriInfo uriInfo,
        @FormDataParam("file") InputStream uploadedInputStream) {
        DynamicInjectionConfig dynamicInjectionConfig = null;
        try {
            dynamicInjectionConfig = this.jsonMapper
                .readValue(uploadedInputStream, DynamicInjectionConfig.class);

            String dynamicInjectionConfigId = rrstore
                .saveDynamicInjectionConfig(dynamicInjectionConfig);
            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully saved Dynamic Injection Config",
                "ID", dynamicInjectionConfigId,
                "dynamicInjectionConfigVersion", dynamicInjectionConfig.version))).toString())
                .build();
        } catch (SolrStoreException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to save Dynamic Injection Config",
                    "dynamicInjectionConfig.version", dynamicInjectionConfig.version)), e);
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.SOLR_STORE_FAILED,
                    "Unable to save Dynamic Injection Config: " +
                        e.getStackTrace()))).build();
        } catch (IOException e) {
            LOGGER.error(
                "Error in parsing JSON file for dynamic injection config", e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    "Error in parsing JSON file for dynamic injection config"))
                .build();
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

        String customerId = Optional.ofNullable(eventQuery.getCustomerId()).orElse("");
        String app = Optional.ofNullable(eventQuery.getApp()).orElse("");
        List<String> collection = eventQuery.getCollections();

        DynamicInjectionRulesLearner diLearner = new DynamicInjectionRulesLearner(paths);

        Result<Event> events = rrstore.getEvents(eventQuery);

        if (events.numResults == 0){
            LOGGER.error(
                String.format(
                    "No events found for customer %s, app %s, collections: %s",
                    customerId, app, collection));
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.NOT_PRESENT,
                    String.format(
                        "No events found for customer %s, app %s, collections: %s",
                        customerId, app, collection))).build();
        }

        diLearner.processEvents(events);

        try {

            List<ExternalInjectionExtraction> finalInjExtList = diLearner
                .generateRules(discardSingleValues);

            return ServerUtils
                .writeResponseToFile("learned_context_propagation_rules", finalInjExtList,
                    ExternalInjectionExtraction.class, Optional.empty(), Optional.of(csvMapper));

        } catch (JsonProcessingException e) {
            LOGGER.error(
                String.format(
                    "Error in converting Event list to csv for customer=%s, app=%s, collections=%s",
                    customerId, app, collection),
                e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    String.format(
                        "Error in converting Event list to csv for customer=%s, app=%s, collections=%s",
                        customerId, app, collection))).build();
        } catch (IOException e) {
            LOGGER.error(
                String.format("Error in file creation for customer=%s, app=%s, collections=%s",
                    customerId, app, collection),
                e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    String.format("Error in file creation for customer=%s, app=%s, collections=%s",
                        customerId, app, collection))).build();
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

        CsvSchema csvSchema = csvMapper.schemaFor(ExternalInjectionExtraction.class)
            .withSkipFirstDataRow(true);
        List<ExternalInjectionExtraction> externalInjExtList;

        try {
            MappingIterator<ExternalInjectionExtraction> mi = csvMapper
                .readerFor(ExternalInjectionExtraction.class).
                    with(csvSchema).readValues(uploadedInputStream);
            externalInjExtList = mi.readAll();
            String dynamicInjectionConfigId = rrstore
                .saveDynamicInjectionConfigFromCsv(customerId, app, version,
                    externalInjExtList);

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
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDynamicInjectionConfig(@Context UriInfo uriInfo,
        @PathParam("customerId") String customerId, @PathParam("app") String app,
        @PathParam("version") String version) {
        Optional<DynamicInjectionConfig> dynamicInjectionConfig = rrstore.getDynamicInjectionConfig(
            customerId, app, version);
        Response resp = dynamicInjectionConfig.map(d -> {
            try{
                return ServerUtils.writeResponseToFile("context_propagation_rules", d,
                    DynamicInjectionConfig.class, Optional.of(jsonMapper), Optional.empty());
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting DynamicInjectionConfig object to Json for customerId=%s, app=%s, version=%s",
                    customerId, app, version), e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Error in converting DynamicInjectionConfig object to Json")).build();
            } catch (IOException e) {
                LOGGER.error(
                    String.format("Error in file creation for customer=%s, app=%s, version=%s",
                        customerId, app, version),e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.IO_EXCEPTION,
                        String.format("Error in file creation for customer=%s, app=%s, version=%s",
                            customerId, app, version))).build();            }
        }).orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                "DynamicInjectionConfig object not found")).build());
        return resp;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("authExtractionConfig/{customerId}/{app}")
    public Response getAuthExtractionConfig(@Context UriInfo uriInfo,
        @PathParam("customerId") String customerId, @PathParam("app") String app) {
        String version = DynamicInjectionConfig.getAuthConfigVersion(customerId, app);
        Optional<DynamicInjectionConfig> dynamicInjectionConfig = rrstore.getDynamicInjectionConfig(
            customerId, app, version);
        String customerAppVersion = String
            .format("customerId=%s, app=%s, version=%s", customerId, app, version);
        return dynamicInjectionConfig.map(d -> {
            try{
                if (d.extractionMetas.size() > 0 && (d.injectionMetas.size() > 0)) {
                    ExtractionMeta ext = d.extractionMetas.get(0);

                    ExternalExtraction externalExtraction = DynamicInjectionGeneratedToActualConvertor
                        .convertInternalExtractionToExternal(ext);

                    InjectionMeta inj = d.injectionMetas.get(0);

                    ExternalInjection externalInjection = DynamicInjectionGeneratedToActualConvertor
                        .convertInternalInjectiontoExternal(inj);

                    ExternalNamedInjectionExtraction externalNamedInjExt = new ExternalNamedInjectionExtraction(ext.name, externalExtraction, externalInjection);

                    String json = jsonMapper.writeValueAsString(externalNamedInjExt);

                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                }
            } catch (JsonProcessingException e) {
                String errorMsg =
                    "Error in converting Auth Extraction Config object to Json for "
                        + customerAppVersion;
                LOGGER.error(errorMsg, e);
                return Response.serverError().entity(
                    buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        errorMsg)).build();
            }
            return null; // Will hit if extConfig was created but was later overwritten with empty config
        }).orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                "Auth Extraction Config not found for " + customerAppVersion)).build());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("authExtractionConfig/{customerId}/{app}")
    public Response saveAuthExtractionConfig(@Context UriInfo uriInfo,
        @PathParam("customerId") String customerId, @PathParam("app") String app,
        ExternalNamedInjectionExtraction namedInjExt) {
        String version = DynamicInjectionConfig.getAuthConfigVersion(customerId, app);
        String customerAppVersion = String
            .format("customerId=%s, app=%s, version=%s", customerId, app, version);

        ExternalInjection injection = namedInjExt.externalInjection;
        injection.injectAllPaths = true;
        if (injection.jsonPath.equals("")) injection.jsonPath = "/hdrs/authorization/0";

        ExtractionMeta internalExtraction = DynamicInjectionGeneratedToActualConvertor.convertExternalExtractionToInternal(
            namedInjExt.externalExtraction, namedInjExt.varName);

        InjectionMeta internalInjection = DynamicInjectionGeneratedToActualConvertor
            .convertExternalInjectionToInternal(namedInjExt.externalInjection, namedInjExt.varName,
                namedInjExt.externalExtraction.apiPath, namedInjExt.externalExtraction.jsonPath,
                namedInjExt.externalExtraction.method);

        DynamicInjectionConfig dynamicInjectionConfig = new DynamicInjectionConfig(version,
            customerId, app, Optional.empty(),
            Collections.singletonList(internalExtraction), Collections.singletonList(internalInjection),
            Collections.emptyList(), Collections.emptyList());

        try {

            String dynamicInjectionConfigId = rrstore
                .saveDynamicInjectionConfig(dynamicInjectionConfig);
            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully saved Dynamic Injection Config",
                "ID", dynamicInjectionConfigId,
                "dynamicInjectionConfigVersion", dynamicInjectionConfig.version))).toString())
                .build();

        } catch (SolrStoreException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to save Dynamic Injection Config",
                    "dynamicInjectionConfig.version", dynamicInjectionConfig.version)), e);
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.SOLR_STORE_FAILED,
                    "Unable to save Dynamic Injection Config: " +
                        e.getStackTrace()))).build();
        }
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("staticInjectionConfig/{customerId}/{app}/{version}")
    public Response getStaticInjectionConfig(@Context UriInfo uriInfo,
        @PathParam("customerId") String customerId, @PathParam("app") String app,
        @PathParam("version") String version) {
        Optional<DynamicInjectionConfig> dynamicInjectionConfig = rrstore.getDynamicInjectionConfig(
            customerId, app, version + DynamicInjectionConfig.staticVersionSuffix);
        Response resp = dynamicInjectionConfig.map(d -> {
            try{
                List<StaticInjectionMeta> staticValues = StaticInjection.getStaticMetasFromDynamicConfig(d);

                final String fileName = "static_injection_rules";
                return ServerUtils
                    .writeResponseToFile(fileName, staticValues, StaticInjectionMeta.class,
                        Optional.empty(), Optional.of(csvMapper));

            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting DynamicInjectionConfig object to Json for customerId=%s, app=%s, version=%s",
                    customerId, app, version), e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Error in converting DynamicInjectionConfig object to Json")).build();
            } catch (IOException e) {
                LOGGER.error(
                    String.format("Error in file creation for customer=%s, app=%s, version=%s",
                        customerId, app, version),e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        String.format("Error in file creation for customer=%s, app=%s, version=%s",
                            customerId, app, version))).build();            }
        }).orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                "DynamicInjectionConfig object not found")).build());
        return resp;
    }

    @POST
    @Path("staticInjectionConfig/{customerId}/{app}/{version}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveStaticInjectionConfig(@PathParam("customerId") String customerId,
        @PathParam("app") String app,
        @PathParam("version") String version,
        @FormDataParam("file") InputStream uploadedInputStream) {

        CsvSchema csvSchema = csvMapper.schemaFor(StaticInjectionMeta.class).withSkipFirstDataRow(true);

        try {
            List<StaticInjectionMeta> staticInjectionMetas;
            MappingIterator<StaticInjectionMeta> mi = csvMapper.readerFor(StaticInjectionMeta.class).
                    with(csvSchema).readValues(uploadedInputStream);
            staticInjectionMetas = mi.readAll();

            // Static config is maintained in a separate version having an additional staticVersionSuffix
            // For the user, both static and dynamic configs are associated with the same version.
            StaticInjection staticInjection = new StaticInjection(customerId, app,
                version + DynamicInjectionConfig.staticVersionSuffix);
            DynamicInjectionConfig diConfig = staticInjection.convertStaticMetasToDynamicConfig(staticInjectionMetas);
            String staticInjectionConfigId = rrstore.saveDynamicInjectionConfig(diConfig);

            return Response.ok().entity((new JSONObject(Map.of(
                "Message", "Successfully saved Static Injection Config",
                "ID", staticInjectionConfigId,
                "staticInjectionConfigVersion", version))).toString()).build();
        } catch (SolrStoreException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Unable to save Static Injection Config",
                    "staticInjectionConfig.version", version)), e);
            return Response.serverError().entity((
                Utils.buildErrorResponse(Constants.ERROR, Constants.SOLR_STORE_FAILED,
                    "Unable to save Static Injection Config: " +
                        e.getStackTrace()))).build();
        } catch (IOException e) {
            LOGGER.error(
                String.format("Error in reading CSV file for customer:%s, app:%s, version:%s",
                    customerId, app, version), e);
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    String.format("Error in reading CSV file for customer:%s, app:%s, version:%s",
                        customerId, app, version))).build();
        }
    }

    @GET
    @Path("filterDynamicInjectionConfigsByReplay/{customerId}/{app}/{version}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response filterDynamicInjectionConfigsByReplay(@Context UriInfo uriInfo,
        @PathParam("customerId") String customerId,
        @PathParam("app") String app,
        @PathParam("version") String version) {

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();

        List<String> replayIds = Optional.ofNullable(queryParams.get("replayId")).orElse(
            Collections.emptyList());

        Optional<List<String>> paths = Optional.ofNullable(queryParams.get("path"));

        if (replayIds.isEmpty()) {
            return Response.serverError().entity(
                Utils.buildErrorResponse(Constants.ERROR, Constants.NOT_PRESENT,
                    "Missing query parameter replayId")).build();
        }

        Optional<DynamicInjectionConfig> dynamicInjectionConfig = rrstore.getDynamicInjectionConfig(
            customerId, app, version);

        DynamicInjectionRulesLearner diLearner = new DynamicInjectionRulesLearner(Optional.empty());

        return dynamicInjectionConfig.map(diConfig -> {

            AnalysisMatchResultQuery analysisMatchResultQuery = new AnalysisMatchResultQuery(
                replayIds);

            ReqRespResultsWithFacets resultWithFacets = rrstore
                .getAnalysisMatchResults(analysisMatchResultQuery);

            Stream<ReqRespMatchResult> reqRespMatchResultStream = resultWithFacets.result
                .getObjects();

            List<ExternalInjectionExtraction> externalInjectionExtractions = diLearner
                .generateFilteredRules(diConfig, reqRespMatchResultStream);

            try {
                return ServerUtils.writeResponseToFile("filtered_context_propagation_rules", externalInjectionExtractions,
                    ExternalInjectionExtraction.class, Optional.empty(), Optional.of(csvMapper));
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format(
                    "Error in converting DynamicInjectionConfig object to Json for customerId:%s, app:%s, version:%s",
                    customerId, app, version), e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Error in converting DynamicInjectionConfig object to Json")).build();
            } catch (IOException e) {
                LOGGER.error(
                    String.format("Error in file creation for customer:%s, app:%s, version:%s",
                        customerId, app, version), e);
                return Response.serverError().entity(
                    Utils.buildErrorResponse(Constants.ERROR, Constants.IO_EXCEPTION,
                        String.format("Error in file creation for customer:%s, app:%s, version:%s",
                            customerId, app, version))).build();
            }
        }).orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                    "DynamicInjectionConfig object not found")).build());
    }

    @POST
    @Path("replay/restart/{customerId}/{app}/{replayId}")
    public Response restartReplay(@Context UriInfo uriInfo, @PathParam("customerId") String customerId,
        @PathParam("app") String app, @PathParam("replayId") String replayId) {

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String instanceId = queryParams.getFirst(Constants.INSTANCE_ID_FIELD);
        List<String> recordingCollections = queryParams.get(Constants.COLLECTION_FIELD);
        String userId = queryParams.getFirst(Constants.USER_ID_FIELD);
        String endPoint = queryParams.getFirst(Constants.END_POINT_FIELD);
        if (instanceId == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("InstaceId needs to be given for a replay")
                .build();
        }
        if (recordingCollections == null || recordingCollections.isEmpty()) {
            return Response.status(Status.BAD_REQUEST)
                .entity("RecordingCollections needs to be given for a replay")
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
            replay.collection = recordingCollections;
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
                recordingCollections, userId)
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

    @Override
    protected CompletableFuture<Void> beforeReplay(@Context HttpHeaders headers,
                                                   MultivaluedMap<String, String> formParams, List<Recording> recordings,
                                                   Replay replay) {
        Optional<String> tagOpt = formParams == null ? Optional.empty()
            : Optional.ofNullable(formParams.getFirst(Constants.TAG_FIELD));

        return tagOpt.map(tag -> this.tagConfig.setTag(recordings.get(0), replay.instanceId, tag))
            .orElse(CompletableFuture.completedFuture(null));
    }

    @Override
    protected CompletableFuture<Void> afterReplay(@Context HttpHeaders headers,
                                                  MultivaluedMap<String, String> formParams, List<Recording> recordings,
        Replay replay, Optional<Analyzer> analyzerOpt) {
        Optional<String> resetTagOpt = formParams == null ? Optional.empty()
            : Optional.ofNullable(formParams.getFirst(Constants.RESET_TAG_FIELD));

         return CompletableFuture.runAsync(() -> analyze(replay, analyzerOpt))
            .thenCompose(v ->
                 resetTagOpt.map(tag -> this.tagConfig.setTag(recordings.get(0), replay.instanceId, tag))
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
