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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.core.Utils;
import com.cube.dao.RRBase;
import com.cube.dao.RRBase.RR;
import com.cube.dao.Recording;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Request;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author prasad
 *
 */
@Path("/cs")
public class CubeStore {

    private static final Logger LOGGER = LogManager.getLogger(CubeStore.class);

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
	public Response storerr(@Context UriInfo ui, @PathParam("var") String path, ReqRespStore.ReqResp rr) {
	
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
	    
	    Optional<String> collection = getCurrentCollectionIfEmpty(inpcollection, customerid, app, instanceid);
	    
	    LOGGER.info(String.format("Got store for type %s, for collection %s, reqid %s, path %s", type.orElse(""), collection.orElse(""), rid.orElse(""), path));

	    if (collection.isEmpty()) {
		    // Dropping if collection is empty, i.e. recording is not started
		    LOGGER.info(String.format("Dropping store for type %s, reqid %s since collection is empty", type.orElse(""), rid.orElse("")));
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
	    	    	} catch (Exception e){
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
				
		if (saveDefaultResponse(customerid, app, serviceid, path, method, respbody, status)) {
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
			@PathParam("app") String app,
			@PathParam("customerid") String customerid,
			@PathParam("instanceid") String instanceid, 
			@PathParam("collection") String collection) {
		
		// check if recording is ongoing for (customer, app, instanceid)
		Optional<Recording> recording = rrstore.getRecording(Optional.ofNullable(customerid), Optional.ofNullable(app), 
				Optional.ofNullable(instanceid), Optional.of(RecordingStatus.Running)).findFirst();
		if (recording.isPresent()) {
			return Response.status(Response.Status.CONFLICT)
					.entity(String.format("Recording ongoing for customer %s, app %s, instance %s.", customerid, app, instanceid))
					.build();
		}
		
		// check if recording collection name is unique for (customerid, app)
		recording = rrstore.getRecordingByCollection(customerid, app, collection);

		if (recording.isPresent()) {
			return Response.status(Response.Status.CONFLICT)
					.entity(String.format("Collection %s already exists for customer %s, app %s. Use different name", collection, customerid, app))
					.build();
		}
		
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


	@GET
	@Path("status/{customerid}/{app}/{collection}")
	public Response status(@Context UriInfo ui, @PathParam("collection") String collection, 
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
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
	public Response stop(@Context UriInfo ui, @PathParam("collection") String collection, 
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
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
		}).orElse(Response.status(Response.Status.NOT_FOUND).entity(String.format("Status not found for for customer %s, app %s, collection %s.", customerid, app, collection)).build());

		return resp;
	}

	
	/**
	 * @param rrstore
	 */
	@Inject
	public CubeStore(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;

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
			String serviceid, String path, String method, String respbody, int status) {
		com.cube.dao.Response resp = new com.cube.dao.Response(Optional.empty(), status, 
				respbody, Optional.empty(), Optional.ofNullable(customerid), Optional.ofNullable(app));
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
