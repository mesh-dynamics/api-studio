/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentracing.Scope;

import com.cube.agent.FnReqResponse;
import com.cube.cache.TemplateKey;
import com.cube.core.ResponseComparator;
import com.cube.core.Utils;
import com.cube.dao.RRBase;
import com.cube.dao.RRBase.RR;
import com.cube.dao.Recording;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Request;

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
		} else
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Not able to store request").build();
		
	}
	
	@POST
	@Path("/resp")
    @Consumes({MediaType.APPLICATION_JSON})
	public Response storeresp(com.cube.dao.Response resp) {
		
		setCollection(resp);
		if (rrstore.save(resp)) {
			return Response.ok().build();
		} else
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Not able to store response").build();		
	}
	

	@POST
	@Path("/rr/{var:.*}")
    @Consumes({MediaType.APPLICATION_JSON})
	public Response storerr(@Context UriInfo ui, 
							@PathParam("var") String path,
							@Context HttpHeaders httpHeaders,
							ReqRespStore.ReqResp rr) {
        try (Scope scope =  Utils.startServerSpan(config.tracer, httpHeaders , "store-req-resp")) {
            MultivaluedMap<String, String> queryParams = ui.getQueryParameters();

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
            Optional<RR> rrtype = Optional.ofNullable(meta.getFirst("rrtype")).flatMap(rrt -> Utils.valueOf(RR.class, rrt));
            Optional<String> customerid = Optional.ofNullable(meta.getFirst("customerid"));
            Optional<String> app = Optional.ofNullable(meta.getFirst("app"));
            Optional<String> instanceid = Optional.ofNullable(meta.getFirst(RRBase.INSTANCEIDFIELD));

            LOGGER.info(String.format("Got store for type %s, for inpcollection %s, reqid %s, path %s", type.orElse("<empty>"), inpcollection.orElse("<empty>"), rid.orElse("<empty>"), path));

            Optional<String> collection = getCurrentCollectionIfEmpty(inpcollection, customerid, app, instanceid);

            LOGGER.info(String.format("Got store for type %s, for collection %s, reqid %s, path %s", type.orElse("<empty>"), collection.orElse("<empty>"), rid.orElse("<empty>"), path));

            if (collection.isEmpty()) {
                // Dropping if collection is empty, i.e. recording is not started
                LOGGER.info(String.format("Dropping store for type %s, reqid %s since collection is empty", type.orElse("<empty>"), rid.orElse("<empty>")));
                return Response.ok().build();
            }


            MultivaluedMap<String, String> fparams = new MultivaluedHashMap<String, String>();

            Optional<String> err = type.map(t -> {
                if (t.equals("request")) {
                    Optional<String> method = Optional.ofNullable(meta.getFirst("method"));
                    return method.map(mval -> {
                        Request req = new Request(path, rid, queryParams, fparams, meta, hdrs, mval, rr.body, collection, timestamp, rrtype, customerid, app);
                        if (!rrstore.save(req))
                            return Optional.of("Not able to store request");
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
                        com.cube.dao.Response resp = new com.cube.dao.Response(rid, sval, meta, hdrs, rr.body, collection, timestamp, rrtype, customerid, app);
                        if (!rrstore.save(resp))
                            return Optional.of("Not able to store response");
                        Optional<String> empty = Optional.empty();
                        return empty;
                    }).orElse(Optional.of("Expecting integer status"));
                } else
                    return Optional.of("Unknown type");
            }).orElse(Optional.of("Type not specified"));

            return err.map(e -> {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
            }).orElse(Response.ok().build());
        }
	}

	@POST
    @Path("/fr")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response storeFunc(String functionReqRespString ,@Context HttpHeaders httpHeaders/* @PathParam("customer") String customer,
                              @PathParam("instance") String instance, @PathParam("app") String app,
                              @PathParam("service") String service*/) {
        try (Scope scope =  Utils.startServerSpan(config.tracer, httpHeaders , "store-func-ret")) {
            scope.span().setBaggageItem("action" , "func");
            FnReqResponse functionReqResp = jsonmapper.readValue(functionReqRespString, FnReqResponse.class);
            Optional<String> collection = getCurrentCollectionIfEmpty(Optional.empty(), Optional.of(functionReqResp.customerId),
                Optional.of(functionReqResp.app) , Optional.of(functionReqResp.instanceId));
            return collection.map(collec -> {
            boolean saveResult = rrstore.storeFunctionReqResp(functionReqResp , collec);
            return (saveResult) ? Response.ok().type(MediaType.APPLICATION_JSON)
                .entity("{\"reason\" : \"Successfully stored function response details\"}").build() :
                Response.serverError().type(MediaType.APPLICATION_JSON)
                    .entity("{\"reason\" : \"Unable to store function response details\"}").build(); })
                .orElse(Response.serverError().type(MediaType.APPLICATION_JSON)
                    .entity("{\"reason\" : \"No ongoing recording, dropping request\"}").build());
        } catch (Exception e) {
            return Response.serverError().type(MediaType.APPLICATION_JSON)
                .entity("{\"reason\" : \"Error while deserializing " + e.getMessage() + "\" }").build();
        }
    }

	@POST
	@Path("/setdefault/{customerid}/{app}/{serviceid}/{method}/{var:.+}")
	@Consumes({MediaType.APPLICATION_FORM_URLENCODED})
	public Response setDefault(@Context UriInfo ui, @PathParam("var") String path, 
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
	@Path("start/{customerid}/{app}/{instanceid}/{collection}")
	@Consumes("application/x-www-form-urlencoded")
	public Response start(@Context UriInfo ui,
                          @Context HttpHeaders httpHeaders,
			@PathParam("app") String app,
			@PathParam("customerid") String customerid,
			@PathParam("instanceid") String instanceid, 
			@PathParam("collection") String collection) {
        try (Scope scope =  Utils.startServerSpan(config.tracer, httpHeaders , "start-recording")) {
            // check if recording or replay is ongoing for (customer, app, instanceid)
            Optional<Response> errResp = WSUtils.checkActiveCollection(rrstore, Optional.ofNullable(customerid), Optional.ofNullable(app),
                Optional.ofNullable(instanceid));
            if (errResp.isPresent()) {
                return errResp.get();
            }

            // check if recording collection name is unique for (customerid, app)
            Optional<Recording> recording = rrstore.getRecordingByCollection(customerid, app, collection);
            errResp = recording.filter(r -> r.status == RecordingStatus.Running)
                .map(recordingv -> Response.status(Response.Status.CONFLICT)
                    .entity(String.format("Collection %s already active for customer %s, app %s, for instance %s. Use different name",
                        collection, customerid, app, recordingv.instanceid))
                    .build());
            if (errResp.isPresent()) {
                return errResp.get();
            }

            // NOTE that if the recording is not active, it will be activated again. This allows the same collection recording to be
            // stopped and started multiple times

            LOGGER.info(String.format("Starting recording for customer %s, app %s, instance %s, collection %s",
                customerid, app, instanceid, collection));

            Optional<Response> resp = Recording.startRecording(customerid, app, instanceid, collection, rrstore)
                .map(newr -> {
                    String json;
                    try {
                        json = jsonmapper.writeValueAsString(newr);
                        return Response.ok(json, MediaType.APPLICATION_JSON).build();
                    } catch (JsonProcessingException ex) {
                        LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s", customerid, app, collection), ex);
                        return Response.serverError().build();
                    }
                });

            return resp.orElse(Response.serverError().build());
        }
		
	}


	@GET
	@Path("status/{customerid}/{app}/{collection}")
	public Response status(@Context UriInfo ui,
                           @Context HttpHeaders httpHeaders,
                           @PathParam("collection") String collection,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
        try (Scope scope =  Utils.startServerSpan(config.tracer, httpHeaders , "status-recording")) {
            Optional<Recording> recording = rrstore.getRecordingByCollection(customerid,
                app, collection);

            Response resp = recording.map(r -> {
                String json;
                try {
                    json = jsonmapper.writeValueAsString(r);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException e) {
                    LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s.", customerid, app, collection), e);
                    return Response.serverError().build();
                }
            }).orElse(Response.status(Response.Status.NOT_FOUND).entity(String.format("Status not found for for customer %s, app %s, collection %s.", customerid, app, collection)).build());

            return resp;
        }
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
			json = jsonmapper.writeValueAsString(recordings);
			return Response.ok(json, MediaType.APPLICATION_JSON).build();
		} catch (JsonProcessingException e) {
			LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, instance %s.", 
					customerid.orElse(""), app.orElse(""), instanceid.orElse("")), e);
			return Response.serverError().build();
		}
	}

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
	@Path("stop/{customerid}/{app}/{collection}")
	public Response stop(@Context UriInfo ui,
                         @Context HttpHeaders httpHeaders,
                         @PathParam("collection") String collection,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
        try (Scope scope =  Utils.startServerSpan(config.tracer, httpHeaders , "stop-recording")) {
            Optional<Recording> recording = rrstore.getRecordingByCollection(customerid,
                app, collection);
            LOGGER.info(String.format("Stoppping recording for customer %s, app %s, collection %s",
                customerid, app, collection));
            Response resp = recording.map(r -> {
                Recording stoppedr = Recording.stopRecording(r, rrstore);
                String json;
                try {
                    json = jsonmapper.writeValueAsString(stoppedr);
                    return Response.ok(json, MediaType.APPLICATION_JSON).build();
                } catch (JsonProcessingException ex) {
                    LOGGER.error(String.format("Error in converting Recording object to Json for customer %s, app %s, collection %s", customerid, app, collection), ex);
                    return Response.serverError().build();
                }
            }).orElse(Response.status(Response.Status.NOT_FOUND).
                entity(String.format("Status not found for for customer %s, app %s, collection %s.", customerid, app, collection)).build());
            return resp;
        }
	}

	@GET
    @Path("/togglestate/{state}")
    public Response toggleClientState(@Context UriInfo uriInfo, @PathParam("state") String state){
	    switch(state) {
            case "record":
                config.setState(Config.AppState.Record);
                break;
            case "mock":
                config.setState(Config.AppState.Mock);
                break;
            case "normal":
                config.setState(Config.AppState.Normal);
                break;
                default:
                    return Response.serverError().type(MediaType.APPLICATION_JSON).entity("{\"reason\" : \"State Not identified\"}").build();
        }
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"reason\" : \"Successfully toggled client state\"}").build();
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
	        TemplateKey key = new TemplateKey("ravivj" , "movieinfo"
                , "movieinfo" , "minfo/listmovies" , TemplateKey.Type.Response);
            ResponseComparator comparator = this.config.responseComparatorCache.getResponseComparator(key);
            LOGGER.info("Got Response Comparator :: " + comparator.toString());
        } catch (Exception e) {

        }
	    return Response.ok().build();
    }


	/**
	 * @param config
	 */
	@Inject
	public CubeStore(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
		this.config = config;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	Config config;

	/**
	 * @param rr
	 * 
	 * Set the collection field, if it is not already set
	 */
	private void setCollection(RRBase rr) {
		rr.collection = getCurrentCollectionIfEmpty(rr.collection, rr.customerid, 
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
		Request req = new Request(resp.getService(), path, method, Optional.of(RR.Manual), resp.customerid,
				resp.app);
		
		// check if default response has been saved earlier
		rrstore.getRequests(req, MockServiceHTTP.mspecForDefault, Optional.of(1))
			.findFirst().ifPresentOrElse(oldreq -> {
			// set the id to the same value, so that this becomes an update operation
			req.reqid = oldreq.reqid;
		}, () -> {
			// otherwise generate a new random uuid
			req.reqid = Optional.of(UUID.randomUUID().toString());
		});
		if (rrstore.save(req)) {
			resp.reqid = req.reqid;
			return rrstore.save(resp) && rrstore.commit();
		}
		return false;
	}
}
