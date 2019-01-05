package com.cubeio.restwrapjdbc;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("/")
public class RestAPIForJDBC {
	final static Logger LOGGER;
	private ConnectionPool jdbcPool = null;
	
	static {
		LOGGER = Logger.getLogger(RestAPIForJDBC.class);
		BasicConfigurator.configure();
	}


	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response health() {
	  return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"status\": \"Rest wrapper for JDBC is healthy\"}").build();
	} 
	
 
	@Path("/initialize")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response Initialize(@QueryParam("username") String username,
                             @QueryParam("password") String passwd,
                             @QueryParam("uri") String uri) {
    try {
      jdbcPool = new ConnectionPool();
      LOGGER.info("mysql uri: " + uri);
      jdbcPool.setUpPool(uri, username, passwd);
      LOGGER.info(jdbcPool.getPoolStatus());
    } catch (Exception e) {
      LOGGER.error("connection pool creation failed; " + e.toString());
    }
    return Response.ok().type(MediaType.APPLICATION_JSON).entity("[{}]").build();
  }
 

	@Path("/query")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response Query(@QueryParam("querystring") String query,
	                      @QueryParam("params") String queryParams) {
		JSONObject result = null;
		JSONArray params = new JSONArray(queryParams);
		try {
		  result = jdbcPool.ExecuteQuery(query, params);
		  return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
		} catch (Exception e) {
			LOGGER.error(e.toString());
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("[{}]").build();
	}
	
  @Path("/update")
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  public Response Update(@QueryParam("querystring") String query, String qparams) {
    JSONArray params = new JSONArray(qparams);
    LOGGER.debug("params: " + params.toString() + " from qparams " + qparams);
    JSONObject res = new JSONObject();
    try {
      res.put("result", jdbcPool.ExecuteUpdate(query, params));
    } catch (Exception e) {
      LOGGER.error(e.toString());
      res.put("result", -10);
    } 
    return Response.ok().type(MediaType.APPLICATION_JSON).entity(res.toString()).build();
  }
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
