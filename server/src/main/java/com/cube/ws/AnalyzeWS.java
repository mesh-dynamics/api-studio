/**
 * Copyright Cube I O
 */
package com.cube.ws;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT;

import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.CompareTemplate;
import com.cube.core.TemplateRegistries;
import com.cube.core.TemplateRegistry;
import com.cube.core.UtilException;
import com.cube.dao.Analysis;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.ReqRespStore;
import com.cube.drivers.Analyzer;

/**
 * @author prasad
 * The replay service
 */
@Path("/as")
public class AnalyzeWS {

    private static final Logger LOGGER = LogManager.getLogger(AnalyzeWS.class);


	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response health() {
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"Analysis service status\": \"AS is healthy\"}").build();
	}


	@POST
	@Path("analyze/{replayid}")
	@Consumes("application/x-www-form-urlencoded")
	public Response analyze(@Context UriInfo ui, @PathParam("replayid") String replayid,
			MultivaluedMap<String, String> formParams) {
		
		String tracefield = Optional.ofNullable(formParams.get("tracefield"))
				.flatMap(vals -> vals.stream().findFirst())
				.orElse(Config.DEFAULT_TRACE_FIELD);
		
		Optional<Analysis> analysis = Analyzer
				.analyze(replayid, tracefield, rrstore
						, jsonmapper , requestComparatorCache , responseComparatorCache );
		
		return analysis.map(av -> {
			String json;
			try {
				json = jsonmapper.writeValueAsString(av);
				return Response.ok(json, MediaType.APPLICATION_JSON).build();
			} catch (JsonProcessingException e) {
				LOGGER.error(String.format("Error in converting Analysis object to Json for replayid %s", replayid), e);
				return Response.serverError().build();
			}
		}).orElse(Response.serverError().build());		
	}


	@GET
	@Path("status/{replayid}")
	public Response status(@Context UriInfo ui,  
			@PathParam("replayid") String replayid) {
		
		Optional<Analysis> analysis = Analyzer.getStatus(replayid, rrstore);
		Response resp = analysis.map(av -> {
			String json;
			try {
				json = jsonmapper.writeValueAsString(av);
				return Response.ok(json, MediaType.APPLICATION_JSON).build();
			} catch (JsonProcessingException e) {
				LOGGER.error(String.format("Error in converting Analysis object to Json for replayid %s", replayid), e);
				return Response.serverError().build();
			}
		}).orElse(Response.status(Response.Status.NOT_FOUND).entity("Analysis not found for replayid: " + replayid).build());
		
		return resp;
	}

	@GET
	@Path("aggrresult/{replayid}")
	public Response getResultAggregate(@Context UriInfo ui,  
			@PathParam("replayid") String replayid) {
		
	    MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
	    Optional<String> service = Optional.ofNullable(queryParams.getFirst("service"));
	    boolean bypath = Optional.ofNullable(queryParams.getFirst("bypath"))
	    		.map(v -> v.equals("y")).orElse(false);

	    Collection<MatchResultAggregate> res = rrstore.getResultAggregate(replayid, service, bypath);
		String json;
		try {
			json = jsonmapper.writeValueAsString(res);
			return Response.ok(json, MediaType.APPLICATION_JSON).build();
		} catch (JsonProcessingException e) {
			LOGGER.error(String.format("Error in converting result aggregate object to Json for replayid %s", replayid), e);
			return Response.serverError().build();
		}		
	}

	@POST
	@Path("registerTemplateApp/{type}/{customerId}/{appId}")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response registerTemplateApp(@Context UriInfo uriInfo , @PathParam("type") String type,
										@PathParam("customerId") String customerId , @PathParam("appId") String appId,
										String templateRegistryArray) {
		try {
			//TODO study the impact of enabling this flag in other deserialization methods
			//jsonmapper.enable(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
			TemplateRegistries registries = jsonmapper.readValue(templateRegistryArray , TemplateRegistries.class);
			List<TemplateRegistry> templateRegistries = registries.getTemplateRegistryList();
			TemplateKey.Type templateKeyType;
			if ("request".equalsIgnoreCase(type)) {
				templateKeyType = TemplateKey.Type.Request;
			} else if ("response".equalsIgnoreCase(type)) {
				templateKeyType = TemplateKey.Type.Response;
			} else {
				return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid template type, should be " +
						"either request or response :: "+ type).build();
			}
			templateRegistries.forEach(UtilException.rethrowConsumer(registry -> {
				TemplateKey key = new TemplateKey(customerId , appId , registry.getService()
						, registry.getPath() , templateKeyType);
				rrstore.saveCompareTemplate(key , jsonmapper.writeValueAsString(registry.getTemplate()));
				requestComparatorCache.invalidateKey(key);
				responseComparatorCache.invalidateKey(key);
			}));
			return Response.ok().type(MediaType.TEXT_PLAIN).entity(type.concat(" Compare Templates Registered for :: ")
					.concat(customerId).concat(" :: ").concat(appId)).build();
		} catch (JsonProcessingException e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid JSON String sent " + e.getMessage()).build();
		} catch (Exception e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Error Occured " + e.getMessage()).build();
		}
	}




	/**
	 * Endpoint to save an analysis template as json in solr
	 * Will send appropriate Error Messages as response if unable to save
	 * Will overwrite any existing template against the same key
	 * @param urlInfo UrlInfo object
	 * @param appId Application Id
	 * @param customerId Customer Id
	 * @param serviceName Service Name
	 * @param templateAsJson Template As Json
	 * @return
	 */
	@POST
	@Path("registerTemplate/{type}/{customerId}/{appId}/{serviceName}/{path:.+}")
	@Consumes({MediaType.APPLICATION_JSON})
	public Response registerTemplate(@Context UriInfo urlInfo, @PathParam("appId") String appId,
									 @PathParam("customerId") String customerId,
									 @PathParam("serviceName") String serviceName,
									 @PathParam("path") String path,
									 @PathParam("type") String type,
									 String templateAsJson) {
    	try {
			//This is just to see the template is not invalid, and can be parsed according
			// to our class definition , otherwise send error response
    		CompareTemplate  template = jsonmapper.readValue(templateAsJson , CompareTemplate.class);
			TemplateKey key;
    		if ("request".equalsIgnoreCase(type)) {
				key = new TemplateKey(customerId, appId, serviceName, path , TemplateKey.Type.Request);
			} else if ("response".equalsIgnoreCase(type)) {
				key = new TemplateKey(customerId, appId, serviceName, path , TemplateKey.Type.Response);
			} else {
    			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid template type, should be " +
						"either request or response :: "+ type).build();
			}
			rrstore.saveCompareTemplate(key , templateAsJson);
			requestComparatorCache.invalidateKey(key);
			responseComparatorCache.invalidateKey(key);
			//Analyzer.removeKey(key);
			return Response.ok().type(MediaType.TEXT_PLAIN).entity("Json String successfully stored in Solr").build();
		} catch (JsonProcessingException e) {
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Invalid JSON String sent").build();
    	} catch (IOException e) {
    		return Response.serverError().type(MediaType.TEXT_PLAIN).entity("Error Occured " + e.getMessage()).build();
		}
	}



	/**
	 * @param config
	 */
	@Inject
	public AnalyzeWS(Config config) {
		super();
		this.rrstore = config.rrstore;
		this.jsonmapper = config.jsonmapper;
		this.requestComparatorCache = config.requestComparatorCache;
		this.responseComparatorCache = config.responseComparatorCache;
	}


	ReqRespStore rrstore;
	ObjectMapper jsonmapper;
	// Template cache to retrieve analysis templates from solr
	final RequestComparatorCache requestComparatorCache;
	final ResponseComparatorCache responseComparatorCache;
}
