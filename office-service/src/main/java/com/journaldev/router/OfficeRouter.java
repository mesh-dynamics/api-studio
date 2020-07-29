package com.journaldev.router;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;

@Path("/office")
public class OfficeRouter {

	@GET
	@Path("/getDepth2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDepth2(@Context HttpHeaders headers, @QueryParam("count") int studentCount) {
		final Span span = GlobalTracer.get().activeSpan();

		System.out.println("TraceId: " + span.context().toTraceId());
		System.out.println("ParentSpanId: " + headers.getRequestHeader("x-datadog-parent-id"));
		System.out.println("SpanId: " + span.context().toSpanId());

		String json = "{" + "\"Key\" : " + "HI from depth2" + "}";
//		String json = "HI from depth2";
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
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

		String json = "{" + "\"Key\" : " + "HI from postDepth2" + "}";
//		String json = "HI from depth2";
		return Response.ok().type(MediaType.APPLICATION_JSON).entity(json).build();
	}

}
