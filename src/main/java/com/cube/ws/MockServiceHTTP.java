package com.cube.ws;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.cube.agent.FnResponse;
import com.cube.core.Utils;
import com.cube.dao.DataObj;
import com.cube.dao.DataObjFactory;
import com.cube.dao.Event;
import com.cube.dao.EventQuery;
import com.cube.dao.Result;
import io.cube.agent.CommonUtils;
import io.cube.agent.UtilException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.cube.dao.RRBase.*;
import static com.cube.dao.Request.*;

import com.cube.agent.FnReqResponse;
import com.cube.cache.ReplayResultCache;
import com.cube.cache.RequestComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.core.ReqMatchSpec;
import com.cube.core.RequestComparator;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplatedRequestComparator;
import com.cube.dao.Analysis;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Request;

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
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"Virtualization service status\": \"VS is healthy\"}").build();
    }


	@GET
    @Path("{customerid}/{app}/{instanceid}/{service}/{var:.+}")
    public Response get(@Context UriInfo ui, @PathParam("var") String path,
                        @Context HttpHeaders headers,
                        @PathParam("customerid") String customerid,
                        @PathParam("app") String app,
                        @PathParam("instanceid") String instanceid,
                        @PathParam("service") String service) {
        LOGGER.debug(String.format("customerid: %s, app: %s, path: %s, uriinfo: %s", customerid, app, path, ui.toString()));
        return getResp(ui, path, new MultivaluedHashMap<>(), customerid, app, instanceid, service, headers);
    }

	// TODO: unify the following two methods and extend them to support all @Consumes types -- not just two.
	// An example here: https://stackoverflow.com/questions/27707724/consume-multiple-resources-in-a-restful-web-service

	@POST
    @Path("{customerid}/{app}/{instanceid}/{service}/{var:.+}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postForms(@Context UriInfo ui,
                              @Context HttpHeaders headers,
                              @PathParam("var") String path,
                              MultivaluedMap<String, String> formParams,
                              @PathParam("customerid") String customerid,
                              @PathParam("app") String app,
                              @PathParam("instanceid") String instanceid,
                              @PathParam("service") String service) {
        LOGGER.info(String.format("customerid: %s, app: %s, path: %s, uriinfo: %s, formParams: %s", customerid, app, path, ui.toString(), formParams.toString()));
        return getResp(ui, path, formParams, customerid, app, instanceid, service, headers);
    }

	@POST
	@Path("{customerid}/{app}/{instanceid}/{service}/{var:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postJson(@Context UriInfo ui,
                             @PathParam("var") String path,
                             @PathParam("customerid") String customerid,
                             @PathParam("app") String app,
                             @PathParam("instanceid") String instanceid,
                             @PathParam("service") String service,
                             @Context HttpHeaders headers,
                             String body) {
        LOGGER.info(String.format("customerid: %s, app: %s, path: %s, uriinfo: %s, headers: %s, body: %s", customerid, app, path, ui.toString(), headers.toString(), body));
        JSONObject obj = new JSONObject(body);
        MultivaluedMap<String, String> mmap = new MultivaluedHashMap<>();
        for (String key : obj.keySet()) {
            ArrayList<String> l = new ArrayList<>();
            l.add(obj.get(key).toString());
            mmap.put(key, l);
        }
        return getResp(ui, path, mmap, customerid, app, instanceid, service, headers);
    }

    private Optional<ReqRespStore.RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId) {
        return rrstore.getCurrentRecordOrReplay(Optional.of(customerId),
            Optional.of(app), Optional.of(instanceId));
    }

    private boolean setFunctionPayloadKeyAndCollection(Event event) {
	    if (event!= null && event.eventType.equals(Event.EventType.JavaRequest)) {
            Optional<ReqRespStore.RecordOrReplay> recordOrReplay = getCurrentRecordOrReplay(event.customerId, event.app, event.instanceId);
            Optional<String> collection = recordOrReplay.flatMap(ReqRespStore.RecordOrReplay::getCollection);
            // check collection, validate, fetch template for request, set key and store. If error at any point stop
            if (collection.isPresent()) {
                event.setCollection(collection.get());
                event.parseAndSetKey(config, getFunctionCompareTemplate(event, recordOrReplay));
                return true;
            } else {
                LOGGER.error(new ObjectMessage(Map.of("reason", "Collection not found" , "customerId",
                    event.customerId, "app", event.app, "instanceId", event.instanceId, "trace_id" , event.traceId)));
                return false;
            }
        } else {
            LOGGER.error(new ObjectMessage(Map.of("reason", "Invalid event - either event is null, or some required field missing, or both binary " +
                "and string payloads set")));
            return false;
        }
    }

    private Response errorResponse(String errorReason) {
        return Response.serverError().type(MediaType.APPLICATION_JSON).
            entity((new JSONObject(Map.of("reason", errorReason))).toString()).build();
    }

    private EventQuery buildFunctionEventQuery(Event event, int offset, int limit, boolean isSortOrderAsc) {
        return new EventQuery.Builder(event.customerId, event.app, EventQuery.EventType.JavaRequest)
            .withService(event.service).withInstanceId(event.instanceId)
            .withPaths(List.of(event.apiPath)).withTraceId(event.traceId).withTimestamp(event.timestamp)
            .withCollection(event.getCollection()).withPayloadKey(event.payloadKey)
            .withOffset(offset).withLimit(limit).withSortOrderAsc(isSortOrderAsc)
            .build();
    }

    private CompareTemplate getFunctionCompareTemplate(Event event, Optional<ReqRespStore.RecordOrReplay> recordOrReplay) {
        TemplateKey tkey =
            new TemplateKey(recordOrReplay.flatMap(ReqRespStore.RecordOrReplay::getTemplateVersion), event.customerId,
                event.app, event.service, event.apiPath, TemplateKey.Type.Request);
        return config.requestComparatorCache.getFunctionComparator(tkey).getCompareTemplate();
    }

    @POST
    @Path("/mockFunction")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mockFunction(Event event) {
        if (setFunctionPayloadKeyAndCollection(event)) {
            EventQuery eventQuery = buildFunctionEventQuery(event, 0, 1, true);
            Result<Event> matchingEvent =  rrstore.getEvents(eventQuery);

            return matchingEvent.getObjects().findFirst().map(retValue -> {
                LOGGER.debug(new ObjectMessage(Map.of("state" , "After Mock" , "func_signature" , event.apiPath ,
                    "trace_id" , event.traceId , "ret_val" , retValue.rawPayloadString)));
                try {
                    FnResponse fnResponse = new FnResponse(DataObjFactory.build(event.eventType,
                        null, retValue.rawPayloadString, config).getValAsString("/response"), Optional.of(retValue.timestamp),
                        FnReqResponse.RetStatus.Success, Optional.empty(), matchingEvent.numFound>1);

                    return Response.ok().type(MediaType.APPLICATION_JSON).entity(fnResponse).build();
                } catch (DataObj.PathNotFoundException e) {
                    LOGGER.error(new ObjectMessage(Map.of("func_signature", event.apiPath,
                        "trace_id", event.traceId)) , e);
                    return errorResponse("Unable to find response path in json " + e.getMessage());
                }
            }).orElseGet(() -> {
                String errorReason = "Unable to find matching request";
                LOGGER.error(new ObjectMessage(Map.of("func_signature" , event.apiPath , "trace_id"
                , event.traceId , "reason" , errorReason)));
                return errorResponse(errorReason);});
        } else {
            String errorReason = "Invalid event or no record/replay found.";
            LOGGER.error(new ObjectMessage(Map.of("func_signature" , event.apiPath , "trace_id"
                , event.traceId , "reason" , errorReason)));
            return errorResponse(errorReason);
        }
    }

    @POST
    @Path("/fr")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response funcJson(@Context UriInfo uInfo,
                             String fnReqResponseAsString) {
        try {
            FnReqResponse fnReqResponse = jsonmapper.readValue(fnReqResponseAsString, FnReqResponse.class);
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
                            String retValueAsString = jsonmapper.writeValueAsString(retValue);
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
                            , traceIdString , "reason" , errorReason)));
                        return errorResponse(errorReason);}))
                .orElseGet(() -> {
                        String errorReason = "Unable to locate collection for given customer, app, instance combo";
                        LOGGER.error(new ObjectMessage(Map.of("func_name" , fnReqResponse.name , "trace_id"
                            , traceIdString , "reason" , errorReason)));
                        return errorResponse(errorReason);});
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON).
                entity("{\"reason\" : \"Unable to parse function request object " + e.getMessage()
                    + " \"}").build();
        }
    }


	private Optional<Request> createRequestMock(String path, MultivaluedMap<String, String> formParams,
												String customerId, String app, String instanceId, String service,
												HttpHeaders headers, MultivaluedMap<String,String> queryParams) {
		// At the time of mock, our lua filters don't get deployed, hence no request id is generated
		// we can generate a new request id here in the mock service
		Optional<String> requestId = Optional.of(service.concat("-mock-").concat(String.valueOf(UUID.randomUUID())));
		return rrstore.getCurrentReplayId(Optional.of(customerId), Optional.of(app), Optional.of(instanceId)).map(replayId -> new Request(
				path, requestId, queryParams, formParams, headers.getRequestHeaders(), service ,
				Optional.of(replayId) , Optional.of(Event.RecordReplayType.Replay), Optional.of(customerId) , Optional.of(app)
		));

	}


    private Request createRequestMockNew(String path, MultivaluedMap<String, String> formParams,
                                                String customerId, String app, String instanceId, String service,
                                                HttpHeaders headers, MultivaluedMap<String,String> queryParams,
                                                String replayId) {
        // At the time of mock, our lua filters don't get deployed, hence no request id is generated
        // we can generate a new request id here in the mock service
        Optional<String> requestId = Optional.of(service.concat("-mock-").concat(String.valueOf(UUID.randomUUID())));
        return new Request(
            path, requestId, queryParams, formParams, headers.getRequestHeaders(), service ,
            Optional.of(replayId) , Optional.of(Event.RecordReplayType.Replay), Optional.of(customerId) , Optional.of(app));

    }



    /**
     * Create a dummy response (just for the records) to save against the dummy mock request
     * @param originalResponse
     * @param mockReqId
     * @param customerId
     * @param app
     * @param instanceId
     * @return
     */
	private com.cube.dao.Response createMockResponse(com.cube.dao.Response originalResponse, Optional<String> mockReqId,
                                                     String customerId, String app, String instanceId) {
        return  new com.cube.dao.Response(mockReqId, originalResponse.status, originalResponse.meta,
            originalResponse.hdrs, originalResponse.body, rrstore.getCurrentReplayId(Optional.of(customerId),
            Optional.of(app), Optional.of(instanceId)) , Optional.of(Instant.now()), Optional.of(Event.RecordReplayType.Replay) ,
            Optional.of(customerId), Optional.of(app));
    }

	private Response getRespOld(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
			String customerid, String app, String instanceid,
			String service, HttpHeaders headers) {

		LOGGER.info(String.format("Mocking request for %s", path));

		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		// first store the original request as a part of the replay
		// this is optional as there might not be any running replay which is a rare case
		// otherwise we'll always be able to construct a new request from the parameters
		Optional<Request> mockRequest = createRequestMock(path, formParams, customerid, app, instanceid,
				service, headers, queryParams);
		mockRequest.ifPresent(mRequest -> rrstore.save(mRequest));

	    // pathParams are not used in our case, since we are matching full path
	    // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = getCurrentRecordOrReplay(customerid, app, instanceid);
        Optional<String> collection = recordOrReplay.flatMap(ReqRespStore.RecordOrReplay::getRecordingCollection);
	    Request r = new Request(path, Optional.empty(), queryParams, formParams,
	    		headers.getRequestHeaders(), service, collection,
	    		Optional.of(Event.RecordReplayType.Record),
	    		Optional.of(customerid),
	    		Optional.of(app));

        Optional<String> templateVersion =
            recordOrReplay.flatMap(rr -> rr.replay.flatMap(replay -> replay.templateVersion));

	    TemplateKey key = new TemplateKey(templateVersion, customerid, app, service, path, TemplateKey.Type.Request);
		RequestComparator comparator = requestComparatorCache.getRequestComparator(key , true);

		Optional<com.cube.dao.Response> resp =  rrstore.getRespForReq(r, comparator)
				.or(() -> {
					r.rrtype = Optional.of(Event.RecordReplayType.Manual);
					LOGGER.info("Using default response");
					return getDefaultResponse(r);
				});


	    return resp.map(respv -> {
		    ResponseBuilder builder = Response.status(respv.status);
		    respv.hdrs.forEach((f, vl) -> vl.forEach((v) -> {
				// System.out.println(String.format("k=%s, v=%s", f, v));
				// looks like setting some headers causes a problem, so skip them
				// TODO: check if this is a comprehensive list
				if (!f.equals("transfer-encoding"))
					builder.header(f, v);
			}));
		    // Increment match counter in cache
            // TODO commenting out call to cache
            //replayResultCache.incrementReqMatchCounter(customerid, app, service, path, instanceid);
			// store a req-resp analysis match result for the mock request (during replay)
			// and the matched recording request
			mockRequest.ifPresent(mRequest -> respv.reqid.ifPresent(recordReqId -> {
				Analysis.ReqRespMatchResult matchResult =
                    new Analysis.ReqRespMatchResult(Optional.of(recordReqId), mRequest.reqid,
                        Comparator.MatchType.ExactMatch, 1, Comparator.MatchType.ExactMatch, "",
                        "", customerid, app, service, path, mRequest.collection.get(),
                        CommonUtils.getTraceId(respv.meta),
                        CommonUtils.getTraceId(mRequest.hdrs));
				rrstore.saveResult(matchResult);
				com.cube.dao.Response mockResponseToStore = createMockResponse(respv , mRequest.reqid,
                    customerid, app, instanceid);
				rrstore.save(mockResponseToStore);
			}));
		    return builder.entity(respv.body).build();
	    }).orElseGet(() -> {
				// Increment not match counter in cache
				// TODO commenting out call to cache
                //replayResultCache.incrementReqNotMatchCounter(customerid, app, service, path, instanceid);
				//TODO this is a hack : as ReqRespMatchResult is calculated from the perspective of
				//a recorded request, here in the mock we have a replay request which did not match
				//with any recorded request, but still to properly calculate no match counts for
				// virtualized services in facet queries, we are creating this dummy req resp
				// match result for now.
                // TODO change it back to MockReqNoMatch
				mockRequest.ifPresent(mRequest -> {
					Analysis.ReqRespMatchResult matchResult =
                        new Analysis.ReqRespMatchResult(Optional.empty(), mRequest.reqid,
                            Comparator.MatchType.NoMatch, 0, Comparator.MatchType.Default, "", "",
                            customerid, app, service, path, mRequest.collection.get(), Optional.empty(),
                            CommonUtils.getTraceId(mRequest.hdrs));
					rrstore.saveResult(matchResult);
				});
				return	Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
	    });

	}

    private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
                             String customerid, String app, String instanceid,
                             String service, HttpHeaders headers) {

        LOGGER.info(String.format("Mocking request for %s", path));

        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();

        // pathParams are not used in our case, since we are matching full path
        // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = rrstore.getCurrentRecordOrReplay(Optional.of(customerid),
            Optional.of(app),
            Optional.of(instanceid));
        Optional<String> collectionOpt = recordOrReplay.flatMap(ReqRespStore.RecordOrReplay::getRecordingCollection);
        Optional<String> replayIdOpt = recordOrReplay.flatMap(ReqRespStore.RecordOrReplay::getCollection);

        if (replayIdOpt.isEmpty()) {
            LOGGER.error("Cannot mock request since replay/collection is empty");
            return notFound();
        }

        String replayId = replayIdOpt.get();

        Request request = new Request(path, Optional.empty(), queryParams, formParams,
            headers.getRequestHeaders(), service, collectionOpt,
            Optional.of(Event.RecordReplayType.Record),
            Optional.of(customerid),
            Optional.of(app));

        Optional<String> templateVersion =
            recordOrReplay.flatMap(rr -> rr.replay.flatMap(replay -> replay.templateVersion));

        TemplateKey key = new TemplateKey(templateVersion, customerid, app, service, path, TemplateKey.Type.Request);
        RequestComparator comparator = requestComparatorCache.getRequestComparator(key , true);


        // first store the original request as a part of the replay
        Request mockRequest = createRequestMockNew(path, formParams, customerid, app, instanceid,
        service, headers, queryParams, replayId);
        Event mockRequestEvent;
        try {
            mockRequestEvent = mockRequest.toEvent(comparator, config);
            rrstore.save(mockRequestEvent);
        } catch (Exception e) {
            LOGGER.error("Exception in creating mock request, message: {}, location: {}",
                e.getMessage(), UtilException.extractFirstStackTraceLocation(e.getStackTrace()));
            return notFound();
        }

        EventQuery reqQuery = getRequestEventQuery(request, mockRequestEvent.payloadKey, 1);
        Optional<com.cube.dao.Response> resp =  rrstore.getEvents(reqQuery).getObjects().findFirst()
            .map(event -> event.reqId)
            .flatMap(matchingReqId -> {
                EventQuery respQuery = getResponseEventQuery(request, matchingReqId, 1);
                return rrstore.getEvents(respQuery).getObjects().findFirst();
            })
            .flatMap(event -> com.cube.dao.Response.fromEvent(event, jsonmapper))
            .or(() -> {
                request.rrtype = Optional.of(Event.RecordReplayType.Manual);
                LOGGER.info("Using default response");
                return getDefaultResponse(request);
            });


        return resp.map(respv -> {
            ResponseBuilder builder = Response.status(respv.status);
            respv.hdrs.forEach((f, vl) -> vl.forEach((v) -> {
                // System.out.println(String.format("k=%s, v=%s", f, v));
                // looks like setting some headers causes a problem, so skip them
                // TODO: check if this is a comprehensive list
                if (!f.equals("transfer-encoding"))
                    builder.header(f, v);
            }));
            // Increment match counter in cache
            // TODO commenting out call to cache
            //replayResultCache.incrementReqMatchCounter(customerid, app, service, path, instanceid);
            // store a req-resp analysis match result for the mock request (during replay)
            // and the matched recording request
            respv.reqid.ifPresent(recordReqId -> {
                Analysis.ReqRespMatchResult matchResult =
                    new Analysis.ReqRespMatchResult(Optional.of(recordReqId), mockRequest.reqid,
                        Comparator.MatchType.ExactMatch, 1, Comparator.MatchType.ExactMatch, "",
                        "", customerid, app, service, path, mockRequest.collection.get(),
                        CommonUtils.getTraceId(respv.meta),
                        CommonUtils.getTraceId(mockRequest.hdrs));
                rrstore.saveResult(matchResult);
                com.cube.dao.Response mockResponseToStore = createMockResponse(respv , mockRequest.reqid,
                    customerid, app, instanceid);
                rrstore.save(mockResponseToStore);
            });
            return builder.entity(respv.body).build();
        }).orElseGet(() -> {
            // Increment not match counter in cache
            // TODO commenting out call to cache
            //replayResultCache.incrementReqNotMatchCounter(customerid, app, service, path, instanceid);
            //TODO this is a hack : as ReqRespMatchResult is calculated from the perspective of
            //a recorded request, here in the mock we have a replay request which did not match
            //with any recorded request, but still to properly calculate no match counts for
            // virtualized services in facet queries, we are creating this dummy req resp
            // match result for now.
            // TODO change it back to MockReqNoMatch

            Analysis.ReqRespMatchResult matchResult =
                new Analysis.ReqRespMatchResult(Optional.empty(), mockRequest.reqid,
                    Comparator.MatchType.NoMatch, 0, Comparator.MatchType.Default, "", "",
                    customerid, app, service, path, mockRequest.collection.get(), Optional.empty(),
                    CommonUtils.getTraceId(mockRequest.hdrs));
            rrstore.saveResult(matchResult);
            return	Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
        });

    }

    public static EventQuery getRequestEventQuery(Request request, int payloadKey, int limit) {
        // eventually we will clean up code and make customerid and app non-optional in Request
        String customerId = request.customerid.orElse("NA");
        String app = request.app.orElse("NA");
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, EventQuery.EventType.HTTPRequest);
        request.collection.ifPresent(builder::withCollection);
        request.getService().ifPresent(builder::withService);
        request.getTraceId().ifPresent(builder::withTraceId);

        return builder.withPaths(List.of(request.path))
            .withPayloadKey(payloadKey)
            .withRRType(Event.RecordReplayType.Record)
            .withSortOrderAsc(true)
            .withLimit(limit)
            .build();
    }

    public static EventQuery getResponseEventQuery(Request request, String reqId, int limit) {
        // eventually we will clean up code and make customerid and app non-optional in Request
        String customerId = request.customerid.orElse("NA");
        String app = request.app.orElse("NA");
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, EventQuery.EventType.HTTPResponse);
        request.collection.ifPresent(builder::withCollection);
        request.getService().ifPresent(builder::withService);
        request.getTraceId().ifPresent(builder::withTraceId);
        return builder.withReqIds(List.of(reqId))
            .withLimit(limit)
            .build();
    }


    private Response notFound() {
	    return Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
    }



    private Optional<com.cube.dao.Response> getDefaultResponse(Request queryrequest) {
		return rrstore.getRespForReq(queryrequest, mspecForDefault);
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
		this.jsonmapper = config.jsonmapper;
		this.requestComparatorCache = config.requestComparatorCache;
		this.replayResultCache = config.replayResultCache;
		LOGGER.info("Cube mock service started");
	}


	private ReqRespStore rrstore;
	private ObjectMapper jsonmapper;
	private RequestComparatorCache requestComparatorCache;
	private ReplayResultCache replayResultCache;
	private static String tracefield = Config.DEFAULT_TRACE_FIELD;
	private final Config config;

	// TODO - make trace field configurable
	private static RequestComparator mspec = (ReqMatchSpec) ReqMatchSpec.builder()
			.withMpath(ComparisonType.Equal)
			.withMqparams(ComparisonType.Equal)
			.withQparamfields(List.of("querystring", "params")) // temporarily for restwrapjdbc
			.withMfparams(ComparisonType.Equal)
			.withMrrtype(ComparisonType.Equal)
			.withMcustomerid(ComparisonType.Equal)
			.withMapp(ComparisonType.Equal)
			.withMreqid(ComparisonType.EqualOptional)
			.withMcollection(ComparisonType.Equal)
			.withMmeta(ComparisonType.Equal)
			.withMetafields(Collections.singletonList(SERVICEFIELD))
			.withMhdrs(ComparisonType.EqualOptional)
			.withHdrfields(Collections.singletonList(tracefield))
			.build();

	private CompareTemplate reqTemplate = new CompareTemplate();

	{
        reqTemplate.addRule(new TemplateEntry(PATHPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		reqTemplate.addRule(new TemplateEntry(QPARAMPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		reqTemplate.addRule(new TemplateEntry(FPARAMPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(RRTYPEPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(CUSTOMERIDPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(APPPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		reqTemplate.addRule(new TemplateEntry(REQIDPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
        reqTemplate.addRule(new TemplateEntry(COLLECTIONPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		reqTemplate.addRule(new TemplateEntry(HDRPATH+"/"+tracefield, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));

		// comment below line if earlier ReqMatchSpec is to be used
		mspec = new TemplatedRequestComparator(reqTemplate, jsonmapper);
	}

	// matching to get default response
	static RequestComparator mspecForDefault = (ReqMatchSpec) ReqMatchSpec.builder()
			.withMpath(ComparisonType.Equal)
			.withMrrtype(ComparisonType.Equal)
			.withMcustomerid(ComparisonType.Equal)
			.withMapp(ComparisonType.Equal)
			.withMcollection(ComparisonType.EqualOptional)
			.withMmeta(ComparisonType.Equal)
			.withMetafields(Collections.singletonList(SERVICEFIELD))
			.build();


	private CompareTemplate defaultReqTemplate = new CompareTemplate();

	{
		defaultReqTemplate.addRule(new TemplateEntry(PATHPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		defaultReqTemplate.addRule(new TemplateEntry(RRTYPEPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		defaultReqTemplate.addRule(new TemplateEntry(CUSTOMERIDPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		defaultReqTemplate.addRule(new TemplateEntry(APPPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
		defaultReqTemplate.addRule(new TemplateEntry(COLLECTIONPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
		defaultReqTemplate.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));

		// comment below line if earlier ReqMatchSpec is to be used
		mspecForDefault = new TemplatedRequestComparator(defaultReqTemplate, jsonmapper);
	}

}
