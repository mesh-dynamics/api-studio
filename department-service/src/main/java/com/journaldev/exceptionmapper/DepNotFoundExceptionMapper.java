package com.journaldev.exceptionmapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.journaldev.exception.DepNotFoundException;
import com.journaldev.model.ErrorResponse;

@Provider
public class DepNotFoundExceptionMapper implements ExceptionMapper<DepNotFoundException> {

	public DepNotFoundExceptionMapper() {
	}

	public Response toResponse(DepNotFoundException depNotFoundException) {
		ErrorResponse errorResponse = new ErrorResponse();
		errorResponse.setErrorId(depNotFoundException.getErrorId());
		errorResponse.setErrorCode(depNotFoundException.getMessage());
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse)
				.type(MediaType.APPLICATION_XML).build();

	}

}
