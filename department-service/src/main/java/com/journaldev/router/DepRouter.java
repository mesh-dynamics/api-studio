package com.journaldev.router;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import com.journaldev.exception.DepNotFoundException;
import com.journaldev.model.DepRequest;
import com.journaldev.model.DepResponse;

@Path("/dept")
public class DepRouter {

	@POST
	@Path("/getDept")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDept(DepRequest depRequest) throws DepNotFoundException {
		DepResponse depResponse = new DepResponse();
		if (depRequest.getId() == 1) {
			depResponse.setId(depRequest.getId());
			depResponse.setName(depRequest.getName());
		} else {
			throw new DepNotFoundException("Wrong ID", depRequest.getId());
		}
		return Response.ok(depResponse).build();
	}
}
