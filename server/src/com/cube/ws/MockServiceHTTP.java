package com.cube.ws;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author prasad
 *
 */
@Path("/ms")
public class MockServiceHTTP {
	
	@GET @Path("/{var:.+}")
	public Response get(@Context UriInfo ui, @PathParam("var") String path) {
	    MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
	    MultivaluedMap<String, String> pathParams = ui.getPathParameters();
	    
	    ResponseBuilder builder = Response.ok();
	    
		return builder.entity(path).build();
	}

}
