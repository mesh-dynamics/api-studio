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

import com.journaldev.router.model.Address;
import com.journaldev.router.model.Office;
import com.journaldev.router.model.Offices;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

@Path("/office")
public class OfficeRouter {

	private static Offices officeList = new Offices();

	static {
		officeList.getOfficeList().add(new Office(1, "USA", new Address("123 First Street", "New York", "01234")));

		officeList.getOfficeList().add(new Office(2, "UK", new Address("123 Second Street", "England", "01235")));

		officeList.getOfficeList().add(new Office(3, "Australia", new Address("123 Third Street", "Sydney", "01236")));
	}

	@GET
	@Path("/getDepth2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount) {
		final Span span = GlobalTracer.get().activeSpan();

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());

		String json = "{" + "\"Key\" : " + "\"HI from depth2\"" + "}";
//		String json = "HI from depth2";
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
	}

	@GET
	@Path("/getOfficeDetails/{officeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOfficeDetails(@Context HttpHeaders headers, @PathParam("officeId") int officeId) {
		System.out.println("Received called to office/getOfficeDetails");
		final Span span = GlobalTracer.get().activeSpan();

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());

		Optional<Office> office = officeList.getOfficeList().stream()
			.filter(ofz -> ofz.getOfficeId() == officeId).findFirst();

		return Response.ok().type(MediaType.APPLICATION_JSON).entity(office.get()).build();
	}


	@POST
	@Path("/postDepth2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount) {
		final Span span = GlobalTracer.get().activeSpan();

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());

		String json = "{" + "\"Key\" : " + "\"HI from depth2\"" + "}";
//		String json = "HI from depth2";
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
	}

}
