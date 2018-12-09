package com.cube.ws;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.Request;

/**
 * @author prasad
 *
 */
@Path("/ms")
public class MockServiceHTTP {
	
	@GET @Path("/{var:.+}")
	public Response get(@Context UriInfo ui, @PathParam("var") String path) {
		
		return getResp(ui, path, new MultivaluedHashMap<>());
	}

	@POST
	@Path("/{var:.+}")
	@Consumes("application/x-www-form-urlencoded")
	public Response post(@Context UriInfo ui, @PathParam("var") String path, MultivaluedMap<String, String> formParams) {
		
		return getResp(ui, path, formParams);
	}

	private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams) {
	    MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
	 
	    // pathParams are not used in our case, since we are matching full path
	    // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
	    
	    Request r = new Request(path, queryParams, formParams);

	    Optional<ReqRespStore.Response> resp = rrstore.getRespForReq(r);
	    
	    return resp.map(respv -> {
		    ResponseBuilder builder = Response.status(respv.status);
		    respv.hdrs.forEach((f, vl) -> {
				vl.forEach((v) -> {
					builder.header(f, v);
				});
			});
		    return builder.entity(respv.body).build();	    	
	    }).orElse(Response.status(Response.Status.NOT_FOUND).entity("Response not found").build());
	    
	}
	

	
	/**
	 * @param rrstore
	 */
	@Inject
	public MockServiceHTTP(Config config) {
		super();
		this.rrstore = config.rrstore;
	}


	ReqRespStore rrstore;
}
