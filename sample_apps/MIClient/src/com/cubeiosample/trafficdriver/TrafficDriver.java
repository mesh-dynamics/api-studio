package com.cubeiosample.trafficdriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;


public class TrafficDriver {
  //private static String MINFO_URI = "http://localhost:8080/MIRest/minfo/";
  private static String MINFO_URI = "http://192.168.99.103:31380/minfo/";

  
  public static void main(String[] args) {
    ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, 100000)  // timing out with default 20000 ms
                .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    Client client = ClientBuilder.newClient(clientConfig);
    try {
    	if (args.length >= 1) {
    		MINFO_URI = args[0];
    	}
        WebTarget service = client.target(MINFO_URI);

        // TODO: ideally, start separate threads.
        // User flow 1: rent movies
        FindAndRentMovies frm = new FindAndRentMovies(service);
      frm.driveTraffic();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      
    // User flow 2: check dues and pay them
    // User flow 3: 
  }

 
}