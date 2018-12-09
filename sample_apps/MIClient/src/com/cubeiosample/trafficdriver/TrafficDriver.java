package com.cubeiosample.trafficdriver;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;


public class TrafficDriver {
  public static void main(String[] args) {

      ClientConfig clientConfig = new ClientConfig()
              .property(ClientProperties.READ_TIMEOUT, 30000)
              .property(ClientProperties.CONNECT_TIMEOUT, 5000);
      // Configuration config = new Configuration();
      // Client client = ClientBuilder.newClient(config);
      Client client = ClientBuilder.newClient(clientConfig);
      WebTarget service = client.target(getBaseURI());

      // TODO: ideally, start separate threads.
      // User flow 1: rent movies
      FindAndRentMovies frm = new FindAndRentMovies(service);
      frm.DriveTraffic();
      
      // User flow 2: check dues and pay them
      // User flow 3: 
  }

  private static URI getBaseURI() {
    return UriBuilder.fromUri("http://localhost:8080/MIRest/rest/minfo/").build();
  }
}