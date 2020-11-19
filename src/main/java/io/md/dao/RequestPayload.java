package io.md.dao;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;

public interface RequestPayload extends Payload {

	public String getMethod();

	public byte[] getBody();

	public MultivaluedMap<String, String> getQueryParams();


}
