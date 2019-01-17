package com.cubeio.restwrapjdbc;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
	private static ConnectionPool jdbcPool = null;
	
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
  public Response initialize(@QueryParam("username") String username,
                             @QueryParam("password") String passwd,
                             @QueryParam("uri") String uri) {
    try {
      jdbcPool = new ConnectionPool();
      jdbcPool.setUpPool(uri, username, passwd);
      LOGGER.info("mysql uri: " + uri);
      LOGGER.info(jdbcPool.getPoolStatus());
      return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"status\": \"Connection pool created.\"}").build();
    } catch (Exception e) {
      LOGGER.error("connection pool creation failed; " + e.toString());
    }
    return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"result\": \"Initialization failed\"}").build();
  }
 

	@Path("/query")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response query(@QueryParam("querystring") String query,
	                      @QueryParam("params") String queryParams) {
		JSONArray result = null;
		JSONArray params = new JSONArray(queryParams);
		try {
		  result = jdbcPool.executeQuery(query, params);
		  return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
		} catch (Exception e) {
			LOGGER.error("Query failed: " + query + "; " + queryParams + "; " + e.toString());
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("{[\"result\": \"Query execution failed\"]}").build();
	}
	
	
  @Path("/update")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response update(String queryAndParamsStr) {
    JSONObject result = null;
    try {
      JSONObject queryAndParams = new JSONObject(queryAndParamsStr);
      JSONArray params = queryAndParams.getJSONArray("params");
      String query = queryAndParams.getString("query");
      LOGGER.debug("params: " + params.toString());
      result = jdbcPool.executeUpdate(query, params);
    } catch (Exception e) {
      result = new JSONObject();
      LOGGER.error("Update query failed: " + queryAndParamsStr + "; " + e.toString());
      result.put("num_updates", -10);
      result.put("exception", e.toString());
    } 
    LOGGER.debug("Update result:" + result.toString());
    return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
  }
	
}
