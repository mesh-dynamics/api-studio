package com.cube.ws;

import static com.cube.core.Utils.buildErrorResponse;

import java.net.URLClassLoader;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;

import com.cube.agent.FnReqResponse;
import com.cube.agent.FnResponse;
import com.cube.cache.ComparatorCache;
import com.cube.cache.ReplayResultCache;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.Utils;
import com.cube.dao.Analysis;
import com.cube.dao.DataObj.PathNotFoundException;
import com.cube.dao.Event;
import com.cube.dao.Event.EventType;
import com.cube.dao.Event.RunType;
import com.cube.dao.EventQuery;
import com.cube.dao.EventQuery.Builder;
import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.RecordOrReplay;
import com.cube.dao.HTTPResponsePayload;
import com.cube.dao.Result;
import com.cube.utils.Constants;

/**
 * @author prasad
 *
 */
@Path("/ms")
public class MockServiceHTTP {

    private static final Logger LOGGER = LogManager.getLogger(MockServiceHTTP.class);

	@Path("/health")
	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        Map solrHealth = WSUtils.solrHealthCheck(config.solr);
        Map respMap = new HashMap(solrHealth);
        respMap.put(Constants.SERVICE_HEALTH_STATUS, "MS is healthy");
        return Response.ok().type(MediaType.APPLICATION_JSON).entity((new JSONObject(respMap)).toString()).build();
    }

	@GET
    @Path("{customerId}/{app}/{instanceId}/{service}/{var:.+}")
    public Response get(@Context UriInfo ui, @PathParam("var") String path,
                        @Context HttpHeaders headers,
                        @PathParam("customerId") String customerId,
                        @PathParam("app") String app,
                        @PathParam("instanceId") String instanceId,
                        @PathParam("service") String service,
                        String body) {
        LOGGER.debug(String.format("customerId: %s, app: %s, path: %s, uriinfo: %s", customerId, app, path, ui.toString()));
        return getResp(ui, path, new MultivaluedHashMap<>(), customerId, app, instanceId, service,
            HttpMethod.GET, body, headers);
    }

	// TODO: unify the following two methods and extend them to support all @Consumes types -- not just two.
	// An example here: https://stackoverflow.com/questions/27707724/consume-multiple-resources-in-a-restful-web-service

	@POST
    @Path("{customerId}/{app}/{instanceId}/{service}/{var:.+}")
    public Response postForms(@Context UriInfo ui,
                              @Context HttpHeaders headers,
                              @PathParam("var") String path,
                              @PathParam("customerId") String customerId,
                              @PathParam("app") String app,
                              @PathParam("instanceId") String instanceId,
                              @PathParam("service") String service,
                              String body) {
        LOGGER.info(String.format("customerId: %s, app: %s, path: %s, uriinfo: %s, body: %s", customerId, app, path,
            ui.toString(), body));
        return getResp(ui, path, new MultivaluedHashMap<>(), customerId, app, instanceId, service, HttpMethod.POST, body, headers);
    }


    private Optional<ReqRespStore.RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId) {
        return rrstore.getCurrentRecordOrReplay(Optional.of(customerId),
            Optional.of(app), Optional.of(instanceId));
    }

    private boolean setFunctionPayloadKeyAndCollection(Event event) {
        if (event != null && event.eventType.equals(Event.EventType.JavaRequest)) {
            Optional<ReqRespStore.RecordOrReplay> recordOrReplay = getCurrentRecordOrReplay(
                event.customerId, event.app, event.instanceId);
            Optional<String> collection = recordOrReplay
                .flatMap(ReqRespStore.RecordOrReplay::getCollection);
            // check collection, validate, fetch template for request, set key and store. If error at any point stop
            if (collection.isPresent()) {
                event.setCollection(collection.get());
                try {
                    event.parseAndSetKey(config, Utils
                        .getRequestCompareTemplate(config, event, recordOrReplay.get().getTemplateVersion()));
                } catch (ComparatorCache.TemplateNotFoundException e) {
                    LOGGER.error(new ObjectMessage(
                        Map.of("message", "Compare template not found.",
                            "type", event.eventType,
                            "reqId", event.reqId,
                            "path", event.apiPath)), e);
                    return false;
                }
                return true;
            } else {
                LOGGER
                    .error(new ObjectMessage(Map.of(
                        Constants.REASON, "Collection not found",
                        Constants.CUSTOMER_ID_FIELD, event.customerId,
                        Constants.APP_FIELD, event.app,
                        Constants.INSTANCE_ID_FIELD, event.instanceId,
                        Constants.TRACE_ID_FIELD, event.traceId)));
                return false;
            }
        } else {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.REASON,
                "Invalid event - either event is null, or some required field missing, or both binary "
                    +
                    "and string payloads set")));
            return false;
        }
    }

    private Response errorResponse(String errorReason) {
        return Response.serverError().type(MediaType.APPLICATION_JSON).
            entity((new JSONObject(Map.of(Constants.REASON, errorReason))).toString()).build();
    }

    private EventQuery buildFunctionEventQuery(Event event, int offset, int limit, boolean isSortOrderAsc) {
        return new EventQuery.Builder(event.customerId, event.app, Event.EventType.JavaRequest)
            .withService(event.service).withInstanceId(event.instanceId)
            .withPaths(List.of(event.apiPath)).withTraceId(event.traceId).withTimestamp(event.timestamp)
            .withCollection(event.getCollection()).withPayloadKey(event.payloadKey)
            .withOffset(offset).withLimit(limit).withSortOrderAsc(isSortOrderAsc)
            .build();
    }

    @POST
    @Path("/mockFunction")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mockFunction(Event event) {
        if (setFunctionPayloadKeyAndCollection(event)) {
            EventQuery eventQuery = buildFunctionEventQuery(event, 0, 1, true);
            Result<Event> matchingEvent = rrstore.getEvents(eventQuery);

            return matchingEvent.getObjects().findFirst()
                .map(retEvent -> getFuncResp(event, matchingEvent.numFound, retEvent))
                .orElseGet(() -> getDefaultFuncResp(event));
        } else {
            String errorReason = "Invalid event or no record/replay found.";
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.API_PATH_FIELD, event.apiPath,
                    Constants.TRACE_ID_FIELD, event.traceId,
                    Constants.REASON, errorReason
                )));
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.FAIL, Constants.INVALID_EVENT,
                    errorReason)).build();
        }
    }

    private Response getFuncResp(Event event, long matchingEventsCount, Event retEvent) {
        LOGGER.debug(new ObjectMessage(
            Map.of(
                Constants.API_PATH_FIELD, retEvent.apiPath,
                Constants.TRACE_ID_FIELD, retEvent.traceId,
                Constants.DATA, retEvent.rawPayloadString)));
        try {
            FnResponse fnResponse = new FnResponse(
                retEvent.parsePayLoad(config).getValAsString(Constants.FN_RESPONSE_PATH),
                Optional.of(retEvent.timestamp),
                FnReqResponse.RetStatus.Success, Optional.empty(),
                matchingEventsCount > 1);
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(fnResponse)
                .build();
        } catch (PathNotFoundException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(
                    Constants.API_PATH_FIELD, event.apiPath,
                    Constants.TRACE_ID_FIELD, event.traceId)) , e);
            return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                    "Unable to find response path in json ")).build();
        }
    }

    private Response getDefaultFuncResp(Event event) {
        String errorReason = "Unable to find matching request, looking for default response";
        LOGGER.error(new ObjectMessage(
            Map.of(
                Constants.API_PATH_FIELD, event.apiPath,
                Constants.TRACE_ID_FIELD, event.traceId,
                Constants.REASON, errorReason)));

        EventQuery.Builder defEventQuery = new EventQuery.Builder(event.customerId,
            event.app, event.eventType);
        defEventQuery.withService(event.service);
        defEventQuery.withRunType(RunType.Manual);
        defEventQuery.withPaths(List.of(event.apiPath));

        Optional<Event> defaultRespEvent = rrstore
            .getSingleEvent(defEventQuery.build());
        if (defaultRespEvent.isPresent()) {
            FnResponse fnResponse = null;
            try {
                fnResponse = new FnResponse(
                    defaultRespEvent.get().parsePayLoad(config)
                        .getValAsString(Constants.FN_RESPONSE_PATH),
                    Optional.of(defaultRespEvent.get().timestamp),
                    FnReqResponse.RetStatus.Success, Optional.empty(),
                    false);
            } catch (PathNotFoundException e) {
                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.API_PATH_FIELD, event.apiPath)), e);
                return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
                    buildErrorResponse(Constants.ERROR, Constants.JSON_PARSING_EXCEPTION,
                        "Unable to find response path in json ")).build();
            }
            return Response.ok().type(MediaType.APPLICATION_JSON).entity(fnResponse)
                .build();
        }

        errorReason = "Unable to find default response!";
        return Response.serverError().type(MediaType.APPLICATION_JSON).entity(
            buildErrorResponse(Constants.FAIL, Constants.EVENT_NOT_FOUND,
                errorReason)).build();
    }

    @POST
    @Path("/thrift")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mockThrift(Event thriftMockRequest) {
        try {
            RecordOrReplay recordOrReplay = rrstore
                .getCurrentRecordOrReplay(Optional.ofNullable(thriftMockRequest.customerId),
                    Optional.ofNullable(thriftMockRequest.app),
                    Optional.ofNullable(thriftMockRequest.instanceId), true).orElseThrow(() ->
                    new Exception(
                        "Could not find running replay for cust:: " + thriftMockRequest.customerId
                            + " , app :: " + thriftMockRequest.app + " , instanceId :: "
                            + thriftMockRequest.instanceId));
            Optional<URLClassLoader> urlClassLoader = recordOrReplay.getClassLoader();
            thriftMockRequest.parseAndSetKey(config, Utils
                .getRequestCompareTemplate(config, thriftMockRequest,
                    recordOrReplay.getTemplateVersion()), urlClassLoader);

            EventQuery.Builder builder = new Builder(thriftMockRequest.customerId,
                thriftMockRequest.app, EventType.ThriftRequest)
                .withRunType(RunType.Record).withPayloadKey(thriftMockRequest.payloadKey)
                .withService(thriftMockRequest.service).withTraceId(thriftMockRequest.traceId);

            Optional<Event> matchingThriftRequest = rrstore.getEvents(builder.build()).getObjects()
                .findFirst();
            return matchingThriftRequest
                .flatMap(matchingRequest ->rrstore.getResponseEvent(matchingRequest.reqId)).map(matchingResponse ->
                    Response.ok().type(MediaType.APPLICATION_JSON).entity(matchingResponse).build())
                .orElseThrow(() -> new Exception("No Matching Response Event Found"));

        } catch (Exception e) {
            return Response.serverError()
                .entity((new JSONObject(Map.of(Constants.MESSAGE, e.getMessage()))).toString())
                .build();
        }
    }

    @POST
    @Path("/fr")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response funcJson(@Context UriInfo uInfo,
                             String fnReqResponseAsString) {
        try {
            FnReqResponse fnReqResponse = jsonMapper.readValue(fnReqResponseAsString, FnReqResponse.class);
            String traceIdString = fnReqResponse.traceId.orElse("N/A");
            LOGGER.info(new ObjectMessage(Map.of("state" , "Before Mock", "func_name" ,  fnReqResponse.name ,
                "trace_id" , traceIdString)));
            var counter = new Object() {int x = 0;};
            if (fnReqResponse.argVals != null) {
                Arrays.stream(fnReqResponse.argVals).forEach(argVal ->
                    LOGGER.info(new ObjectMessage(Map.of("state" , "Before Mock", "func_name" ,  fnReqResponse.name ,
                        "trace_id" , traceIdString , "arg_hash" , fnReqResponse.argsHash[counter.x] , "arg_val_" + counter.x++ , argVal))));
            }
            Utils.preProcess(fnReqResponse);
            Optional<String> collection = rrstore.getCurrentRecordingCollection(Optional.of(fnReqResponse.customerId),
                Optional.of(fnReqResponse.app), Optional.of(fnReqResponse.instanceId));
            return collection.map(collec ->
                rrstore.getFunctionReturnValue(fnReqResponse, collec).map(retValue -> {
                        LOGGER.info(new ObjectMessage(Map.of("state" , "After Mock" , "func_name" , fnReqResponse.name ,
                            "trace_id" , traceIdString , "ret_val" , retValue.retVal)));
                        try {
                            String retValueAsString = jsonMapper.writeValueAsString(retValue);
                            return Response.ok().type(MediaType.APPLICATION_JSON).entity(retValueAsString).build();
                        } catch (JsonProcessingException e) {
                            LOGGER.error(new ObjectMessage(Map.of("func_name", fnReqResponse.name,
                                "trace_id", traceIdString)) , e);
                            String errorReason = "Unable to parse func response ";
                            return errorResponse(errorReason + e.getMessage());
                        }
                    }
                ).orElseGet(() -> {
                        String errorReason = "Unable to find matching request";
                        LOGGER.error(new ObjectMessage(Map.of("func_name" , fnReqResponse.name , "trace_id"
                            , traceIdString , Constants.REASON , errorReason)));
                        return errorResponse(errorReason);}))
                .orElseGet(() -> {
                        String errorReason = "Unable to locate collection for given customer, app, instance combo";
                        LOGGER.error(new ObjectMessage(Map.of("func_name" , fnReqResponse.name , "trace_id"
                            , traceIdString , Constants.REASON , errorReason)));
                        return errorResponse(errorReason);});
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).
                entity("{\"reason\" : \"Unable to parse function request object " + e.getMessage()
                    + " \"}").build();
        }
    }


    private Event createRequestMockNew(String path, MultivaluedMap<String, String> formParams,
                                         String customerId, String app, String instanceId, String service,
                                         String method, String body,
                                         HttpHeaders headers, MultivaluedMap<String, String> queryParams,
                                         String replayId, Comparator comparator) throws Event.EventBuilder.InvalidEventException, JsonProcessingException {
        // At the time of mock, our lua filters don't get deployed, hence no request id is generated
        // we can generate a new request id here in the mock service
        Optional<String> requestId = Optional.of(service.concat("-mock-").concat(String.valueOf(UUID.randomUUID())));

        MultivaluedMap<String, String> meta = new MultivaluedHashMap<>();
        meta.putSingle(Constants.SERVICE_FIELD, service);
        meta.putSingle(Constants.INSTANCE_ID_FIELD, instanceId);
        return Utils.createHTTPRequestEvent(path, requestId, queryParams, formParams, meta, headers.getRequestHeaders(),
            method, body, Optional.of(replayId), Instant.now(), Optional.of(RunType.Replay), Optional.of(customerId),
            Optional.of(app), config, comparator);

    }




    /**
     * Create a dummy response event (just for the records) to save against the dummy mock request
     * @param originalResponse
     * @param mockReqId
     * @param instanceId
     * @param replayCollection
     * @return
     */
    private Event createMockResponseEvent(Event originalResponse,
                                          Optional<String> mockReqId,
                                          String instanceId,
                                          String replayCollection) throws Event.EventBuilder.InvalidEventException {
        Event.EventBuilder builder = new Event.EventBuilder(originalResponse.customerId, originalResponse.app,
            originalResponse.service,
            instanceId, replayCollection, originalResponse.traceId, Event.RunType.Replay, Instant.now(),
            mockReqId.orElse("NA"),
            originalResponse.apiPath, Event.EventType.HTTPResponse);
        return builder.setRawPayloadString(originalResponse.rawPayloadString).createEvent();
    }



    private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
        String customerId, String app, String instanceId,
        String service, String method, String body, HttpHeaders headers) {

        LOGGER.info(String.format("Mocking request for %s", path));

        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();

        // pathParams are not used in our case, since we are matching full path
        // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = rrstore
            .getCurrentRecordOrReplay(Optional.of(customerId),
                Optional.of(app),
                Optional.of(instanceId));
        Optional<String> collectionOpt = recordOrReplay
            .flatMap(ReqRespStore.RecordOrReplay::getRecordingCollection);
        Optional<String> replayIdOpt = recordOrReplay
            .flatMap(ReqRespStore.RecordOrReplay::getCollection);
        /* TODO: looks like we are not using this any more -- can be removed once we are sure
        boolean considerTrace = Utils.strToBool(headers.getRequestHeaders()
            .getFirst("cube-consider-trace")).orElse(true);
            */

        if (replayIdOpt.isEmpty() || collectionOpt.isEmpty()) {
            LOGGER.error("Cannot mock request since replay/collection is empty");
            return notFound();
        }

        String replayId = replayIdOpt.get();
        String collection = collectionOpt.get();

        String templateVersion = recordOrReplay.get().getTemplateVersion();

        TemplateKey key = new TemplateKey(templateVersion, customerId, app, service, path, TemplateKey.Type.Request);
        Comparator comparator = null;
        try {
            comparator = comparatorCache.getComparator(key , EventType.HTTPRequest);
        } catch (ComparatorCache.TemplateNotFoundException e) {
            LOGGER.error(new ObjectMessage(Map.of(
                "message", "Compare template not found",
                "key", key
            )));
            return notFound();
        }

        // first store the original request as a part of the replay
        Event mockRequestEvent;
        try {
            mockRequestEvent = createRequestMockNew(path, formParams, customerId, app, instanceId,
                service, method, body, headers, queryParams, replayId, comparator);
            rrstore.save(mockRequestEvent);
        } catch (Exception e) {
            LOGGER.error("Exception in creating mock request, message: {}, location: {}",
                e.getMessage(), UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
            return notFound();
        }

        EventQuery reqQuery = getRequestEventQuery(mockRequestEvent, collection, 1 /*, considerTrace */);
        Optional<Event> respEvent = rrstore.getSingleEvent(reqQuery)
            .flatMap(event -> rrstore.getRespEventForReqEvent(event))
            .or(() -> {
                LOGGER.info("Using default response");

                EventQuery respQuery = getDefaultRespEventQuery(mockRequestEvent);
                Optional<Event> defRespEvent = rrstore.getSingleEvent(respQuery);
                if (defRespEvent.isPresent()) {
                    return defRespEvent;
                }

                LOGGER.error(new ObjectMessage(
                    Map.of(Constants.MESSAGE, "No default response found for request.",
                        Constants.CUSTOMER_ID_FIELD, mockRequestEvent.customerId,
                        Constants.APP_FIELD, mockRequestEvent.app,
                        Constants.SERVICE_FIELD, mockRequestEvent.service,
                        Constants.API_PATH_FIELD, mockRequestEvent.apiPath)));

                return Optional.empty();
            });

        return respEvent
            .flatMap(respEventVal -> createResponseFromEvent(mockRequestEvent, respEventVal))
            .orElseGet(() -> {
            // Increment not match counter in cache
            // TODO commenting out call to cache
            //replayResultCache.incrementReqNotMatchCounter(customerId, app, service, path, instanceId);
            //TODO this is a hack : as ReqRespMatchResult is calculated from the perspective of
            //a recorded request, here in the mock we have a replay request which did not match
            //with any recorded request, but still to properly calculate no match counts for
            // virtualized services in facet queries, we are creating this dummy req resp
            // match result for now.
            // TODO change it back to MockReqNoMatch

            Analysis.ReqRespMatchResult matchResult =
                new Analysis.ReqRespMatchResult(Optional.empty(), Optional.of(mockRequestEvent.reqId),
                    Comparator.MatchType.NoMatch, 0, Comparator.MatchType.Default, "", "",
                    customerId, app, service, path, mockRequestEvent.getCollection(), Optional.empty(),
                    Optional.of(mockRequestEvent.traceId));
            rrstore.saveResult(matchResult);
            return Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
        });

    }

    private Optional<Response> createResponseFromEvent(Event mockRequestEvent, Event respEventVal) {

        HTTPResponsePayload responsePayload;
        try {
            responsePayload = Utils.getResponsePayload(respEventVal, config);
        } catch (IOException e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Not able to deserialize response event",
                Constants.ERROR, e.getMessage()
            )), e);
            return Optional.empty();
        }

        ResponseBuilder builder = Response.status(responsePayload.status);
        responsePayload.hdrs.forEach((fieldName, fieldValList) -> fieldValList.forEach((val) -> {
            // System.out.println(String.format("key=%s, val=%s", fieldName, val));
            // looks like setting some headers causes a problem, so skip them
            // TODO: check if this is a comprehensive list
            if (Utils.ALLOWED_HEADERS.test(fieldName) && !fieldName.startsWith(":")) {
                builder.header(fieldName, val);
            }
        }));
        // Increment match counter in cache
        // TODO commenting out call to cache
        //replayResultCache.incrementReqMatchCounter(customerId, app, service, path, instanceId);
        // store a req-resp analysis match result for the mock request (during replay)
        // and the matched recording request
        String recordReqId = respEventVal.reqId;
        Analysis.ReqRespMatchResult matchResult =
            new Analysis.ReqRespMatchResult(Optional.of(recordReqId), Optional.of(mockRequestEvent.reqId),
                Comparator.MatchType.ExactMatch, 1, Comparator.MatchType.ExactMatch, "",
                "", mockRequestEvent.customerId, mockRequestEvent.app, mockRequestEvent.service,
                mockRequestEvent.apiPath,
                mockRequestEvent.getCollection(),
                Optional.of(respEventVal.traceId),
                Optional.of(mockRequestEvent.traceId));
        rrstore.saveResult(matchResult);
        try {
            Event mockResponseToStore = createMockResponseEvent(respEventVal,
                Optional.of(mockRequestEvent.reqId),
                mockRequestEvent.instanceId, mockRequestEvent.getCollection());
            rrstore.save(mockResponseToStore);
        } catch (Event.EventBuilder.InvalidEventException e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Not able to store mock event",
                    Constants.TRACE_ID_FIELD, respEventVal.traceId,
                    Constants.REQ_ID_FIELD, respEventVal.reqId)));
        }
        return Optional.of(builder.entity(responsePayload.body).build());
    }

    private EventQuery getDefaultRespEventQuery(Event mockRequest) {
        EventQuery.Builder eventQuery = new EventQuery.Builder(
            mockRequest.customerId,
            mockRequest.app, EventType.HTTPResponse);
        return eventQuery
            .withService(mockRequest.service)
            .withPaths(List.of(mockRequest.apiPath))
            .withRunType(RunType.Manual)
            .build();
    }

    public static EventQuery getRequestEventQuery(Event mockRequest, String collection, int limit) {
        String customerId = mockRequest.customerId;
        String app = mockRequest.app;
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, Event.EventType.HTTPRequest);
        return builder
            .withCollection(collection)
            .withService(mockRequest.service)
            .withTraceId(mockRequest.traceId)
            .withPaths(List.of(mockRequest.apiPath))
            .withPayloadKey(mockRequest.payloadKey)
            .withRunType(Event.RunType.Record)
            .withSortOrderAsc(true)
            .withLimit(limit)
            .build();
    }


    private Response notFound() {
	    return Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
    }


    /**
     *
     * @param config
     */
	@Inject
	public MockServiceHTTP(Config config) {
		super();
		this.config = config;
		this.rrstore = config.rrstore;
		this.jsonMapper = config.jsonMapper;
		this.comparatorCache = config.comparatorCache;
		this.replayResultCache = config.replayResultCache;
		LOGGER.info("Cube mock service started");
	}


	private ReqRespStore rrstore;
	private ObjectMapper jsonMapper;
	private ComparatorCache comparatorCache;
	private ReplayResultCache replayResultCache;
	private static String tracefield = Constants.DEFAULT_TRACE_FIELD;
	private final Config config;



}
