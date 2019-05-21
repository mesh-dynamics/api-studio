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

import java.util.Date;
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

    private Double randomGuassianPercentGivenStdDevAndMean;
    private long requestTimeStamp;

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

        randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * config.FAIL_PERCENT_STD_DEV + config.FAIL_PERCENT;
        requestTimeStamp = new Date().getTime();
    }

    // get book info
    public JSONObject getBookInfo(String title, int id) {
        // TODO: we are not using title yet because the product details api doesn't support it
        Response response = null;
        JSONObject bookInfo = new JSONObject();
    	JSONObject result = null;
        try {
            // get details
            /*
                Randomly not making the request call to 'details' rest application to mimic null response,
                unlike for the other two ('ratings' and 'reviews' apps, where the code to fail the response randomly is written).
                Ideally we can do in the details (ruby application) but since not familiar with the ruby syntax at this point of time, doing it here temporarily.
             */

            /*
                Changing the random fail percent between runs.
                Ideally it should be updated with an API hook when a new replay starts.
                For now it is updated every 60 seconds assuming we dont run replays too often
             */
            long currentRequestTimeStamp = new Date().getTime();
            if (requestTimeStamp + config.TIME_BETWEEN_RUNS > currentRequestTimeStamp) {
                LOGGER.debug("Random fail percent updated");
                randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * config.FAIL_PERCENT_STD_DEV + config.FAIL_PERCENT;
            }
            requestTimeStamp = currentRequestTimeStamp;

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
  	    return bookInfo;
  	  } catch (Exception e) {
  		  LOGGER.error(String.format("getBookInfo failed: %s, params: %d; %s", title, id, e.toString()));
  		  if (response != null) {
  			  LOGGER.error(response.toString());
  		  }
  	  }
      return null;
    }


}
