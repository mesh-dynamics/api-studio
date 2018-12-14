/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStoreSolr;
import com.cube.dao.ReqRespStore.Request;

/**
 * @author prasad
 *
 */
@Path("/cs")
public class CubeStore {

    private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreSolr.class);

	@POST
	@Path("/req")
    @Consumes({MediaType.APPLICATION_JSON})
	public Response storereq(ReqRespStore.Request req) {
		
		if (rrstore.save(req)) {
			return Response.ok().build();
		} else
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Not able to store request").build();
		
	}
	
	@POST
	@Path("/resp")
    @Consumes({MediaType.APPLICATION_JSON})
	public Response storeresp(ReqRespStore.Response resp) {
		
		if (rrstore.save(resp)) {
			return Response.ok().build();
		} else
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Not able to store response").build();		
	}
	

	@POST
	@Path("/rr/{var:.*}")
    @Consumes({MediaType.APPLICATION_JSON})
	public Response storeresp(@Context UriInfo ui, @PathParam("var") String path, ReqRespStore.ReqResp rr) {
	
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
	    
	    MultivaluedMap<String, String> fparams = new MultivaluedHashMap<String, String>(); 

	    Optional<String> err = type.map(t -> {
	    	if (t.equals("request")) {
	    	    Optional<String> method = Optional.ofNullable(meta.getFirst("method"));
	    	    return method.map(mval -> {
		    	    Request req = new Request(path, rid, queryParams, fparams, meta, hdrs, mval, rr.body);
		    	    if (!rrstore.save(req))
		    	    	return Optional.of("Not able to store request");	    	    	
		    		Optional<String> empty = Optional.empty();
		    		return empty;
	    	    }).orElse(Optional.of("Method field missing"));
	    	} else if (t.equals("response")) {
	    	    Optional<String> status = Optional.ofNullable(meta.getFirst("status"));
	    	    Optional<Integer> s = status.flatMap(sval -> {
	    	    	try {
	    	    		return Optional.of(new Integer(Integer.parseInt(sval)));
	    	    	} catch (Exception e){
	    	    		LOGGER.error(String.format("Expecting integer status, got %s", sval));
	    	    		return Optional.empty();
	    	    	}
	    	    });
	    	    return s.map(sval -> {
		    		ReqRespStore.Response resp = new ReqRespStore.Response(rid, sval, meta, hdrs, rr.body);
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

	
	/**
	 * @param rrstore
	 */
	@Inject
	public CubeStore(Config config) {
		super();
		this.rrstore = config.rrstore;
	}


	ReqRespStore rrstore;

}
