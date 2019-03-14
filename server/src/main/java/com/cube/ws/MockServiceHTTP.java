package com.cube.ws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.ReqMatchSpec;
import com.cube.core.RequestComparator;
import com.cube.dao.RRBase;
import com.cube.dao.RRBase.RR;
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
			@PathParam("customerid") String customerid,
			@PathParam("app") String app, 
			@PathParam("instanceid") String instanceid, 
			@PathParam("service") String service, 
			@Context HttpHeaders headers) {
		
		LOGGER.debug(String.format("customerid: %s, app: %s, path: %s, uriinfo: %s", customerid, app, path, ui.toString()));
		return getResp(ui, path, new MultivaluedHashMap<>(), customerid, app, instanceid, service, headers);
	}
	
	// TODO: unify the following two methods and extend them to support all @Consumes types -- not just two. 
	// An example here: https://stackoverflow.com/questions/27707724/consume-multiple-resources-in-a-restful-web-service

	@POST
	@Path("{customerid}/{app}/{instanceid}/{service}/{var:.+}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response postForms(@Context UriInfo ui, 
			@PathParam("var") String path, 
			MultivaluedMap<String, String> formParams,
			@PathParam("customerid") String customerid,
			@PathParam("app") String app, 
			@PathParam("instanceid") String instanceid, 
			@PathParam("service") String service, 
			@Context HttpHeaders headers) {
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
		MultivaluedMap<String, String> mmap = new MultivaluedHashMap<String, String>();
		for (String key : obj.keySet()) {
			ArrayList<String> l = new ArrayList<String>();
			l.add(obj.get(key).toString());
			mmap.put(key, l);
		}
		return getResp(ui, path, mmap, customerid, app, instanceid, service, headers);
	}


	private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
			String customerid, String app, String instanceid, 
			String service, HttpHeaders headers) {

		LOGGER.info(String.format("Mocking request for %s", path));

		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		
	 
	    // pathParams are not used in our case, since we are matching full path
	    // MultivaluedMap<String, String> pathParams = ui.getPathParameters();
		Optional<String> collection = rrstore.getCurrentRecordingCollection(Optional.of(customerid), Optional.of(app), Optional.of(instanceid));
	    Request r = new Request(path, Optional.empty(), queryParams, formParams, 
	    		headers.getRequestHeaders(), service, collection, 
	    		Optional.of(RRBase.RR.Record), 
	    		Optional.of(customerid), 
	    		Optional.of(app));

	    Optional<com.cube.dao.Response> resp = rrstore.getRespForReq(r, mspec)
	    		.or(() -> {
	    			r.rrtype = Optional.of(RR.Manual);
	    			LOGGER.info("Using default response");
	    			return getDefaultResponse(r);	
	    		}); // use default response if nothing matches
	    
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

	
	private Optional<com.cube.dao.Response> getDefaultResponse(Request queryrequest) {
		return rrstore.getRespForReq(queryrequest, mspecForDefault);
	}
	

	
	/**
	 * @param config
	 */
	@Inject
	public MockServiceHTTP(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
		LOGGER.info("Cube mock service started");
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	static String tracefield = Config.DEFAULT_TRACE_FIELD;
	
	// TODO - make trace field configurable
	static RequestComparator mspec = (ReqMatchSpec) ReqMatchSpec.builder()
			.withMpath(ComparisonType.Equal)
			.withMqparams(ComparisonType.Equal)
			.withMfparams(ComparisonType.Equal)
			.withMrrtype(ComparisonType.Equal)
			.withMcustomerid(ComparisonType.Equal)
			.withMapp(ComparisonType.Equal)
			.withMreqid(ComparisonType.EqualOptional)
			.withMcollection(ComparisonType.Equal)
			.withMmeta(ComparisonType.Equal)
			.withMetafields(Collections.singletonList(RRBase.SERVICEFIELD))
			.withMhdrs(ComparisonType.EqualOptional)
			.withHdrfields(Collections.singletonList(tracefield))
			.build();

	// matching to get default response
	static RequestComparator mspecForDefault = (ReqMatchSpec) ReqMatchSpec.builder()
			.withMpath(ComparisonType.Equal)
			.withMrrtype(ComparisonType.Equal)
			.withMcustomerid(ComparisonType.Equal)
			.withMapp(ComparisonType.Equal)
			.withMcollection(ComparisonType.EqualOptional)
			.withMmeta(ComparisonType.Equal)
			.withMetafields(Collections.singletonList(RRBase.SERVICEFIELD))
			.build();

}
