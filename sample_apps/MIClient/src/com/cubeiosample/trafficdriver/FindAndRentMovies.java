package com.cubeiosample.trafficdriver;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Random;
import org.json.*;

public class FindAndRentMovies {
	
	WebTarget targetService; 
	public FindAndRentMovies(WebTarget service) {
		targetService = service;
	}
	
	// Generated using query against the sakila db: 
	// select language_id,  group_concat(concat('"', title, '"') separator ', ') 
	// from film 
	// where film_id %23 = 0 
	// group by language_id;
	private static String[] movies = {"ANACONDA CONFESSIONS", "AUTUMN CROW", "BEVERLY OUTLAW", "BOWFINGER GABLES", "CAMPUS REMEMBER", "CHARIOTS CONSPIRACY", "CLUE GRAIL", "CORE SUIT", "DANGEROUS UPTOWN", "DIARY PANIC", "DRIFTER COMMANDMENTS", "ELEMENT FREDDY", "FACTORY DRAGON", "FLATLINERS KILLER", "GABLES METROPOLIS", "GONE TROUBLE", "HALF OUTFIELD", "HELLFIGHTERS SIERRA", "HOUSE DYNAMITE", "INNOCENT USUAL", "JERICHO MULAN", "LADY STAGE", "LONELY ELEPHANT", "MAJESTIC FLOATS", "MIDSUMMER GROUNDHOG", "MOSQUITO ARMAGEDDON", "NETWORK PEAK", "OSCAR GOLD", "PEACH INNOCENT", "POND SEATTLE", "RAINBOW SHOCK", "ROBBERY BRIGHT", "SALUTE APOLLO", "SHAKESPEARE SADDLE", "SLEEPLESS MONSOON", "SPIKING ELEMENT", "STRAIGHT HOURS", "TADPOLE PARK", "TORQUE BOUND", "UNBREAKABLE KARATE", "VILLAIN DESPERATE", "WEDDING APOLLO", "WORKING MICROCOSMOS"};
	
	//  Generating query: select language_id, group_concat(concat('"', substring_index(title, ' ', 1), '"') separator ', ') from film where film_id % 29 = 0 group by language_id
	private static String[] movie_keywords = {"ANTITRUST", "BEACH", "BOONDOCK", "CANDIDATE", "CHISUM", "CONFIDENTIAL", "DAISY", "DIRTY", "DUFFEL", "EVERYONE", "FISH", "GANDHI", "GREASE", "HAUNTING", "HOTEL", "INTENTIONS", "KANE", "LIFE", "MAIDEN", "MINE", "MUSCLE", "OPERATION", "PEACH", "PRIDE", "REQUIEM", "RUSHMORE", "SHANE", "SMOKING", "STAR", "SWARM", "TOWERS", "UPTOWN", "WAR", "WONKA"};
	
	private static int maxCustomerId = 599;
	private static int maxStaffId = 2;
	
	private static Random randGen = new Random();
	
	// get userid, storeid
	public void DriveTraffic() {
		// 
		for (String movie : movies) {
			// list films
			Response response1 = CallWithRetries(targetService.path("listmovies").queryParam("filmName", movie).request(MediaType.APPLICATION_JSON), null, true, 3);
			if (response1 == null || response1.getStatus() != 200) {
				continue;
			}
			System.out.println(response1.getStatus());
			
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
			Response response2 = CallWithRetries(targetService.path("liststores").queryParam("filmId", movieId).request(MediaType.APPLICATION_JSON), null, true, 3);
			if (response2 == null || response2.getStatus() != 200) {
				continue;
			}
			System.out.println(response2.getStatus());
			JSONArray stores = new JSONArray(response2.readEntity(String.class));
			response2.close();
			System.out.println(stores.toString());
			if (stores.length() == 0) {
				continue;
			}
			JSONObject storeObj = stores.getJSONObject(randGen.nextInt(stores.length()));
			System.out.println(storeObj.toString());
			int storeId = storeObj.getInt("store_id");
			
			// rent movie_id
			JSONObject rentalInfo = new JSONObject();
			rentalInfo.put("filmid", movieId);
			rentalInfo.put("storeid", storeId);
			rentalInfo.put("duration", 2);
			rentalInfo.put("customerid", 1 + randGen.nextInt(maxCustomerId-1));
			rentalInfo.put("staffid", 1 + randGen.nextInt(maxStaffId-1));   
			System.out.println("client rentalInfo: " + rentalInfo.toString());
			Response response3 = CallWithRetries(targetService.path("rentmovie").request(), rentalInfo, false, 3);
			if (response3 != null) {
				System.out.println(response3.getStatus());
			}
		}
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
