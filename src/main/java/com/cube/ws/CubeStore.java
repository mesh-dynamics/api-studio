/**
 * Copyright Cube I O
 */
package com.cube.ws;

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
import org.json.JSONObject;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import static com.cube.dao.RRBase.*;
import static com.cube.dao.Request.PATHPATH;
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
        Optional<String> inpcollection = Optional.ofNullable(meta.getFirst("collection"));
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

        Optional<String> collection = getCurrentCollectionIfEmpty(inpcollection, customerid, app, instanceid);

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
                    Optional<String> templateVersion = rrstore.getCurrentRecordOrReplay(customerid, app, instanceid)
                        .flatMap(RecordOrReplay::getTemplateVersion);
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
                    com.cube.dao.Response resp = new com.cube.dao.Response(rid, sval, meta, hdrs, rr.body, collection, timestamp, runType, customerid, app);
                    Event responseEvent;
                    try {
                        // todo: consider creating the Event object directly instead of creating a Response
                        responseEvent = resp.toEvent(config);
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
    public Response storeEventBatch(@Context UriInfo ui, @Context HttpHeaders headers,
                               byte[] messageBytes) {
        Optional<String> contentType = Optional.ofNullable(headers.getRequestHeaders().getFirst("content-type"));
        LOGGER.info("Batch Events received. Content Type: " + contentType);
        return contentType.map(
            ct -> {
                switch(ct) {
                    case Constants.APPLICATION_X_NDJSON:
                        try {
                            String jsonMultiline = new String(messageBytes);
                            String[] jsons = jsonMultiline.split("\n");
                            LOGGER.info("JSON batch size: " + jsons.length);
                            int numSuccess = Arrays.stream(jsons)
                                .mapToInt(j -> {
                                    return processEventJson(j);
                                })
                                .sum();
                            String jsonResp = new JSONObject(Map.of(
                                "total", jsons.length,
                                "success", numSuccess
                            )).toString();
                            return Response.ok(jsonResp).type(MediaType.APPLICATION_JSON_TYPE).build();
                        } catch (Exception e) {
                            LOGGER.error("Error while processing multiline json " + e.getMessage());
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
                                    LOGGER.error("Unidentified format type in message pack stream " + nextType.name());
                                    unpacker.skipValue();
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error while unpacking message pack byte stream " + e.getMessage());
                            return Response.serverError().entity("Error while processing :: " + e.getMessage()).build();
                        }
                        String jsonResp = new JSONObject(Map.of(
                            "total", total,
                            "success", numSuccess
                        )).toString();
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
            LOGGER.error(String.format("Dropping store for event. Error: %s", e));
            try {
                LOGGER.error(String.format("Event: %s", event == null ? "NULL" :
                    config.jsonMapper.writeValueAsString(event)));
            } catch (JsonProcessingException ex) {
                LOGGER.error(String.format("Event: %s", event == null ? "NULL" : event.toString()));
            }
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }).orElseGet(() -> {
            LOGGER.info(String.format("Completed store for type %s, for collection %s, reqid %s, path %s"
                , event.eventType, event.getCollection(), event.reqId, event.apiPath));
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
            LOGGER.error("Error parsing Event JSON: " + e.getMessage());
            return 0;
        }
        Optional<String> err = processEvent(event);
        if(err.isPresent()) {
            LOGGER.error(String.format("Dropping store for event. Error: %s", err.get()));
            return 0;
        } else {
            LOGGER.info(String.format("Completed store for type %s, for collection %s, reqid %s, path %s"
                , event.eventType, event.getCollection(), event.reqId, event.apiPath));
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
                Optional.of(event.app), Optional.of(event.instanceId));
        collection = recordOrReplay.flatMap(RecordOrReplay::getCollection);

        // check collection, validate, fetch template for request, set key and store. If error at any point stop
        if (collection.isEmpty()) {
            return Optional.of("Collection is missing");
        }
        event.setCollection(collection.get());
        if (event.isRequestType()) {
            // if request type, need to extract keys from request and index it, so that it can be
            // used while mocking
            event.parseAndSetKey(config, getCompareTemplate(event, recordOrReplay));
        }

        boolean saveResult = rrstore.save(event);
        if (!saveResult) {
            return Optional.of("Not able to store event");
        }

        return Optional.empty();
    }

    private CompareTemplate getCompareTemplate(Event event, Optional<RecordOrReplay> recordOrReplay) {
        TemplateKey tkey =
            new TemplateKey(recordOrReplay.flatMap(RecordOrReplay::getTemplateVersion), event.customerId,
                event.app, event.service, event.apiPath, TemplateKey.Type.Request);

        CompareTemplate compareTemplate;
        if (event.eventType.equals(Event.EventType.JavaRequest)) {
            compareTemplate = config.requestComparatorCache.getFunctionComparator(tkey).getCompareTemplate();
        } else{
            compareTemplate =
                config.requestComparatorCache.getRequestComparator(tkey, false).getCompareTemplate();
        }
        return compareTemplate;
    }

    @POST
    @Path("/frbatch")
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
                          @PathParam("customerid") String customerid,
                          @PathParam("instanceid") String instanceid,
                          @PathParam("collection") String collection,
                          @PathParam("templateSetVersion") String templateSetVersion) {
//        String templateSetVersion = Recording.DEFAULT_TEMPLATE_VER;
	    // check if recording or replay is ongoing for (customer, app, instanceid)
        Optional<Response> errResp = WSUtils.checkActiveCollection(rrstore, Optional.ofNullable(customerid), Optional.ofNullable(app),
            Optional.ofNullable(instanceid));
        if (errResp.isPresent()) {
            return errResp.get();
        }

        // check if recording collection name is unique for (customerid, app)
        Optional<Recording> recording = rrstore
            .getRecordingByCollectionAndTemplateVer(customerid, app, collection, Optional.of(templateSetVersion));
        errResp = recording.filter(r -> r.status == RecordingStatus.Running)
            .map(recordingv -> Response.status(Response.Status.CONFLICT)
                .entity(String.format("Collection %s already active for customer %s, app %s, for instance %s. Use different name",
                    collection, customerid, app, recordingv.instanceId))
                .build());
        if (errResp.isPresent()) {
            return errResp.get();
        }

        // NOTE that if the recording is not active, it will be activated again. This allows the same collection recording to be
        // stopped and started multiple times

        LOGGER.info(String.format("Starting recording for customer %s, app %s, instance %s, collection %s",
            customerid, app, instanceid, collection));



        Optional<Response> resp = Recording.startRecording(customerid, app, instanceid, collection, Optional.of(templateSetVersion), rrstore)
            .map(newr -> {
                String json;
                try {
                    json = jsonMapper.writeValueAsString(newr);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException ex) {
                    LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s", customerid, app, collection), ex);
                    return Response.serverError().build();
                }
            });

        return resp.orElse(Response.serverError().build());
    }


	@GET
	@Path("status/{customerid}/{app}/{collection}")
    public Response status(@Context UriInfo ui,
                           @PathParam("collection") String collection,
                           @PathParam("customerid") String customerid,
                           @PathParam("app") String app
                           /*@PathParam("templateSetVersion") String templateSetVersion*/) {
        String templateSetVersion = Recording.DEFAULT_TEMPLATE_VER;
	    Optional<Recording> recording = rrstore.getRecordingByCollectionAndTemplateVer(customerid,
            app, collection, Optional.of(templateSetVersion));

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
            TemplateKey key = new TemplateKey(Optional.empty(), "ravivj", "movieinfo"
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

        Request queryRequest = new Request(path, Optional.empty(), queryParams, formParams, hdrs, service, collection,
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

	private boolean saveDefaultResponse(String customerid, String app,
			String serviceid, String path, String method, String respbody, int status, Optional<String> contenttype) {
		com.cube.dao.Response resp = new com.cube.dao.Response(Optional.empty(), status,
				respbody, Optional.empty(), Optional.ofNullable(customerid), Optional.ofNullable(app), contenttype);
		resp.setService(serviceid);
		return saveDefaultResponse(path, method, resp);
	}

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
        drilldownQueryReqTemplate.addRule(new TemplateEntry(PATHPATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(RUNTYPEPATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(CUSTOMERIDPATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(APPPATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(COLLECTIONPATH, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        drilldownQueryReqTemplate.addRule(new TemplateEntry(HDRPATH + "/" + HDRPATHFIELD,
            CompareTemplate.DataType.Str,
            CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));

        // comment below line if earlier ReqMatchSpec is to be used
        mspecForDrillDownQuery = new TemplatedRequestComparator(drilldownQueryReqTemplate, jsonMapper);
    }

}
