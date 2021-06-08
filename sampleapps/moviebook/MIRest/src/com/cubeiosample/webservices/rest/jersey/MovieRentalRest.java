package com.cubeiosample.webservices.rest.jersey;
// TODO: change the package name to com.cubeio.samples.MIRest

import java.util.Map;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import javax.ws.rs.core.UriInfo;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import io.cube.utils.Tracing;
import io.jaegertracing.internal.JaegerTracer;
import io.opentracing.Scope;


// TODO: @Secured decorators are all commented out since we have to modify request matching during replay and analysis to ignore certain fields.

@Path("/")
public class MovieRentalRest {
	final static Logger LOGGER;
	static MovieRentals mv;
	static ListMoviesCache lmc;
	static JaegerTracer tracer;
	static Config config;

	private static StringBuffer twentykReviews = new StringBuffer();

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

		//cooked up reviews
		for (int i=0; i < 20000; i++) {
			// reviewer 1:
			twentykReviews.append(", {");
			twentykReviews.append("  \"reviewer\": \"Reviewer" + i + "\",");
			twentykReviews.append("  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"");
			twentykReviews.append("}");
		}
	}
	
	
	@Path("/health")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response health() {
	  return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"MIRest status\": \"MovieInfo is healthy\"}").build();
  }


	@Path("/secure/health")
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response secure_health() {
		return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"MIRest status\": \"MovieInfo is healthy\"}").build();
	}


	// TODO: createuser API
	
	@Path("/login")
	@POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response authenticateUser(@FormParam("username") String username,
                                   @FormParam("password") String password, @Context HttpHeaders httpHeaders) {
	  try (Scope scope = Tracing.startServerSpan(tracer, httpHeaders , "login")) {
      scope.span().setTag("login", username);

	    Authenticator.authenticate(username, password, mv);

	    String token = Authenticator.issueToken(username);

	    return Response.ok().entity(Map.of("token", "Bearer " + token)).build();
	    
	  } catch (Exception e) {
	    return Response.status(Status.BAD_REQUEST).entity(Map.of("error", e.getMessage())).build();
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
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response findStoreswithFilm(@QueryParam("filmId") Integer filmId,
	                                   @Context HttpHeaders httpHeaders) {
		// NOTE: currently, our database is returning empty results for the foll. query. Hence, not using the zipcode.
		// select * from inventory, store, address where inventory.store_id = store.store_id and store.address_id = address.address_id and (postal_code is not null and length(postal_code) > 3)
		JSONArray stores = null;
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "liststores")) {
      		scope.span().setTag("liststores", filmId.toString());
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
  @Secured
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

	@Path("/reviewslist")
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response listReviews (@QueryParam("count") Integer reviewsCount) {
		long starttime = System.currentTimeMillis();
		StringBuffer reviews = new StringBuffer();

		StringBuffer reviewResult = new StringBuffer("{");
		reviewResult.append("\"id\": \"69\",") ;
		reviewResult.append("\"reviews\": [");
		// reviewer 1:
		reviewResult.append("{");
		reviewResult.append("\"reviewer\": \"Reviewer1\",");
		reviewResult.append("  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"");
		reviewResult.append("}");

		if (reviewsCount < 20000) {
			//It should be a cooked up reviews
			for (int i=0; i < reviewsCount; i++) {
				// reviewer 1:
				reviews.append(", {");
				reviews.append("  \"reviewer\": \"Reviewer" + i + "\",");
				reviews.append("  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"");
				reviews.append("}");
			}
			reviewResult.append(reviews);
		} else {
				int howmany20ks = reviewsCount / 20000;
				int remaining = reviewsCount % 20000;
				if (howmany20ks > 0) {
					for (int i =0; i < howmany20ks; i++) {
						reviews.append(twentykReviews);
					}
					for (int i=0; i < remaining; i++) {
						// reviewer 1:
						reviews.append(", {");
						reviews.append("  \"reviewer\": \"Reviewer" + i + "\",");
						reviews.append("  \"text\": \"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"");
						reviews.append("}");
					}
					reviewResult.append(reviews);
				}
		}

		reviewResult.append("]");
		reviewResult.append("}");
		JSONObject result = new JSONObject(reviewResult.toString());
		long endtime = System.currentTimeMillis();
		LOGGER.info("Time took to construct review response (in ms) :" + (endtime-starttime));
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(result.toString()).build();
	}

	@POST
	@Path("/updateInventory/{number}")
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateInventory(@PathParam("number") int number, @Context HttpHeaders httpHeaders) {
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "updateInventory")) {
			scope.span().setTag("updateInventory", number);
			int result = mv.updateInventory(number);
			return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"result\":\"" + result + "\"}").build();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error while updating the inventory table");
			return Response.serverError().type(MediaType.APPLICATION_JSON).entity("{\"error\":\"" + e.toString() + "\"}").build();
		}
	}

	@POST
	@Path("/genre-group")
	@Secured
	public Response createUpdateGenreGroup(GenreGroupDTO genreGroupDTO, @Context HttpHeaders httpHeaders, @Context SecurityContext securityContext) {
		String username = securityContext.getUserPrincipal().getName();
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "createGenreGroup")) {
			scope.span().setTag("createGenreGroup", "createGenreGroup");
			int customerId = mv.getCustomerId(username);
			JSONObject obj;
			/**
			 * If will be used for put
			 */
			if(genreGroupDTO.id != null) {
				obj = mv.getGenreGroupById(genreGroupDTO.id);
				if(genreGroupDTO.name != null) {
					obj = mv.getGenreGroupForCustomerByName(genreGroupDTO.name, customerId);
					if(obj != null & obj.getInt("genre_group_id") != genreGroupDTO.id) {
						throw new Exception("Genre Group with same name already exists");
					}
					mv.updateGenreGroupName(genreGroupDTO.name, genreGroupDTO.id);
				}
			} else {
				if(genreGroupDTO.name == null) {
					throw new Exception("Genre Group name is Mandatory");
				}
				obj = mv.getGenreGroupForCustomerByName(genreGroupDTO.name, customerId);
				if(obj != null) {
					throw new Exception("Genre Group with same name already exists");
				}
				mv.createGenreGroup(genreGroupDTO.name, customerId);
				obj = mv.getGenreGroupForCustomerByName(genreGroupDTO.name, customerId);
			}
			int genreGroupId = obj.getInt("genre_group_id");
			mv.genre_group_category_mapping(genreGroupDTO.categories, genreGroupId);
			return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
		} catch (Exception e) {
			LOGGER.error("Error while creat/update the categoryGroup table");
			return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(Map.of("error", e.toString())).build();
		}
	}

	@GET
	@Path("/genre-groups")
	@Secured
	public Response getGenreGroups(@Context HttpHeaders httpHeaders, @Context SecurityContext securityContext) {
		String username = securityContext.getUserPrincipal().getName();
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "getGenreGroups")) {
			scope.span().setTag("getGenreGroups", "getGenreGroups");
			return Response.ok().type(MediaType.APPLICATION_JSON).entity(mv.getALlGenreGroupsForCustomer(username).toString()).build();
		} catch (Exception e) {
			LOGGER.error("Error while fetching the genreGroups ");
			return Response.serverError().type(MediaType.APPLICATION_JSON).entity(Map.of("error", e.toString())).build();
		}
	}

	@DELETE
	@Path("/delete-genre-group/{id}")
	@Secured
	public Response deleteGenreGroup(@Context HttpHeaders httpHeaders, @Context SecurityContext securityContext,
			@PathParam("id") int id) {
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "deleteGenreGroup")) {
			scope.span().setTag("deleteGenreGroup", "deleteGenreGroup");
			mv.deleteGenreGroup(id);
			return Response.ok().type(MediaType.APPLICATION_JSON).entity(Map.of("response", String.format("Genre group is deleted"))).build();
		} catch (Exception e) {
			LOGGER.error("Error while deleting the genreGroup ");
			return Response.serverError().type(MediaType.APPLICATION_JSON).entity(Map.of("error", e.toString())).build();
		}
	}

	@GET
	@Path("/getMovieList")
	@Secured
	public Response getMovies(@Context HttpHeaders httpHeaders, @Context SecurityContext securityContext, @Context UriInfo uriInfo) {
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "getMovies")) {
			scope.span().setTag("getMovies", "getMovies");
			Optional<Integer> genreGroupId =
					Optional.ofNullable(uriInfo.getQueryParameters().getFirst("genreGroupId")).map(Integer::valueOf);
			JSONArray movies = null;
			if(genreGroupId.isPresent()) {
				movies = mv.getMoviesForGroup(genreGroupId.get());
			} else {
				movies = mv.getAllMovies();
			}
			return Response.ok().type(MediaType.APPLICATION_JSON).entity(movies.toString()).build();
		} catch (Exception e) {
			LOGGER.error("Error while getting movies");
			return Response.serverError().type(MediaType.APPLICATION_JSON).entity(Map.of("error", e.toString())).build();
		}
	}

	@GET
	@Path("/categories")
	@Secured
	public Response getCategories(@Context HttpHeaders httpHeaders, @Context SecurityContext securityContext) {
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "getCategories")) {
			scope.span().setTag("getCategories", "getCategories");
			return Response.ok().type(MediaType.APPLICATION_JSON).entity(mv.getAllCategories().toString()).build();
		} catch (Exception e) {
			LOGGER.error("Error while fetching the categories ");
			return Response.serverError().type(MediaType.APPLICATION_JSON).entity(Map.of("error", e.toString())).build();
		}
	}

	@DELETE
	@Path("deleteRental")
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteRental(@Context HttpHeaders httpHeaders) {
		try (Scope scope =  Tracing.startServerSpan(tracer, httpHeaders , "deleteRental")) {
			scope.span().setTag("deleteRental", "delete all data from rental");
			int result = mv.deleteRental();
			return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"result\":\"" + result + "\"}").build();
		} catch (Exception e) {
			LOGGER.error("Error while deleting the rentals");
			return Response.serverError().type(MediaType.APPLICATION_JSON).entity("{\"err\":\"" + e.toString() + "\"}").build();
		}
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

	