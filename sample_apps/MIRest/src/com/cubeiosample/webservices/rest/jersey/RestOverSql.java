package com.cubeiosample.webservices.rest.jersey;

import java.net.URI;

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
import org.json.JSONObject;

public class RestOverSql {
  private WebTarget restJDBCService = null;
  private static boolean LOCAL_RUN = true;
  final static Logger LOGGER = Logger.getLogger(RestOverSql.class);
  
  public RestOverSql() {
    ClientConfig clientConfig = new ClientConfig()
        .property(ClientProperties.READ_TIMEOUT, 100000)  
        .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    Client client = ClientBuilder.newClient(clientConfig);
    restJDBCService = client.target(getBaseURI());
    InitializeJDBCService();
  }
  
  private static URI getBaseURI() {
    if (LOCAL_RUN) {
      // war is not copied to the root of tomcat/catalina_home
      return UriBuilder.fromUri("http://localhost:8080/RestWrapperForJDBC/restsql/").build();
    }
    // assuming root.war is copied to $CATALINA_HOME
    return UriBuilder.fromUri("http://localhost:8080/restsql").build();
  }
  
  private void InitializeJDBCService() {
    String username = MovieRentals.userName();
    String pwd = MovieRentals.passwd();
    String uri = MovieRentals.baseUri();
    Response response = CallWithRetries(restJDBCService.path("initialize").queryParam("username", username).queryParam("password", pwd).queryParam("uri", uri).request(MediaType.APPLICATION_JSON), null, true, 3);
  }
  
  public String getHealth() {
    Response response = CallWithRetries(restJDBCService.path("health").request(MediaType.APPLICATION_JSON), null, true, 3);
    return response.getEntity().toString();
  }
  
  public JSONArray ExecuteQuery(String query, JSONArray params) {
//    JSONObject paramsObj = new JSONObject();
//    paramsObj.put("params", params);
    Response response = CallWithRetries(restJDBCService.path("query").queryParam("querystring", query).queryParam("params", UriComponent.encode(params.toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED)).request(MediaType.APPLICATION_JSON), null, true, 3);
    LOGGER.debug(response.toString());
    // TODO: figure out the best way of extracting json array from the entity
    JSONArray result = new JSONArray(response.getEntity().toString());
    LOGGER.debug(result.toString());
    return result;
  }
  
  public int ExecuteUpdate(String query, JSONArray params) {
    return -1;
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
        ++numAttempts;
      }
    }
    return null;
  }

}
