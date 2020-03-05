package com.journaldev.router;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import com.journaldev.exception.EmpNotFoundException;
import com.journaldev.model.DepRequest;
import com.journaldev.model.DepResponse;
import com.journaldev.model.EmpRequest;
import com.journaldev.model.EmpResponse;
import com.journaldev.model.ErrorResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import com.cube.interceptor.jersey.egress.ClientLoggingFilter;
import com.cube.interceptor.jersey.egress.ClientTracingFilter;

@Path("/emp")
public class EmpRouter {

	@POST
	@Path("/getEmp")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmp(JAXBElement<EmpRequest> empRequest) throws EmpNotFoundException {
		EmpResponse empResponse = new EmpResponse();
		if (empRequest.getValue().getId() == 1) {
			empResponse.setId(empRequest.getValue().getId());
			empResponse.setName(empRequest.getValue().getName());
			try {
				String uri = "http://34.221.6.181:8082/dept/dept/getDept";
				DepRequest request = new DepRequest();
				// set id as 1 for OK response
				request.setId(1);
				request.setName("HR");
				Client client = Client.create();
				client.addFilter(new ClientLoggingFilter(client.getMessageBodyWorkers()));
				client.addFilter(new ClientTracingFilter());
				WebResource r = client.resource(uri);
				System.out.println("Dept service URI: "+uri);
				ClientResponse response = r.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, request);
				System.out.println(response.getStatus());
				if (response.getStatus() == 200) {
					DepResponse deptResponse = response.getEntity(DepResponse.class);
					//System.out.println(empResponse.getId() + "::" + empResponse.getName());
					System.out.println("Response name : " + deptResponse.getName());
					empResponse.setDeptName(deptResponse.getName());
				} else {
					ErrorResponse exc = response.getEntity(ErrorResponse.class);
					System.out.println(exc.getErrorCode());
					System.out.println(exc.getErrorId());
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		} else {
			throw new EmpNotFoundException("Wrong ID", empRequest.getValue().getId());
		}
		return Response.ok(empResponse).build();
	}
}
