/**
 * Copyright Cube I O
 */
package com.cube.ws;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cube.dao.ReqRespStore;

/**
 * @author prasad
 *
 */
@Path("/cs")
public class CubeStore {

	
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
