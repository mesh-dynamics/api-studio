/**
 * Copyright Cube I O
 */
package com.cube.ws;

import com.cube.dao.Event.RunType;
import com.cube.dao.EventBuilder.InvalidEventException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
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

import com.cube.dao.*;

import com.cube.utils.Constants;
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

import static com.cube.core.Utils.buildErrorResponse;
import static com.cube.core.Utils.buildSuccessResponse;
import static com.cube.dao.RRBase.*;
import com.cube.agent.FnReqResponse;
import com.cube.cache.TemplateKey;
import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.core.ResponseComparator;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplatedRequestComparator;
import com.cube.core.Utils;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.ReqRespStore.RecordOrReplay;

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
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"Cube store service status\": \"CS is healthy\"}").build();
    }


	@POST
	@Path("/req")
    @Consumes({MediaType.APPLICATION_JSON})
    // TODO: Event redesign cleanup: This can be removed
    public Response storereq(Request req) {
        setCollection(req);
        if (rrstore.save(req)) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Not able to store request").build();
        }
    }

	@POST
	@Path("/resp")
    @Consumes({MediaType.APPLICATION_JSON})
    // TODO: Event redesign cleanup: This can be removed
    public Response storeresp(com.cube.dao.Response resp) {
        setCollection(resp);
        if (rrstore.save(resp)) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Not able to store response").build();
        }
    }


	@POST
	@Path("/rr/{var:.*}")
    @Consumes({MediaType.APPLICATION_JSON})
    // TODO: Event redesign cleanup: This can be removed
    public Response storerr(@Context UriInfo ui,
                            @PathParam("var") String path,
                            ReqRespStore.ReqResp rr) {
	    LOGGER.info("/cs/rr request received");
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> error = storeSingleReqResp(rr, path, queryParams);
        return error.map(e -> {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }).orElse(Response.ok().build());

    }

    // TODO: Event redesign cleanup: This can be removed
    private Optional<String> storeSingleReqResp(ReqRespStore.ReqResp rr, String path, MultivaluedMap<String, String> queryParams) {
        MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<String, String>();
        rr.hdrs.forEach(kv -> {
            hdrs.add(kv.getKey(), kv.getValue());
        });

        MultivaluedMap<String, String> meta = new MultivaluedHashMap<String, String>();
        rr.meta.forEach(kv -> {
            meta.add(kv.getKey(), kv.getValue());
        });

        Optional<String> rid = Optional.ofNullable(meta.getFirst("c-request-id"));
        Optional<String> type = Optional.ofNullable(meta.getFirst("type"));
        // TODO: the following can pass replayid to cubestore but currently requests don't match in the mock
        // since we don't have the ability to ignore certain fields (in header and body)
        //if (inpcollection.isEmpty()) {
        //	inpcollection = Optional.ofNullable(hdrs.getFirst(Constants.CUBE_REPLAYID_HDRNAME));
        //}
        Optional<Instant> timestamp = Optional.ofNullable(meta.getFirst("timestamp")).map(v -> {
            Instant t = null;
            try {
                t = Instant.parse(v);
            } catch (Exception e) {
                LOGGER.error(String.format("Expecting time stamp, got %s", v));
                t = Instant.now();
            }
            return t;
        });
        Optional<Event.RunType> runType = Optional.ofNullable(meta.getFirst("runType")).flatMap(rrt -> Utils.valueOf(Event.RunType.class, rrt));
        Optional<String> customerid = Optional.ofNullable(meta.getFirst("customerid"));
        Optional<String> app = Optional.ofNullable(meta.getFirst("app"));
        Optional<String> service = Optional.ofNullable(meta.getFirst("service"));
        Optional<String> instanceid = Optional.ofNullable(meta.getFirst(RRBase.INSTANCEIDFIELD));

        //LOGGER.info(String.format("Got store for type %s, for inpcollection %s, reqId %s, path %s", type.orElse("<empty>"), inpcollection.orElse("<empty>"), rid.orElse("<empty>"), path));

        Optional<RecordOrReplay> recordOrReplay = rrstore.getCurrentRecordOrReplay(customerid, app, instanceid, true);
        if (recordOrReplay.isEmpty()) {
            // Dropping if there is no current recording.
            LOGGER.info(String.format("Dropping store for type %s, reqId %s since no current recording"
                , type.orElse("<empty>"), rid.orElse("<empty>")));
            return Optional.of("No current record/replay!");
        }

        Optional<String> collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);

        if (collection.isEmpty()) {
            // Dropping if collection is empty, i.e. recording is not started
            LOGGER.info(String.format("Dropping store for type %s, reqId %s since collection is empty"
                , type.orElse("<empty>"), rid.orElse("<empty>")));
            return Optional.of("Collection is empty");
        } else {
            LOGGER.info(String.format("Performing store for type %s, for collection %s, reqId %s, path %s"
                , type.orElse("<empty>"), collection.orElse("<empty>"), rid.orElse("<empty>"), path));
        }


        MultivaluedMap<String, String> formParams = new MultivaluedHashMap<String, String>();

        return  type.map(t -> {
            if (t.equals("request")) {
                Optional<String> method = Optional.ofNullable(meta.getFirst("method"));
                return method.map(mval -> {
                    Request req = new Request(path, rid, queryParams, formParams, meta, hdrs, mval, rr.body, collection, timestamp, runType, customerid, app);

                    // create Event object from Request
                    // fetch the template version, create template key and get a request comparator
                    String templateVersion = recordOrReplay.get().getTemplateVersion();
                    if(!(customerid.isPresent() && app.isPresent() && service.isPresent())) {
                        LOGGER.error("customer id, app or service not present");
                        return Optional.of("customer id, app or service not present");
                    }

                    TemplateKey tkey =
                        new TemplateKey(templateVersion, customerid.get(),
                            app.get(), service.get(), path, TemplateKey.Type.Request);

                    RequestComparator requestComparator =
                        config.requestComparatorCache.getRequestComparator(tkey, false);

                    Event requestEvent = null;
                    try {
                        // todo: consider creating the Event object directly instead of creating a Request
                        requestEvent = req.toEvent(requestComparator, config);
                    } catch (JsonProcessingException e) {
                        LOGGER.error("error in processing JSON: " + e);
                        return Optional.of("error in processing JSON");
                    } catch (EventBuilder.InvalidEventException e) {
                        LOGGER.error("error converting Request to Event: " + e);
                        return Optional.of("error converting Request to Event");
                    }

                    if (!rrstore.save(requestEvent))
                        return Optional.of("Not able to store request event");

                    Optional<String> empty = Optional.empty();
                    return empty;
                }).orElse(Optional.of("Method field missing"));
            } else if (t.equals("response")) {
                Optional<String> status = Optional.ofNullable(meta.getFirst("status"));
                Optional<Integer> s = status.flatMap(sval -> {
                    try {
                        return Optional.of(Integer.valueOf(sval));
                    } catch (Exception e) {
                        LOGGER.error(String.format("Expecting integer status, got %s", sval));
                        return Optional.empty();
                    }
                });
                return s.map(sval -> {
                    String reqApiPath = Optional.ofNullable(meta.getFirst(METAPATHFIELD)).orElse("");
                    com.cube.dao.Response resp = new com.cube.dao.Response(rid, sval, meta, hdrs, rr.body, collection
                        , timestamp, runType, customerid, app, reqApiPath);
                    Event responseEvent;
                    try {
                        // todo: consider creating the Event object directly instead of creating a Response
                        responseEvent = resp.toEvent(config, reqApiPath);
                    } catch (JsonProcessingException e) {
                        LOGGER.error("error in processing JSON: " + e);
                        return Optional.of("error in processing JSON");
                    } catch (EventBuilder.InvalidEventException e) {
                        LOGGER.error("error converting Response to Event: " + e);
                        return Optional.of("error converting Response to Event");
                    }
                    if (!rrstore.save(responseEvent))
                        return Optional.of("Not able to store response");
                    return Optional.<String>empty();
                }).orElse(Optional.of("Expecting integer status"));
            } else
                return Optional.of("Unknown type");
        }).orElse(Optional.of("Type not specified"));

    }

    private void processRRJson(String rrJson) throws Exception {
        ReqRespStore.ReqResp rr = jsonMapper.readValue(rrJson, ReqRespStore.ReqResp.class);

        // extract path and query params
        URIBuilder uriBuilder = new URIBuilder(rr.pathwparams);
        String path = uriBuilder.getPath();
        if(path.startsWith("/")){
            path = path.substring(1);
        }
        List<NameValuePair> queryParams = uriBuilder.getQueryParams();
        MultivaluedHashMap queryParamsMap = new MultivaluedHashMap();
        queryParams.forEach(nameValuePair -> {
            queryParamsMap.add(nameValuePair.getName(), nameValuePair.getValue());
        });

        Optional<String> err = storeSingleReqResp(rr, path, queryParamsMap);
        err.ifPresent(e -> {
           LOGGER.error("error processing and storing JSON: " + e);
        });
    }

    @POST
    @Path("/rrbatch")
    //@Consumes("application/msgpack")
    public Response storeRrBatch(@Context UriInfo uriInfo , @Context HttpHeaders headers,
                                 byte[] messageBytes) {

        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst("content-type"));
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
                            LOGGER.error("Error while processing multiline json " + e.getMessage());
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }

                    case Constants.APPLICATION_X_MSGPACK:
                        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(messageBytes));
                        try {
                            while (unpacker.hasNext()) {
                                ValueType nextType = unpacker.getNextFormat().getValueType();
                                if (nextType.isMapType()) {
                                    processRRJson(unpacker.unpackValue().toJson());
                                } else {
                                    LOGGER.error("Unidentified format type in message pack stream " + nextType.name());
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error while unpacking message pack byte stream " + e.getMessage());
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        return Response.ok().build();
                    default :
                        return Response.serverError().entity("Content type not recognized :: " + ct).build();
                }
            }

        ).orElse(Response.serverError().entity("Content type not specified").build());
    }

    // TODO: Event redesign cleanup: This can be removed
    private Optional<String> storeFnReqResp(String fnReqResponseString) throws Exception {
        FnReqResponse fnReqResponse = jsonMapper.readValue(fnReqResponseString, FnReqResponse.class);
        LOGGER.info("STORING FUNCTION  :: " + fnReqResponse.name);
        if (fnReqResponse.argVals != null) {
            Arrays.asList(fnReqResponse.argVals).stream().forEach(argVal
                -> LOGGER.info("ARG VALUE :: " + argVal));
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
    // TODO: Event redesign cleanup: This can be removed
    public Response storeFunc(String functionReqRespString /* @PathParam("customer") String customer,
                              @PathParam("instance") String instance, @PathParam("app") String app,
                              @PathParam("service") String service*/) {
        try {
            return storeFnReqResp(functionReqRespString)
                .map(errMessage -> Response.serverError().type(MediaType.APPLICATION_JSON)
                    .entity("{\"reason\" : \"" + errMessage + "\"}").build()).orElse(Response.ok().build());
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON)
                .entity("{\"reason\" : \"Error while deserializing " + e.getMessage() + "\" }").build();
        }
    }


    @POST
    @Path("/storeEventBatch")
    public Response storeEventBatch(@Context HttpHeaders headers, byte[] messageBytes) {
        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst("content-type"));
        LOGGER.info(new ObjectMessage(
            Map.of(
                "message", "Batch Events received.",
                "content type",  contentType
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
                                    "message", "finished processing",
                                    "result", jsonResp
                                )));
                            return Response.ok(jsonResp).type(MediaType.APPLICATION_JSON_TYPE).build();
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of("message", "Error while processing multiline json",
                                    "reason" , e.getMessage()
                                )));
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
                                        Map.of("reason",
                                            "Unidentified format type in message pack stream " + nextType.name())));
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(
                                Map.of("message", "Error while unpacking message pack byte stream ",
                                    "reason", e.getMessage())
                            ));
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        String jsonResp = new JSONObject(Map.of(
                            "total", total,
                            "success", numSuccess
                        )).toString();
                        LOGGER.info(new ObjectMessage(
                            Map.of(
                                "message", "finished processing",
                                "result", jsonResp
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

        Optional<String> err = processEvent(event);

        return err.map(e -> {
            LOGGER.error(new ObjectMessage(
                Map.of("message", "Dropping store for event.",
                    "reason", e)));
            /*
            try {
                LOGGER.error(String.format("Event: %s", event == null ? "NULL" :
                    config.jsonmapper.writeValueAsString(event)));
            } catch (JsonProcessingException ex) {
                LOGGER.error(String.format("Event: %s", event == null ? "NULL" : event.toString()));
            }
            */
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }).orElseGet(() -> {
            LOGGER.info(new ObjectMessage(
                Map.of("message", "Completed store",
                    "type", event.eventType,
                    "collection", event.getCollection(),
                    "reqId", event.reqId,
                    "path", event.apiPath)));
            return Response.ok().build();
        });
    }

    // converts event from json to Event, stores it,
    // returns 1 on success, else 0 in case of failure or exception
    private int processEventJson(String eventJson) {
        Event event = null;
        try {
            event = jsonMapper.readValue(eventJson, Event.class);
        } catch (IOException e) {
            LOGGER.error(new ObjectMessage(
                Map.of("message", "Error parsing Event JSON",
                    "reason", e.getMessage())));
            return 0;
        }
        Optional<String> err = processEvent(event);
        if(err.isPresent()) {
            LOGGER.error(new ObjectMessage(
                Map.of("message", "Dropping store for event",
                    "reason", err.get())));
            return 0;
        } else {
            LOGGER.info(new ObjectMessage(
                Map.of("message", "Completed store",
                    "type", event.eventType,
                    "collection", event.getCollection(),
                    "reqId", event.reqId,
                    "path", event.apiPath)));
            return 1;
        }
	}

	// process and store Event
    // return error string (Optional<String>)
    private Optional<String> processEvent(Event event) {
        if (event == null) {
            LOGGER.error("event is null");
            return Optional.of("event is null");
        }

        Optional<String> collection;

        event.setCollection("NA"); // so that validate doesn't fail

        if (!event.validate()) {
            return Optional.of("Invalid event - some required field missing, or both binary and string payloads set");
        }

        Optional<RecordOrReplay> recordOrReplay =
            rrstore.getCurrentRecordOrReplay( Optional.of(event.customerId),
                Optional.of(event.app), Optional.of(event.instanceId), true);

        if (recordOrReplay.isEmpty()) {
            return Optional.of("No current record/replay!");
        }

        collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);

        // check collection, validate, fetch template for request, set key and store. If error at any point stop
        if (collection.isEmpty()) {
            return Optional.of("Collection is missing");
        }
        event.setCollection(collection.get());
        if (event.isRequestType()) {
            // if request type, need to extract keys from request and index it, so that it can be
            // used while mocking
            event.parseAndSetKey(config,
                Utils.getRequestCompareTemplate(config, event, recordOrReplay.get().getTemplateVersion()));
        }

        boolean saveResult = rrstore.save(event);
        if (!saveResult) {
            return Optional.of("Not able to store event");
        }

        return Optional.empty();
    }

    @POST
    @Path("/frbatch")
    // TODO: Event redesign cleanup: This can be removed
    public Response storeFuncBatch(@Context UriInfo uriInfo , @Context HttpHeaders headers,
                                   byte[] messageBytes) {
        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst("content-type"));

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
                            LOGGER.error("Error while processing multiline json " + e.getMessage());
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
                                    LOGGER.error("Unidentified format type in message pack stream " + nextType.name());
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error while unpacking message pack byte stream " + e.getMessage());
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        return Response.ok().build();
                    default:
                        return Response.serverError().entity("Content type not recognized :: " + ct).build();
                }
            }
        ).orElse(Response.serverError().entity("Content type not specified").build());
    }


	@POST
	@Path("/setdefault/{customerid}/{app}/{serviceid}/{method}/{var:.+}")
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    public Response setDefault(@Context UriInfo ui,
                               @PathParam("var") String path,
                               MultivaluedMap<String, String> formParams,
                               @PathParam("customerid") String customerid,
                               @PathParam("app") String app,
                               @PathParam("serviceid") String serviceid,
                               @PathParam("method") String method) {
        String respbody = Optional.ofNullable(formParams.getFirst("body")).orElse("");
        Optional<String> contenttype = Optional.ofNullable(formParams.getFirst("content-type"));
        int status = Status.OK.getStatusCode();
        Optional<String> sparam = Optional.ofNullable(formParams.getFirst("status"));
        if (sparam.isPresent()) {
            Optional<Integer> sval = Utils.strToInt(sparam.get());
            if (sval.isEmpty()) {
                return Response.status(Status.BAD_REQUEST).entity("Status parameter is not an integer").build();
            } else {
                status = sval.get();
            }
        }

        if (saveDefaultResponse(customerid, app, serviceid, path, method, respbody, status, contenttype)) {
            return Response.ok().build();
        }
        return Response.serverError().entity("Not able to store default response").build();
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
    @Path("event/setDefaultResponse")
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
                    defaultEvent.getRawRespPayloadString())) {
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
        Event defaultReqEvent, String payload) throws InvalidEventException {
        //Store default response
        EventBuilder eventBuilder = new EventBuilder(defaultReqEvent.customerId,
            defaultReqEvent.app,
            defaultReqEvent.service, "NA", "NA",
            "NA", RunType.Manual, Instant.now(),
            "NA", defaultReqEvent.apiPath,
            Event.EventType.getResponseType(defaultReqEvent.eventType));
        eventBuilder.setRawPayloadString(payload);
        Event defaultRespEvent = eventBuilder.createEvent();
        defaultRespEvent.parseAndSetKey(config,
            Utils.getRequestCompareTemplate(config, defaultRespEvent, Constants.DEFAULT_TEMPLATE_VER));

        //We cannot use storeEvent API as it checks for an active record/replay.
        //This API is standalone and should work without an active record/replay.
        if (!rrstore.save(defaultRespEvent)) {
            LOGGER.debug(new ObjectMessage(
                Map.of("message", "Storing Response Event failed.",
                    "type", defaultReqEvent.eventType,
                    "reqId", defaultReqEvent.reqId,
                    "path", defaultReqEvent.apiPath)));

            return false;
        }

        rrstore.commit();
        return true;
    }

    private Optional<Event> getOrStoreDefaultReqEvent(Event reqEvent) throws InvalidEventException {
        if (reqEvent == null || !reqEvent.validate()) {
            LOGGER.debug(new ObjectMessage(
                Map.of("message", "Invalid Request event!")));
            throw new InvalidEventException();
        }

        EventQuery reqQuery = new EventQuery.Builder(reqEvent.customerId, reqEvent.app,
            reqEvent.eventType)
            .withService(reqEvent.service)
            .withPaths(List.of(reqEvent.apiPath))
            .withOffset(0).withLimit(1)
            .build();

        Optional<Event> matchingReqEvent = rrstore.getEvents(reqQuery).getObjects().findFirst();

        //Store request event if not present.
        if (matchingReqEvent.isEmpty()) {
            LOGGER.debug(new ObjectMessage(
                Map.of("message", "Request Event not found. Storing request event",
                    "type", reqEvent.eventType,
                    "reqId", reqEvent.reqId,
                    "path", reqEvent.apiPath)));

            EventBuilder eventBuilder = new EventBuilder(reqEvent.customerId, reqEvent.app,
                reqEvent.service, "NA", "NA",
                "NA", RunType.Manual, Instant.now(),
                "NA", reqEvent.apiPath, reqEvent.eventType);
            eventBuilder.setRawPayloadString(reqEvent.rawPayloadString);
            Event defaultReqEvent = eventBuilder.createEvent();
            defaultReqEvent.parseAndSetKey(config, Utils.
                getRequestCompareTemplate(config, defaultReqEvent, Constants.DEFAULT_TEMPLATE_VER));

            //We cannot use storeEvent API as it checks for a running record/replay.
            //This API is standalone and should work without an active record/replay.
            if (!rrstore.save(defaultReqEvent)) {
                LOGGER.debug(new ObjectMessage(
                    Map.of("message", "Storing Request Event failed.",
                        "type", reqEvent.eventType,
                        "reqId", reqEvent.reqId,
                        "path", reqEvent.apiPath)));

                return Optional.empty();
            }

            return Optional.of(defaultReqEvent);
        }

        return matchingReqEvent;
    }

    /* here the body is the full json response */
	@POST
	@Path("/setdefault/{method}/{var:.+}")
	@Consumes({MediaType.APPLICATION_JSON})
    public Response setDefaultFullResp(@Context UriInfo ui, @PathParam("var") String path,
                                       com.cube.dao.Response resp,
                                       @PathParam("method") String method) {
        if (saveDefaultResponse(path, method, resp)) {
            return Response.ok().build();
        }
        return Response.serverError().entity("Not able to store default response").build();
    }



	@POST
	@Path("start/{customerid}/{app}/{instanceid}/{collection}/{templateSetVersion}")
	@Consumes("application/x-www-form-urlencoded")
    public Response start(@Context UriInfo ui,
                          MultivaluedMap<String, String> formParams,
                          @PathParam("app") String app,
                          @PathParam("customerid") String customerId,
                          @PathParam("instanceid") String instanceId,
                          @PathParam("collection") String collection,
                          @PathParam("templateSetVersion") String templateSetVersion) {
	    // check if recording or replay is ongoing for (customer, app, instanceid)
        Optional<Response> errResp = WSUtils.checkActiveCollection(rrstore, Optional.ofNullable(customerId), Optional.ofNullable(app),
            Optional.ofNullable(instanceId), Optional.empty());
        if (errResp.isPresent()) {
            return errResp.get();
        }

        // check if recording collection name is unique for (customerid, app)
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
        if (name==null) {
            return Response.status(Status.BAD_REQUEST)
                .entity("Collection %s already active for customer %s, app %s, for instance %s. Use different name")
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

        Optional<Response> resp = Recording.startRecording(customerId, app, instanceId, collection, templateSetVersion, rrstore, name, codeVersion, branch, tags,
            false, gitCommitId, Optional.empty(), Optional.empty(), comment)
            .map(newr -> {
                String json;
                try {
                    json = jsonMapper.writeValueAsString(newr);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException ex) {
                    LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s", customerId, app, collection), ex);
                    return Response.serverError().build();
                }
            });

        return resp.orElse(Response.serverError().build());
    }


	@GET
	@Path("status/{customerid}/{app}/{collection}/{templateSetVersion}")
    public Response status(@Context UriInfo ui,
                           @PathParam("collection") String collection,
                           @PathParam("customerid") String customerid,
                           @PathParam("app") String app,
                           @PathParam("templateSetVersion") String templateSetVersion) {
	    Optional<Recording> recording = rrstore.getRecordingByCollectionAndTemplateVer(customerid,
            app, collection, templateSetVersion);

        Response resp = recording.map(r -> {
            String json;
            try {
                json = jsonMapper.writeValueAsString(r);
                return Response.ok(json, MediaType.APPLICATION_JSON).build();
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s.", customerid, app, collection), e);
                return Response.serverError().build();
            }
        }).orElse(Response.status(Response.Status.NOT_FOUND).entity(String.format("Status not found for for customer %s, app %s, collection %s.", customerid, app, collection)).build());
        return resp;
    }

	@GET
	@Path("recordings")
    public Response recordings(@Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> instanceid = Optional.ofNullable(queryParams.getFirst("instanceid"));
        Optional<String> customerid = Optional.ofNullable(queryParams.getFirst("customerid"));
        Optional<String> app = Optional.ofNullable(queryParams.getFirst("app"));
        Optional<RecordingStatus> status = Optional.ofNullable(queryParams.getFirst("status"))
            .flatMap(s -> Utils.valueOf(RecordingStatus.class, s));

        List<Recording> recordings = rrstore.getRecording(customerid, app, instanceid, status)
            .collect(Collectors.toList());

        String json;
        try {
            json = jsonMapper.writeValueAsString(recordings);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, instance %s.",
                customerid.orElse(""), app.orElse(""), instanceid.orElse("")), e);
            return Response.serverError().build();
        }
    }

    /*@GET
    @Path("goldenSet/get")
    public Response getGoldenSetList(@Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> instanceid = Optional.ofNullable(queryParams.getFirst("instanceid"));
        Optional<String> customerid = Optional.ofNullable(queryParams.getFirst("customerid"));
        Optional<String> app = Optional.ofNullable(queryParams.getFirst("app"));
        List<GoldenSet> recordings = rrstore.getGoldenSetStream(customerid, app, instanceid).collect(Collectors.toList());
        String json;
        try {
            json = jsonMapper.writeValueAsString(recordings);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Golden Set object to Json for customer %s, app %s, instance %s.",
                customerid.orElse(""), app.orElse(""), instanceid.orElse("")), e);
            return Response.serverError().build();
        }
    }*/

    @GET
	@Path("currentcollection")
    public Response currentcollection(@Context UriInfo ui) {
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        Optional<String> instanceid = Optional.ofNullable(queryParams.getFirst("instanceid"));
        Optional<String> customerid = Optional.ofNullable(queryParams.getFirst("customerid"));
        Optional<String> app = Optional.ofNullable(queryParams.getFirst("app"));
        String currentcollection = rrstore.getCurrentCollection(customerid, app, instanceid)
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
                , "movieinfo", "minfo/listmovies", TemplateKey.Type.Response);
            ResponseComparator comparator = this.config.responseComparatorCache.getResponseComparator(key);
            LOGGER.info("Got Response Comparator :: " + comparator.toString());
        } catch (Exception e) {
            LOGGER.error("Error occured :: " + e.getMessage() + " "
                + UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
        }
        return Response.ok().build();
    }


    /**
     *
     * @param ui
     * @return the requests corresponding to a path
     */
    @GET
    @Path("requests")
    // TODO: Event redesign cleanup: This can be removed
    public Response requests(@Context UriInfo ui) {
        MultivaluedMap<String, String> uriQueryParams = ui.getQueryParameters();
        Optional<String> customerid = Optional.ofNullable(uriQueryParams.getFirst("customerid"));
        Optional<String> app = Optional.ofNullable(uriQueryParams.getFirst("app"));
        Optional<String> collection = Optional.ofNullable(uriQueryParams.getFirst("collection"));
        String service = Optional.ofNullable(uriQueryParams.getFirst("service")).orElse("*");
        String path = Optional.ofNullable(uriQueryParams.getFirst("path")).orElse("*"); // the path to drill down on
        Optional<String> pattern = Optional.ofNullable(uriQueryParams.getFirst("pattern")); // the url should match
        // this pattern
        Optional<Integer> start = Optional.ofNullable(uriQueryParams.getFirst("start")).flatMap(Utils::strToInt); // for
        // paging
        Optional<Integer> nummatches =
            Optional.ofNullable(uriQueryParams.getFirst("nummatches")).flatMap(Utils::strToInt).or(() -> Optional.of(20)); //
        // for paging

        MultivaluedMap<String, String> emptyMap = new MultivaluedHashMap<>();

        MultivaluedMap<String, String> queryParams = emptyMap;
        MultivaluedMap<String, String> formParams = emptyMap;
        MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<>();
        pattern.ifPresent(p -> hdrs.add(HDRPATHFIELD, p));

        Request queryRequest = new Request(path, Optional.empty(), queryParams, formParams, hdrs, service, "", "",
            collection,
            Optional.of(Event.RunType.Record), customerid, app);

        List<Request> requests =
            rrstore.getRequests(queryRequest, mspecForDrillDownQuery, nummatches, start)
                .collect(Collectors.toList());

        String json;
        try {
            json = jsonMapper.writeValueAsString(requests);
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Request list to Json for customer %s, app %s, " +
                    "collection %s.",
                customerid.orElse(""), app.orElse(""), collection.orElse("")), e);
            return Response.serverError().build();
        }
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

	/**
	 * @param rr
	 *
	 * Set the collection field, if it is not already set
	 */
	private void setCollection(RRBase rr) {
		rr.collection = getCurrentCollectionIfEmpty(rr.collection, rr.customerId,
				rr.app, rr.getInstance());
	}

	private Optional<String> getCurrentCollectionIfEmpty(Optional<String> collection,
			Optional<String> customerid, Optional<String> app, Optional<String> instanceid) {
		return collection.or(() -> {
			return rrstore.getCurrentCollection(customerid, app, instanceid);
		});
	}

    // TODO: Event redesign : This needs to be rewritten to store as event
    private boolean saveDefaultResponse(String customerid, String app,
			String serviceid, String path, String method, String respbody, int status, Optional<String> contenttype) {
		com.cube.dao.Response resp = new com.cube.dao.Response(Optional.empty(), status,
				respbody, Optional.empty(), Optional.ofNullable(customerid), Optional.ofNullable(app), contenttype, path);
		resp.setService(serviceid);
		return saveDefaultResponse(path, method, resp);
	}

    // TODO: Event redesign: This needs to be rewritten to store as event
	private boolean saveDefaultResponse(String path, String method, com.cube.dao.Response resp) {
		Request req = new Request(resp.getService(), path, method, Optional.of(Event.RunType.Manual), resp.customerId,
				resp.app);

		// check if default response has been saved earlier
		rrstore.getRequests(req, MockServiceHTTP.mspecForDefault, Optional.of(1))
			.findFirst().ifPresentOrElse(oldreq -> {
			// set the id to the same value, so that this becomes an update operation
			req.reqId = oldreq.reqId;
		}, () -> {
			// otherwise generate a new random uuid
			req.reqId = Optional.of(UUID.randomUUID().toString());
		});
		if (rrstore.save(req)) {
			resp.reqId = req.reqId;
			return rrstore.save(resp) && rrstore.commit();
		}
		return false;
	}

    private CompareTemplate drilldownQueryReqTemplate = new CompareTemplate();
    static RequestComparator mspecForDrillDownQuery;

    {
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.PATH_PATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.RUN_TYPE_PATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.CUSTOMER_ID_PATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.APP_PATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.COLLECTION_PATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.META_PATH + "/" + SERVICEFIELD, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(Constants.HDR_PATH + "/" + HDRPATHFIELD,
            CompareTemplate.DataType.Str,
            CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));

        // comment below line if earlier ReqMatchSpec is to be used
        mspecForDrillDownQuery = new TemplatedRequestComparator(drilldownQueryReqTemplate, jsonMapper);
    }

}
