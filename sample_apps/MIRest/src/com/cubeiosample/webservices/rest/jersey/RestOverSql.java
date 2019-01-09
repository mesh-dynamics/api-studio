package com.cubeiosample.webservices.rest.jersey;

import java.net.URI;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.uri.UriComponent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RestOverSql {
  private Client restClient = null;
  private WebTarget restJDBCService = null;
  final static Logger LOGGER = Logger.getLogger(RestOverSql.class);
  private static boolean LOCAL_RUN = true;
  Properties properties;
  private static final String CONFFILE = "conf/MIRest.conf";
  
  public RestOverSql() {
    ClientConfig clientConfig = new ClientConfig()
        .property(ClientProperties.READ_TIMEOUT, 100000)  
        .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    restClient = ClientBuilder.newClient(clientConfig);
    LOGGER.debug("LOCAL_RUN is " + LOCAL_RUN + "; Rest jdbc service uri:" + getBaseURI());
    configureLocalRun();
    LOGGER.debug("LOCAL_RUN is " + LOCAL_RUN + "; Rest jdbc service uri:" + getBaseURI());
    restJDBCService = restClient.target(getBaseURI());
    InitializeJDBCService();
  }
  
  private static URI getBaseURI() {
    if (LOCAL_RUN) {
      // war is not copied to the root of tomcat/catalina_home
      return UriBuilder.fromUri("http://localhost:8080/RestWrapJDBC/restsql").build();
    }
    // assuming root.war is copied to $CATALINA_HOME
    return UriBuilder.fromUri("http://localhost:8080/restsql").build();
  }
  
  private void configureLocalRun() {
    properties = new java.util.Properties();
    String localRun = null;
    try {
      properties.load(this.getClass().getClassLoader().
          getResourceAsStream(CONFFILE));
      localRun = properties.getProperty("LOCAL_RUN");
    } catch (Exception e) {
      // ignore
      LOGGER.info("Conf file not found.");
    }
    if (localRun != null && localRun.equalsIgnoreCase("true")) {
      LOCAL_RUN = true;
    } 
  }
  
  private void InitializeJDBCService() {
    String username = MovieRentals.userName();
    String pwd = MovieRentals.passwd();
    String uri = MovieRentals.baseUri();
    Response response = CallWithRetries(restJDBCService.path("initialize").queryParam("username", username).queryParam("password", pwd).queryParam("uri", uri).request(MediaType.APPLICATION_JSON), null, true, 3);
    response.close();
  }
  
  
  public String getHealth() {
    Response response = CallWithRetries(restJDBCService.path("health").request(MediaType.APPLICATION_JSON), null, true, 3);
    String result = response.getEntity().toString();
    response.close();
    return result;
  }
  
  
  public JSONArray ExecuteQuery(String query, JSONArray params) {
    Response response = CallWithRetries(restJDBCService.path("query").queryParam("querystring", query).queryParam("params", UriComponent.encode(params.toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED)).request(MediaType.APPLICATION_JSON), null, true, 3);
    JSONArray result = new JSONArray(response.readEntity(String.class));
    LOGGER.debug("Query: " + query + "; " + params.toString() + "; NumRows=" + result.length());
    response.close();
    return result;
  }
  
  
  public JSONObject ExecuteUpdate(String query, JSONArray params) {
    JSONObject body = new JSONObject();
    body.put("query", query);
    body.put("params", params);
    Response response = CallWithRetries(restJDBCService.path("update").request(), body, false, 3);
    
    // TODO: figure out the best way of extracting json array from the entity
    JSONObject result = new JSONObject(response.readEntity(String.class));
    LOGGER.debug("Update: " + query + "; " + params.toString() + "; " + result.toString());
    response.close();
    return result;
  }
  
  // parameter binding methods
  public static void AddStringParam(JSONArray params, String value) throws JSONException {
    JSONObject param = new JSONObject();
    param.put("index", params.length() + 1);
    param.put("type", "string");
    param.put("value", value);
    params.put(param);
  }

  public static void AddIntegerParam(JSONArray params, Integer value) throws JSONException {
    JSONObject param = new JSONObject();
    param.put("index", params.length() + 1);
    param.put("type", "integer");
    param.put("value", value);
    params.put(param);
  }

  public static void AddDoubleParam(JSONArray params, Double value) throws JSONException {
    JSONObject param = new JSONObject();
    param.put("index", params.length() + 1);
    param.put("type", "double");
    param.put("value", value);
    params.put(param);
  }

    
  private Response CallWithRetries(Builder req, JSONObject body, boolean isGetRequest, int numRetries) {
    int numAttempts = 0;
    while (numAttempts < numRetries) {
      try {
        if (isGetRequest) {
          return req.get();
        } 
        return req.post(Entity.entity(body.toString(), MediaType.APPLICATION_JSON));
      } catch (Exception e) {
        LOGGER.error("request: " + req.toString() + "; exception: " + e.toString());
        ++numAttempts;
      }
    }
    return null;
  }

}
