package com.cubeiosample.webservices.rest.jersey;

import io.cube.utils.RestUtils;
import io.opentracing.Tracer;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.uri.UriComponent;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Properties;
import java.util.Random;

public class BookInfo {
    private Client restClient = null;
    //private WebTarget bookInfoService = null;
    private WebTarget bookDetailsService = null;
    private WebTarget bookRatingsService = null;
    private WebTarget bookReviewsService = null;
    private Tracer tracer = null;
    private Config config = null;

    final static Logger LOGGER = Logger.getLogger(BookInfo.class);
    private final Random random = new Random();

    private static String PRODUCTPAGE_URI = "http://productpage:9080";
    private static String BOOKDETAILS_URI = "http://details:9080";
    private static String BOOKRATINGS_URI = "http://ratings:9080";
    private static String BOOKREVIEWS_URI = "http://reviews:9080";

    public BookInfo(Tracer tracer, Config config) {
        ClientConfig clientConfig = new ClientConfig()
                .property(ClientProperties.READ_TIMEOUT, 100000)
                .property(ClientProperties.CONNECT_TIMEOUT, 10000);
        restClient = ClientBuilder.newClient(clientConfig);
        //bookInfoService = restClient.target(PRODUCTPAGE_URI);
        bookDetailsService = restClient.target(BOOKDETAILS_URI);
        bookRatingsService = restClient.target(BOOKRATINGS_URI);
        bookReviewsService = restClient.target(BOOKREVIEWS_URI);

        this.tracer = tracer;
        this.config = config;
    }

    // get book info
    public JSONObject getBookInfo(String title, int id) {
        // TODO: we are not using title yet because the product details api doesn't support it
        Response response = null;
        JSONObject bookInfo = new JSONObject();
    	JSONObject result = null;
        try {
            Double randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * config.FAIL_PERCENT_STD_DEV + config.FAIL_PERCENT;

        	// get details
            if (random.nextDouble() < randomGuassianPercentGivenStdDevAndMean) {
                JSONObject detailsResult = null;
                bookInfo.put("details", detailsResult);
            } else {
                response = RestUtils.callWithRetries(tracer,
                        bookDetailsService.path("details").path(String.format("%d", id)).request(MediaType.APPLICATION_JSON),
                        null, "GET", 3, config.ADD_TRACING_HEADERS);
                result = new JSONObject(response.readEntity(String.class));
                bookInfo.put("details", result);
            }
            
            // get ratings
            response = RestUtils.callWithRetries(tracer, 
        			bookRatingsService.path("ratings").path(String.format("%d", id)).request(MediaType.APPLICATION_JSON), 
        	   	    null, "GET", 3, config.ADD_TRACING_HEADERS);
            result = new JSONObject(response.readEntity(String.class));
            bookInfo.put("ratings", result);

            // get reviews
            response = RestUtils.callWithRetries(tracer, 
        			bookReviewsService.path("reviews").path(String.format("%d", id)).request(MediaType.APPLICATION_JSON), 
        	   	    null, "GET", 3, config.ADD_TRACING_HEADERS);
            result = new JSONObject(response.readEntity(String.class));
            bookInfo.put("reviews", result);
            
        	response.close();
  	    return result;
  	  } catch (Exception e) {
  		  LOGGER.error(String.format("getBookInfo failed: %s, params: %d; %s", title, id, e.toString()));
  		  if (response != null) {
  			  LOGGER.error(response.toString());
  		  }
  	  }
      return null;
    }


}
