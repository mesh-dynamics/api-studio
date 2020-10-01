/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static io.md.core.Utils.buildErrorResponse;
import static io.md.core.Utils.buildSuccessResponse;
import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;

import com.cube.core.ServerUtils;
import com.cube.core.TagConfig;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.solr.common.util.Pair;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.FnReqResponse;
import io.cube.agent.UtilException;
import io.md.constants.ReplayStatus;
import io.md.core.Comparator;
import io.md.core.Comparator.Match;
import io.md.core.Comparator.MatchType;
import io.md.core.ConfigApplicationAcknowledge;
import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.core.Utils.BadValueException;
import io.md.core.ValidateAgentStore;
import io.md.dao.DefaultEvent;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.RunType;
import io.md.dao.EventQuery;
import io.md.dao.MDTraceInfo;
import io.md.dao.Payload;
import io.md.dao.ProtoDescriptorDAO;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingSaveFailureException;
import io.md.dao.Recording.RecordingStatus;
import io.md.dao.Recording.RecordingType;
import io.md.dao.Replay;
import io.md.dao.ReplayBuilder;
import io.md.dao.ReqRespMatchResult;
import io.md.dao.UserReqRespContainer;
import io.md.dao.CubeMetaInfo;
import io.md.dao.agent.config.AgentConfigTagInfo;
import io.md.dao.agent.config.ConfigDAO;
import io.md.services.DataStore.TemplateNotFoundException;
import io.md.exception.ParameterException;
import io.md.utils.Constants;
import io.md.core.Utils;

import com.cube.dao.CubeEventMetaInfo;
import com.cube.dao.RecordingBuilder;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr.SolrStoreException;
import com.cube.dao.Result;
import com.cube.dao.WrapperEvent;
import com.cube.queue.DisruptorEventQueue;
import com.cube.queue.RREvent;
import com.cube.queue.StoreUtils;

//import com.cube.queue.StoreUtils;

/**
 * @author prasad
 *
 */
@Path("/cs")
public class CubeStore {

    private static final Logger LOGGER = LogManager.getLogger(CubeStore.class);

	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map solrHealth = ServerUtils.solrHealthCheck(config.solr);
        Map respMap = new HashMap(solrHealth);
        respMap.put(Constants.SERVICE_HEALTH_STATUS, "CS is healthy");
        return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }



	@POST
	@Path("/rr/{var:.*}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response storerr(@Context UriInfo ui,
                            @PathParam("var") String path,
                            ReqRespStore.ReqResp rr) {
	    LOGGER.info("/cs/rr request received");
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        eventQueue.enqueue(new RREvent(rr, path, queryParams));
        return Response.ok().build();
    }

    private void logStoreError(CubeStoreException e) {
        Map<String, String> propertiesMap = e.cubeEventMetaInfo.getPropertiesMap();
        propertiesMap.put(Constants.MESSAGE, e.getMessage());
        LOGGER.info(new ObjectMessage(propertiesMap));
	    LOGGER.error(new ObjectMessage(e.cubeEventMetaInfo.getPropertiesMap()), e.getCause());
    }

    private void logStoreInfo(String message, CubeEventMetaInfo e, boolean debug){
	    Map<String, String> propertiesMap = e.getPropertiesMap();
	    propertiesMap.put(Constants.MESSAGE, message);
	    if (debug) {
            LOGGER.debug(new ObjectMessage(propertiesMap));
        } else {
            LOGGER.info(new ObjectMessage(propertiesMap));
        }

    }

    public static class CubeStoreException extends Exception {
        CubeEventMetaInfo cubeEventMetaInfo;

        public CubeStoreException(Exception e, String message, CubeEventMetaInfo cubeEventMetaInfo) {
	        super(message , e);
	        this.cubeEventMetaInfo = cubeEventMetaInfo;
        }

        public CubeStoreException(Exception e, String message, Event event){
	        super(message, e);
	        this.cubeEventMetaInfo = new CubeEventMetaInfo(event);
        }

    }


    private void processRRJson(String rrJson) throws Exception {
        ReqRespStore.ReqResp rr = jsonMapper.readValue(rrJson, ReqRespStore.ReqResp.class);

        // extract path and query params
        URIBuilder uriBuilder = new URIBuilder(rr.pathwparams);
        String path = uriBuilder.getPath();
        List<NameValuePair> queryParams = uriBuilder.getQueryParams();
        MultivaluedHashMap queryParamsMap = new MultivaluedHashMap();
        queryParams.forEach(nameValuePair -> {
            queryParamsMap.add(nameValuePair.getName(), nameValuePair.getValue());
        });
        eventQueue.enqueue(new RREvent(rr, path, queryParamsMap));
    }

    @POST
    @Path("/rrbatch")
    //@Consumes("application/msgpack")
    public Response storeRrBatch(@Context UriInfo uriInfo , @Context HttpHeaders headers,
                                 byte[] messageBytes) {

        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst(Constants.CONTENT_TYPE));
        LOGGER.info("Batch RR received. Content Type: " + contentType);
        return contentType.map(
            ct -> {
                switch(ct) {
                    case Constants.APPLICATION_X_NDJSON:
                        try {
                            String jsonMultiline = new String(messageBytes);
                            String[] jsons = jsonMultiline.split("\n");
                            LOGGER.info("JSON batch size: " + jsons.length);
                            Arrays.stream(jsons).forEach(UtilException.rethrowConsumer(this::processRRJson));
                            return Response.ok().build();
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE
                                    , "Error while processing multiline json")), e);
                            return Response.serverError().entity("Error while processing :: "
                                + e.getMessage()).build();
                        }

                    case Constants.APPLICATION_X_MSGPACK:
                        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(messageBytes));
                        try {
                            while (unpacker.hasNext()) {
                                ValueType nextType = unpacker.getNextFormat().getValueType();
                                if (nextType.isMapType()) {
                                    processRRJson(unpacker.unpackValue().toJson());
                                } else {
                                    LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                                            "Unidentified format type in message pack stream" ,
                                        "format" , nextType.name())));
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                                "Error while unpacking message pack byte stream ")), e);
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        return Response.ok().build();
                    default :
                        return Response.serverError().entity("Content type not recognized :: " + ct).build();
                }
            }

        ).orElse(Response.serverError().entity("Content type not specified").build());
    }

    // Event redesign cleanup: This can be removed - will keep for now
    private Optional<String> storeFnReqResp(String fnReqResponseString) throws Exception {
        FnReqResponse fnReqResponse = jsonMapper.readValue(fnReqResponseString, FnReqResponse.class);
        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Storing function"
            , "funcName", fnReqResponse.name)));
        if (fnReqResponse.argVals != null) {
            Arrays.asList(fnReqResponse.argVals).stream().forEach(argVal
                -> LOGGER.debug(new ObjectMessage(Map.of("funcName"
                , fnReqResponse.name, "argVal" , argVal))));
        }
        ServerUtils.preProcess(fnReqResponse);
        Optional<String> collection = getCurrentCollectionIfEmpty(Optional.empty(), Optional.of(fnReqResponse.customerId),
            Optional.of(fnReqResponse.app), Optional.of(fnReqResponse.instanceId));
        return collection.map(collec -> {
            return rrstore.storeFunctionReqResp(fnReqResponse, collec) ? null : "Unable to Store FnReqResp Object";
        }).or(() -> Optional.of("No current running collection/recording"));
    }


	@POST
    @Path("/fr")
    @Consumes(MediaType.TEXT_PLAIN)
    // Event redesign cleanup: This can be removed - will keep for now
    public Response storeFunc(String functionReqRespString /* @PathParam("customer") String customer,
                              @PathParam("instance") String instance, @PathParam("app") String app,
                              @PathParam("service") String service*/) {
        try {
            return storeFnReqResp(functionReqRespString)
                .map(errMessage -> Response.serverError().type(MediaType.APPLICATION_JSON)
                    .entity(new JSONObject(Map.of(Constants.REASON, errMessage))
                        .toString()).build()).orElse(Response.ok().build());
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON)
                .entity( new JSONObject(Map.of(Constants.MESSAGE, "Error while deserializing"
                    , Constants.REASON, e.getMessage())).toString()).build();
        }
    }


    @POST
    @Path("/storeEventBatch")
    public Response storeEventBatch(@Context HttpHeaders headers, byte[] messageBytes) {
        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst(Constants.CONTENT_TYPE));
        LOGGER.info(new ObjectMessage(
            Map.of(
                Constants.MESSAGE, "Batch Events received",
                Constants.CONTENT_TYPE,  contentType
            )));
        return contentType.map(
            ct -> {
                switch(ct) {
                    case Constants.APPLICATION_X_NDJSON:
                        try {
                            String jsonMultiline = new String(messageBytes);
                            String[] jsons = jsonMultiline.split("\n");
                            LOGGER.info(new ObjectMessage(
                                Map.of(
                                    "JSON batch size", jsons.length
                                )));
                            int numSuccess = Arrays.stream(jsons)
                                .mapToInt(this::processEventJson)
                                .sum();
                            String jsonResp = new JSONObject(Map.of(
                                "total", jsons.length,
                                "success", numSuccess
                            )).toString();
                            LOGGER.info(new ObjectMessage(
                                Map.of(
                                    Constants.MESSAGE, "finished processing",
                                    "total", jsons.length,
                                    "success", numSuccess
                                )));
                            return Response.ok(jsonResp).type(MediaType.APPLICATION_JSON_TYPE).build();
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE, "Error while processing multiline json"
                                )),e);
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }

                    case Constants.APPLICATION_X_MSGPACK:
                        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(messageBytes));
                        int total = 0, numSuccess = 0;
                        try {
                            while (unpacker.hasNext()) {
                                total++;
                                ValueType nextType = unpacker.getNextFormat().getValueType();
                                if (nextType.isMapType()) {
                                    int s = processEventJson(unpacker.unpackValue().toJson());
                                    numSuccess += s;
                                } else {
                                    LOGGER.error(new ObjectMessage(
                                        Map.of(Constants.REASON,
                                            "Unidentified format type in message pack stream " + nextType.name())));
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE, "Error while unpacking message pack byte stream ")), e
                            );
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        String jsonResp = new JSONObject(Map.of(
                            "total", total,
                            "success", numSuccess
                        )).toString();
                        LOGGER.info(new ObjectMessage(
                            Map.of(
                                Constants.MESSAGE, "finished processing",
                                "total", total,
                                "success", numSuccess
                                )));
                        return Response.ok(jsonResp).type(MediaType.APPLICATION_JSON_TYPE).build();
                    default :
                        return Response.serverError().entity("Content type not recognized :: " + ct).build();
                }
            }
        ).orElse(Response.serverError().entity("Content type not specified").build());

    }

    @POST
    @Path("/storeEvent")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response storeEvent(Event event) {
	    try {
	        event.validateEvent();
      } catch (InvalidEventException e) {
          LOGGER.error(new ObjectMessage(
              Map.of(Constants.MESSAGE, "Invalid Event")), e);
          return Response.status(Status.BAD_REQUEST).entity(Utils.buildErrorResponse(
              Status.BAD_REQUEST.toString(),Constants.ERROR,  e.getMessage())).build();
      }
	    eventQueue.enqueue(event);
	    /*
        try {
            StoreUtils.processEvent(event, config.rrstore);
        } catch (CubeStoreException e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                "Error while storing event/rr in solr")), e);
        }
	    */
        logStoreInfo("Enqueued Event", new CubeEventMetaInfo(event), true);
            return Response.ok().build();
    }

    @POST
    @Path("/deleteEventByReqId/{reqId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEventByReqId(Event event , @PathParam("reqId") String reqId) throws ParameterException {

	    if(event.customerId == null) throw new ParameterException("customerId is not present in the request");


	    boolean deletionSuccess = rrstore.deleteReqResByReqId(reqId , event.customerId , Optional.ofNullable(event.eventType));
	    return Response.ok().type(MediaType.APPLICATION_JSON).
            entity(buildSuccessResponse(Constants.SUCCESS , new JSONObject(Map.of("deletion_success" , deletionSuccess)) )).build();
    }

    @POST
    @Path("/deleteEventByTraceId/{traceId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEventByTraceId(Event event , @PathParam("traceId") String traceId) throws ParameterException {

        if(event.customerId == null) throw new ParameterException("customerId is not present in the request");
        if(event.getCollection() == null) throw new ParameterException("collection is not present in the request");

        boolean deletionSuccess = rrstore.deleteReqResByTraceId(traceId , event.customerId , event.getCollection(), Optional.ofNullable(event.eventType));
        return Response.ok().type(MediaType.APPLICATION_JSON).
            entity(buildSuccessResponse(Constants.SUCCESS , new JSONObject(Map.of("deletion_success" , deletionSuccess)) )).build();
    }

    // converts event from json to Event, stores it,
    // returns 1 on success, else 0 in case of failure or exception
    private int processEventJson(String eventJson) {
        Event event = null;
        try {
            WrapperEvent wrapperEvent = jsonMapper.readValue(eventJson, WrapperEvent.class);
            if (wrapperEvent.cubeEvent == null) {
                logStoreError(new CubeStoreException(new NullPointerException(), "Cube Event is null" ,
                    new CubeEventMetaInfo()));
                return 0;
            }
            event = wrapperEvent.cubeEvent;
            event.validateEvent();
        } catch (IOException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Error parsing Event JSON")),e);
            return 0;
        }catch (InvalidEventException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Invalid Event")), e);
            return 0;
        }

        eventQueue.enqueue(event);
        logStoreInfo("Enqueued event for storing", new CubeEventMetaInfo(event) , true);
        return 1;
	}

    @POST
    @Path("/frbatch")
    // Event redesign cleanup: This can be removed -- keep for now
    public Response storeFuncBatch(@Context UriInfo uriInfo , @Context HttpHeaders headers,
                                   byte[] messageBytes) {
        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst(Constants.CONTENT_TYPE));

        return contentType.map(
            ct -> {
                switch (ct) {
                    case Constants.APPLICATION_X_NDJSON:
                        try {
                            String jsonMultiline = new String(messageBytes);
                            // split on '\n' using the regex "\\\\n" because it's being interpreted as '\' and 'n' literals
                            Arrays.stream(jsonMultiline.split("\\\\n")).forEach(UtilException.rethrowConsumer(this::storeFunc));
                            return Response.ok().build();
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE, "Error while processing multiline json"
                                )),e);
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                    case Constants.APPLICATION_X_MSGPACK:
                        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(messageBytes));
                        try {
                            while (unpacker.hasNext()) {
                                ValueType nextType = unpacker.getNextFormat().getValueType();
                                if (nextType.isMapType()) {
                                    storeFunc(unpacker.unpackValue().toJson());
                                } else {
                                    LOGGER.error(new ObjectMessage(
                                        Map.of(Constants.REASON,
                                            "Unidentified format type in message pack stream " + nextType.name())));
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE, "Error while unpacking message pack byte stream ")), e
                            );
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        return Response.ok().build();
                    default:
                        return Response.serverError().entity("Content type not recognized :: " + ct).build();
                }
            }
        ).orElse(Response.serverError().entity("Content type not specified").build());
    }


    /**
     * @param defaultEvent defaultEvent is a wrapper on top of Event to accomodate both request and
     * response payload. While the request payload is sent as part of the Event object, response
     * payload will be specified as the rawRespPayloadString or rawRespPayloadBinary depending on the
     * type.
     *
     * If the request event is already present, response event is stored.
     * If the request event is not present, both request and response events are stored.
     *
     * @return
     * success - successful setting of default response for the request
     * fail - if storing request/response event fails
     * error - if an exception occurs.
     */
    @POST
    @Path("/event/setDefaultResponse")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response setDefaultRespForEvent(DefaultEvent defaultEvent) {

        if (defaultEvent == null) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.FAIL, Constants.INVALID_INPUT,
                    "Invalid input!")).build();
        }

        try {
            Event eventData = defaultEvent.getEvent();
            Optional<Event> defaultReqEvent = getOrStoreDefaultReqEvent(eventData);
            if (defaultReqEvent.isPresent() && storeDefaultRespEvent(defaultReqEvent.get(),
                    defaultEvent.getRespPayload())) {
                return Response.ok().type(MediaType.APPLICATION_JSON)
                    .entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject())).build();
            } else {
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.FAIL, Constants.STORE_EVENT_FAILED,
                        "Storing default response for event failed!")).build();
            }

        } catch (InvalidEventException e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.INVALID_EVENT,
                    "Trying to store invalid request/response event : " + e.getMessage())).build();
        } catch (RuntimeException e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.RUNTIME_EXCEPTION,
                    "Runtime exception occured. Check if the inputs are valid : " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/setCurrentAgentConfigTag")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response setAgentConfigTag(AgentConfigTagInfo tagInfo) {
        Pair<List, Stream<ConfigDAO>> result = rrstore
            .getAgentConfigWithFacets(tagInfo.customerId, tagInfo.app, Optional.of(tagInfo.service),
                Optional.of(tagInfo.instanceId), Optional.empty(), Optional.empty(),
                Optional.of(tagInfo.tag));
        if (!result.second().findAny().isPresent()) {
            String message = "Error while updating the config tag. Cannot find config for tag to update";
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE, message,
                    Constants.CUSTOMER_ID_FIELD, tagInfo.customerId, Constants.APP_FIELD,
                    tagInfo.app, Constants.SERVICE_FIELD, tagInfo.service,
                    Constants.INSTANCE_ID_FIELD, tagInfo.instanceId, Constants.TAG_FIELD,
                    tagInfo.tag)));
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                    message)).build();
        }

        if (rrstore.updateAgentConfigTag(tagInfo)) {
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(
                buildSuccessResponse(Constants.SUCCESS,
                    new JSONObject(
                        Map.of(Constants.MESSAGE, "The agent config tag has been changed",
                            Constants.CUSTOMER_ID_FIELD, tagInfo.customerId, Constants.APP_FIELD
                            , tagInfo.app, Constants.SERVICE_FIELD, tagInfo.service,
                            Constants.INSTANCE_ID_FIELD, tagInfo.instanceId, Constants.TAG_FIELD,
                            tagInfo.tag)))).build();
        } else {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                    "Error while trying to change the tag for config")).build();
        }
    }

    @POST
    @Path("/storeAgentConfig")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response storeAgentConfig(ConfigDAO configDAO) {
        if(configDAO == null) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.FAIL, Constants.INVALID_INPUT,
                    "Invalid input!")).build();
        }
        try {
            ValidateAgentStore.validate(configDAO);
            if(!rrstore.storeAgentConfig(configDAO)) {
                throw new SolrStoreException("Cannot store object in solr");
            };
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(
                buildSuccessResponse(Constants.SUCCESS,
                    new JSONObject(Map.of(Constants.MESSAGE, "The config is saved",
                        Constants.CUSTOMER_ID_FIELD, configDAO.customerId, Constants.APP_FIELD, configDAO.app,
                        Constants.VERSION_FIELD, configDAO.version, Constants.SERVICE_FIELD, configDAO.service,
                        Constants.INSTANCE_ID_FIELD, configDAO.instanceId)))).build();

        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE, "Data fields cannot be null or empty")), e);

            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.INVALID_INPUT,
                    "Data fields cannot be null or empty")).build();
        }catch (Exception e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE, "Error while saving the config",
                    Constants.CUSTOMER_ID_FIELD, configDAO.customerId, Constants.APP_FIELD, configDAO.app,
                    Constants.VERSION_FIELD, configDAO.version, Constants.SERVICE_FIELD, configDAO.service,
                    Constants.INSTANCE_ID_FIELD, configDAO.instanceId)), e);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                    e.getMessage())).build();
        }
    }

    @GET
    @Path("/fetchAgentConfig/{customerId}/{app}/{service}/{instanceId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response fetchAgentConfig(@PathParam("customerId") String customerId, @PathParam("app") String app,
        @PathParam("service") String service, @PathParam("instanceId") String instanceId , @Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        String existingTag = queryParams.getFirst(Constants.TAG_FIELD);
        String existingVersion = queryParams.getFirst(Constants.VERSION_FIELD);
        try {

            Optional<ConfigDAO> response = rrstore.getAgentConfig(customerId, app,
                service, instanceId);
            if(response.isPresent()) {
                //String json = jsonMapper.writeValueAsString(response.get());
                ConfigDAO responseConfig = response.get();
                if (responseConfig.tag.equals(existingTag) && String.valueOf(responseConfig.version).
                    equals(existingVersion))
                    return Response.notModified().build();
                return Response.ok().type(MediaType.APPLICATION_JSON).entity(responseConfig).build();
            } else {
                LOGGER.error(
                    new ObjectMessage(Map.of(Constants.MESSAGE, "No Config found for given Customer",
                        Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app,
                        Constants.SERVICE_FIELD, service, Constants.INSTANCE_ID_FIELD, instanceId)));
                return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                        "No Config found for given Customer")).build();
            }
        } catch (Exception e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE, "Error while retrieving the response",
                    Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app,
                    Constants.SERVICE_FIELD, service, Constants.INSTANCE_ID_FIELD, instanceId)), e);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                    "Error while retrieving the response")).build();
        }
    }

    @POST
    @Path("/deleteAgentConfig/{customerId}/{app}/{service}/{instanceId}")
    public Response deleteAgentConfig(@PathParam("customerId") String customerId, @PathParam("app") String app,
        @PathParam("service") String service, @PathParam("instanceId") String instanceId , @Context UriInfo ui) {
        boolean deletionSuccess = rrstore.deleteAgentConfig(customerId , app, service, instanceId);
        return Response.ok().type(MediaType.APPLICATION_JSON).
            entity(buildSuccessResponse(Constants.SUCCESS , new JSONObject(Map.of("deletion_success" , deletionSuccess)) )).build();
    }

    @GET
    @Path("/fetchAgentConfigWithFacets/{customerId}/{app}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response fetchAgentConfigWithFacets(@Context UriInfo ui, @PathParam("customerId") String customerId,
        @PathParam("app") String app) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> service = Optional
            .ofNullable(queryParams.getFirst(Constants.SERVICE_FIELD));
        Optional<String> instanceId = Optional
            .ofNullable(queryParams.getFirst(Constants.INSTANCE_ID_FIELD));
        Optional<Integer> numResults = Optional.ofNullable(queryParams.getFirst(Constants.NUM_RESULTS_FIELD))
            .flatMap(Utils::strToInt);
        Optional<Integer> start = Optional.ofNullable(queryParams.getFirst(Constants.START_FIELD))
            .flatMap(Utils::strToInt);
        try {
            Pair<List, Stream<ConfigDAO>> result = rrstore.getAgentConfigWithFacets(customerId, app, service, instanceId,
                numResults, start, Optional.empty());
            Map response = Map.of("facets", Map.of("instance_facets", result.first()), "configs", result.second().collect(Collectors.toList()));
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(response).build();

        } catch (Exception e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE, "Error while retrieving the response",
                    Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app)), e);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                    "Error while retrieving the response")).build();
        }
    }

    @POST
    @Path("/ackConfigApplication")
    @Produces({MediaType.APPLICATION_JSON})
    public Response acknowledgeConfigApplication(ConfigApplicationAcknowledge confApplicationAck) {
        try {
            if (rrstore.saveAgentConfigAcknowledge(confApplicationAck)) {
                return Response.ok().build();
            } else {
                throw new Exception("Unable to store acknowledge info");
            }
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Error while processing "
                + "agent config acknowledge")), e);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                    "Error while processing acknowledge")).build();
        }
    }


    /**
     * API to get the sampling info for agents running for a particular {customerId}/{app}/{service}/{instanceId}
     * @param customerId
     * @param app
     * @param service
     * @param instanceId
     * @param ui
     * @return
     */
    @GET
    @Path("/getAgentSamplingFacets/{customerId}/{app}/{service}/{instanceId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getAgentSamplingFacets(@PathParam("customerId") String customerId, @PathParam("app") String app,
        @PathParam("service") String service, @PathParam("instanceId") String instanceId , @Context UriInfo ui) {
        try {
            CubeMetaInfo cubeMetaInfo = new CubeMetaInfo(customerId, instanceId,
                app, service);

            // Returns ack from now to last AGENT_ACK_DEFAULT_DELAY_SEC secs
            // AGENT_ACK_DEFAULT_DELAY_SEC is set to match agent's default acknowledge delay
            // This is done to ensure that we have "mostly" unique acknowledgements in this time frame.
            // To ensure absolute unique ack all agents must send the ack in sync.
            Pair<Result<ConfigApplicationAcknowledge>, List> pair = rrstore
                .getLatestAgentConfigAcknowledge(cubeMetaInfo, true, Constants.AGENT_ACK_DEFAULT_DELAY_SEC);

            List<ConfigApplicationAcknowledge> results = pair.first().getObjects()
                .collect(Collectors.toList());
            List samplingFacets = pair.second();
            Map jsonMap = new HashMap();
            jsonMap.put("Results" , results);
            jsonMap.put("SamplingFacets" , samplingFacets);
            return Response.ok().entity(jsonMapper.writeValueAsString(jsonMap)).build();
        } catch (JsonProcessingException e) {
            String message = "Error while parsing SamplingFacets";
            LOGGER.error(
                new ObjectMessage(
                    Map.of(Constants.MESSAGE, message,
                        Constants.CUSTOMER_ID_FIELD, customerId, Constants.APP_FIELD, app,
                        Constants.INSTANCE_ID_FIELD, instanceId, Constants.SERVICE_FIELD, service)), e);
            return Response.serverError().entity(
                buildErrorResponse(Constants.ERROR, message,
                    e.getMessage())).build();
        }
    }

    @POST
    @Path("/saveResult")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response saveResult(ReqRespMatchResult reqRespMatchResult) {
        if (reqRespMatchResult == null) {
            LOGGER.error(Map.of(Constants.MESSAGE, "ReqRespMatchResult is null"));
            return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.FAIL, Constants.INVALID_INPUT,
                    "Invalid input!")).build();
        }
        boolean result = rrstore.saveResult(reqRespMatchResult);
        if (!result) {
            LOGGER.error(Map.of(Constants.MESSAGE, "Unable to store result in solr",
                    Constants.REPLAY_ID_FIELD, reqRespMatchResult.replayId));
            return Response.serverError().entity(
                buildErrorResponse(Constants.ERROR, Constants.REPLAY_ID_FIELD,
                    "Unable to store result in solr")).build();
        }
        return Response.ok().type(MediaType.APPLICATION_JSON)
            .entity("The Result is saved in Solr").build();
    }

    @GET
    @Path("getCurrentRecordOrReplay/{customerId}/{app}/{instanceId}")
    public Response getCurrentRecordOrReplay(@Context UriInfo ui, @PathParam("customerId") String customerId,
        @PathParam("app") String app, @PathParam("instanceId") String instanceId) {
        Optional<RecordOrReplay> recordOrReplay = rrstore.getCurrentRecordOrReplay(customerId, app,
            instanceId);
        Response resp = recordOrReplay.map(rr -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(rr);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting RecordOrReplay object to Json for "
                    + "customerId=%s, app=%s, instanceId=%s", customerId, app, instanceId), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND)
            .entity(String.format("RecordOrReplay Object not found for customerId=%s", customerId)).build());
        return resp;
    }

    private boolean storeDefaultRespEvent(
        Event defaultReqEvent, Payload payload) throws InvalidEventException {
        //Store default response
        EventBuilder eventBuilder = new EventBuilder(defaultReqEvent.customerId,
            defaultReqEvent.app,
            defaultReqEvent.service, "NA", "NA",
            new MDTraceInfo("NA", null, null), RunType.Manual
            , Optional.of(Instant.now()), defaultReqEvent.reqId, defaultReqEvent.apiPath,
            Event.EventType.getResponseType(defaultReqEvent.eventType), defaultReqEvent.recordingType).withRunId(defaultReqEvent.runId);
        eventBuilder.setPayload(payload);
        Event defaultRespEvent = eventBuilder.createEvent();
        // parseAndSetKey is needed only for requests

        //We cannot use storeEvent API as it checks for an active record/replay.
        //This API is standalone and should work without an active record/replay.
        if (!rrstore.save(defaultRespEvent)) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Storing Response Event failed.",
                    Constants.EVENT_TYPE_FIELD, defaultReqEvent.eventType,
                    Constants.REQ_ID_FIELD, defaultReqEvent.reqId,
                    Constants.API_PATH_FIELD, defaultReqEvent.apiPath)));

            return false;
        }

        rrstore.commit();
        return true;
    }

    private Optional<Event> getOrStoreDefaultReqEvent(Event reqEvent) throws InvalidEventException {
        if (reqEvent == null || !reqEvent.validate()) {
            LOGGER.debug(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Invalid Request event!")));
            throw new InvalidEventException();
        }

        EventQuery reqQuery = new EventQuery.Builder(reqEvent.customerId, reqEvent.app,
            reqEvent.eventType)
            .withService(reqEvent.service)
            .withPaths(List.of(reqEvent.apiPath))
            .withOffset(0).withLimit(1)
            .build();

        Optional<Event> matchingReqEvent = rrstore.getSingleEvent(reqQuery);

        //Store request event if not present.
        if (matchingReqEvent.isEmpty()) {
            LOGGER.debug(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Request Event not found. Storing request event",
                    Constants.EVENT_TYPE_FIELD, reqEvent.eventType,
                    Constants.REQ_ID_FIELD, reqEvent.reqId,
                    Constants.API_PATH_FIELD, reqEvent.apiPath)));

            EventBuilder eventBuilder = new EventBuilder(reqEvent.customerId, reqEvent.app,
                reqEvent.service, "NA", "NA"
                , new MDTraceInfo("NA", null, null), RunType.Manual,
                Optional.of(Instant.now()),
                reqEvent.reqId, reqEvent.apiPath, reqEvent.eventType, reqEvent.recordingType).withRunId(reqEvent.runId);

            //TODO:Add support for Binary payload.
            eventBuilder.setPayload(reqEvent.payload);
            Event defaultReqEvent = eventBuilder.createEvent();
            try {
                defaultReqEvent.parseAndSetKey(rrstore.
                    getTemplate(defaultReqEvent.customerId, defaultReqEvent.app, defaultReqEvent.service,
                        defaultReqEvent.apiPath, DEFAULT_TEMPLATE_VER,
                        Type.RequestMatch, Optional.ofNullable(defaultReqEvent.eventType)
                        , Optional.empty(), UUID.randomUUID().toString()));
            } catch (TemplateNotFoundException e) {
                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.EVENT_TYPE_FIELD, defaultReqEvent.eventType,
                        Constants.REQ_ID_FIELD, defaultReqEvent.reqId,
                        Constants.API_PATH_FIELD, defaultReqEvent.apiPath)) , e);
                return Optional.empty();
            }

            //We cannot use storeEvent API as it checks for a running record/replay.
            //This API is standalone and should work without an active record/replay.
            if (!rrstore.save(defaultReqEvent)) {
                LOGGER.debug(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "Storing Request Event failed.",
                        Constants.EVENT_TYPE_FIELD, reqEvent.eventType,
                        Constants.REQ_ID_FIELD, reqEvent.reqId,
                        Constants.API_PATH_FIELD, reqEvent.apiPath)));

                return Optional.empty();
            }

            rrstore.commit();

            return Optional.of(defaultReqEvent);
        }

        return matchingReqEvent;
    }

	@POST
	@Path("start/{customerId}/{app}/{instanceId}/{templateSetVersion}")
	@Consumes("application/x-www-form-urlencoded")
    public void start(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui,
                          MultivaluedMap<String, String> formParams,
                          @PathParam("app") String app,
                          @PathParam("customerId") String customerId,
                          @PathParam("instanceId") String instanceId,
                          @PathParam("templateSetVersion") String templateSetVersion) {
	    // check if recording or replay is ongoing for (customer, app, instanceId)

      Optional<RecordingType> recordingType =
          Optional.ofNullable(formParams.getFirst(Constants.RECORDING_TYPE_FIELD))
              .flatMap(r -> Utils.valueOf(RecordingType.class, r)).or(() -> Optional.of(RecordingType.Golden));
      Optional<Response> errResp = recordingType.flatMap(rt -> {
          if(rt == RecordingType.History || rt == RecordingType.UserGolden) {
              return Optional.empty();
          } else {
              return Utils.checkActiveCollection(rrstore, customerId, app,
                  instanceId, Optional.empty());
          }
      });
        if (errResp.isPresent()) {
            asyncResponse.resume(errResp.get());
            return;
        }

        String name = formParams.getFirst("name");
        String userId = formParams.getFirst("userId");
        String label = formParams.getFirst("label");

        Optional<String> jarPath = Optional.ofNullable(formParams.getFirst("jarPath"));

        if (name==null) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST)
                .entity("Name needs to be given for a golden")
                .build());
            return;
        }

        if (userId==null) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST)
                .entity("userId should be specified for a golden")
                .build());
            return;
        }

        if (label==null) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST)
                .entity("label should be specified for a golden")
                .build());
            return;
        }

        String collection = UUID.randomUUID().toString();;

        // check if recording collection name is unique for (customerId, app)
        Optional<Recording> recording = rrstore
            .getRecordingByName(customerId, app, name, Optional.ofNullable(label));
        errResp = recording.filter(r -> r.status == RecordingStatus.Running)
            .map(recordingv -> Response.status(Response.Status.CONFLICT)
                .entity(String.format("Collection %s already active for customer %s, app %s, for instance %s. Use different name",
                    collection, customerId, app, recordingv.instanceId))
                .build());
        if (errResp.isPresent()) {
            asyncResponse.resume(errResp.get());
            return;
        }

        // NOTE that if the recording is not active, it will be activated again. This allows the same collection recording to be
        // stopped and started multiple times

        LOGGER.info(String.format("Starting recording for customer %s, app %s, instance %s, collection %s",
            customerId, app, instanceId, collection));

        // Ensure name is unique for a customer and app
        Optional<Recording> recWithSameName = rrstore.getRecordingByName(customerId, app, name, Optional.ofNullable(label));
        if (recWithSameName.isPresent()) {
            asyncResponse.resume(Response.status(Response.Status.CONFLICT)
            .entity("Golden already present for name/label - " + name + "/" + label + ". Specify unique name/label combination")
            .build());
            return;
        }

        Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst("codeVersion"));
        Optional<String> branch = Optional.ofNullable(formParams.getFirst("branch"));
        Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst("gitCommitId"));
        List<String> tags = Optional.ofNullable(formParams.get("tags")).orElse(new ArrayList<String>());
        Optional<String> comment = Optional.ofNullable(formParams.getFirst("comment"));

        RecordingBuilder recordingBuilder = new RecordingBuilder(customerId, app,
            instanceId, collection).withTemplateSetVersion(templateSetVersion).withName(name)
            .withLabel(label).withUserId(userId).withTags(tags);
        codeVersion.ifPresent(recordingBuilder::withCodeVersion);
        branch.ifPresent(recordingBuilder::withBranch);
        gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
        comment.ifPresent(recordingBuilder::withComment);
        recordingType.ifPresent(recordingBuilder::withRecordingType);
        try {
            jarPath.ifPresent(UtilException.rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));
        } catch (Exception e) {
            asyncResponse.resume(Response.serverError().entity((new JSONObject(Map.of(Constants.ERROR
                , e.getMessage()))).toString()).build());
            return;
        }

      CompletableFuture<Response> resp =  beforeRecording(formParams, recordingBuilder.build())
            .thenApply(v -> {
                Response response = ReqRespStore.startRecording(recordingBuilder.build() ,rrstore)
                    .map(newr -> {
                        if (newr.recordingType == RecordingType.History) {
                            ReplayBuilder replayBuilder = new ReplayBuilder(ui.getBaseUri().toString(),
                                newr.customerId, newr.app, userId, newr.collection, userId)
                                .withTemplateSetVersion(newr.templateVersion)
                                .withRecordingId(newr.id)
                                .withGoldenName(newr.name)
                                .withReplayStatus(ReplayStatus.Running)
                                .withReplayId(newr.collection)
                                .withRunId(newr.collection + " " + Instant.now().toString());
                            Replay replay = replayBuilder.build();
                            rrstore.saveReplay(replay);
                        }
                        String json;
                        try {
                            json = jsonMapper.writeValueAsString(newr);
                            return Response.ok(json, MediaType.APPLICATION_JSON).build();
                        } catch (JsonProcessingException ex) {
                            LOGGER.error(String.format(
                                "Error in converting Recording object to Json for customer %s, app %s, collection %s",
                                customerId, app, collection), ex);
                            return Response.serverError().build();
                        }
                    }).orElse(Response.serverError().build());
                return response;
            });
        resp.thenApply(response -> asyncResponse.resume(response))
        .exceptionally(e -> asyncResponse.resume(
            Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(String.format("Server error: " + e.getMessage())).build()));;
    }

    protected CompletableFuture<Void> beforeRecording(MultivaluedMap<String, String> formParams, Recording recording) {
        Optional<String> tagOpt = formParams == null ? Optional.empty()
                                    : Optional.ofNullable(formParams.getFirst(Constants.TAG_FIELD));

        return tagOpt.map(tag -> this.tagConfig.setTag(recording, recording.instanceId, tag))
            .orElse(CompletableFuture.completedFuture(null));
    }

    protected CompletableFuture<Void> afterRecording(MultivaluedMap<String, String> params, Recording recording) {
        Optional<String> tagOpt = params == null ? Optional.empty()
                                    :Optional.ofNullable(params.getFirst(Constants.RESET_TAG_FIELD));

        return tagOpt.map(tag -> this.tagConfig.setTag(recording, recording.instanceId, tag))
            .orElse(CompletableFuture.completedFuture(null));
    }

    @POST
    @Path("resumeRecording/{recordingId}")
    public void resumeRecording(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui,
        @PathParam("recordingId") String recordingId) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        CompletableFuture<Response> resp = resumeRecording(recording, ui.getQueryParameters());
        resp.thenApply(response -> asyncResponse.resume(response));
    }

    @POST
    @Path("resumeRecordingByNameLabel/")
    public void resumeRecordingByNameLabel(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        String customerId = queryParams.getFirst(Constants.CUSTOMER_ID_FIELD);
        String app = queryParams.getFirst(Constants.APP_FIELD);
        String name = queryParams.getFirst(Constants.GOLDEN_NAME_FIELD);
        if(customerId ==null || app ==null || name == null) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST)
                .entity("CustomerId/app/name needs to be given for a golden")
                .build());
            return;
        }
        Optional<String> label = Optional.ofNullable(queryParams.getFirst(Constants.GOLDEN_LABEL_FIELD));


        Optional<Recording> recording = rrstore.getRecordingByName(customerId, app, name, label);
        CompletableFuture<Response> resp = resumeRecording(recording, ui.getQueryParameters());
        resp.thenApply(response -> asyncResponse.resume(response));
    }

    public CompletableFuture<Response> resumeRecording(Optional<Recording> recording, MultivaluedMap<String, String> queryParams) {
        return recording.map(r -> {
            CompletableFuture<Response> response = beforeRecording(queryParams, r).thenApply(v ->
            {
                Recording resumedRecording = ReqRespStore.resumeRecording(r, rrstore);
                String json;
                try {
                    json = jsonMapper.writeValueAsString(resumedRecording);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException ex) {
                    LOGGER.error(new ObjectMessage(Map.of(
                        Constants.MESSAGE, "Error in converting response and match results to Json",
                        Constants.RECORDING_ID, r.id
                    )));
                    return Response.serverError()
                        .entity(buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                            ex.getMessage())).build();
                }
            });
            return response;
        }).orElse(CompletableFuture.completedFuture(Response.status(Response.Status.NOT_FOUND).
            entity(buildErrorResponse(Constants.ERROR, Constants.RECORDING_NOT_FOUND,
                String.format("Recording not found"))).build()));
    }

    @GET
    @Path("searchRecording")
    public Response searchRecording(@Context UriInfo ui) {
        MultivaluedMap<String, String> formParams = ui.getQueryParameters();
        Optional<String> customerId = Optional.ofNullable(formParams.getFirst(Constants.CUSTOMER_ID_FIELD));
        Optional<String> app = Optional.ofNullable(formParams.getFirst(Constants.APP_FIELD));
        Optional<String> instanceId = Optional.ofNullable(formParams.getFirst(Constants.INSTANCE_ID_FIELD));
        Optional<RecordingStatus> status = Optional.ofNullable(formParams.getFirst(Constants.STATUS))
            .flatMap(s -> Utils.valueOf(RecordingStatus.class, s));
        Optional<String> collection = Optional.ofNullable(formParams.getFirst(Constants.COLLECTION_FIELD));
        Optional<String> templateVersion = Optional.ofNullable(formParams.getFirst(Constants.VERSION_FIELD));
        Optional<String> parentRecordingId = Optional.ofNullable(formParams.getFirst(Constants.PARENT_RECORDING_FIELD));
        Optional<String> rootRecordingId = Optional.ofNullable(formParams.getFirst(Constants.ROOT_RECORDING_FIELD));
        Optional<String> name = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_NAME_FIELD));
        Optional<String> label = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_LABEL_FIELD));
        Optional<String> userId = Optional.ofNullable(formParams.getFirst(Constants.USER_ID_FIELD));
        Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst(Constants.CODE_VERSION_FIELD));
        Optional<String> branch = Optional.ofNullable(formParams.getFirst(Constants.BRANCH_FIELD));
        Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst(Constants.GIT_COMMIT_ID_FIELD));
        List<String> tags = Optional.ofNullable(formParams.get(Constants.TAGS_FIELD)).orElse(new ArrayList<String>());
        Optional<String> collectionUpdOpSetId = Optional.ofNullable(formParams.getFirst(Constants.COLLECTION_UPD_OP_SET_ID_FIELD));
        Optional<String> templateUpdOpSetId = Optional.ofNullable(formParams.getFirst(Constants.TEMPLATE_UPD_OP_SET_ID_FIELD));
        String archivedString = formParams.getFirst(Constants.ARCHIVED_FIELD);
        Optional<String> recordingType = Optional.ofNullable(formParams.getFirst(Constants.RECORDING_TYPE_FIELD));
        Optional<Boolean> archived = Optional.empty();
        Optional<String> recordingId = Optional.ofNullable(formParams.getFirst(Constants.RECORDING_ID));
        Integer numResults =
            Optional.ofNullable(formParams.getFirst(Constants.NUM_RESULTS_FIELD)).flatMap(Utils::strToInt).orElse(20);
        Optional<Integer> start = Optional.ofNullable(formParams.getFirst(Constants.START_FIELD)).flatMap(Utils::strToInt);

        try {

            if(archivedString!=null) {
                if (archivedString.equalsIgnoreCase("true") || archivedString
                    .equalsIgnoreCase("false")) {
                    archived = Optional.of(Boolean.valueOf(archivedString));
                } else {
                    throw new BadValueException(
                        "Only \"true\" or \"false\" value allowed for archived(boolean) fields");
                }
            }

            Result<Recording> result = rrstore.getRecording(customerId, app, instanceId, status, collection, templateVersion, name, parentRecordingId, rootRecordingId,
                codeVersion, branch, tags, archived, gitCommitId, collectionUpdOpSetId, templateUpdOpSetId, userId, label, recordingType, recordingId, Optional.of(numResults), start);
            Map jsonMap = new HashMap();

            jsonMap.put("recordings", result.getObjects().collect(Collectors.toList()));
            jsonMap.put("numFound", result.getNumFound());

            String json;
            json = jsonMapper.writeValueAsString(jsonMap);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();

        } catch (JsonProcessingException je) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR, "Error in converting Recording object to Json for recordingId", Constants.REASON, je)));
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    "Unable to parse JSON ")).build();

        } catch (BadValueException bve) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.BAD_VALUE_EXCEPTION,
                    bve.getMessage())).build();
        }
    }



    @GET
	@Path("status/{customerId}/{app}/{name}/{label}")
    public Response status(@Context UriInfo ui,
                           @PathParam("name") String name,
                           @PathParam("customerId") String customerId,
                           @PathParam("app") String app,
                           @PathParam("label") String label) {
	    Optional<Recording> recording = rrstore.getRecordingByName(customerId,
            app, name, Optional.of(label));

        Response resp = recording.map(r -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(r);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, golden %s with label %s.", customerId, app, name, label), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity(String.format("Status not found for for customer %s, app %s, golden %s with label %s.", customerId, app, name, label)).build());
        return resp;
    }

    @GET
	@Path("currentcollection")
    public Response currentcollection(@Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> instanceId = Optional.ofNullable(queryParams.getFirst(Constants.INSTANCE_ID_FIELD));
        Optional<String> customerId = Optional.ofNullable(queryParams.getFirst(Constants.CUSTOMER_ID_FIELD));
        Optional<String> app = Optional.ofNullable(queryParams.getFirst(Constants.APP_FIELD));
        String currentcollection = rrstore.getCurrentCollection(customerId, app, instanceId)
            .orElse("No current collection");
        return Response.ok(currentcollection).build();
    }

    @POST
    @Path("stop/{recordingid}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void stop(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui,
                         @PathParam("recordingid") String recordingid,
            MultivaluedMap<String, String> formParams) {
        Optional<Recording> recording = rrstore.getRecording(recordingid);
        Response resp = stopRecording(recording);
        if(recording.isPresent()) {
            afterRecording(formParams, recording.get()).thenApply(v -> asyncResponse.resume(resp));
        } else {
            asyncResponse.resume(resp);
        }
    }

    @POST
    @Path("forcestop/{recordingid}")
    public Response forceStop(@Context UriInfo ui,
        @PathParam("recordingid") String recordingid) {
        Optional<Recording> recording = rrstore.getRecording(recordingid);
        return recording.map(r -> {
            Recording stoppedr = ReqRespStore.forceStopRecording(r, rrstore);
            String json;
            try {
                json = jsonMapper.writeValueAsString(stoppedr);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException ex) {
                LOGGER.error(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Error in converting response and match results to Json",
                    Constants.RECORDING_ID, r.id
                )));
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).
            entity(String.format("Recording not found")).build());
    }

    @POST
    @Path("stopRecordingByNameLabel/")
    public void stopRecordingByNameLabel(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        String customerId = queryParams.getFirst(Constants.CUSTOMER_ID_FIELD);
        String app = queryParams.getFirst(Constants.APP_FIELD);
        String name = queryParams.getFirst(Constants.GOLDEN_NAME_FIELD);
        if(customerId ==null || app ==null || name == null) {
             asyncResponse.resume(Response.status(Status.BAD_REQUEST)
                .entity("CustomerId/app/name needs to be given for a golden")
                .build());
             return;
        }
        Optional<String> label = Optional.ofNullable(queryParams.getFirst(Constants.GOLDEN_LABEL_FIELD));
        Optional<Recording> recording = rrstore.getRecordingByName(customerId, app, name, label);
        Response resp = stopRecording(recording);
        if(recording.isPresent()) {
            afterRecording(queryParams, recording.get()).thenApply(v -> asyncResponse.resume(resp));
        } else {
            asyncResponse.resume(resp);
        }

    }

    public Response stopRecording(Optional<Recording> recording) {
        return recording.map(r -> {
            Recording stoppedr = ReqRespStore.stopRecording(r, rrstore);
            String json;
            try {
                json = jsonMapper.writeValueAsString(stoppedr);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException ex) {
                LOGGER.error(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Error in converting response and match results to Json",
                    Constants.RECORDING_ID, r.id
                )));
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).
            entity(String.format("Recording not found")).build());
    }

    @POST
    @Path("softDelete/{recordingId}")
    public Response softDelete(@PathParam("recordingId") String recordingId) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(rec -> {
            try {
                Recording deletedR = ReqRespStore.softDeleteRecording(rec, rrstore);
                String json;
                LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE, "Soft deleting recording", "RecordingId", recordingId)));
                json = jsonMapper.writeValueAsString(deletedR);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException ex) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR, "Error in converting Recording object to Json for recordingId", "RecordingId", recordingId,
                    Constants.REASON, ex)));
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Unable to parse JSON ")).build();
            } catch (RecordingSaveFailureException re) {
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.ERROR, Constants.RECORDING_SAVE_FAILURE_EXCEPTION,
                        re.getMessage())).build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).
            entity(buildErrorResponse(Constants.ERROR, Constants.RECORDING_NOT_FOUND,
                "Recording not found for recordingId" + recordingId)).build());
        return resp;
    }


    @POST
    @Path("updateGoldenFields/{recordingId}")
    @Consumes("application/x-www-form-urlencoded")
    public Response updateGoldenFields(@PathParam("recordingId") String recordingId,
        MultivaluedMap<String, String> formParams) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(rec -> {
            try {
                Optional<String> name = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_NAME_FIELD));
                Optional<String> label = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_LABEL_FIELD));
                Optional<String> userId = Optional.ofNullable(formParams.getFirst(Constants.USER_ID_FIELD));
                Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst(Constants.CODE_VERSION_FIELD));
                Optional<String> branch = Optional.ofNullable(formParams.getFirst(Constants.BRANCH_FIELD));
                Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst(Constants.GIT_COMMIT_ID_FIELD));
                List<String> tags = Optional.ofNullable(formParams.get(Constants.TAGS_FIELD)).orElse(new ArrayList<String>());
                Optional<String> comment = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_COMMENT_FIELD));


                if(name.isPresent() && label.isPresent()) {
                    String nameVal = name.get();
                    String labelVal = label.get();
                    Optional<Recording> recWithSameName = rrstore.getRecordingByName(rec.customerId, rec.app, nameVal, Optional.ofNullable(labelVal));
                    if (recWithSameName.isPresent()) {
                        String errorMessage = "Golden with same name/label present " + nameVal;
                        LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR,
                            errorMessage, "RecordingId", recordingId)));
                        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                            buildErrorResponse(Constants.ERROR, Constants.RECORDING_SAME_NAME_EXCEPTION,
                                errorMessage)).build();
                    }
                }


                RecordingBuilder recordingBuilder = new RecordingBuilder(rec.customerId, rec.app,
                rec.instanceId, rec.collection)
                    .withStatus(rec.status)
                    .withTemplateSetVersion(rec.templateVersion)
                    .withRootRecordingId(rec.rootRecordingId)
                    .withArchived(rec.archived)
                    .withId(rec.id) // same recording is updated, so carry over id
                    .withRecordingType(rec.recordingType);
                rec.parentRecordingId.ifPresent(recordingBuilder::withParentRecordingId);
                recordingBuilder.withName(name.orElse(rec.name));
                recordingBuilder.withLabel(label.orElse(rec.label));
                recordingBuilder.withUserId(userId.orElse(rec.userId));
                recordingBuilder.withCodeVersion(codeVersion.orElse(rec.codeVersion.orElse(null)));
                recordingBuilder.withBranch(branch.orElse(rec.branch.orElse(null)));

                if(tags.isEmpty()) {
                    recordingBuilder.withTags(rec.tags);
                } else {
                    recordingBuilder.withTags(tags);
                }
                recordingBuilder.withGitCommitId(gitCommitId.orElse(rec.gitCommitId.orElse(null)));
                recordingBuilder.withComment(comment.orElse(rec.comment.orElse(null)));
                rec.collectionUpdOpSetId.ifPresent(recordingBuilder::withCollectionUpdateOpSetId);
                rec.templateUpdOpSetId.ifPresent(recordingBuilder::withTemplateUpdateOpSetId);
                rec.generatedClassJarPath.ifPresent(UtilException.rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));

                Recording updatedRecording = recordingBuilder.build();

                rrstore.saveRecording(updatedRecording);

                String json;
                LOGGER.info(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "Recording updated", "RecordingId",
                        recordingId)));
                json = jsonMapper.writeValueAsString(updatedRecording);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException ex) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR,
                    "Error in converting Recording object to Json for recordingId", "RecordingId",
                    recordingId)), ex);
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Unable to parse JSON ")).build();
            } catch (Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR,
                    "Generic exception", "RecordingId",
                    recordingId)), e);
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.ERROR, Constants.GENERIC_EXCEPTION,
                        e.getMessage())).build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).
            entity(buildErrorResponse(Constants.ERROR, Constants.RECORDING_NOT_FOUND,
                "Recording not found for recordingId" + recordingId)).build());
        return resp;
    }

    /**
     * This is just a test api
     * @param uriInfo
     * @return
     */
    @GET
    @Path("/warmupcache")
    public Response warmUpCache(@Context UriInfo uriInfo) {
        try {
            TemplateKey key = new TemplateKey(DEFAULT_TEMPLATE_VER, "ravivj", "movieinfo"
                , "movieinfo", "minfo/listmovies", Type.ResponseCompare);
            Comparator comparator = rrstore.getComparator(key, Event.EventType.HTTPResponse);
            LOGGER.info("Got Response Comparator :: " + comparator.toString());
        } catch (Exception e) {
            LOGGER.error("Error occured :: " + e.getMessage() + " "
                + UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
        }
        return Response.ok().build();
    }




    /**
     *
     * @param eventQuery
     * @return matching events based on constraints
     */
    @POST
    @Path("getEvents")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getEvents(EventQuery eventQuery)
    {
        Result<Event> events = rrstore.getEvents(eventQuery);

        String json;
        try {
            json = jsonMapper.writeValueAsString(events);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Event list to Json for customer %s, app %s, " +
                    "collection %s.",
                eventQuery.getCustomerId(), eventQuery.getApp(), eventQuery.getCollection().orElse(""), e));
            return Response.serverError().build();
        }
    }

    @POST
    @Path("storeUserReqResp/{recordingId}")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response storeUserReqResp(@Context UriInfo ui,
        @PathParam("recordingId") String recordingId,
        List<UserReqRespContainer> userReqRespContainers) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(rec -> {
            if(rec.recordingType == RecordingType.History
                || rec.recordingType == RecordingType.UserGolden) {
                List<String> responseList = new ArrayList<>();
                final String generatedTraceId = io.md.utils.Utils.generateTraceId();
                for (UserReqRespContainer userReqRespContainer : userReqRespContainers) {
                    Event response = userReqRespContainer.response;
                    Event request = userReqRespContainer.request;
                    try {
                        request.validateEvent();
                        response.validateEvent();
                        String traceId = request.getTraceId();
                        if (rec.recordingType == RecordingType.UserGolden) {
                            String oldTraceId = request.getTraceId();
                            rrstore.deleteReqResByTraceId(oldTraceId, rec.collection);
                            rrstore.commit();
                            traceId = generatedTraceId;
                        }

                        TemplateKey tkey = new TemplateKey(rec.templateVersion, request.customerId,
                            request.app, request.service, request.apiPath, Type.RequestMatch,
                            io.md.utils.Utils.extractMethod(request), UUID.randomUUID().toString());
                        Comparator comparator = rrstore
                            .getComparator(tkey, request.eventType);
                        final String reqId = io.md.utils.Utils.generateRequestId(
                                request.service, traceId);
                        Event requestEvent = buildEvent(request, rec.collection, rec.recordingType,
                            reqId, traceId);
                        requestEvent.parseAndSetKey(comparator.getCompareTemplate());
                        Event responseEvent = buildEvent(response, rec.collection,
                            rec.recordingType, reqId, traceId);

                        if (!rrstore.save(requestEvent) || !rrstore.save(responseEvent)) {
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE, "Unable to store event in solr",
                                    Constants.RECORDING_ID, recordingId)));
                            return Response.serverError().entity(
                                buildErrorResponse(Constants.ERROR, Constants.RECORDING_ID,
                                    "Unable to store event in solr")).build();
                        }
                        String responseString = jsonMapper.writeValueAsString(Map.of("oldReqId", request.reqId,
                            "oldTraceId", request.getTraceId(), "newReqId", reqId, "newTraceId", traceId));
                        responseList.add(responseString);

                        if (rec.recordingType == RecordingType.History) {
                            TemplateKey templateKey = new TemplateKey(rec.templateVersion,
                                response.customerId,
                                response.app, response.service, response.apiPath,
                                Type.ResponseCompare, io.md.utils.Utils.extractMethod(request)
                                , rec.collection);
                            Comparator respComparator = rrstore
                                .getComparator(templateKey, response.eventType);
                            Optional<Event> optionalResponseEvent = rrstore
                                .getResponseEvent(request.reqId);
                            Match responseMatch = Match.NOMATCH;
                            if (optionalResponseEvent.isPresent()) {
                                responseMatch = respComparator
                                    .compare(response.payload, optionalResponseEvent.get().payload);
                            }

                            ReqRespMatchResult reqRespMatchResult = new ReqRespMatchResult(
                                Optional.of(request.reqId), Optional.of(reqId),
                                MatchType.ExactMatch, 1,
                                rec.collection, request.service, request.apiPath,
                                Optional.of(request.getTraceId()),
                                Optional.of(traceId), Optional.of(request.spanId),
                                Optional.of(request.parentSpanId), Optional.of(requestEvent.spanId),
                                Optional.of(requestEvent.parentSpanId), responseMatch,
                                Match.DONT_CARE);
                            if (!rrstore.saveResult(reqRespMatchResult)) {
                                LOGGER.error(new ObjectMessage(
                                    Map.of(Constants.MESSAGE, "Unable to store result in solr",
                                        Constants.RECORDING_ID, recordingId)));
                                return Response.serverError().entity(
                                    buildErrorResponse(Constants.ERROR, Constants.RECORDING_ID,
                                        "Unable to store result in solr")).build();
                            }
                        }
                    } catch (TemplateNotFoundException e) {
                        LOGGER.error(new ObjectMessage(
                            Map.of(Constants.MESSAGE, "Request Comparator Not Found",
                                Constants.RECORDING_ID, recordingId)), e);
                        return Response.serverError().entity("Request Comparator Not Found"
                            + e.getMessage()).build();
                    } catch (InvalidEventException e) {
                        LOGGER.error(new ObjectMessage(
                            Map.of(Constants.MESSAGE, "Invalid Event",
                                Constants.RECORDING_ID, recordingId)), e);
                        return Response.status(Status.BAD_REQUEST).entity(Utils.buildErrorResponse(
                            Status.BAD_REQUEST.toString(),Constants.ERROR,  e.getMessage())).build();
                    } catch (JsonProcessingException e) {
                        LOGGER.error(new ObjectMessage(
                            Map.of(Constants.MESSAGE, "Error while creating response",
                                Constants.RECORDING_ID, recordingId)), e);
                        return Response.serverError().entity("Error while creating response"
                            + e.getMessage()).build();
                    }
                }
                rrstore.commit();
                return Response.ok()
                    .entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject(
                        Map.of(Constants.MESSAGE, "The UserData is saved",
                            Constants.RECORDING_ID, recordingId,
                            Constants.RESPONSE, responseList)))).build();
            }
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Recording is not a UserGolden or History ",
                    Constants.RECORDING_ID, recordingId, Constants.RECORDING_TYPE_FIELD, rec.recordingType)));
            return Response.status(Status.BAD_REQUEST).
                entity(buildErrorResponse(Constants.ERROR, Constants.RECORDING_ID,
                    "Recording is not a UserGolden or History " + recordingId)).build();

        }).orElse(Response.status(Response.Status.NOT_FOUND).
            entity(buildErrorResponse(Constants.ERROR, Constants.RECORDING_NOT_FOUND,
                "Recording not found for recordingId " + recordingId)).build());
        return resp;
    }

    @GET
    @Path("status/{recordingId}")
    public Response status(@Context UriInfo ui,
        @PathParam("recordingId") String recordingId) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(r -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(r);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Recording object to Json for recordingId %s", recordingId), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity(String.format("Status not found for for recordingId %s", recordingId)).build());
        return resp;
    }

    @POST
    @Path("cache/flushall")
    public Response cacheFlushAll() {
        return ServerUtils.flushAll(config);
    }


    @POST
    @Path("/protoDescriptorFileUpload/{customerId}/{app}/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response protoDescriptorFileUpload(@PathParam("customerId") String customerId,
        @PathParam("app") String app,
        @FormDataParam("protoDescriptorFile") InputStream uploadedInputStream) {

        if(uploadedInputStream==null) {
            return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(
                Map.of("Message",
                    "Uploaded file stream null. Ensure the variable name is \"protoDescriptorFile\" for the file")
            )).toString()).build();
        }


        byte[] encodedFileBytes;
        try {
            encodedFileBytes = Base64.getEncoder().encode(uploadedInputStream.readAllBytes());
        } catch (IOException e) {
            LOGGER.error("Cannot encode uploaded proto descriptor file",e);
            return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(
                Map.of("Message", "Cannot encode uploaded proto descriptor file",
                    "Error", e.getMessage())).toString())).build();
        }

        ProtoDescriptorDAO protoDescriptorDAO = new ProtoDescriptorDAO(customerId, app);
        protoDescriptorDAO.setEncodedFile(new String(encodedFileBytes, StandardCharsets.UTF_8));
        boolean status = rrstore.storeProtoDescriptorFile(protoDescriptorDAO);
        return status ? Response.ok().build() : Response.serverError().entity(Map.of("Error", "Cannot store proto descriptor file")).build();
    }


    private Event buildEvent(Event event, String collection, RecordingType recordingType, String reqId, String traceId)
        throws InvalidEventException {
        EventBuilder eventBuilder = new EventBuilder(event.customerId, event.app,
            event.service, event.instanceId, collection,
            new MDTraceInfo(traceId, event.spanId, event.parentSpanId),
            event.getRunType(), Optional.of(Instant.now()), reqId, event.apiPath,
            event.eventType, recordingType).withRunId(event.runId);
        eventBuilder.setPayload(event.payload);
        eventBuilder.withMetaData(event.metaData);
        eventBuilder.withRunId(event.runId);
        return eventBuilder.createEvent();
    }


    /**
	 * @param config
	 */
	@Inject
	public CubeStore(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
		this.eventQueue = config.disruptorEventQueue;
		this.tagConfig = new TagConfig(config.rrstore);
	}


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	Config config;
	TagConfig tagConfig;
    DisruptorEventQueue eventQueue;

	private Optional<String> getCurrentCollectionIfEmpty(Optional<String> collection,
			Optional<String> customerId, Optional<String> app, Optional<String> instanceId) {
		return collection.or(() -> {
			return rrstore.getCurrentCollection(customerId, app, instanceId);
		});
	}

}
