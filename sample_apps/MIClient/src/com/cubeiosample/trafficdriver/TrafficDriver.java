package com.cubeiosample.trafficdriver;

import java.net.URI;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;


public class TrafficDriver {
  //private static String MINFO_URI = "http://localhost:8080/MIRest/minfo/";
  private static String MINFO_URI = "http://a8a48b951150f11e99e65021e7b28c68-286862219.us-east-2.elb.amazonaws.com/minfo/";

  
  public static void main(String[] args) {
    ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, 100000)  // timing out with default 20000 ms
                .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    Client client = ClientBuilder.newClient(clientConfig);
    WebTarget service = client.target(MINFO_URI);

    // TODO: ideally, start separate threads.
    // User flow 1: rent movies
    FindAndRentMovies frm = new FindAndRentMovies(service);
    frm.DriveTraffic();
      
    // User flow 2: check dues and pay them
    // User flow 3: 
  }

 
}