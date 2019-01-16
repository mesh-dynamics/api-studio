package com.cubeiosample.trafficdriver;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Random;
import java.util.Scanner;


import org.json.*;


public class FindAndRentMovies {
	
	WebTarget targetService; 
	private static String token;
	
	public FindAndRentMovies(WebTarget service) {
		targetService = service;
	}
	
	// Generated using the following query against the sakila db: 
	// SELECT language_id,  group_concat(concat('"', title, '"') separator ', ') 
	// FROM film WHERE film_id %23 = 0 GROUP BY language_id;
	private static String[] movies = {"ANACONDA CONFESSIONS", "AUTUMN CROW", "BEVERLY OUTLAW", "BOWFINGER GABLES", "CAMPUS REMEMBER", "CHARIOTS CONSPIRACY", "CLUE GRAIL", "CORE SUIT", "DANGEROUS UPTOWN", "DIARY PANIC", "DRIFTER COMMANDMENTS", "ELEMENT FREDDY", "FACTORY DRAGON", "FLATLINERS KILLER", "GABLES METROPOLIS", "GONE TROUBLE", "HALF OUTFIELD", "HELLFIGHTERS SIERRA", "HOUSE DYNAMITE", "INNOCENT USUAL", "JERICHO MULAN", "LADY STAGE", "LONELY ELEPHANT", "MAJESTIC FLOATS", "MIDSUMMER GROUNDHOG", "MOSQUITO ARMAGEDDON", "NETWORK PEAK", "OSCAR GOLD", "PEACH INNOCENT", "POND SEATTLE", "RAINBOW SHOCK", "ROBBERY BRIGHT", "SALUTE APOLLO", "SHAKESPEARE SADDLE", "SLEEPLESS MONSOON", "SPIKING ELEMENT", "STRAIGHT HOURS", "TADPOLE PARK", "TORQUE BOUND", "UNBREAKABLE KARATE", "VILLAIN DESPERATE", "WEDDING APOLLO", "WORKING MICROCOSMOS"};
	
	//  Generating query: select language_id, group_concat(concat('"', substring_index(title, ' ', 1), '"') separator ', ') from film where film_id % 29 = 0 group by language_id
	private static String[] movie_keywords = {"ANTITRUST", "BEACH", "BOONDOCK", "CANDIDATE", "CHISUM", "CONFIDENTIAL", "DAISY", "DIRTY", "DUFFEL", "EVERYONE", "FISH", "GANDHI", "GREASE", "HAUNTING", "HOTEL", "INTENTIONS", "KANE", "LIFE", "MAIDEN", "MINE", "MUSCLE", "OPERATION", "PEACH", "PRIDE", "REQUIEM", "RUSHMORE", "SHANE", "SMOKING", "STAR", "SWARM", "TOWERS", "UPTOWN", "WAR", "WONKA"};
	
	private static int maxCustomerId = 599;
	private static int maxStaffId = 2;
	
	private static Random randGen = new Random();
	
	private void WarmMovieCache() {
	  for (String movie : movies) {
	    int rand = randGen.nextInt(100);
	    if (rand % 10 == 0) {
	      Response response = CallWithRetries(targetService.path("listmovies").queryParam("filmName", movie).request(MediaType.APPLICATION_JSON), null, true, 3);
	      response.close();
	    }
	  }
	}

	private void WaitForListenerDeploy() {
    Scanner scanner = new Scanner(System.in);
    scanner.nextLine();
    scanner.close();
	}
	
	public void GetToken() throws Exception {
	  Form form = new Form();
    form.param("username", "cube");
    form.param("password", "cubeio");
    
	  Response response = CallWithRetries(targetService.path("authenticate").request(MediaType.APPLICATION_JSON), Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), false, 3);
	  System.out.println(response.getStatus());
	  
	  System.out.println("header string: " + response.getHeaders());
    if (response.getHeaderString(HttpHeaders.AUTHORIZATION) == null) {
      throw new Exception();
    }
    token = response.getHeaderString(HttpHeaders.AUTHORIZATION);
	}

	public void DriveTraffic() throws Exception {
	  GetToken();
		WarmMovieCache();
		WaitForListenerDeploy();
		
		// play traffic for recording. 
		for (String movie : movies) {
			// list films
			Response response1 = CallWithRetries(targetService.path("listmovies").queryParam("filmName", movie).request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token), null, true, 3);
			if (response1 == null || response1.getStatus() != 200) {
				continue;
			}
			
			// pick one at random
			// TODO: couldn't directly read into JSONArray.class without the new JSONArray. 
			// Missing JSONArray exception. @prasad may know.
			JSONArray movies = new JSONArray(response1.readEntity(String.class));
			response1.close();
			if (movies.length() == 0) {
				continue;
			}
			int chosen_movie_indx = randGen.nextInt(movies.length());
			JSONObject movieObj = movies.getJSONObject(chosen_movie_indx);
			System.out.println(movieObj.toString());
			int movieId = movieObj.getInt("film_id");
			
			// find stores with movie
			Response response2 = CallWithRetries(targetService.path("liststores").queryParam("filmId", movieId).request(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, token), null, true, 3);
			if (response2 == null || response2.getStatus() != 200) {
			  System.out.println("list stores wasn't successful");
				continue;
			}
			JSONArray stores = new JSONArray(response2.readEntity(String.class));
			response2.close();
			if (stores.length() == 0) {
			  System.out.println("No store found for film " + movie);
				continue;
			}
			JSONObject storeObj = stores.getJSONObject(randGen.nextInt(stores.length()));
			System.out.println(storeObj.toString());
			int storeId = storeObj.getInt("store_id");
			
			// rent movie_id
			int userId = 1 + randGen.nextInt(maxCustomerId-1);
			int staffId = 1 + randGen.nextInt(maxStaffId-1);
			JSONObject rentalInfo = new JSONObject();
			rentalInfo.put("filmId", movieId);
			rentalInfo.put("storeId", storeId);
			rentalInfo.put("duration", 2);
			rentalInfo.put("customerId", userId);
			rentalInfo.put("staffId", staffId);   
			System.out.println("client rentalInfo: " + rentalInfo.toString());
			Response response3 = CallWithRetries(targetService.path("rentmovie").request().header(HttpHeaders.AUTHORIZATION, token), Entity.entity(rentalInfo.toString(), MediaType.APPLICATION_JSON), false, 1); // TOFIX: why is it retrying? is it timing out while debugging?
			JSONObject rentalResult = new JSONObject(response3.readEntity(String.class));
			response3.close();
			if (response3.getStatus() != 200) {
			  System.out.println("Rent movie failed or returned null response");
			  continue;
			} 
			
			System.out.println("rentmovie result: " + rentalResult.toString());
			int inventoryId = rentalResult.getInt("inventory_id");
			if (inventoryId < 0) {
			  System.out.println("Couldn't rent film: " + movieId + " @" + storeId);
			  continue;
			}
			
			int numUpdates = rentalResult.getInt("num_updates");
      if (numUpdates < 0) {
        System.out.println("Couldn't rent film: " + movieId + " @" + storeId);
      }

			// return movie
			// int inventoryId, int customerId, int staffId, double rent
			JSONObject returnMovieInfo = new JSONObject();
			returnMovieInfo.put("inventoryId", inventoryId);
			returnMovieInfo.put("userId", userId);
			returnMovieInfo.put("staffId", staffId);
			returnMovieInfo.put("rent", rentalResult.getDouble("rent"));  
			Response response4 = CallWithRetries(targetService.path("returnmovie").request().header(HttpHeaders.AUTHORIZATION, token), Entity.entity(returnMovieInfo.toString(), MediaType.APPLICATION_JSON), false, 1);
			JSONObject returnMovieResult = new JSONObject(response4.readEntity(String.class));
			response4.close();
			if (response4.getStatus() != 200) {
        System.out.println(response4.getStatus());
      }
      System.out.println("return movie result: " + returnMovieResult.toString() +"\n\n");
		}
	}
	
	
	private Response CallWithRetries(Builder req, Entity<Object> body, boolean isGetRequest, int numRetries) {
		int numAttempts = 0;
		while (numAttempts < numRetries) {
			try {
				if (isGetRequest) {
					return req.get();
				} 
				//return req.post(Entity.entity(body.toString(), MediaType.APPLICATION_JSON));
				return req.post(body);
			} catch (Exception e) {
			  System.out.println("request: " + req.toString() + "; exception: " + e.toString());
				++numAttempts;
			}
		}
		return null;
	}
}
