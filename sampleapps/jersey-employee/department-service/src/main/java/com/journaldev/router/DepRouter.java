/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.journaldev.router;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.journaldev.exception.DepNotFoundException;
import com.journaldev.model.DepRequest;
import com.journaldev.model.DepResponse;
import com.journaldev.model.Department;
import com.journaldev.model.Departments;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

@Path("/dept")
public class DepRouter {

	private static Departments departmentList = new Departments();

	static {
		departmentList.getDepartmentList().add(new Department(1, "Engineering"));

		departmentList.getDepartmentList().add(new Department(2, "HR"));

		departmentList.getDepartmentList().add(new Department(3, "Management"));
	}

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


	public static String fromEnvOrSystemProperties(String propertyName) {
//		Optional<String> or = Optional.ofNullable(System.getenv(propertyName)).or(() -> {
//			return Optional.ofNullable(System.getProperty(propertyName));
//		});
		if (System.getenv(propertyName) != null ) {
			return System.getenv(propertyName);
		} else if (System.getProperty(propertyName) != null ) {
			return System.getProperty(propertyName);
		}
		return "35.160.68.101:8082";
	}

	@GET
	@Path("/getDepartmentDetails/{departmentId}/{officeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDepartmentDetails(@Context HttpHeaders headers, @PathParam("departmentId") int departmentId, @PathParam("officeId") int officeId)
		throws DepNotFoundException {
		System.out.println("Received called to dept/getDepartmentDetails");

		final Span span = GlobalTracer.get().activeSpan();
		if (span != null) {
			// customer_id -> 254889
			// customer_tier -> platinum
			// cart_value -> 867
			span.setTag("customer.id2", "customer_id");
			span.setTag("customer.tier2", "customer_tier");
			span.setTag("cart.value2", "cart_value");
			span.setBaggageItem("CustomBaggage", "Replacing it here Beaches");
			span.setBaggageItem("Random_MT2", "asdasdsad");
			span.setBaggageItem("RAHS2", "asdasd ");
		}

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());

		Optional<Department> department = departmentList.getDepartmentList().stream()
			.filter(dept -> dept.getDepartmentId() == departmentId).findFirst();

		if (department.isPresent()) {

			String deptBaseURL = fromEnvOrSystemProperties("officeURL");
			String uri = deptBaseURL + "/office/office/getOfficeDetails/"+officeId;

			Client client = Client.create();
			WebResource r = client.resource(uri);
			System.out.println("Dept service URI: " + uri);
			ClientResponse response = r.accept(MediaType.APPLICATION_JSON)
				.type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			System.out.println(response.getStatus());

			System.out.println(
				"Recieved response from office/getOffice. \nStatus" + response.getStatus()
					+ "\nResponse: " + response.toString());

			ObjectMapper mapper = new ObjectMapper();
			String deptJson = "";
			try {
				deptJson = mapper.writeValueAsString(department.get());
			} catch (IOException e) {
				e.printStackTrace();
			}

			JSONObject finalObj = new JSONObject();
			try {
				JSONObject officeObj = new JSONObject(response.getEntity(String.class));
				JSONObject deptObj = new JSONObject(deptJson);
				finalObj.put("department", deptObj);
				finalObj.put("office", officeObj);

			} catch (JSONException e) {
				e.printStackTrace();
			}

			System.out.println("JSON Object : " + finalObj);

			return Response.ok().type(MediaType.APPLICATION_JSON)
				.entity(finalObj).build();
		} else {
			throw new DepNotFoundException("Wrong Department ID", departmentId);
		}
	}

	@GET
	@Path("/getDepth2")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount)
		throws URISyntaxException {
		System.out.println("Received called to dept/getDepth2");

		final Span span = GlobalTracer.get().activeSpan();
		if (span != null) {
			// customer_id -> 254889
			// customer_tier -> platinum
			// cart_value -> 867
			span.setTag("customer.id2", "customer_id");
			span.setTag("customer.tier2", "customer_tier");
			span.setTag("cart.value2", "cart_value");
			span.setBaggageItem("CustomBaggage", "Replacing it here Beaches");
			span.setBaggageItem("Random_MT2", "asdasdsad");
			span.setBaggageItem("RAHS2", "asdasd ");
		}

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());



		String deptBaseURL = fromEnvOrSystemProperties("officeURL");
		String uri = deptBaseURL+"/office/office/getDepth2";


		Client client = Client.create();
		WebResource r = client.resource(uri);
		System.out.println("Dept service URI: "+uri);
		ClientResponse response = r.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		System.out.println(response.getStatus());

		System.out.println(
			"Recieved response from office/getDepth2. \nStatus" + response.getStatus()
				+ "\nResponse: " + response.toString());
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(response.getEntity(String.class)).build();
	}


	@POST
	@Path("/postDepth2")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount)
		throws URISyntaxException {
		System.out.println("Received called to dept/postDepth2");

		final Span span = GlobalTracer.get().activeSpan();
		if (span != null) {
			// customer_id -> 254889
			// customer_tier -> platinum
			// cart_value -> 867
			span.setTag("customer.id2", "customer_id");
			span.setTag("customer.tier2", "customer_tier");
			span.setTag("cart.value2", "cart_value");
			span.setBaggageItem("CustomBaggage", "Replacing it here Beaches");
			span.setBaggageItem("Random_MT2", "asdasdsad");
			span.setBaggageItem("RAHS2", "asdasd ");
		}

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());



		String deptBaseURL = fromEnvOrSystemProperties("officeURL");
		String uri = deptBaseURL+"/office/office/postDepth2";


		Client client = Client.create();
		WebResource r = client.resource(uri);
		System.out.println("Dept service URI: "+uri);
		ClientResponse response = r.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
		System.out.println(response.getStatus());

		System.out.println(
			"Recieved response from office/postDepth2. \nStatus" + response.getStatus()
				+ "\nResponse: " + response.toString());
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(response.getEntity(String.class)).build();
	}

}
