package com.cubeiosample.webservices.rest.jersey;

import io.cube.utils.RestUtils;
import io.opentracing.Tracer;

import java.util.Hashtable;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
  private Tracer tracer = null;
  
  final static Logger LOGGER = Logger.getLogger(RestOverSql.class);
  
  private static String RESTWRAPJDBC_URI = "http://restwrapjdbc:8080/restsql";
  Properties properties;
  private static final String CONFFILE = "conf/MIRest.conf";
  
  // private Hashtable<String, HeaderParams> requestHeaders = null;
  
  public RestOverSql(Tracer tracer) {
    ClientConfig clientConfig = new ClientConfig()
        .property(ClientProperties.READ_TIMEOUT, 100000)  
        .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    restClient = ClientBuilder.newClient(clientConfig);
    configureRestWrapUri();
    LOGGER.debug("RESTWRAPJDBC_URI is " + RESTWRAPJDBC_URI);
    restJDBCService = restClient.target(RESTWRAPJDBC_URI);
    
    this.tracer = tracer;
    initializeJDBCService();
    
    // requestHeaders = new Hashtable<String, HeaderParams>();
  }
  
  
//  public void addRequestHeaders(String signature, HeaderParams hd) {
//    requestHeaders.put(signature, hd);
//  }
//  
//  
//  public void removeRequestHeaders(String signature) {
//    requestHeaders.remove(signature);
//  }
  
  private void configureRestWrapUri() {
    // try the conf file and then the env. otherwise, default
    properties = new java.util.Properties();
    try {
      properties.load(this.getClass().getClassLoader().
          getResourceAsStream(CONFFILE));
      RESTWRAPJDBC_URI = properties.getProperty("RESTWRAPJDBC_URI");
    } catch (Exception e) {
      LOGGER.info("Conf file not found.");
      String rwUri = System.getenv("RESTWRAPJDBC_URI");
      if (rwUri != null) {
        RESTWRAPJDBC_URI = rwUri;
      }
    } 
  }
  
  
  private void initializeJDBCService() {
    String username = MovieRentals.userName();
    String pwd = MovieRentals.passwd();
    String uri = MovieRentals.baseUri();
    LOGGER.debug("init jdbc service tracer: ");
    LOGGER.debug(tracer.toString());
    Response response = RestUtils.callWithRetries(tracer, restJDBCService.path("initialize").queryParam("username", username).queryParam("password", pwd).queryParam("uri", uri).request(MediaType.APPLICATION_JSON), null, "GET", 3);
    LOGGER.debug("intialized jdbc service " + uri + "; " + username + "; " + response.getStatus() + "; "+ response.readEntity(String.class));
    response.close();
  }
  
  
  public String getHealth() {
    Response response = RestUtils.callWithRetries(tracer, restJDBCService.path("health").request(MediaType.APPLICATION_JSON), null, "GET", 3);
    String result = response.readEntity(String.class);
    response.close();
    return result;
  }
 
  
  public JSONArray executeQuery(String query, JSONArray params) {
    LOGGER.debug("Query: " + query + "; " + params.toString());    
    Response response = RestUtils.callWithRetries(tracer, 
        restJDBCService.path("query").queryParam("querystring", query).queryParam("params", UriComponent.encode(params.toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED)).request(MediaType.APPLICATION_JSON), 
        null, "GET", 3);
    JSONArray result = new JSONArray(response.readEntity(String.class));
    LOGGER.debug("Query: " + query + "; " + params.toString() + "; NumRows=" + result.length());
    response.close();
    return result;
  }
  
  
  public JSONObject executeUpdate(String query, JSONArray params) {
    JSONObject body = new JSONObject();
    body.put("query", query);
    body.put("params", params);
    Response response = RestUtils.callWithRetries(tracer, restJDBCService.path("update").request(), body, "POST", 3);
    
    // TODO: figure out the best way of extracting json array from the entity
    JSONObject result = new JSONObject(response.readEntity(String.class));
    LOGGER.debug("Update: " + query + "; " + params.toString() + "; " + result.toString());
    response.close();
    return result;
  }
  
  // parameter binding methods
  public static void addStringParam(JSONArray params, String value) throws JSONException {
    JSONObject param = new JSONObject();
    param.put("index", params.length() + 1);
    param.put("type", "string");
    param.put("value", value);
    params.put(param);
  }
 
  public static void addIntegerParam(JSONArray params, Integer value) throws JSONException {
    JSONObject param = new JSONObject();
    param.put("index", params.length() + 1);
    param.put("type", "integer");
    param.put("value", value);
    params.put(param);
  }

  public static void addDoubleParam(JSONArray params, Double value) throws JSONException {
    JSONObject param = new JSONObject();
    param.put("index", params.length() + 1);
    param.put("type", "double");
    param.put("value", value);
    params.put(param);
  }
  
}
