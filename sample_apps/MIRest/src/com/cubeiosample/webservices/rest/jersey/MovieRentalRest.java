package com.cubeiosample.webservices.rest.jersey;
// TODO: change the package name to com.cubeio.samples.MIRest

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;


@Path("/")
public class MovieRentalRest {
	final static Logger LOGGER;
	static MovieRentals mv;
	static ListMoviesCache lmc;
	
	static {
		LOGGER = Logger.getLogger(MovieRentalRest.class);
		BasicConfigurator.configure();
	}

	static {
		try {
			mv = new MovieRentals();
			lmc = new ListMoviesCache(mv);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Couldn't initialize MovieRentals instance: " + e.toString());
		}
	}
	
	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response health() {
	  return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"MIRest status\": \"MovieInfo is healthy\"}").build();
  }
	
	// TODO: createuser API
	
	@Path("/authenticate")
	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response authenticateUser(@FormParam("username") String username,
                                   @FormParam("password") String password) {
	  try {
          
	    Authenticator.authenticate(username, password);

	    String token = Authenticator.issueToken(username);

	    return Response.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
	    
	  } catch (Exception e) {
	    return Response.status(Response.Status.FORBIDDEN).build();
    }      
  }

	
	// User flow: Rent a movie
	// Find movies by title/keyword/genre/actor
	// Check available stores in zip code
	// Rent a movie for a certain duration
	// Pay for the rental
	// Return a movie
	@Path("/listmovies")
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response ListMovies(@QueryParam("filmName") String filmName,
							               @QueryParam("keyword") String keyword,
							               @QueryParam("actor") String actor,
							               @Context SecurityContext securityContext) {
		JSONArray films = null;
		//		Principal principal = securityContext.getUserPrincipal();
		//		String username = principal.getName();
		try {
			films = lmc.getMovieList(filmName);
			if (films != null) {
				return Response.ok().type(MediaType.APPLICATION_JSON).entity(films.toString()).build();
			}
			films = lmc.getMovieList(keyword);
			if (films != null) {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(films.toString()).build();
      }
		} catch (Exception e) {
			LOGGER.error("ListMovies args: " + filmName + ", " + keyword + "; " + e.toString());
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("[{}]").build();
	}
	
	
	@Path("/liststores")
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response findStoreswithFilm(@QueryParam("filmId") Integer filmId) {
		// NOTE: currently, our database is returning empty results for the foll. query. Hence, not using the zipcode.
		// select * from inventory, store, address where inventory.store_id = store.store_id and store.address_id = address.address_id and (postal_code is not null and length(postal_code) > 3)
		JSONArray stores = null;
		try {
			stores = mv.findAvailableStores(filmId);
		} catch (Exception e) {
			LOGGER.error("FindStoreswithFilm args: " + filmId + "; " + e.toString());
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
		if (stores == null) {
		  stores = new JSONArray("[{}]");
		}
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(stores.toString()).build();
	}
	
	
	@POST
	@Path("/rentmovie")
	@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response rentMovie(String rentalInfoStr, 
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
	    int filmId = rentalInfo.getInt("filmId");
	    int storeId = rentalInfo.getInt("storeId");
	    int customerId = rentalInfo.getInt("customerId");
	    int duration = rentalInfo.getInt("duration");
	    int staffId = rentalInfo.getInt("staffId");
	    if (filmId <= 0 || storeId <= 0 || customerId <= 0) {
	      return Response.serverError().type(MediaType.TEXT_PLAIN).entity("{\"Invalid query params\"}").build();
	    }
	    JSONObject result = mv.rentMovie(filmId, storeId, duration, customerId, staffId);
	    return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
	  } catch (Exception e) {
	    e.printStackTrace();
	    LOGGER.error("Rent movie params:" + rentalInfoStr + "; " + e.toString());
	    return Response.serverError().type(MediaType.TEXT_PLAIN).entity("{\"" + e.toString() + "\"}").build();
	  }
  }
	
	
	// Pay and return movies
  @Path("/returnmovie")
  @POST
  @Secured
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response ReturnMovie(String returnInfoStr) {
    JSONObject returnInfo = new JSONObject(returnInfoStr);
    int inventoryId = returnInfo.getInt("inventoryId");
    int userId = returnInfo.getInt("userId");
    int staffId = returnInfo.getInt("staffId");
    double rent = returnInfo.getDouble("rent");
 
    JSONObject result = null;
    try {
      LOGGER.debug("ReturnMovie Params: " + inventoryId + ", " + userId + ", " + staffId + ", " + rent);
      result = mv.returnMovie(inventoryId, userId, staffId, rent);
    } catch (Exception e) {
      result = new JSONObject("{\"Returnmovie didn't succeed\"}");
      LOGGER.error("Args: [" + inventoryId + ", " + userId + ", " + staffId + ", " + rent + "]; " + e.toString());
    }
    return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
  }
  
  
	//// FOLLOWING METHODS NOT CHECKED FOR CORRECTNESS. MOST LIKELY WON'T WORK WITHOUT SOME FIXING
	// Check due rentals
	@Path("/overduerentals")
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response OverdueRentals(@QueryParam("userid") int userId) {
		JSONArray dues = new JSONArray();
		try {
			dues = mv.findDues(userId);			
		} catch (Exception e) {
			e.printStackTrace();
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
    return Response.ok().type(MediaType.APPLICATION_JSON).entity(dues.toString()).build();
	}
	
	
	@Path("/ismovieavailable")
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response isFilmAvailableAtStore(@QueryParam("filmid") int filmId, 
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
//	  try {
//	    obj = mv.RentMovie(filmId, storeId, duration, customerId, staffId);
//	  } catch (Exception e) {
//	    LOGGER.error(e.toString());
//	    return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
//	  }
	  return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
	}
	
	
	// Corporate flow: check store value & productivity
	// Sales by store, number of rentals relative to inventory, #customers
	// Best performing genre
	@Path("/salesbystore")
	@GET
	@Secured
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
	    obj = mv.getSalesByStore(storename);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
    } 
	  return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
  }
	
}

	