package com.cubeiosample.webservices.rest.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;


@Path("/minfo")
public class MovieRentalRest {
	@Path("/health")
    @GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok().type(MediaType.APPLICATION_JSON).entity("{\"status\": \"MovieInfo is healthy\"}").build();
    }

	@Path("/salesbystore")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSalesByStore(@QueryParam("storeid") String storeid,
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
	        obj = mv.GetSalesByStore(storeid);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: not clear why the return type here is not throwing an error.
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
		
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
    }

	@Path("/rentmovie")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response rentMovie(@QueryParam("filmid") int filmId,
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
        //HeaderParams hd = new HeaderParams(user, xreq, xtraceid, xspanid, xparentspanid, xsampled, xflags, xotspan);
        MovieRentals mv;
        JSONObject obj = new JSONObject();
        try {
			mv = new MovieRentals();
			double val = mv.RentMovie(filmId, storeId, duration, customerId, staffId);
	        obj.put("rental_amount", val);
	    } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.toString()).build();
		}
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(obj.toString()).build();
    }
}

	