/**
 * Copyright Cube I O
 */
package com.cube.ws;

import static com.cube.core.Utils.buildErrorResponse;
import static com.cube.core.Utils.buildSuccessResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.json.JSONObject;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.JsonPayload;
import io.md.dao.MDTraceInfo;
import io.md.dao.Payload;

import com.cube.agent.FnReqResponse;
import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.cache.TemplateKey.Type;
import com.cube.core.Utils;
import com.cube.dao.CubeEventMetaInfo;
import com.cube.dao.CubeMetaInfo;
import com.cube.dao.DefaultEvent;
import com.cube.dao.EventQuery;
import com.cube.dao.Recording;
import com.cube.dao.Recording.RecordingSaveFailureException;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.RecordingBuilder;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.RecordOrReplay;
import com.cube.dao.Result;
import com.cube.dao.WrapperEvent;
import com.cube.utils.Constants;
import com.cube.ws.WSUtils.BadValueException;

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
        Map solrHealth = WSUtils.solrHealthCheck(config.solr);
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
        try {
            storeSingleReqResp(rr, path, queryParams);
            return Response.ok().build();
        } catch (CubeStoreException e) {
            logStoreError(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
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

    static class CubeStoreException extends Exception {
        CubeEventMetaInfo cubeEventMetaInfo;

        CubeStoreException(Exception e, String message, CubeEventMetaInfo cubeEventMetaInfo) {
	        super(message , e);
	        this.cubeEventMetaInfo = cubeEventMetaInfo;
        }

        CubeStoreException(Exception e, String message, Event event){
	        super(message, e);
	        this.cubeEventMetaInfo = new CubeEventMetaInfo(event);
        }

    }

    private void storeSingleReqResp(ReqRespStore.ReqResp rr, String path,
        MultivaluedMap<String, String> queryParams) throws CubeStoreException {

	    path = CompareTemplate.normaliseAPIPath(path);

        MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
        rr.hdrs.forEach(kv -> {
            hdrs.add(kv.getKey(), kv.getValue());
        });

        MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>();
        rr.meta.forEach(kv -> {
            meta.add(kv.getKey(), kv.getValue());
        });

        CubeEventMetaInfo cubeEventMetaInfo = new CubeEventMetaInfo();

        Optional<String> customerId = Optional
            .ofNullable(meta.getFirst(Constants.CUSTOMER_ID_FIELD));
        cubeEventMetaInfo.setCustomer(customerId);
        Optional<String> app = Optional.ofNullable(meta.getFirst(Constants.APP_FIELD));
        cubeEventMetaInfo.setApp(app);
        Optional<String> instanceId = Optional
            .ofNullable(meta.getFirst(Constants.INSTANCE_ID_FIELD));
        cubeEventMetaInfo.setInstance(instanceId);
        Optional<String> service = Optional.ofNullable(meta.getFirst(Constants.SERVICE_FIELD));
        cubeEventMetaInfo.setService(service);
        Optional<String> rid = Optional.ofNullable(meta.getFirst("c-request-id"));
        cubeEventMetaInfo.setReqId(rid);
        Optional<String> type = Optional.ofNullable(meta.getFirst("type"));
        cubeEventMetaInfo.setEventType(type);
        // TODO: the following can pass replayid to cubestore but currently requests don't match in the mock
        // since we don't have the ability to ignore certain fields (in header and body)
        //if (inpcollection.isEmpty()) {
        //	inpcollection = Optional.ofNullable(hdrs.getFirst(Constants.CUBE_REPLAYID_HDRNAME));
        //}
        Instant timestamp = Optional.ofNullable(meta.getFirst("timestamp"))
            .flatMap(Utils::strToTimeStamp)
            .orElseGet(() -> {
                LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Timestamp missing in event, using current time")));
                return Instant.now();
            });

        Optional<Event.RunType> runType = Optional
            .ofNullable(meta.getFirst(Constants.RUN_TYPE_FIELD))
            .flatMap(rrt -> Utils.valueOf(Event.RunType.class, rrt));
        cubeEventMetaInfo.setRunType(runType.map(Enum::name));

        //LOGGER.info(String.format("Got store for type %s, for inpcollection %s, reqId %s, path %s"
        // , type.orElse("<empty>"), inpcollection.orElse("<empty>"), rid.orElse("<empty>"), path));

        Optional<RecordOrReplay> recordOrReplay = rrstore
            .getCurrentRecordOrReplay(customerId, app, instanceId, true);

        if (recordOrReplay.isEmpty()) {
            throw new CubeStoreException(null, "Unable to find running record/replay", cubeEventMetaInfo);
        }

        Optional<String> collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);
        cubeEventMetaInfo.setCollection(collection);
        if (collection.isEmpty()) {
            // Dropping if collection is empty, i.e. recording is not started
            throw new CubeStoreException(null, "Collection is empty", cubeEventMetaInfo);
        } else {
            logStoreInfo("Attempting store", cubeEventMetaInfo, true);
        }

        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();

        String typeStr = type.orElseThrow(() ->
            new CubeStoreException(null, "Type not specified", cubeEventMetaInfo));

        if (typeStr.equals(Constants.REQUEST)) {
            String method = Optional.ofNullable(meta.getFirst("method")).orElseThrow(() ->
                new CubeStoreException(null, "Method field missing", cubeEventMetaInfo));

            // create Event object from Request
            // fetch the template version, create template key and get a request comparator
            String templateVersion = recordOrReplay.get().getTemplateVersion();
            if (!(customerId.isPresent() && app.isPresent() && service.isPresent())) {
                throw new CubeStoreException(null, "customer id, app or service not present"
                    , cubeEventMetaInfo);
            }

            TemplateKey tkey = new TemplateKey(templateVersion, customerId.get(),
                app.get(), service.get(), path, Type.RequestMatch);

            Comparator requestComparator = null;
            try {
                requestComparator = config.comparatorCache
                    .getComparator(tkey, Event.EventType.HTTPRequest);
            } catch (ComparatorCache.TemplateNotFoundException e) {
                throw new CubeStoreException(e, "Request Comparator Not Found"
                    , cubeEventMetaInfo);
            }

            Event requestEvent = null;
            try {
                requestEvent = Utils
                    .createHTTPRequestEvent(path, rid, queryParams, formParams, meta,
                        hdrs, method, rr.body, collection, timestamp, runType, customerId,
                        app, config, requestComparator);
            } catch (JsonProcessingException | EventBuilder.InvalidEventException e) {
                throw new CubeStoreException(e, "Invalid Event"
                    , cubeEventMetaInfo);
            }

            if (!rrstore.save(requestEvent)) {
                throw new CubeStoreException(null, "Unable to store request event in solr"
                    , cubeEventMetaInfo);
            }
        } else if (typeStr.equals(Constants.RESPONSE)) {
            int status;
            try {
                status =
                    Optional.ofNullable(meta.getFirst(Constants.STATUS))
                        .map(Integer::valueOf).orElseThrow(() ->
                        new CubeStoreException(null, "Status missing", cubeEventMetaInfo));
                // to catch number format exception
            } catch (Exception e) {
                throw new CubeStoreException(e, "Expecting Integer status"
                    , cubeEventMetaInfo);
            }
            // pick apiPath from meta fields
            String reqApiPath = Optional
                .ofNullable(meta.getFirst(Constants.API_PATH_FIELD)).orElse("");

            Event responseEvent;
            try {
            	if (!reqApiPath.isEmpty()) {
		            URIBuilder uriBuilder = new URIBuilder(reqApiPath);
		            reqApiPath = uriBuilder.getPath();
	            }
	            responseEvent = Utils
                    .createHTTPResponseEvent(reqApiPath, rid, status, meta, hdrs, rr.body,
                        collection, timestamp, runType, customerId, app, config);

            } catch (JsonProcessingException | InvalidEventException | URISyntaxException e) {
                throw new CubeStoreException(e, "Invalid Event"
                    , cubeEventMetaInfo);
            }
            if (!rrstore.save(responseEvent)) {
                throw new CubeStoreException(null, "Unable to store response event in solr"
                    , cubeEventMetaInfo);
            }

        } else {
            throw new CubeStoreException(null, "Unknown type"
                , cubeEventMetaInfo);
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
        try {
            storeSingleReqResp(rr, path, queryParamsMap);
        } catch (CubeStoreException e) {
            logStoreError(e);
        }
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
        Utils.preProcess(fnReqResponse);
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
            processEvent(event);
            logStoreInfo("Completed Store", new CubeEventMetaInfo(event), true);
            return Response.ok().build();
        } catch (CubeStoreException e) {
	        logStoreError(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
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
        } catch (IOException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Error parsing Event JSON")),e);
            return 0;
        }
        try {
            processEvent(event);
            logStoreInfo("Completed Store", new CubeEventMetaInfo(event) , true);
            return 1;
        } catch (CubeStoreException e) {
            logStoreError(e);
            return 0;
        }
	}
	// process and store Event
    // return error string (Optional<String>)
    private void processEvent(Event event) throws CubeStoreException {
        if (event == null) {
            throw new CubeStoreException(null, "Event is null", new CubeEventMetaInfo());
        }

        Optional<String> collection;

        event.setCollection("NA"); // so that validate doesn't fail

        if (!event.validate()) {
            throw new CubeStoreException(null, "some required field missing,"
                + " or both binary and string payloads set", event);
        }

        Optional<RecordOrReplay> recordOrReplay =
            rrstore.getCurrentRecordOrReplay(Optional.of(event.customerId),
                Optional.of(event.app), Optional.of(event.instanceId), true);

        if (recordOrReplay.isEmpty()) {
            throw new CubeStoreException(null, "No current record/replay!", event);
        }

        collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);

        // check collection, validate, fetch template for request, set key and store. If error at any point stop
        if (collection.isEmpty()) {
            throw new CubeStoreException(null, "Collection is missing", event);
        }
        event.setCollection(collection.get());
        if (event.isRequestType()) {
            // if request type, need to extract keys from request and index it, so that it can be
            // used while mocking
            try {
                Optional<URLClassLoader> classLoader = Optional.empty();
                if (event.eventType.equals(EventType.ThriftRequest)) {
                    classLoader = recordOrReplay.flatMap(RecordOrReplay::getClassLoader);
                }

                event.parseAndSetKey(Utils.getRequestMatchTemplate(config, event,
                        recordOrReplay.get().getTemplateVersion()), classLoader);
            } catch (ComparatorCache.TemplateNotFoundException e) {
                throw new CubeStoreException(e, "Compare Template Not Found", event);
            }
        }

        boolean saveResult = rrstore.save(event);
        if (!saveResult) {
            throw new CubeStoreException(null, "Unable to store event in solr", event);
        }

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
            if (eventData.eventType.equals(EventType.JavaRequest)) {
                //For Java Functions, request and response are stored in the same event.
                return Response.ok().type(MediaType.APPLICATION_JSON)
                    .entity(buildSuccessResponse(Constants.SUCCESS, new JSONObject())).build();
            }
            if (defaultReqEvent.isPresent() && storeDefaultRespEvent(defaultReqEvent.get(),
                    defaultEvent.getEvent().payload)) {
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

    private boolean storeDefaultRespEvent(
        Event defaultReqEvent, Payload payload) throws InvalidEventException {
        //Store default response
        EventBuilder eventBuilder = new EventBuilder(defaultReqEvent.customerId,
            defaultReqEvent.app,
            defaultReqEvent.service, "NA", "NA",
            new MDTraceInfo("NA", null, null), RunType.Manual
            , Optional.of(Instant.now()), defaultReqEvent.reqId, defaultReqEvent.apiPath,
            Event.EventType.getResponseType(defaultReqEvent.eventType));
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
                reqEvent.reqId, reqEvent.apiPath, reqEvent.eventType);

            //TODO:Add support for Binary payload.
            eventBuilder.setPayload(reqEvent.payload);
            Event defaultReqEvent = eventBuilder.createEvent();
            try {
                defaultReqEvent.parseAndSetKey(Utils.
                    getRequestMatchTemplate(config, defaultReqEvent
                        , Constants.DEFAULT_TEMPLATE_VER));
            } catch (ComparatorCache.TemplateNotFoundException e) {
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
	@Path("start/{customerId}/{app}/{instanceId}/{collection}/{templateSetVersion}")
	@Consumes("application/x-www-form-urlencoded")
    public Response start(@Context UriInfo ui,
                          MultivaluedMap<String, String> formParams,
                          @PathParam("app") String app,
                          @PathParam("customerId") String customerId,
                          @PathParam("instanceId") String instanceId,
                          @PathParam("collection") String collection,
                          @PathParam("templateSetVersion") String templateSetVersion) {
	    // check if recording or replay is ongoing for (customer, app, instanceId)
        Optional<Response> errResp = WSUtils.checkActiveCollection(rrstore, Optional.ofNullable(customerId), Optional.ofNullable(app),
            Optional.ofNullable(instanceId), Optional.empty());
        if (errResp.isPresent()) {
            return errResp.get();
        }

        // check if recording collection name is unique for (customerId, app)
        Optional<Recording> recording = rrstore
            .getRecordingByCollectionAndTemplateVer(customerId, app, collection, templateSetVersion);
        errResp = recording.filter(r -> r.status == RecordingStatus.Running)
            .map(recordingv -> Response.status(Response.Status.CONFLICT)
                .entity(String.format("Collection %s already active for customer %s, app %s, for instance %s. Use different name",
                    collection, customerId, app, recordingv.instanceId))
                .build());
        if (errResp.isPresent()) {
            return errResp.get();
        }

        // NOTE that if the recording is not active, it will be activated again. This allows the same collection recording to be
        // stopped and started multiple times

        LOGGER.info(String.format("Starting recording for customer %s, app %s, instance %s, collection %s",
            customerId, app, instanceId, collection));

        String name = formParams.getFirst("name");
        String userId = formParams.getFirst("userId");
        Optional<String> jarPath = Optional.ofNullable(formParams.getFirst("jarPath"));

        if (name==null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("Name needs to be given for a golden")
                .build();
        }

        if (userId==null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("userId should be specified for a golden")
                .build();
        }

        // Ensure name is unique for a customer and app
        Optional<Recording> recWithSameName = rrstore.getRecordingByName(customerId, app, name);
        if (recWithSameName.isPresent()) {
            return Response.status(Response.Status.CONFLICT)
            .entity("Golden already present for name - " + name + ". Specify unique name")
            .build();
        }

        Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst("codeVersion"));
        Optional<String> branch = Optional.ofNullable(formParams.getFirst("branch"));
        Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst("gitCommitId"));
        List<String> tags = Optional.ofNullable(formParams.get("tags")).orElse(new ArrayList<String>());
        Optional<String> comment = Optional.ofNullable(formParams.getFirst("comment"));

        RecordingBuilder recordingBuilder = new RecordingBuilder(new CubeMetaInfo(customerId, app
            , instanceId), collection).withTemplateSetVersion(templateSetVersion).withName(name)
            .withUserId(userId).withTags(tags);
        codeVersion.ifPresent(recordingBuilder::withCodeVersion);
        branch.ifPresent(recordingBuilder::withBranch);
        gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
        comment.ifPresent(recordingBuilder::withComment);
        try {
            jarPath.ifPresent(UtilException.rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));
        } catch (Exception e) {
            return Response.serverError().entity((new JSONObject(Map.of(Constants.ERROR
                , e.getMessage()))).toString()).build();
        }

        Optional<Response> resp = Recording
            .startRecording(recordingBuilder.build() ,rrstore)
            .map(newr -> {
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
            });

        return resp.orElse(Response.serverError().build());
    }


    @GET
    @Path("searchRecording")
    @Consumes("application/x-www-form-urlencoded")
    public Response searchRecording(MultivaluedMap<String, String> formParams) {
        Optional<String> customerId = Optional.ofNullable(formParams.getFirst(Constants.CUSTOMER_ID_FIELD));
        Optional<String> app = Optional.ofNullable(formParams.getFirst(Constants.APP_FIELD));
        Optional<String> instanceId = Optional.ofNullable(formParams.getFirst(Constants.INSTANCE_ID_FIELD));
        Optional<RecordingStatus> status = Optional.ofNullable(formParams.getFirst(Constants.STATUS))
            .flatMap(s -> Utils.valueOf(RecordingStatus.class, s));
        Optional<String> collection = Optional.ofNullable(formParams.getFirst(Constants.COLLECTION_FIELD));
        Optional<String> templateVersion = Optional.ofNullable(formParams.getFirst(Constants.TEMPLATE_VERSION_FIELD));
        Optional<String> parentRecordingId = Optional.ofNullable(formParams.getFirst(Constants.PARENT_RECORDING_FIELD));
        Optional<String> rootRecordingId = Optional.ofNullable(formParams.getFirst(Constants.ROOT_RECORDING_FIELD));
        Optional<String> name = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_NAME_FIELD));
        Optional<String> userId = Optional.ofNullable(formParams.getFirst(Constants.USER_ID_FIELD));
        Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst(Constants.CODE_VERSION_FIELD));
        Optional<String> branch = Optional.ofNullable(formParams.getFirst(Constants.BRANCH_FIELD));
        Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst(Constants.GIT_COMMIT_ID_FIELD));
        List<String> tags = Optional.ofNullable(formParams.get(Constants.TAGS_FIELD)).orElse(new ArrayList<String>());
        Optional<String> collectionUpdOpSetId = Optional.ofNullable(formParams.getFirst(Constants.COLLECTION_UPD_OP_SET_ID_FIELD));
        Optional<String> templateUpdOpSetId = Optional.ofNullable(formParams.getFirst(Constants.TEMPLATE_UPD_OP_SET_ID_FIELD));
        String archivedString = formParams.getFirst(Constants.ARCHIVED_FIELD);
        Optional<Boolean> archived = Optional.empty();

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

            List<Recording> recordings = rrstore.getRecording(customerId, app, instanceId, status, collection, templateVersion, name, parentRecordingId, rootRecordingId,
                codeVersion, branch, tags, archived, gitCommitId, collectionUpdOpSetId, templateUpdOpSetId, userId).collect(Collectors.toList());

            String json;
            json = jsonMapper.writeValueAsString(recordings);
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
	@Path("status/{customerId}/{app}/{collection}/{templateSetVersion}")
    public Response status(@Context UriInfo ui,
                           @PathParam("collection") String collection,
                           @PathParam("customerId") String customerId,
                           @PathParam("app") String app,
                           @PathParam("templateSetVersion") String templateSetVersion) {
	    Optional<Recording> recording = rrstore.getRecordingByCollectionAndTemplateVer(customerId,
            app, collection, templateSetVersion);

        Response resp = recording.map(r -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(r);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s.", customerId, app, collection), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity(String.format("Status not found for for customer %s, app %s, collection %s.", customerId, app, collection)).build());
        return resp;
    }

	@GET
	@Path("recordings")
    public Response recordings(@Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> instanceId = Optional.ofNullable(queryParams.getFirst(Constants.INSTANCE_ID_FIELD));
        Optional<String> customerId = Optional.ofNullable(queryParams.getFirst(Constants.CUSTOMER_ID_FIELD));
        Optional<String> app = Optional.ofNullable(queryParams.getFirst(Constants.APP_FIELD));
        Optional<RecordingStatus> status = Optional.ofNullable(queryParams.getFirst(Constants.STATUS))
            .flatMap(s -> Utils.valueOf(RecordingStatus.class, s));

        List<Recording> recordings = rrstore.getRecording(customerId, app, instanceId, status).collect(Collectors.toList());

        String json;
        try {
            json = jsonMapper.writeValueAsString(recordings);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, instance %s.",
                customerId.orElse(""), app.orElse(""), instanceId.orElse("")), e);
            return Response.serverError().build();
        }
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
    public Response stop(@Context UriInfo ui,
                         @PathParam("recordingid") String recordingid) {
        Optional<Recording> recording = rrstore.getRecording(recordingid);
        LOGGER.info(String.format("Stoppping recording for recordingid %s", recordingid));
        Response resp = recording.map(r -> {
            Recording stoppedr = Recording.stopRecording(r, rrstore);
            String json;
            try {
                json = jsonMapper.writeValueAsString(stoppedr);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException ex) {
                LOGGER.error(String.format("Error in converting Recording object to Json for recordingid %s", recordingid), ex);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).
            entity(String.format("Status not found for recordingid %s", recordingid)).build());
        return resp;
    }

    @POST
    @Path("softDelete/{recordingId}")
    public Response softDelete(@PathParam("recordingId") String recordingId) {
        Optional<Recording> recording = rrstore.getRecording(recordingId);
        Response resp = recording.map(rec -> {
            try {
                Recording deletedR = rec.softDeleteRecording(rrstore);
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
                Optional<String> userId = Optional.ofNullable(formParams.getFirst(Constants.USER_ID_FIELD));
                Optional<String> codeVersion = Optional.ofNullable(formParams.getFirst(Constants.CODE_VERSION_FIELD));
                Optional<String> branch = Optional.ofNullable(formParams.getFirst(Constants.BRANCH_FIELD));
                Optional<String> gitCommitId = Optional.ofNullable(formParams.getFirst(Constants.GIT_COMMIT_ID_FIELD));
                List<String> tags = Optional.ofNullable(formParams.get(Constants.TAGS_FIELD)).orElse(new ArrayList<String>());
                Optional<String> comment = Optional.ofNullable(formParams.getFirst(Constants.GOLDEN_COMMENT_FIELD));


                if(name.isPresent()) {
                    String nameVal = name.get();
                    Optional<Recording> recWithSameName = rrstore.getRecordingByName(rec.customerId, rec.app, nameVal);
                    if (recWithSameName.isPresent()) {
                        String errorMessage = "Golden with same name present " + nameVal;
                        LOGGER.error(new ObjectMessage(Map.of(Constants.ERROR,
                            errorMessage, "RecordingId", recordingId)));
                        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                            buildErrorResponse(Constants.ERROR, Constants.RECORDING_SAME_NAME_EXCEPTION,
                                errorMessage)).build();
                    }
                }


                RecordingBuilder recordingBuilder = new RecordingBuilder(new CubeMetaInfo(rec.customerId, rec.app
                    , rec.instanceId), rec.collection)
                    .withStatus(rec.status)
                    .withTemplateSetVersion(rec.templateVersion)
                    .withRootRecordingId(rec.rootRecordingId)
                    .withArchived(rec.archived);
                rec.parentRecordingId.ifPresent(recordingBuilder::withParentRecordingId);
                recordingBuilder.withName(name.orElse(rec.name));
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
            TemplateKey key = new TemplateKey(Constants.DEFAULT_TEMPLATE_VER, "ravivj", "movieinfo"
                , "movieinfo", "minfo/listmovies", Type.ResponseCompare);
            Comparator comparator = this.config.comparatorCache.getComparator(key, Event.EventType.HTTPResponse);
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


    /**
	 * @param config
	 */
	@Inject
	public CubeStore(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.config = config;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonMapper;
	Config config;


	private Optional<String> getCurrentCollectionIfEmpty(Optional<String> collection,
			Optional<String> customerId, Optional<String> app, Optional<String> instanceId) {
		return collection.or(() -> {
			return rrstore.getCurrentCollection(customerId, app, instanceId);
		});
	}

}
