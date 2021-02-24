/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static io.md.core.Utils.buildErrorResponse;
import static io.md.core.Utils.buildSuccessResponse;
import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;

import com.cube.core.ServerUtils;
import com.cube.core.TagConfig;
import com.cube.queue.StoreUtils;
import com.cube.sequence.SeqMgr;

import io.md.core.ApiGenPathMgr;
import io.md.core.CollectionKey;
import io.md.dao.CustomerAppConfig.Builder;
import io.md.dao.DataObj.DataObjProcessingException;
import io.md.dao.Event.EventType;
import io.md.injection.DynamicInjector;
import io.md.injection.DynamicInjectorFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import java.util.stream.Stream;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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

import io.md.dao.*;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.solr.common.util.Pair;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
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
import io.md.dao.agent.config.AgentConfigTagInfo;
import io.md.dao.agent.config.ConfigDAO;
import io.md.services.CustAppConfigCache;
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
import com.cube.utils.ScheduledCompletable;
import com.cube.ws.SanitizationFilters.BadStatuses;
import com.cube.ws.SanitizationFilters.IgnoreStaticContent;
import com.cube.ws.SanitizationFilters.ReqRespMissing;

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


    /**
     *
     * @param headers
     * @param messageBytes
     * @param direct
     * @return
     * This api is used mostly by agents to store events. It write events to a queue. The consumer dequeues and
     * writes the events after setting some fields based on current running recording or replay
     * When contentType is MediaType.APPLICATION_JSON, the behavior is different. This writes the events directly
     * without putting into queue (sync mode). Further, When direct is true, store directly to rrstore without any
     * change in event and when direct is false, set payloadKey and other fields (but not the collection)
     */
    @POST
    @Path("/storeEventBatch")
    public Response storeEventBatch(@Context HttpHeaders headers, byte[] messageBytes,
                                    @DefaultValue("false") @QueryParam("direct") boolean direct) {
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
                    case MediaType.APPLICATION_JSON:
                        try{

                            Event[] events = jsonMapper.readValue(messageBytes , Event[].class);
                            boolean success = true;
                            if (direct) {
                                success = rrstore.save(Arrays.stream(events).map(this::mapApiGenPath));
                            } else {
                                if (events.length > 0) {
                                    Event event = events[0];
                                    Optional<RecordOrReplay> recordOrReplay =
                                        rrstore.getRecordOrReplayFromCollection(event.customerId, event.app,
                                            event.getCollection());
                                    StoreUtils.processEvents(Arrays.stream(events).map(this::mapApiGenPath), rrstore,
                                        Optional.of(config.protoDescriptorCache), recordOrReplay);
                                }
                            }
                            return success && rrstore.commit() ?  Response.ok().build()
                                : Response.serverError().entity("Bulk save error").build();
                        } catch (Exception e){
                            LOGGER.error(new ObjectMessage(
                                Map.of(Constants.MESSAGE, "Error while parsing the events json")), e
                            );
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                    default :
                        return Response.serverError().entity("Content type not recognized :: " + ct).build();
                }
            }
        ).orElse(Response.serverError().entity("Content type not specified").build());

    }

    @POST
    @Path("/storeEvent")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response storeEvent(Event event,
        @DefaultValue("false") @QueryParam("direct") boolean direct) {
        try {
            event.validateEvent();
        } catch (InvalidEventException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Invalid Event")), e);
            return Response.status(Status.BAD_REQUEST).entity(Utils.buildErrorResponse(
                Status.BAD_REQUEST.toString(), Constants.ERROR, e.getMessage())).build();
        }
        mapApiGenPath(event);
        boolean success = true;
        if (direct) {
            success = rrstore.save(event) && rrstore.commit();
        } else {
            eventQueue.enqueue(event);
	        logStoreInfo("Enqueued Event", new CubeEventMetaInfo(event), true);
        }
        return success ? Response.ok().build()
            : Response.serverError().entity("Event save error").build();
    }

    private Event mapApiGenPath(Event event){
        apiGenPathMgr.getGenericPath(event).ifPresent(event::setApiPath);
        return event;
    }

    @POST
    @Path("/deleteEventByReqId")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEventByReqId(Event event) throws ParameterException {

	    if(event.customerId == null) throw new ParameterException("customerId is not present in the request");
	    if(event.getReqId() == null) throw new ParameterException("ReqId is not present in the request");

	    boolean deletionSuccess = rrstore.deleteReqResByReqId(event.getReqId() , event.customerId , Optional.ofNullable(event.eventType));
	    return Response.ok().type(MediaType.APPLICATION_JSON).
            entity(buildSuccessResponse(Constants.SUCCESS , new JSONObject(Map.of("deletion_success" , deletionSuccess)) )).build();
    }

    @POST
    @Path("/deleteEventByTraceId")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEventByTraceId(Event event) throws ParameterException {

        if(event.customerId == null) throw new ParameterException("customerId is not present in the request");
        if(event.getCollection() == null) throw new ParameterException("collection is not present in the request");
        if(event.getTraceId() == null) throw new ParameterException("TraceId is not present in the request");

        boolean deletionSuccess = rrstore.deleteReqResByTraceId(event.getTraceId() , event.customerId , event.getCollection(), Optional.ofNullable(event.eventType));
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
            mapApiGenPath(event);
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
        if (reqRespMatchResult == null || reqRespMatchResult.replayId == null ) {
            LOGGER.error(Map.of(Constants.MESSAGE, "ReqRespMatchResult is null"));
            return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.FAIL, Constants.INVALID_INPUT,
                    "Invalid input!")).build();
        }
        Optional<Replay> replay = rrstore.getReplay(reqRespMatchResult.replayId);
        return replay.map(r -> {
            boolean result = rrstore.saveResult(reqRespMatchResult, r.customerId);
            if (!result) {
                LOGGER.error(Map.of(Constants.MESSAGE, "Unable to store result in solr",
                    Constants.REPLAY_ID_FIELD, reqRespMatchResult.replayId));
                return Response.serverError().entity(
                    buildErrorResponse(Constants.ERROR, Constants.REPLAY_ID_FIELD,
                        "Unable to store result in solr")).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON)
                .entity("The Result is saved in Solr").build();
        }).orElse(Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(
            buildErrorResponse(Constants.FAIL, Constants.RESPONSE,
                "No replay found for replayId= " + reqRespMatchResult.replayId)).build());

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
      String userId = formParams.getFirst(Constants.USER_ID_FIELD);
      if (userId==null) {
          asyncResponse.resume(Response.status(Status.BAD_REQUEST)
              .entity("userId should be specified for a golden")
              .build());
          return;
      }
      Optional<Response> errResp = recordingType.flatMap(rt -> {
          if(rt == RecordingType.History || rt == RecordingType.UserGolden) {
              return Optional.empty();
          } else {
              return Utils.checkActiveCollection(rrstore, customerId, app,
                  instanceId, Optional.of(userId));
          }
      });
        if (errResp.isPresent()) {
            asyncResponse.resume(errResp.get());
            return;
        }

        String name = formParams.getFirst("name");
        String label = formParams.getFirst(Constants.GOLDEN_LABEL_FIELD);

        Optional<String> jarPath = Optional.ofNullable(formParams.getFirst(Constants.JAR_PATH_FIELD));

        if (name==null) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST)
                .entity("Name needs to be given for a golden")
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
        Optional<String> branch = Optional.ofNullable(formParams.getFirst(Constants.BRANCH_FIELD));
        Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst("gitCommitId"));
        List<String> tags = Optional.ofNullable(formParams.get(Constants.TAGS_FIELD)).orElse(new ArrayList<String>());
        Optional<String> comment = Optional.ofNullable(formParams.getFirst("comment"));
        Optional<String> dynamicInjectionConfigVersion = Optional.ofNullable(formParams.getFirst(Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD)) ;
        Optional<String> runId = Optional.ofNullable(formParams.getFirst(Constants.RUN_ID_FIELD));
        Optional<Boolean> ignoreStaticContent = io.md.utils.Utils.strToBool(formParams.getFirst(Constants.IGNORE_STATIC_CONTENT));


        RecordingBuilder recordingBuilder = new RecordingBuilder(customerId, app,
            instanceId, collection).withTemplateSetVersion(templateSetVersion).withName(name)
            .withLabel(label).withUserId(userId).withTags(tags);
        codeVersion.ifPresent(recordingBuilder::withCodeVersion);
        branch.ifPresent(recordingBuilder::withBranch);
        gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
        comment.ifPresent(recordingBuilder::withComment);
        recordingType.ifPresent(recordingBuilder::withRecordingType);
        dynamicInjectionConfigVersion.ifPresent(recordingBuilder::withDynamicInjectionConfigVersion);
        runId.ifPresentOrElse(recordingBuilder::withRunId, () -> recordingBuilder.withRunId(Instant.now().toString()));
        ignoreStaticContent.ifPresent(recordingBuilder::withIgnoreStatic);

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
                            newr.dynamicInjectionConfigVersion.ifPresent(replayBuilder::withDynamicInjectionConfigVersion);

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

    protected CompletableFuture<Void> afterRecording(MultivaluedMap<String, String> params,
        Recording recording) {
        Optional<String> tagOpt = params == null ? Optional.empty()
            : Optional.ofNullable(params.getFirst(Constants.RESET_TAG_FIELD));
        CompletableFuture<Void> tagCfgTask = tagOpt
            .map(tag -> this.tagConfig.setTag(recording, recording.instanceId, tag))
            .orElse(CompletableFuture.completedFuture(null));

        return CompletableFuture.allOf(tagCfgTask, Sanitize(recording));
    }

    private CompletableFuture<?>  Sanitize(Recording recording){
        String recordingId = recording.id;
        if (!recording.ignoreStatic) {
            LOGGER.debug("Not sanitizing as ignoreStatic flag is false "+recordingId);
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.info("Waiting for 15s to commit before Sanitize. current time" + Instant.now() + " "+recordingId);
        CompletableFuture<Set<String>> sanitizeFilterTask = ScheduledCompletable
            .schedule(Config.scheduler, () -> {
                LOGGER.info("Finished waiting for 15 sec to commit the recording for Sanitize "+recordingId);
                rrstore.commit();
                return SanitizationFilters
                    .getBadRequests(getValidEvents(recording), List.of(new IgnoreStaticContent()));
            }, 16, TimeUnit.SECONDS);

        CompletableFuture<Response> copyTask = sanitizeFilterTask.thenApply(badReq -> {
            if(badReq.isEmpty()) {
                LOGGER.info("Not doing sanitize copy recording as there are no bad requests to be filtered "+recordingId);
                return null;
            }
            LOGGER.info("Starting sanitize copy recording. Total bad reqIds "+badReq.size() +" "+recordingId);
            CompletableFuture<Response> rs = copyRecording(recording.id, Optional.empty(), Optional.empty(),
                Optional.empty(),
                recording.userId, recording.recordingType,
                Optional.of(e -> !badReq.contains(e.reqId)));
            try {
                var resp = rs.get();
                LOGGER.info("Finished Copy recording for sanitization "+recordingId);
                return resp;
            } catch (Exception e) {
                LOGGER.error("copyRecording failure "+recordingId, e);
            }
            return null;
        });
        return copyTask;
    }

    @POST
    @Path("resumeRecording/{recordingId}")
    public void resumeRecording(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui,
        @PathParam("recordingId") String recordingId, MultivaluedMap<String, String> formParams) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        CompletableFuture<Response> resp = resumeRecording(recording, formParams);
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
    @Path("copyRecording/{recordingId}/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void copyRecording(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui, @PathParam("recordingId") String recordingId,
        @PathParam("userId") String userId) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> templateVersion = Optional.ofNullable(queryParams.getFirst(Constants.VERSION_FIELD));
        Optional<String> name = Optional.ofNullable(queryParams.getFirst(Constants.GOLDEN_NAME_FIELD));
        Optional<String> label = Optional.ofNullable(queryParams.getFirst(Constants.GOLDEN_LABEL_FIELD));
        Optional<RecordingType> recordingType =
            Optional.ofNullable(queryParams.getFirst(Constants.RECORDING_TYPE_FIELD))
                .flatMap(r -> Utils.valueOf(RecordingType.class, r));
        if(recordingType.isEmpty()) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).
                entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,"No such Recording Type found")).build());
            return;
        }
        RecordingType type = recordingType.get();
        if(type== RecordingType.Golden && templateVersion.isEmpty()) {
            asyncResponse.resume(Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
                .entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,"version needs to be given for a golden")).build());
            return;
        }
        copyRecording(recordingId, name, label, templateVersion, userId, type, Optional.empty()).thenApply(v -> asyncResponse.resume(v));
    }

    private CompletableFuture<Response> copyRecording(String recordingId, Optional<String> name,
        Optional<String> label,
        Optional<String> templateVersion, String userId, RecordingType type,
        Optional<Predicate<Event>> eventFilter) {
        Instant timeStamp = Instant.now();
        String labelValue = label.orElse(""+timeStamp.getEpochSecond());
        Optional<Recording> recordingForId = rrstore.getRecording(recordingId);
        return recordingForId.map(recording -> {
            Optional<Recording> recordingWithSameName = name.flatMap(nameValue -> rrstore
                .getRecordingByName(recording.customerId, recording.app, nameValue, Optional.of(labelValue)));
            if(recordingWithSameName.isPresent()) {
                return CompletableFuture.completedFuture(Response.status(Response.Status.CONFLICT)
                    .entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,String.format("Collection %s already active for customer %s, app %s, for instance %s. Use different name or label",
                        recordingWithSameName.get().collection, recording.customerId, recording.app, recordingWithSameName.get().instanceId)))
                    .build());
            }
            /**
             * Check the collection is not empty

            EventQuery.Builder builder = new EventQuery.Builder(recording.customerId, recording.app, Collections.emptyList());
            builder.withCollection(recording.collection);
            builder.withLimit(1);
            builder.withOffset(1);
            Result<Event> result = rrstore.getEvents(builder.build());
            if(result.numFound < 1) {
                return CompletableFuture.completedFuture(Response.status(Status.BAD_REQUEST)
                    .entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,"Collection is empty"))
                    .build());
            }
            */
            Recording  updatedRecording = createRecordingObjectFrom(recording, templateVersion,
                name, Optional.of(userId), timeStamp, labelValue, type);
            if(rrstore.saveRecording(updatedRecording)) {
                return CompletableFuture.supplyAsync(() -> copyEvents(recording, updatedRecording, timeStamp, eventFilter)).thenApply(success ->{
                    if(success) return Response.ok().type(MediaType.APPLICATION_JSON).entity(updatedRecording).build();
                    rrstore.deleteAllRecordingData(updatedRecording);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,"Error while copying events"))
                        .build();
                });
            }
            return CompletableFuture.completedFuture(Response.status(Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,"Error while saving recording"))
                .build());

        }).orElse(CompletableFuture.completedFuture(Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,String.format("No Recording found for recordingId=%s", recordingId)))
            .build()));
    }


    public boolean copyEvents(Recording fromRecording, Recording toRecording, Instant timeStamp, Optional<Predicate<Event>> eventFilter) {
        EventQuery.Builder builder = new EventQuery.Builder(fromRecording.customerId, fromRecording.app, Collections.emptyList());
        builder.withCollection(fromRecording.collection);
        builder.withoutScoreOrder().withSeqIdAsc(true).withTimestampAsc(true);
        Result<Event> result = rrstore.getEvents(builder.build());
        Map<String, String> reqIdMap = new HashMap<>();
        final boolean[] eventCreationFailure = new boolean[1];
        Stream<Event> eventStream = result.getObjects().filter(eventFilter.orElse(event -> true)).map(event -> {
            try {
                String reqId = reqIdMap.get(event.getReqId());
                if(reqId == null) {
                    String oldReqId = event.getReqId();
                    reqId = io.md.utils.Utils.generateRequestId(
                        event.service, event.getTraceId());
                    reqIdMap.put(oldReqId, reqId);
                }
                return buildEvent(event, toRecording.collection,  toRecording.recordingType, reqId, event.getTraceId(),
                    Optional.of(timeStamp.toString()), event.timestamp);
            } catch (InvalidEventException e) {
                eventCreationFailure[0] = true;
                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "Error while creating Event",
                        Constants.RECORDING_ID, toRecording.id)), e);
            }
            return null;
        });/*.filter(Objects::nonNull)*/;
        if(eventCreationFailure[0]){
            LOGGER.error("Event Creation Failure ");
            return false;
        }

        // unique requests will be almost half. 0.6 to be on safe side
        long estimatedReqSize = (long)(result.numResults*0.6) +1;
        //check whether num of results and numFound are same
        var eventsStream =  SeqMgr.createSeqId(eventStream , estimatedReqSize);
        boolean batchSaveResult = rrstore.save(eventsStream) && rrstore.commit();
        return batchSaveResult;
    }

    /**
     * Takes a Recording Id and a list of optional status codes. Creates a new
     * Recording having only requests with responses, eliminating any responses
     * with the said status codes.
     *
     * @param asyncResponse
     * @param recordingId Recording Id to be sanitized
     * @param status List of status codes to remove from the Recording
     */
    @POST
    @Path("sanitizeGolden")
    @Produces(MediaType.APPLICATION_JSON)
    public void sanitizeGoldenRecording(@Suspended AsyncResponse asyncResponse,
        @QueryParam("recordingId") String recordingId,
        @QueryParam("ignoreStatus") List<String> status) {

        Optional<Recording> recording = rrstore.getRecording(recordingId);

        if (recording.isEmpty()) {
            asyncResponse
                .resume(Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).
                    entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                        "No such Recording found")).build());
            return;
        }

        Recording originalRec = recording.get();
        List<SanitizationFilter> list = new ArrayList<>();
        if(status!=null && !status.isEmpty()){
            list.add(new ReqRespMissing());
            list.add(new BadStatuses(new HashSet<>(status)));
        }
        if(originalRec.ignoreStatic){
            list.add(new IgnoreStaticContent());
        }

        Set<String> badReqIds = SanitizationFilters.getBadRequests(getValidEvents(originalRec) , list);
        if(badReqIds.isEmpty()){
            asyncResponse.resume(Response.ok().entity("No Bad requests found for sanitization").build());
        }else{
            copyRecording(recordingId, Optional.empty(), Optional.empty(), Optional.empty(),
                originalRec.userId, originalRec.recordingType, Optional.of((e)->!badReqIds.contains(e.reqId)))
                .thenApply(v -> asyncResponse.resume(v));
        }

    }

    private Stream<Event> getValidEvents(Recording originalRec){
        EventQuery.Builder reqBuilder = new EventQuery.Builder(originalRec.customerId,
            originalRec.app,
            Collections.emptyList());
        reqBuilder.withCollection(originalRec.collection);
        reqBuilder.withoutScoreOrder().withSeqIdAsc(true).withTimestampAsc(true);
        Result<Event> reqRespEvents = rrstore.getEvents(reqBuilder.build());
        return reqRespEvents.getObjects();
    }

    @POST
    @Path("moveEvents/{recordingId}")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public void moveEvents(@Suspended AsyncResponse asyncResponse, @Context UriInfo ui, @PathParam("recordingId") String recordingId , MultivaluedMap<String, String> formParams ) {

        Optional<String> insertAfterEventSeqId = Optional.ofNullable(formParams.getFirst(Constants.INSERT_AFTER_SEQ_ID));
        List<String> moveEventIds = Optional.ofNullable(formParams.get(Constants.REQ_IDS_FIELD)).orElse(new ArrayList<>());

        Optional<Recording>  recordingType =  rrstore.getRecording(recordingId);
        if(recordingType.isEmpty() || moveEventIds.isEmpty() || moveEventIds.size()>100){
            String errorMsg = recordingType.isEmpty() ? "No such Recording Type found. RecordingId : "+recordingId : moveEventIds.isEmpty() ? "Move Events Ids are not provided" : "Move Events more then 100 are not allowed";
            asyncResponse.resume(Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).
                entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE, errorMsg)).build());
            return;
        }

        Recording recording = recordingType.get();
        Optional<String> insertBeforeEventSeqId = getNextSeqIdEvent(recording , insertAfterEventSeqId).map(e->e.getSeqId());
        if(insertBeforeEventSeqId.isEmpty() && insertAfterEventSeqId.isEmpty()){
            String errorMsg = "Invalid insert before SeqId";
            asyncResponse.resume(Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).
                entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE, errorMsg)).build());
            return;
        }

        CompletableFuture.supplyAsync(()->{
            try{
                Map<String , String> newReqIdSeqIdMap =  insertEvents(recording , insertAfterEventSeqId , insertBeforeEventSeqId , moveEventIds);
                Response response = Response.ok().type(MediaType.APPLICATION_JSON).entity(newReqIdSeqIdMap).build();
                return asyncResponse.resume(response);
            }catch (Exception e){
                throw new CompletionException(e);
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Error "+throwable.getMessage());
            Response response = Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE,"Error while inserting events events" + throwable.getMessage())).build();
            return asyncResponse.resume(response);
        });
    }

    private Map<String , String> insertEvents(Recording recording , Optional<String> insertAfterEventSeqId , Optional<String> insertBeforeEventSeqId ,   List<String> moveEventIds) throws Exception{
        EventQuery.Builder builder = new EventQuery.Builder(recording.customerId , recording.app , Collections.EMPTY_LIST);
        builder.withCollection(recording.collection).withSeqIdAsc(true).withReqIds(moveEventIds);

        Result<Event> result = rrstore.getEvents(builder.build());
        /*
        if(result.numFound!=moveEventIds.size()*2){
            throw new Exception("Did not get all the events for reqIds. Found:"+result.numFound + " expected:"+moveEventIds.size());
        }*/

        List<Event> moveEvents = result.getObjects().collect(Collectors.toList());
        //check whether insertAfter & insertbefore seqId range is valid

        boolean invalidRange = moveEvents.stream().anyMatch(event -> {
            String seqId = event.getSeqId();
            return insertAfterEventSeqId.map(id->id.equals(seqId)).orElse(false) || insertBeforeEventSeqId.map(id->id.equals(seqId)).orElse(false);
        });
        if(invalidRange){
            throw new Exception("Invalid Range for MoveEvents");
        }

        var eventsStream =  SeqMgr.insertBetween(insertAfterEventSeqId , insertBeforeEventSeqId , moveEvents.stream() , moveEventIds.size());
        final Map<String , String> response = new HashMap<>();
        eventsStream = eventsStream.map(e->{

            if(e.eventType==EventType.HTTPRequest){
                response.put(e.getReqId() , e.getSeqId());
            }
            return e;
        });
        boolean batchSaveResult = rrstore.save(eventsStream) && rrstore.commit();
        if(!batchSaveResult){
            throw new Exception("Error saving the batch events");
        }
        return response;
    }


    private Optional<Event> getNextSeqIdEvent(Recording recording , Optional<String> insertAfterEventSeqId){

        EventQuery.Builder builder = new EventQuery.Builder(recording.customerId , recording.app , EventType.HTTPRequest);
        builder.withCollection(recording.collection).withSeqIdAsc(true).withLimit(1);
        insertAfterEventSeqId.ifPresent(builder::withStartSeqId);

        return rrstore.getSingleEvent(builder.build());
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
    @Path("delete/{recordingId}")
    public Response delete(@Context UriInfo ui, @PathParam("recordingId") String recordingId) {
        boolean hardDelete = Optional.ofNullable(ui.getQueryParameters().getFirst(io.md.constants.Constants.HARD_DELETE))
                .flatMap(Utils::strToBool).orElse(false);
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(rec -> {
            try {
                String json;
                if(hardDelete) {
                    boolean deleteRecordingMeta = rrstore.deleteAllRecordingData(rec);
                    if(deleteRecordingMeta) {
                       Stream<Replay> replays = rrstore.getReplay(Optional.of(rec.customerId), Optional.of(rec.app),
                            Optional.empty(), Collections.EMPTY_LIST, Optional.empty(),
                            List.of(rec.collection));
                       rrstore.deleteAllReplayData(replays.collect(Collectors.toList()));
                    } else {
                        LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR, "Recording Data is not deleted", "RecordingId", recordingId)));
                        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                            buildErrorResponse(Constants.ERROR, Constants.MESSAGE,
                                "Recording Data is  not deleted ")).build();
                    }
                    json = "Recording is completely deleted";
                } else {
                    Recording deletedR = ReqRespStore.softDeleteRecording(rec, rrstore);
                    LOGGER.info(new ObjectMessage(
                        Map.of(Constants.MESSAGE, "Soft deleting recording", "RecordingId",
                            recordingId)));
                    json = jsonMapper.writeValueAsString(deletedR);
                }
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
                    .withRecordingType(rec.recordingType).withRunId(rec.runId).withIgnoreStatic(rec.ignoreStatic);
                rec.parentRecordingId.ifPresent(recordingBuilder::withParentRecordingId);
                recordingBuilder.withName(name.orElse(rec.name));
                recordingBuilder.withLabel(label.orElse(rec.label));
                recordingBuilder.withUserId(userId.orElse(rec.userId));
                recordingBuilder.withCodeVersion(codeVersion.orElse(rec.codeVersion.orElse(null)));
                recordingBuilder.withBranch(branch.orElse(rec.branch.orElse(null)));
                rec.dynamicInjectionConfigVersion.ifPresent(recordingBuilder::withDynamicInjectionConfigVersion);

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
                eventQuery.getCustomerId(), eventQuery.getApp(), eventQuery.getCollections(), e));
            return Response.serverError().build();
        }
    }

    @POST
    @Path("afterResponse/{recordingId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response afterResponse(@Context UriInfo ui,
        @PathParam("recordingId") String recordingId,
        List<UserReqRespContainer> userReqRespContainers) {
        return saveReqRespEvents(ui, recordingId, userReqRespContainers, true);
    }

    @POST
    @Path("storeUserReqResp/{recordingId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeUserReqResp(@Context UriInfo ui,
        @PathParam("recordingId") String recordingId,
        List<UserReqRespContainer> userReqRespContainers) {
        return saveReqRespEvents(ui, recordingId, userReqRespContainers, false);
    }

    private Response saveReqRespEvents(UriInfo ui, String recordingId, List<UserReqRespContainer> userReqRespContainers, boolean extraction) {
        Optional<String> dynamicCfgVersion = Optional
            .ofNullable(ui.getQueryParameters().getFirst(Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD));

        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(rec -> {
            List<Map> responseList = new ArrayList<>();
            Map<String, String> traceIdMap = new HashMap<>();
            Map<String, String> extractionMap = new HashMap<>();
            final String generatedTraceId = io.md.utils.Utils.generateTraceId();
            List<Event> reqRespEvents = new ArrayList<>();
            for (UserReqRespContainer userReqRespContainer : userReqRespContainers) {
                // NOTE - Check if response needs to be modified in grpc/binary cases.
                // Ideally deserialisation and serialisation should take care of it.
                Event response = userReqRespContainer.response;
                Event request = userReqRespContainer.request;
                try {
                    request.validateEvent();
                    response.validateEvent();
                    String extractionMapString = "";
                    if(extraction) {
                        DynamicInjector dynamicInjector = this.factory
                            .getMgr(request.customerId, request.app, dynamicCfgVersion);
                        dynamicInjector.extract(request, response.payload);
                        Map<String, String> strMap = DynamicInjector
                            .convertToStrMap(dynamicInjector.getExtractionMap());
                        extractionMapString = jsonMapper.writeValueAsString(strMap);
                    }
                    String traceId = request.getTraceId();
                    if (rec.recordingType == RecordingType.UserGolden) {
                        traceId = traceIdMap.get(request.getTraceId());
                        if(traceId == null) {
                            String oldTraceId = request.getTraceId();
                            rrstore.deleteReqResByTraceId(oldTraceId, rec.collection);
                            rrstore.commit();

                            //traceId = io.md.utils.Utils.generateTraceId() ; // reuse same trace id for now
                            traceId = request.getTraceId();
                            traceIdMap.put(request.getTraceId(), traceId);
                        }
                    }

                    TemplateKey tkey = new TemplateKey(rec.templateVersion, request.customerId,
                        request.app, request.service, request.apiPath, Type.RequestMatch,
                        io.md.utils.Utils.extractMethod(request), UUID.randomUUID().toString());
                    Comparator comparator = rrstore
                        .getComparator(tkey, request.eventType);
                    final String reqId = io.md.utils.Utils.generateRequestId(
                        request.service, traceId);
                    Event requestEvent = buildEvent(request, rec.collection, rec.recordingType,
                        reqId, traceId, Optional.empty());
                    requestEvent.parseAndSetKey(comparator.getCompareTemplate());
                    Event responseEvent = buildEvent(response, rec.collection,
                        rec.recordingType, reqId, traceId, Optional.empty());

                    if(requestEvent.payload instanceof GRPCPayload) {
                        // Unwrap on body will be called internally after setting protoDescriptor
                        io.md.utils.Utils.setProtoDescriptorGrpcEvent(requestEvent, config.protoDescriptorCache);
                    }

                    if(responseEvent.payload instanceof GRPCPayload) {
                        // Unwrap on body will be called internally after setting protoDescriptor
                        io.md.utils.Utils.setProtoDescriptorGrpcEvent(responseEvent, config.protoDescriptorCache);
                    }

                    reqRespEvents.add(requestEvent);
                    reqRespEvents.add(responseEvent);


//                    String responseString = jsonMapper.writeValueAsString(Map.of("oldReqId", request.reqId,
//                        "oldTraceId", request.getTraceId(), "newReqId", reqId, "newTraceId", traceId, "extractionMap", extractionMapString));
//                    responseList.add(responseString);

                    Map<String, Object> responseEntry = Map.of("oldReqId", request.reqId,
                        "oldTraceId", request.getTraceId(), "newReqId", reqId, "newTraceId",
                        traceId, "extractionMap", extractionMapString, "requestEvent", jsonMapper.writeValueAsString(requestEvent),
                        "responseEvent", jsonMapper.writeValueAsString(responseEvent));


                    responseList.add(responseEntry);

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
                        if (!rrstore.saveResult(reqRespMatchResult, request.customerId)) {
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
                    return Response.status(Status.BAD_REQUEST).entity(buildErrorResponse(
                        Status.BAD_REQUEST.toString(),Constants.ERROR,  e.getMessage())).build();
                } catch (JsonProcessingException e) {
                    LOGGER.error(new ObjectMessage(
                        Map.of(Constants.MESSAGE, "Error while creating response",
                            Constants.RECORDING_ID, recordingId)), e);
                    return Response.serverError().entity("Error while creating response"
                        + e.getMessage()).build();
                } catch (DataObjProcessingException e) {
                    LOGGER.error(new ObjectMessage(
                        Map.of(Constants.MESSAGE, "Error while converting extraction Map",
                            Constants.RECORDING_ID, recordingId)), e);
                    return Response.serverError().entity("Error while converting extraction Map"
                        + e.getMessage()).build();
                }
            }
            if (!rrstore.save(reqRespEvents.stream()))  {
                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "Unable to store events in solr",
                        Constants.RECORDING_ID, recordingId)));
                return Response.serverError().entity(
                    buildErrorResponse(Constants.ERROR, Constants.RECORDING_ID,
                        "Unable to store event in solr")).build();
            }

            rrstore.commit();
            return Response.ok().type(MediaType.APPLICATION_JSON)
                .entity(buildSuccessResponse(
                    Constants.SUCCESS, new JSONObject(
                        Map.of(
                            "userReqRespContainers", userReqRespContainers,
                            Constants.MESSAGE, "The UserData is saved",
                            Constants.RECORDING_ID, recordingId,
                            Constants.RESPONSE, responseList)))).build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).
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
    @Path("deleteCustomerData/{customerId}")
    public Response deleteCustomerData(@Context UriInfo ui,
        @PathParam("customerId") String customerId) {
        if(rrstore.deleteAllData(customerId)) {
            return Response.ok().type(MediaType.APPLICATION_JSON).
                entity(buildSuccessResponse(Constants.SUCCESS ,
                    new JSONObject(Map.of(customerId, "All corresponding data is deleted"))))
                .build();
        }
        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
            buildErrorResponse(Constants.ERROR, Constants.BODY,
                "Unable to delete data for customer=" + customerId)).build();
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
        @FormDataParam("protoDescriptorFile") List<FormDataBodyPart>  bodyParts,
        @DefaultValue("false") @QueryParam("appendExisting") boolean appendExisting) {
//        @FormDataParam("protoDescriptorFile") FormDataContentDisposition fileDetail) {


        if(bodyParts==null || bodyParts.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(
                Map.of("Message",
                    "Uploaded file stream null. Ensure the variable name is \"protoDescriptorFile\" for the file")
            )).toString()).build();
        }

        Map<String, String> protoFileMap = new HashMap<>();
        boolean status = false;
        byte[] encodedFileBytes;
        try {
            String tmpDir = io.md.constants.Constants.TEMP_DIR;
            String descFileName = "tmp_" + UUID.randomUUID() +  ".desc";
            List<String> commandList = new ArrayList<>();
            commandList.add("protoc");
            commandList.add("--descriptor_set_out=" + descFileName);

            // If appending add existing protos for compiler
            if(appendExisting) {
                Optional<ProtoDescriptorDAO> existingProtoDescriptorDAOOptional = rrstore
                    .getLatestProtoDescriptorDAO(customerId, app);
                existingProtoDescriptorDAOOptional.ifPresent(UtilException.rethrowConsumer(existingProtoDescriptorDAO ->
                {
                    existingProtoDescriptorDAO.protoFileMap.forEach(UtilException.rethrowBiConsumer(
                        (uniqueFileName,fileContent) -> {
                            File targetFile = new File(tmpDir + "/" + uniqueFileName);
                            OutputStream outStream = new FileOutputStream(targetFile);
                            byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);
                            outStream.write(fileBytes);
                            //Add to new fileMap and list of commands
                            protoFileMap.put(uniqueFileName, fileContent);
                            commandList.add(uniqueFileName);
                        }));
                }));
            }

            // Add newly uploaded protos
            for (FormDataBodyPart bodyPart : bodyParts) {

                BodyPartEntity bodyPartEntity = (BodyPartEntity) bodyPart.getEntity();
                String fileName = bodyPart.getContentDisposition().getFileName();
                String uniqueFileName = "TAG_" + UUID.randomUUID() + "_" + fileName;
                byte[] fileBytes = bodyPartEntity.getInputStream().readAllBytes();
                protoFileMap.put(uniqueFileName, new String(fileBytes, StandardCharsets.UTF_8));
                commandList.add(uniqueFileName);
                File targetFile = new File(tmpDir + "/" + uniqueFileName);
                OutputStream outStream = new FileOutputStream(targetFile);
                outStream.write(fileBytes);
            }

            Files.deleteIfExists(Paths.get(tmpDir + "/" + descFileName));
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(new File(tmpDir));
            // Need to ensure protoc compiler is installed in the docker container env
            builder.command(commandList);
            Process process = builder.start();
            int exitCode = process.waitFor();
            if(exitCode != 0) {
                throw new Exception("Cannot initiate process to compile descriptor from protos");
            }

            InputStream initialStream = new FileInputStream(
                new File(tmpDir + "/" + descFileName));
            byte[] buffer = new byte[initialStream.available()];
            initialStream.read(buffer);

            encodedFileBytes = Base64.getEncoder().encode(buffer);
            ProtoDescriptorDAO protoDescriptorDAO = new ProtoDescriptorDAO(customerId, app, new String(encodedFileBytes, StandardCharsets.UTF_8), protoFileMap);
            status = rrstore.storeProtoDescriptorFile(protoDescriptorDAO);
        } catch (Exception e) {
            String message = "Cannot encode uploaded proto descriptor file";
            if(e instanceof FileNotFoundException) {
                message = "Cannot compile descriptor file from protos using protoc compiler."
                    + " Make sure the files are not duplicated in case of appending to existing protos";
            }
            LOGGER.error("Cannot encode uploaded proto descriptor file",e);
            return Response.status(Response.Status.BAD_REQUEST).entity((new JSONObject(
                Map.of("Message", message,
                    "Error", e.getMessage())).toString())).build();
        }
        return status ? Response.ok().type(MediaType.APPLICATION_JSON)
            .entity("The protofile is successfully saved in Solr").build() : Response.serverError().entity(Map.of("Error", "Cannot store proto descriptor file")).build();
    }

    @GET
    @Path("/getProtoDescriptor/{customerId}/{app}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProtoDescriptorFile(@PathParam("customerId") String customerId, @PathParam(
        "app") String app) {
        try {
            Optional<ProtoDescriptorDAO> latestProtoDescDao =
                rrstore.getLatestProtoDescriptorDAO(customerId, app);
            return latestProtoDescDao.map(protoDescriptorDAO -> Response.ok()
                .entity(protoDescriptorDAO.convertToJsonDescriptor()).build())
                .orElse(Response.serverError().entity("Proto Descriptor not present for the "
                    + "customer and app combo").build());
        } catch (Exception e) {
            return Response.serverError().entity("Exception occurred while retrieving proto "
                + "descriptor " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/preRequest/{recordingOrReplayId}/{runId}")
    public Response preRequest(@Context UriInfo uriInfo,@PathParam("recordingOrReplayId") String recordingOrReplayId,
        @PathParam("runId") String runId, DynamicInjectionEventDao dynamicInjectionEventDao) {
        if(dynamicInjectionEventDao == null || dynamicInjectionEventDao.getInjectionConfigVersion() == null ||
            dynamicInjectionEventDao.getContextMap() == null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("dynamicInjectionEventDao or InjectionConfigVersion or ContextMap is not present for given request").build();
        }
        final Event requestEvent = dynamicInjectionEventDao.getRequestEvent();
        try {
            requestEvent.validateEvent();
            DynamicInjector dynamicInjector = this.factory.getMgrFromStrMap(requestEvent.customerId,
                requestEvent.app, Optional.of(dynamicInjectionEventDao.getInjectionConfigVersion()),
                dynamicInjectionEventDao.getContextMap());
            dynamicInjector.inject(requestEvent);

            if(requestEvent.payload instanceof GRPCPayload) {
                io.md.utils.Utils.setProtoDescriptorGrpcEvent(requestEvent, config.protoDescriptorCache);
                // Note the state for stored event in solr will be UnwrappedDecoded if this is directly coming from devtool
                // then the state has to be set as UnwrappedDecoded by devtool.
            }
            if (requestEvent.payload instanceof HTTPPayload) {
                // wrap and encode the body to be used by UI
                ((HTTPPayload) requestEvent.payload).wrapBodyAndEncode();
            }

            Optional<Recording> optionalRecording = rrstore.getRecording(recordingOrReplayId);
            Optional<String> recordOrReplayRunId = optionalRecording.map(recording -> {
                recording.runId = runId;
                rrstore.saveRecording(recording);
                return recording.runId;
            }).or(() -> {
                Optional<Replay> optionalReplay = rrstore.getReplay(recordingOrReplayId);
                return optionalReplay.map(replay -> {
                    replay.runId = runId;
                    rrstore.saveReplay(replay);
                    return replay.replayId;
                });
            });
            if(recordOrReplayRunId.isEmpty()) {
                LOGGER.error("No record or replay found for the id=" + recordingOrReplayId);
                return Response.status(Status.BAD_REQUEST)
                    .entity(Utils.buildErrorResponse(Status.BAD_REQUEST.toString(),
                        Constants.MESSAGE, "No record or replay found for the id=" + recordingOrReplayId))
                    .build();

            }
            return  Response.ok(requestEvent , MediaType.APPLICATION_JSON).build();
        } catch (InvalidEventException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Invalid Event")), e);
            return Response.status(Status.BAD_REQUEST).entity(Utils.buildErrorResponse(
                Status.BAD_REQUEST.toString(),Constants.ERROR,  e.getMessage())).build();

        }
    }

    private Event buildEvent(Event event, String collection, RecordingType recordingType, String reqId, String traceId, Optional<String> runId)
        throws InvalidEventException {
        return buildEvent(event, collection, recordingType, reqId, traceId, runId, Instant.now());
    }


    private Event buildEvent(Event event, String collection, RecordingType recordingType, String reqId, String traceId, Optional<String> runId, Instant timeStamp)
        throws InvalidEventException {
        EventBuilder eventBuilder = new EventBuilder(event.customerId, event.app,
            event.service, event.instanceId, collection,
            new MDTraceInfo(traceId, event.spanId, event.parentSpanId),
            event.getRunType(), Optional.of(event.timestamp), reqId, event.apiPath,
            event.eventType, recordingType);
        eventBuilder.setPayload(event.payload);
        eventBuilder.withMetaData(event.metaData);
        eventBuilder.withRunId(runId.orElse(event.runId));
        eventBuilder.setPayloadKey(event.payloadKey);
        return eventBuilder.createEvent();
    }

    @GET
    @Path("getAppConfiguration/{customerId}/{app}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppConfiguration(@Context UriInfo uriInfo,
                                              @PathParam("customerId") String customerId, @PathParam("app") String app) {
        Optional<CustomerAppConfig> custAppConfig = rrstore.getAppConfiguration(customerId, app);
        Response resp = custAppConfig.map(d -> Response.ok(d , MediaType.APPLICATION_JSON).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                "CustomerAppConfig object not found")).build());
        return resp;
    }


    @POST
    @Path("getAppConfigurations/{customerId}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    public void getAppConfigurations(@Suspended AsyncResponse asyncResponse,
        @PathParam("customerId") String customerId, List<String> apps) {

        List<CompletableFuture<CustomerAppConfig>> futures =  apps.stream().map(app->CompletableFuture.supplyAsync(()->custAppConfigCache.getCustomerAppConfig(customerId, app).orElse(new CustomerAppConfig.Builder(customerId,app).build()))).collect(Collectors.toList());

        Utils.sequence(futures).thenApply(appConfigs->{

            Map<String , CustomerAppConfig> appCfgs = appConfigs.stream().collect(Collectors.toMap(cfg->cfg.app , cfg->cfg));

            return asyncResponse.resume(Response.ok(appCfgs , MediaType.APPLICATION_JSON).build());
        }).exceptionally(e-> asyncResponse.resume(Response.status(Status.INTERNAL_SERVER_ERROR)
            .entity(String.format("Server error: " + e.getMessage())).build()));
    }

    @POST
    @Path("setAppConfiguration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setAppConfiguration(CustomerAppConfig custAppCfg ) {

        //Get existing appCfg
        Optional<CustomerAppConfig> existing = rrstore.getAppConfiguration(custAppCfg.customerId , custAppCfg.app);
        //If the existing app cfg Id in solr is different then autoCalculated
        if(existing.isPresent() && !existing.get().id.equals(custAppCfg.id)){
            CustomerAppConfig.Builder builder = new Builder(custAppCfg.customerId , custAppCfg.app);
            custAppCfg.tracer.ifPresent(builder::withTracer);
            custAppCfg.apiGenericPaths.ifPresent(builder::withApiGenericPaths);
            builder.withId(existing.get().id);
            custAppCfg = builder.build();
        }
        if(rrstore.saveConfig(custAppCfg)){
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(
                buildSuccessResponse(Constants.SUCCESS, new JSONObject(Map.of(Constants.MESSAGE, "The customer app config tag has been changed",
                            Constants.CUSTOMER_ID_FIELD, custAppCfg.customerId, Constants.APP_FIELD, custAppCfg.app)))).build();
        }

        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(buildErrorResponse(Constants.ERROR, Constants.MESSAGE , "Error saving the customer app config")).build();
    }

    @POST
    @Path("mergeRecordings/{firstRecordingId}/{secondRecordingId}")
    public void mergeRecordings(@Suspended AsyncResponse asyncResponse, @Context UriInfo uriInfo,
        @PathParam("firstRecordingId") String firstRecordingId, @PathParam("secondRecordingId") String secondRecordingId) {
        Optional<Recording> firstRecordingOptional = rrstore.getRecording(firstRecordingId);
        Optional<Recording> secondRecordingOptional = rrstore.getRecording(secondRecordingId);
        Optional<RecordingType> newType =
            Optional.ofNullable(uriInfo.getQueryParameters().getFirst(Constants.RECORDING_TYPE_FIELD))
                .flatMap(r -> Utils.valueOf(RecordingType.class, r));

        if(firstRecordingOptional.isEmpty()) {
            asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                String.format("Recording object not found for recordingId=%s", firstRecordingId))).build());
            return;
        }
        if(secondRecordingOptional.isEmpty()) {
            asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
                String.format("Recording object not found for recordingId=%s", secondRecordingId))).build());
            return;
        }

        Recording firstRecording = firstRecordingOptional.get();
        Recording secondRecording = secondRecordingOptional.get();
        Recording thirdRecording ;
        Instant timeStamp = Instant.now();
        if(firstRecording.recordingType == RecordingType.UserGolden && newType.isEmpty()) {
            thirdRecording = firstRecording;
            CompletableFuture.runAsync(() -> copyEvents(secondRecording, thirdRecording, timeStamp, Optional.empty()))
                .thenApply(v -> asyncResponse.resume(Response.ok().type(MediaType.APPLICATION_JSON).entity(thirdRecording).build()));
        } else {
            thirdRecording = createRecordingObjectFrom(firstRecording, Optional.empty(),
                Optional.of(firstRecording.name + "-" + timeStamp.toString()), Optional.empty(),
                timeStamp, timeStamp.toString(), newType.orElse(RecordingType.UserGolden));
            if(rrstore.saveRecording(thirdRecording)) {
                CompletableFuture.runAsync(() -> {
                    copyEvents(firstRecording, thirdRecording, timeStamp, Optional.empty());
                    copyEvents(secondRecording, thirdRecording, timeStamp, Optional.empty());
                }).thenApply(v -> asyncResponse.resume(Response.ok().type(MediaType.APPLICATION_JSON).entity(thirdRecording).build()));
            }
        }
    }

    @Path("deduplicate/{recordingId}")
    @POST
    public Response deDuplicate(@Context UriInfo uriInfo, @PathParam("recordingId") String recordingId) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        boolean delete = Optional.ofNullable(uriInfo.getQueryParameters().getFirst("delete")).flatMap(Utils::strToBool).orElse(false);
        return recording.map(r -> {
                Instant timeStamp = Instant.now();
                Recording newRecording = createRecordingObjectFrom(r, Optional.empty(),
                    Optional.empty(), Optional.empty(), timeStamp, timeStamp.toString(), r.recordingType);
                rrstore.saveRecording(newRecording);
                EventQuery query = createEventQuery(r);
                Result<Event> result =  rrstore.getEvents(query);
                List<Event> pending = new ArrayList<>();
                AtomicReference<String> prevReqId = new AtomicReference<>("");
                result.getObjects().forEach(event -> {
                    if(!event.isRequestType()) {
                        TemplateKey key = new TemplateKey(newRecording.templateVersion, event.customerId, event.app, event.service,
                            event.apiPath, Type.ResponseCompare);
                        try {
                            Comparator comparator = rrstore.getComparator(key, event.eventType);
//                            Comparator comparator = rrstore.getDefaultComparator(event.eventType, Type.ResponseCompare);
                            /**
                             * Setting payload key of response event to enable deduplication of events.
                             *  based on the default template for response comparison
                             */
                            event.parseAndSetKey(comparator.getCompareTemplate());
                        } catch (TemplateNotFoundException e) {
                            LOGGER.error(new ObjectMessage(Map.of(
                                Constants.MESSAGE, "Comparator not found",
                                Constants.REQ_ID_FIELD, event.reqId
                            )), e);
                        }
                    }
                    if(!event.reqId.equals(prevReqId.get())) {
                        processPendingEvents(pending, newRecording.collection, newRecording.recordingType, prevReqId.get());
                    }
                    pending.add(event);
                    prevReqId.set(event.reqId);
                });
                processPendingEvents(pending, newRecording.collection, newRecording.recordingType, prevReqId.get());
                if(delete) {
                    try {
                        ReqRespStore.softDeleteRecording(r, rrstore);
                        LOGGER.info(new ObjectMessage(
                            Map.of(Constants.MESSAGE, "Soft deleting recording", "RecordingId",
                                r.id)));
                    } catch (RecordingSaveFailureException e) {
                        LOGGER.error(new ObjectMessage(
                            Map.of(Constants.MESSAGE, "Error while Soft deleting recording", "RecordingId",
                                r.id)));
                    }
                }
                return Response.ok().type(MediaType.APPLICATION_JSON).entity(newRecording)
                    .build();
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity(Utils.buildErrorResponse(Status.NOT_FOUND.toString(), Constants.NOT_PRESENT,
            String.format("Recording object not found for recordingId=%s", recordingId))).build());
    }

    private void processPendingEvents(final List<Event> pending, String collection, RecordingType recordingType, String reqId) {
        int payloadHash = 0;
        List<Integer> payloadKeys = pending.stream().map(e -> e.payloadKey).collect(Collectors.toList());
        for(Integer payloadKey: payloadKeys) {
            payloadHash ^= payloadKey;
        }
        int finalPayloadHash = payloadHash;
        var newEventsStream =   pending.stream().map(event -> {
            String newReqId = "Update-" + finalPayloadHash;
            try {
                Event newEvent = buildEvent(event, collection, recordingType, newReqId, newReqId, Optional.empty(), event.timestamp);
                return newEvent;
            } catch (InvalidEventException e) {
                LOGGER.error(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Invalid Event",
                    Constants.REQ_ID_FIELD, reqId
                )), e);
            }
            return null;
        }).filter(Objects::nonNull);

        rrstore.save(newEventsStream);

        pending.clear();
    }

    private EventQuery createEventQuery(Recording recording) {
        EventQuery.Builder builder = new EventQuery.Builder(recording.customerId, recording.app, Collections.emptyList());
        builder.withCollection(recording.collection);
        LinkedHashMap sortingOrder = new LinkedHashMap();
        sortingOrder.put(Constants.REQ_ID_FIELD, true);
        builder.withSortingOrder(sortingOrder);
        return builder.build();
    }

    private Recording createRecordingObjectFrom(Recording recording, Optional<String> templateVersion,
        Optional<String> name, Optional<String> userId, Instant timeStamp, String labelValue, RecordingType type) {
        String collection = UUID.randomUUID().toString();
        RecordingBuilder recordingBuilder = new RecordingBuilder(
            recording.customerId, recording.app, recording.instanceId, collection)
            .withStatus(RecordingStatus.Completed).withTemplateSetVersion(templateVersion.orElse(recording.templateVersion))
            .withName(name.orElse(recording.name))
            .withUserId(userId.orElse(recording.userId)).withTags(recording.tags).withUpdateTimestamp(timeStamp)
            .withRootRecordingId(recording.rootRecordingId).withLabel(labelValue)
            .withRecordingType(type).withRunId(timeStamp.toString()).withIgnoreStatic(recording.ignoreStatic);
        recording.parentRecordingId.ifPresent(recordingBuilder::withParentRecordingId);
        recording.codeVersion.ifPresent(recordingBuilder::withCodeVersion);
        recording.branch.ifPresent(recordingBuilder::withBranch);
        recording.gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
        recording.collectionUpdOpSetId.ifPresent(recordingBuilder::withCollectionUpdateOpSetId);
        recording.templateUpdOpSetId.ifPresent(recordingBuilder::withTemplateUpdateOpSetId);
        recording.comment.ifPresent(recordingBuilder::withComment);
        recording.dynamicInjectionConfigVersion.ifPresent(recordingBuilder::withDynamicInjectionConfigVersion);
        try {
            recording.generatedClassJarPath
                .ifPresent(UtilException.rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Error while generatedClassJarPath",
                Constants.CUSTOMER_ID_FIELD, recording.customerId,
                Constants.APP_FIELD, recording.app,
                Constants.INSTANCE_ID_FIELD, recording.instanceId
            )), e);
        }
        return recordingBuilder.build();
    }

    @POST
    @Path("/populateCache")
    public Response populateCache(@Context UriInfo uriInfo, RecordOrReplay recordOrReplay) {

        CollectionKey key = recordOrReplay.getCollectionKey();
        LOGGER.debug(new ObjectMessage(
            Map.of(Constants.MESSAGE, "populateCache for collectionKey :"+key + " recordorReplay :"+recordOrReplay)));

        rrstore.populateCache(key , recordOrReplay);

        return Response.ok().build();
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
		this.factory = new DynamicInjectorFactory(rrstore, jsonMapper);
		this.apiGenPathMgr = ApiGenPathMgr.getInstance(rrstore);
		this.custAppConfigCache = CustAppConfigCache.getInstance(rrstore);
	}


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	DynamicInjectorFactory factory;
	Config config;
	TagConfig tagConfig;
    DisruptorEventQueue eventQueue;
    ApiGenPathMgr apiGenPathMgr;
    CustAppConfigCache custAppConfigCache;

	private Optional<String> getCurrentCollectionIfEmpty(Optional<String> collection,
			Optional<String> customerId, Optional<String> app, Optional<String> instanceId) {
		return collection.or(() -> rrstore.getCurrentCollection(customerId, app, instanceId));
	}

}
