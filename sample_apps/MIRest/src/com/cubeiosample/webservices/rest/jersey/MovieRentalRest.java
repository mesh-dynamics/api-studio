package com.cubeiosample.webservices.rest.jersey;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


@Path("/")
public class MovieRentalRest {
	final static Logger LOGGER = Logger.getLogger(MovieRentalRest.class);
	
	@Path("/health")
    @GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"status\": \"MovieInfo is healthy\"}").build();
    }

	// User flow: Rent a movie
	// Find movies by title/keyword/genre/actor
	// Check available stores in zip code
	// Rent a movie for a certain duration
	// Pay for the rental
	// Return a movie
	@Path("/listmovies")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response ListMovies(@QueryParam("filmName") String filmName,
							    @QueryParam("keyword") String keyword,
							    @QueryParam("actor") String actor) {
		JSONArray films = null;
		try {
			// TODO: figure out a way to create a MovieRentals object without having to create it each call.
			MovieRentals mv = new MovieRentals();
			films = mv.ListMovies(filmName, keyword);
			if (films != null) {
				// TODO: couldn't return films directly; the client fails
				return Response.ok().type(MediaType.APPLICATION_JSON).entity(films.toString()).build();
			}
		} catch (Exception e) {
			LOGGER.error(e.toString());
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("[{}]").build();
	}
	
	
	@Path("/liststores")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response FindStoreswithFilm(@QueryParam("filmId") Integer filmId) {
		// NOTE: currently, our database is returning empty results for the foll. query. Hence, not using the zipcode.
		// select * from inventory, store, address where inventory.store_id = store.store_id and store.address_id = address.address_id and (postal_code is not null and length(postal_code) > 3)
		JSONArray stores = new JSONArray();
		try {
			MovieRentals mv = new MovieRentals();
			stores = mv.FindAvailableStores(filmId);
		} catch (Exception e) {
			LOGGER.error(e.toString());
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(stores.toString()).build();
	}
	
	
	@POST
	@Path("/rentmovie")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response RentMovie(String rentalInfoStr, 
    						  //JSONObject rentalInfo,
                              @HeaderParam("end-user") String user,
                              @HeaderParam("x-request-id") String xreq,
                              @HeaderParam("x-b3-traceid") String xtraceid,
                              @HeaderParam("x-b3-spanid") String xspanid,
                              @HeaderParam("x-b3-parentspanid") String xparentspanid,
                              @HeaderParam("x-b3-sampled") String xsampled,
                              @HeaderParam("x-b3-flags") String xflags,
                              @HeaderParam("x-ot-span-context") String xotspan) {
        //HeaderParams hd = new HeaderParams(user, xreq, xtraceid, xspanid, xparentspanid, xsampled, xflags, xotspan);
		
        try {
        	JSONObject rentalInfo = new JSONObject(rentalInfoStr);
			int filmId = rentalInfo.getInt("filmid");
			int storeId = rentalInfo.getInt("storeid");
			int customerId = rentalInfo.getInt("customerid");
			int duration = rentalInfo.getInt("duration");
			int staffId = rentalInfo.getInt("staffid");
			LOGGER.debug("filmid: " + filmId + " storeid: " + storeId + " duration: " + duration + " customerid: "+ customerId);
			if (filmId <= 0 || storeId <= 0 || customerId <= 0) {
				return Response.serverError().type(MediaType.TEXT_PLAIN).entity("{Invalid query params}").build();
			}
			
			MovieRentals mv = new MovieRentals();
			JSONObject obj = new JSONObject();
			double val = mv.RentMovie(filmId, storeId, duration, customerId, staffId);
	        obj.put("rental_amount", val);
	        Response.ok().type(MediaType.APPLICATION_JSON).entity(obj).build();
	    } catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{}").build();
    }
	
	// Check due rentals
	@Path("/overduerentals")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response OverdueRentals(@QueryParam("userid") int userId) {
		JSONArray dues = new JSONArray();
		try {
			MovieRentals mv = new MovieRentals();
			dues = mv.FindDues(userId);			
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(dues.toString()).build();
	}
	
	
	// Pay and return movies
	@Path("/returnmovie")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response ReturnMovie(@QueryParam("filmId") int filmId,
								@QueryParam("storeId") int storeId,
								@QueryParam("userId") int userId,
								@QueryParam("amount") double rent) {
		JSONObject result = new JSONObject();
		try {
			MovieRentals mv = new MovieRentals();
			int returnUpdate = mv.ReturnMovie(filmId, storeId, userId, rent);
			result.put("result", returnUpdate);
		} catch (Exception e) {
			LOGGER.error(e.toString());
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
	}
	
	
	
	@Path("/rentmovie")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response IsFilmAvailableAtStore(@QueryParam("filmid") int filmId, 
										   @QueryParam("storeid") int storeId,
										   @QueryParam("duration") int duration,
				                           @QueryParam("customerid") int customerId,
				                           @QueryParam("staffid") int staffId,
				                           @HeaderParam("end-user") String user,
				                           @HeaderParam("x-request-id") String xreq,
				                           @HeaderParam("x-b3-traceid") String xtraceid,
				                           @HeaderParam("x-b3-spanid") String xspanid,
				                           @HeaderParam("x-b3-parentspanid") String xparentspanid,
				                           @HeaderParam("x-b3-sampled") String xsampled,
				                           @HeaderParam("x-b3-flags") String xflags,
				                           @HeaderParam("x-ot-span-context") String xotspan) {
        JSONObject obj = new JSONObject();
        try {
			MovieRentals mv = new MovieRentals();
			double val = mv.RentMovie(filmId, storeId, duration, customerId, staffId);
	        obj.put("rental_amount", val);
	    } catch (Exception e) {
			LOGGER.error(e.toString());
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
	}
	
	
	// Corporate flow: check store value & productivity
	// Sales by store, number of rentals relative to inventory, #customers
	// Best performing genre
	@Path("/salesbystore")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSalesByStore(@QueryParam("storename") String storename,
                                    @HeaderParam("end-user") String user,
                                    @HeaderParam("x-request-id") String xreq,
                                    @HeaderParam("x-b3-traceid") String xtraceid,
                                    @HeaderParam("x-b3-spanid") String xspanid,
                                    @HeaderParam("x-b3-parentspanid") String xparentspanid,
                                    @HeaderParam("x-b3-sampled") String xsampled,
                                    @HeaderParam("x-b3-flags") String xflags,
                                    @HeaderParam("x-ot-span-context") String xotspan) {
        // HeaderParams hd = new HeaderParams(user, xreq, xtraceid, xspanid, xparentspanid, xsampled, xflags, xotspan);
		JSONArray obj = null;
		try {
	        MovieRentals mv = new MovieRentals();
	        obj = mv.GetSalesByStore(storename);
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
		
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
    }
	
}

	