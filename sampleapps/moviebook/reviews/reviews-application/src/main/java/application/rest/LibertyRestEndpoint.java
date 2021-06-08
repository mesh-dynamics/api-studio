/*******************************************************************************
 * Copyright (c) 2017 Istio Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package application.rest;

import java.io.StringReader;
import java.util.Date;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class LibertyRestEndpoint extends Application {

    private final static Boolean ratings_enabled = Boolean.valueOf(System.getenv("ENABLE_RATINGS"));
    private final static String star_color = System.getenv("STAR_COLOR") == null ? "black" : System.getenv("STAR_COLOR");
    private final static String services_domain = System.getenv("SERVICES_DOMAIN") == null ? "" : ("." + System.getenv("SERVICES_DOMAIN"));
    private final static String ratings_hostname = System.getenv("RATINGS_HOSTNAME") == null ? "ratings" : System.getenv("RATINGS_HOSTNAME");
    private final static String ratings_service = "http://" + ratings_hostname + services_domain + ":9080/ratings";

    private Random random = new Random();
    private static Double FAIL_PERCENT = 0.01;
    private static Double FAIL_PERCENT_STD_DEV = 0.002;
    private static long TIME_BETWEEN_RUNS = 60000L;
    private static int NUMBER_OF_REVIEWS = 2;

    private long requestTimeStamp = new Date().getTime();
    private Double randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * FAIL_PERCENT_STD_DEV + FAIL_PERCENT;

    public static String reviews = "";

    static {
          String failPercent = System.getenv("FAIL_PERCENT");
        String failPercentStdDev = System.getenv("FAIL_PERCENT_STD_DEV");
        String timeBetweenRuns = System.getenv("TIME_BETWEEN_RUNS");
        String numberOfReviews = System.getenv("NUMBER_OF_REVIEWS");
        if (failPercent != null) {
            FAIL_PERCENT = Double.parseDouble(failPercent);
        }
        if (failPercentStdDev != null) {
            FAIL_PERCENT_STD_DEV = Double.parseDouble(failPercentStdDev);
        }
        if (timeBetweenRuns != null) {
            TIME_BETWEEN_RUNS = Long.parseLong(timeBetweenRuns);
        }

        if (numberOfReviews != null) {
            NUMBER_OF_REVIEWS = Integer.parseInt(numberOfReviews);
        }

        for (int i=0; i < NUMBER_OF_REVIEWS-2; i++) {
            // reviewer 1:
            reviews += ", {";
            reviews += "  \"reviewer\": \"Reviewer" + i + "\",";
            reviews += "  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"";
            if (ratings_enabled) {
                    reviews += ", \"rating\": {\"stars\": " + 3 + ", \"color\": \"" + star_color + "\"}";
            }
            reviews += "}";
        }
    }

    private String getJsonResponse (String productId, int starsReviewer1, int starsReviewer2) {
    	String result = "{";
    	result += "\"id\": \"" + productId + "\",";
    	result += "\"reviews\": [";
        // reviewer 1:
        result += "{";
        result += "  \"reviewer\": \"Reviewer1\",";
        result += "  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"";
        if (ratings_enabled) {
            if (starsReviewer1 != -1) {
                result += ", \"rating\": {\"stars\": " + starsReviewer1 + ", \"color\": \"" + star_color + "\"}";
            }
            else {
                result += ", \"rating\": {\"error\": \"Ratings service is currently unavailable\"}";
            }
        }
        result += "},";

        // reviewer 2:
        result += "{";
        result += "  \"reviewer\": \"Reviewer2\",";
        result += "  \"text\": \"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"";
        if (ratings_enabled) {
            if (starsReviewer2 != -1) {
                result += ", \"rating\": {\"stars\": " + starsReviewer2 + ", \"color\": \"" + star_color + "\"}";
            }
            else {
                result += ", \"rating\": {\"error\": \"Ratings service is currently unavailable\"}";
            }
        }
        result += "}";

        result += reviews;

    	result += "]";
    	result += "}";

    	return result;
    }
    
    private JsonObject getRatings(String productId, String user, String useragent, String xreq, String xtraceid, String xspanid,
                                  String xparentspanid, String xsampled, String xflags, String xotspan, String baggageParentId){
      ClientBuilder cb = ClientBuilder.newBuilder();
      String timeout = star_color.equals("black") ? "10000" : "2500";
      cb.property("com.ibm.ws.jaxrs.client.connection.timeout", timeout);
      cb.property("com.ibm.ws.jaxrs.client.receive.timeout", timeout);
      Client client = cb.build();
      WebTarget ratingsTarget = client.target(ratings_service + "/" + productId);
      Invocation.Builder builder = ratingsTarget.request(MediaType.APPLICATION_JSON);
      if(xreq!=null) {
        builder.header("x-request-id",xreq);
      }
      if(xtraceid!=null) {
        builder.header("x-b3-traceid",xtraceid);
      }
      if(xspanid!=null) {
        builder.header("x-b3-spanid",xspanid);
      }
      if(xparentspanid!=null) {
        builder.header("x-b3-parentspanid",xparentspanid);
      }
      if(xsampled!=null) {
        builder.header("x-b3-sampled",xsampled);
      }
      if(xflags!=null) {
        builder.header("x-b3-flags",xflags);
      }
      if(xotspan!=null) {
        builder.header("x-ot-span-context",xotspan);
      }
      if(user!=null) {
        builder.header("end-user", user);
      }
      if(useragent!=null) {
        builder.header("user-agent", useragent);
      }
      if (baggageParentId != null) {
        builder.header("baggage-parent-span-id", baggageParentId);
      }

      Response r = builder.get();
      int statusCode = r.getStatusInfo().getStatusCode();
      if (statusCode == Response.Status.OK.getStatusCode() ) {
        StringReader stringReader = new StringReader(r.readEntity(String.class));
        try (JsonReader jsonReader = Json.createReader(stringReader)) {
           JsonObject j = jsonReader.readObject();
           return j;
        }
      }else{
        System.out.println("Error: unable to contact "+ratings_service+" got status of "+statusCode);
        return null;
      }
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"status\": \"Reviews is healthy\"}").build();
    }

    @GET
    @Path("/reviews/{productId}")
    public Response bookReviewsById(@PathParam("productId") int productId,
                                    @HeaderParam("end-user") String user,
                                    @HeaderParam("user-agent") String useragent,
                                    @HeaderParam("x-request-id") String xreq,
                                    @HeaderParam("x-b3-traceid") String xtraceid,
                                    @HeaderParam("x-b3-spanid") String xspanid,
                                    @HeaderParam("x-b3-parentspanid") String xparentspanid,
                                    @HeaderParam("x-b3-sampled") String xsampled,
                                    @HeaderParam("x-b3-flags") String xflags,
                                    @HeaderParam("x-ot-span-context") String xotspan,
                                    @HeaderParam("baggage-parent-span-id") String baggageParentId) {
        /*
            Changing the random fail percent between runs.
            Ideally it should be updated with an API hook when a new replay starts.
            For now it is updated every 60 seconds assuming we dont run replays too often
         */
        long currentRequestTimeStamp = new Date().getTime();
        if (requestTimeStamp + TIME_BETWEEN_RUNS > currentRequestTimeStamp) {
            randomGuassianPercentGivenStdDevAndMean = random.nextGaussian() * FAIL_PERCENT_STD_DEV + FAIL_PERCENT;
        }
        requestTimeStamp = currentRequestTimeStamp;

        if (random.nextDouble() < randomGuassianPercentGivenStdDevAndMean) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .header(HttpHeaders.RETRY_AFTER, " :=120")
                    .build();
        }
      int starsReviewer1 = -1;
      int starsReviewer2 = -1;

      if (ratings_enabled) {
        JsonObject ratingsResponse = getRatings(Integer.toString(productId), user, useragent, xreq, xtraceid, xspanid, xparentspanid, xsampled, xflags, xotspan, baggageParentId);
        if (ratingsResponse != null) {
          if (ratingsResponse.containsKey("ratings")) {
            JsonObject ratings = ratingsResponse.getJsonObject("ratings");
            if (ratings.containsKey("Reviewer1")){
          	  starsReviewer1 = ratings.getInt("Reviewer1");
            }
            if (ratings.containsKey("Reviewer2")){
              starsReviewer2 = ratings.getInt("Reviewer2");
            }
          }
        }
      } 

      String jsonResStr = getJsonResponse(Integer.toString(productId), starsReviewer1, starsReviewer2);
      return Response.ok().type(MediaType.APPLICATION_JSON).entity(jsonResStr).build();
    }
}
