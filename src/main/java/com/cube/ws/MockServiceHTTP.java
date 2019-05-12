package com.cube.ws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

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
import com.cube.core.Utils;
import com.cube.dao.Analysis;
import com.cube.dao.RRBase;
import com.cube.dao.RRBase.*;
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
		MultivaluedMap<String, String> mmap = new MultivaluedHashMap<>();
		for (String key : obj.keySet()) {
			ArrayList<String> l = new ArrayList<>();
			l.add(obj.get(key).toString());
			mmap.put(key, l);
		}
		return getResp(ui, path, mmap, customerid, app, instanceid, service, headers);
	}

	@POST
    @Path("/fr")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response funcJson(@Context UriInfo uInfo,
                             String fnReqResponseAsString) {
	    try {
	        FnReqResponse fnReqResponse = jsonmapper.readValue(fnReqResponseAsString , FnReqResponse.class);
            Optional<String> collection = rrstore.getCurrentRecordingCollection(Optional.of(fnReqResponse.customerId),
                Optional.of(fnReqResponse.app), Optional.of(fnReqResponse.instanceId));
            return collection.map(collec ->
                rrstore.getFunctionReturnValue(fnReqResponse, collec).map(retValue ->
                Response.ok().type(MediaType.APPLICATION_JSON).entity(retValue).build()).
                orElse(Response.serverError().type(MediaType.APPLICATION_JSON).
                    entity("{\"reason\" : \"Unable to find matching function request\"}").build()))
                .orElse(Response.serverError().type(MediaType.APPLICATION_JSON).
                    entity("{\"reason\" : \"Unable to locate collection for given customer, app, instance combo\"}")
                    .build());
        } catch (IOException e) {
	        return Response.serverError().type(MediaType.APPLICATION_JSON).
                entity("{\"reason\" : \"Unable to parse function request object "+ e.getMessage()
                    +  " \"}").build();
        }
    }


	private Optional<Request> createRequestMock(String path, MultivaluedMap<String, String> formParams,
												String customerId, String app, String instanceId, String service,
												HttpHeaders headers, MultivaluedMap<String,String> queryParams) {
		// At the time of mock, our lua filters don't get deployed, hence no request id is generated
		// we can generate a new request id here in the mock service
		Optional<String> requestId = Optional.of(service.concat("-mock-").concat(String.valueOf(UUID.randomUUID())));
		return replayResultCache.getCurrentReplayId(customerId, app, instanceId).map(replayId -> new Request(
				path, requestId, queryParams, formParams, headers.getRequestHeaders(), service ,
				Optional.of(replayId) , Optional.of(RR.Replay), Optional.of(customerId) , Optional.of(app)
		));

	}

	private Response getResp(UriInfo ui, String path, MultivaluedMap<String, String> formParams,
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
		Optional<String> collection = rrstore.getCurrentRecordingCollection(Optional.of(customerid), Optional.of(app), Optional.of(instanceid));
	    Request r = new Request(path, Optional.empty(), queryParams, formParams, 
	    		headers.getRequestHeaders(), service, collection, 
	    		Optional.of(RRBase.RR.Record), 
	    		Optional.of(customerid), 
	    		Optional.of(app));

	    TemplateKey key = new TemplateKey(customerid, app , service , path , TemplateKey.Type.Request);
		RequestComparator comparator = requestComparatorCache.getRequestComparator(key , true);

		Optional<com.cube.dao.Response> resp =  rrstore.getRespForReq(r, comparator)
				.or(() -> {
					r.rrtype = Optional.of(RR.Manual);
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
			replayResultCache.incrementReqMatchCounter(customerid, app, service, path, instanceid);
			// store a req-resp analysis match result for the mock request (during replay)
			// and the matched recording request
			mockRequest.ifPresent(mRequest -> respv.reqid.ifPresent(recordReqId -> {
				Analysis.ReqRespMatchResult matchResult =
                    new Analysis.ReqRespMatchResult(Optional.of(recordReqId), mRequest.reqid.get(),
                        Comparator.MatchType.ExactMatch, 1, Comparator.MatchType.ExactMatch, "",
                        "", customerid, app, service, path, mRequest.collection.get(),
                        Utils.getTraceId(respv.meta),
                        Utils.getTraceId(mRequest.hdrs));
				rrstore.saveResult(matchResult);
			}));
		    return builder.entity(respv.body).build();
	    }).orElseGet(() -> {
				// Increment not match counter in cache
				replayResultCache.incrementReqNotMatchCounter(customerid, app, service, path, instanceid);
				//TODO this is a hack : as ReqRespMatchResult is calculated from the perspective of
				//a recorded request, here in the mock we have a replay request which did not match
				//with any recorded request, but still to properly calculate no match counts for
				// virtualized services in facet queries, we are creating this dummy req resp
				// match result for now.
				mockRequest.ifPresent(mRequest -> {
					Analysis.ReqRespMatchResult matchResult =
                        new Analysis.ReqRespMatchResult(Optional.empty(), mRequest.reqid.get(),
                            Comparator.MatchType.NoMatch, 0, Comparator.MatchType.Default, "", "",
                            customerid, app, service, path, mRequest.collection.get(), Optional.empty(),
                            Utils.getTraceId(mRequest.hdrs));
					rrstore.saveResult(matchResult);
				});
				return	Response.status(Response.Status.NOT_FOUND).entity("Response not found").build();
	    });
	    
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
		this.requestComparatorCache = config.requestComparatorCache;
		this.replayResultCache = config.replayResultCache;
		LOGGER.info("Cube mock service started");
	}


	private ReqRespStore rrstore;
	private ObjectMapper jsonmapper;
	private RequestComparatorCache requestComparatorCache;
	private ReplayResultCache replayResultCache;
	private static String tracefield = Config.DEFAULT_TRACE_FIELD;
	
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
