/**
 * 
 */
package com.cube.ws;
import javax.ws.rs.*;



/**
 * @author prasad
 *
 */
@Path("/ts")
public class TestService  {

	@GET @Path("/score")
	@Produces("text/plain")
	public int getScore() {return 1;}

	
	@GET @Path("/score/wins")
	@Produces("text/plain")
	public int getWins() {return 2;}
}
