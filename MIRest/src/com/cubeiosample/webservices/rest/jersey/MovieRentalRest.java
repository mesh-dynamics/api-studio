package com.cubeiosample.webservices.rest.jersey;
// TODO: change the package name to com.cubeio.samples.MIRest

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import io.cube.utils.Tracing;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;
import io.opentracing.Tracer;


// TODO: @Secured decorators are all commented out since we have to modify request matching during replay and analysis to ignore certain fields.

@Path("/")
public class MovieRentalRest {
	final static Logger LOGGER;
	static MovieRentals mv;
	static ListMoviesCache lmc;
	static JaegerTracer tracer;
	static Config config;
	
	static {
		LOGGER = Logger.getLogger(MovieRentalRest.class);
		BasicConfigurator.configure();
	}

	static {
	  Scope scope = null;
		try {
		  tracer = Tracing.init("MIRest");
		  scope = tracer.buildSpan("startingup").startActive(true);
		  scope.span().setTag("starting-up", "MovieRentalRest");
		  LOGGER.debug("MIRest tracer: " + tracer.toString());
		  config = new Config();
		  mv = new MovieRentals(tracer, config);
		  lmc = new ListMoviesCache(mv, config);
		} catch (ClassNotFoundException e) {
			LOGGER.error("Couldn't initialize MovieRentals instance: " + e.toString());
		} finally {
		  scope.span().finish();
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
	  try (Scope scope = tracer.buildSpan("authenticate").startActive(true)) {
      scope.span().setTag("authenticate", username);
      
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
	//@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response listMovies(@QueryParam("filmName") String filmname,
							               @QueryParam("keyword") String keyword,
							               @QueryParam("actor") String actor,
							               @Context SecurityContext securityContext,
							               @Context HttpHeaders httpHeaders) {
//							               @HeaderParam("end-user") String user,
//							               @HeaderParam("x-request-id") String xreq,
//							               @HeaderParam("x-b3-traceid") String xtraceid,
//							               @HeaderParam("x-b3-spanid") String xspanid,
//							               @HeaderParam("x-b3-parentspanid") String xparentspanid,
//							               @HeaderParam("x-b3-sampled") String xsampled,
//							               @HeaderParam("x-b3-flags") String xflags,
//							               @HeaderParam("x-ot-span-context") String xotspan) {
		JSONArray films = null;
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "listmovies")) {

		  LOGGER.debug("list movies headers: " + httpHeaders.toString());
		  String listParams = filmname + ";" + keyword + ";" + actor;
		  scope.span().setTag("listmovies", listParams);
			films = lmc.getMovieList(filmname);
			if (films != null) {
				return Response.ok().type(MediaType.APPLICATION_JSON).entity(films.toString()).build();
			}
			films = lmc.getMovieList(keyword);
			if (films != null) {
        		return Response.ok().type(MediaType.APPLICATION_JSON).entity(films.toString()).build();
      		}
		} catch (Exception e) {
			LOGGER.error("ListMovies args: " + filmname + ", " + keyword + "; " + e.toString());
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		} 
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("[{}]").build();
	}
	
	
	@Path("/liststores")
	@GET
	//@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response findStoreswithFilm(@QueryParam("filmId") Integer filmId,
	                                   @Context HttpHeaders httpHeaders) {
		// NOTE: currently, our database is returning empty results for the foll. query. Hence, not using the zipcode.
		// select * from inventory, store, address where inventory.store_id = store.store_id and store.address_id = address.address_id and (postal_code is not null and length(postal_code) > 3)
		JSONArray stores = null;
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "listmovies")) {
      		scope.span().setTag("listmovies", filmId.toString());
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
	//@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response rentMovie(String rentalInfoStr,
	                          @Context HttpHeaders httpHeaders) {
//	    @HeaderParam("end-user") String user,
//	    @HeaderParam("x-request-id") String xreq,
//	    @HeaderParam("x-b3-traceid") String xtraceid,
//	    @HeaderParam("x-b3-spanid") String xspanid,
//	    @HeaderParam("x-b3-parentspanid") String xparentspanid,
//	    @HeaderParam("x-b3-sampled") String xsampled,
//	    @HeaderParam("x-b3-flags") String xflags,
//	    @HeaderParam("x-ot-span-context") String xotspan) {
	  try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "rentmovie")) {
      scope.span().setTag("rentmovie", rentalInfoStr);
	    JSONObject rentalInfo = new JSONObject(rentalInfoStr);
	    int filmId = rentalInfo.getInt("filmId");
	    int storeId = rentalInfo.getInt("storeId");
	    int customerId = rentalInfo.getInt("customerId");
	    int duration = rentalInfo.getInt("duration");
	    int staffId = rentalInfo.getInt("staffId");
	    LOGGER.debug("Rent movie params:" + rentalInfoStr + "; ");
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
  //@Secured
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response ReturnMovie(String returnInfoStr, @Context HttpHeaders httpHeaders) {
    JSONObject returnInfo = new JSONObject(returnInfoStr);
    int inventoryId = returnInfo.getInt("inventoryId");
    int userId = returnInfo.getInt("userId");
    int staffId = returnInfo.getInt("staffId");
    double rent = returnInfo.getDouble("rent");
 
    JSONObject result = null;
    try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "returnmovie")) {
      scope.span().setTag("returnmovie", returnInfoStr);
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
	//@Secured
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
	
	/*
	@Path("/ismovieavailable")
	@GET
	//@Secured
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
	//@Secured
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
	  JSONArray obj = null;
	  try {
	    obj = mv.getSalesByStore(storename);
    } catch (Exception e) {
      e.printStackTrace();
      return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
    } 
	  return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
  }
  */
	
	
}

	