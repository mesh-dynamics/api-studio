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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Properties;

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

import com.journaldev.exception.EmpNotFoundException;
import com.journaldev.model.Address;
import com.journaldev.model.DepRequest;
import com.journaldev.model.DepResponse;
import com.journaldev.model.EmpRequest;
import com.journaldev.model.EmpResponse;
import com.journaldev.model.Employee;
import com.journaldev.model.Employees;
import com.journaldev.model.ErrorResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

//import io.cube.interceptor.jersey_1x.egress.ClientLoggingFilter;
//import io.cube.interceptor.jersey_1x.egress.ClientMockingFilter;
//import io.cube.interceptor.jersey_1x.egress.ClientTracingFilter;

@Path("/emp")
public class EmpRouter {

	private static Employees employeeList;

	static {
		employeeList = new Employees();

		employeeList.getEmployeeList().add(new Employee(1, "Jane", "Lane", "Doe", "Jane Lane Doe", "true", "505.555.9999", "janedoe@example.com", "regular", new Address
			("123 Long Street", "Sunnyvale", "01234"),2, 3 ));

		employeeList.getEmployeeList().add(new Employee(2, "Jane2", "Lane2", "Doe2", "Jane2 Lane2 Doe2", "false", "505.555.8888", "jane2doe2@example.com", "regular", new Address
			("123 Short Street", "Santa Clara", "01235"),1, 1 ));

		employeeList.getEmployeeList().add(new Employee(3, "Jane3", "Lane3", "Doe3", "Jane3 Lane3 Doe3", "true", "505.555.7777", "jane3doe3@example.com", "regular", new Address
			("123 Mid Street", "San Francisco", "01236"),3, 2 ));
	}

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
				//String hostPort = fromEnvOrSystemProperties("depURL").orElse("35.160.68.101:8082");
				String deptBaseURL = fromEnvOrSystemProperties("depURL");
				String uri = deptBaseURL+"/dept/getDept";

				DepRequest request = new DepRequest();
				// set id as 1 for OK response
				request.setId(1);
				request.setName("HR");
				Client client = Client.create();
//				client.addFilter(new ClientMockingFilter());
//				client.addFilter(new ClientTracingFilter());
//				client.addFilter(new ClientLoggingFilter(client.getMessageBodyWorkers()));
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


	@GET
	@Path("/getEmployeeDetails/{employeeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEmployee(@Context HttpHeaders headers, @PathParam("employeeId") int employeeId) throws EmpNotFoundException {
		System.out.println("Received called to emp/getEmployeeDetails");

		final Span span = GlobalTracer.get().activeSpan();
		if (span != null) {
			// customer_id -> 254889
			// customer_tier -> platinum
			// cart_value -> 867
			span.setTag("customer.id", "customer_id");
			span.setTag("customer.tier", "customer_tier");
			span.setTag("cart.value", "cart_value");
			span.setBaggageItem("CustomBaggage", "LetsGetRollingBeaches");
			span.setBaggageItem("Random_MT", "asdasdsad");
			span.setBaggageItem("RAHS", "asdasd ");
		}

		System.out.println("Headers " + headers.getRequestHeaders());

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("SpanId: " + span.context().toSpanId());

		String resourceName = "myconfig.properties"; // could also be a constant
		Properties props = new Properties();
		try {
			try(InputStream resourceStream = EmpRouter.class.getClassLoader().getResourceAsStream(resourceName)) {
				props.load(resourceStream);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Optional<Employee> employee = employeeList.getEmployeeList().stream()
			.filter(emp -> emp.getEmployeeId() == employeeId).findFirst();

		if (employee.isPresent()) {

			String deptBaseURL = fromEnvOrSystemProperties("depURL");
			String uri = deptBaseURL+"/dept/dept/getDepartmentDetails/"+employee.get().getDepartmentId()+"/"+employee.get().getOfficeId();

			Client client = Client.create();
			WebResource r = client.resource(uri);
			System.out.println("Dept service URI: "+uri);
			ClientResponse response = r.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			System.out.println(response.getStatus());

			System.out.println(
				"Received response from dept/getDepartment. \nStatus" + response.getStatus()
					+ "\nResponse: " + response.toString());

			ObjectMapper mapper = new ObjectMapper();
			String empJson = "";
			try {
				empJson = mapper.writeValueAsString(employee.get());
			} catch (IOException e) {
				e.printStackTrace();
			}

			JSONObject finalObj = new JSONObject();
			try {
				JSONObject orgObj = new JSONObject(response.getEntity(String.class));
				JSONObject empObj = new JSONObject(empJson);
				finalObj.put("employee", empObj);
				finalObj.put("org", orgObj);
				if (props.getProperty("version").equalsIgnoreCase("v2")) {
					JSONObject metadata = new JSONObject();
					metadata.put("createdTime", ZonedDateTime.now().minusYears(5).toInstant());
					metadata.put("updatedTime", Instant.now());
					finalObj.put("metadata", metadata);
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}

			System.out.println("JSON Object : " + finalObj);

			return Response.ok().type(MediaType.APPLICATION_JSON).entity(finalObj).build();

		} else {
			throw new EmpNotFoundException("Wrong ID", employeeId);
		}
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
	@Path("/getDepth2")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount)
		throws URISyntaxException {
		System.out.println("Received called to emp/getDepth2");

		final Span span = GlobalTracer.get().activeSpan();
		if (span != null) {
			// customer_id -> 254889
			// customer_tier -> platinum
			// cart_value -> 867
			span.setTag("customer.id", "customer_id");
			span.setTag("customer.tier", "customer_tier");
			span.setTag("cart.value", "cart_value");
			span.setBaggageItem("CustomBaggage", "LetsGetRollingBeaches");
			span.setBaggageItem("Random_MT", "asdasdsad");
			span.setBaggageItem("RAHS", "asdasd ");
		}

		System.out.println("Headers " + headers.getRequestHeaders());

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("SpanId: " + span.context().toSpanId());


		String deptBaseURL = fromEnvOrSystemProperties("depURL");
		String uri = deptBaseURL+"/dept/dept/getDepth2";


		Client client = Client.create();
		WebResource r = client.resource(uri);
		System.out.println("Dept service URI: "+uri);
		ClientResponse response = r.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		System.out.println(response.getStatus());

		System.out.println(
			"Recieved response from dept/getDepth2. \nStatus" + response.getStatus()
				+ "\nResponse: " + response.toString());
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(response.getEntity(String.class)).build();
	}

	@POST
	@Path("/postDepth2")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount)
		throws URISyntaxException {
		System.out.println("Received called to emp/postDepth2");

		final Span span = GlobalTracer.get().activeSpan();
		if (span != null) {
			// customer_id -> 254889
			// customer_tier -> platinum
			// cart_value -> 867
			span.setTag("customer.id", "customer_id");
			span.setTag("customer.tier", "customer_tier");
			span.setTag("cart.value", "cart_value");
			span.setBaggageItem("CustomBaggage", "LetsGetRollingBeaches");
			span.setBaggageItem("Random_MT", "asdasdsad");
			span.setBaggageItem("RAHS", "asdasd ");
		}

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("SpanId: " + span.context().toSpanId());


		String deptBaseURL = fromEnvOrSystemProperties("depURL");
		String uri = deptBaseURL+"/dept/dept/postDepth2";


		Client client = Client.create();
		WebResource r = client.resource(uri);
		System.out.println("Dept service URI: "+uri);
		ClientResponse response = r.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).post(ClientResponse.class);
		System.out.println(response.getStatus());

		System.out.println(
			"Recieved response from dept/postDepth2. \nStatus" + response.getStatus()
				+ "\nResponse: " + response.toString());
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(response.getEntity(String.class)).build();
	}
}
