package io.md.dao;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;

import io.md.constants.Constants;

public interface RequestPayload extends Payload {

	public String getMethod();

	public byte[] getBody();

	public MultivaluedMap<String, String> getQueryParams();

	default List<String> getPayloadFields() {
		return Arrays.asList(String.format("%s:%s", Constants.METHOD_PATH, getMethod()));
	}
}
