package com.cubeiosample.trafficdriver;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;


public class TrafficDriver {
  //private static String MINFO_URI = "http://localhost:8080/MIRest/minfo/";
  private static String MINFO_URI = "http://192.168.99.103:31380/minfo/";

  public static Optional<Integer> strToInt(String s) {
    try {
      return Optional.ofNullable(Integer.valueOf(s));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  static {
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true"); // needed to set the Host header
  }

  public static void main(String[] args) {
    ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, 100000)  // timing out with default 20000 ms
                .property(ClientProperties.CONNECT_TIMEOUT, 10000);
    Client client = ClientBuilder.newClient(clientConfig);
    Optional<Integer> numMovies = Optional.empty();
    try {
        Map<String, String> headers = new HashMap<>();
    	if (args.length >= 1) {
    		MINFO_URI = args[0];
    	}
    	if (args.length >= 2) {
    	  numMovies = strToInt(args[1]);
        }
    	if (args.length >= 3) {
    	  // this argument is needed when driving traffic to minkube
          // the host header determines which namespace the request gets routed to
    	  String host = args[2];
    	  headers.put(HttpHeaders.HOST, host);
        }
      System.out.println("Setting URI to " + MINFO_URI);
      System.out.println("Num requests = " + numMovies);
        WebTarget service = client.target(MINFO_URI);

        // TODO: ideally, start separate threads.
        // User flow 1: rent movies
        FindAndRentMovies frm = new FindAndRentMovies(service, headers);
      frm.driveTraffic(numMovies);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
      
    // User flow 2: check dues and pay them
    // User flow 3: 
  }

 
}