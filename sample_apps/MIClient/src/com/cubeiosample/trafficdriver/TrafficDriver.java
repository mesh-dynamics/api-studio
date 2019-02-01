package com.cubeiosample.trafficdriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;


public class TrafficDriver {
  //private static String MINFO_URI = "http://localhost:8080/MIRest/minfo/";
  private static String MINFO_URI = "http://aafb5ce36233811e9b9860291e25dda3-792383606.us-east-2.elb.amazonaws.com/minfo/";

  
  public static void main(String[] args) {
    ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, 100000)  // timing out with default 20000 ms
                .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    Client client = ClientBuilder.newClient(clientConfig);
    WebTarget service = client.target(MINFO_URI);

    // TODO: ideally, start separate threads.
    // User flow 1: rent movies
    FindAndRentMovies frm = new FindAndRentMovies(service);
    try {
      frm.driveTraffic();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      
    // User flow 2: check dues and pay them
    // User flow 3: 
  }

 
}