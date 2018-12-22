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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.UriInfo;

import com.cube.dao.ReqRespStore;
import com.cube.dao.ReqRespStore.Request;

/**
 * @author prasad
 *
 */
@Path("/ms")
public class MockServiceHTTP {
	
    private static final Logger LOGGER = LogManager.getLogger(MockServiceHTTP.class);
	
	@GET @Path("{customerid}/{app}/{var:.+}")
	public Response get(@Context UriInfo ui, @PathParam("var") String path, 
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
		return getResp(ui, path, new MultivaluedHashMap<>(), customerid, app);
	}

	@POST
	@Path("{customerid}/{app}/{var:.+}")
	@Consumes("application/x-www-form-urlencoded")
	public Response post(@Context UriInfo ui, @PathParam("var") String path, 
			MultivaluedMap<String, String> formParams,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app) {
		
		return getResp(ui, path, formParams, customerid, app);
	}

	private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
			String customerid, String app) {

		LOGGER.info(String.format("Mocking request for %s", path));

		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
	 
	    // pathParams are not used in our case, since we are matching full path
	    // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
	    
	    // TODO: extract reqid and collection from headers and pass to request
	    Request r = new Request(path, Optional.empty(), queryParams, formParams, Optional.empty(), 
	    		Optional.of(ReqRespStore.RR.Record.toString()), 
	    		Optional.of(customerid), 
	    		Optional.of(app));

	    Optional<ReqRespStore.Response> resp = rrstore.getRespForReq(r);
	    
	    return resp.map(respv -> {
		    ResponseBuilder builder = Response.status(respv.status);
		    respv.hdrs.forEach((f, vl) -> {
				vl.forEach((v) -> {
					// System.out.println(String.format("k=%s, v=%s", f, v));
					// looks like setting some headers causes a problem, so skip them
					// TODO: check if this is a comprehensive list
					if (!f.equals("transfer-encoding"))
						builder.header(f, v);
				});
			});
		    //return Response.status(Response.Status.NOT_FOUND).entity("Dummy response").build();
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
		LOGGER.info("Cube mock service started");
	}


	ReqRespStore rrstore;
}
