package io.md.dao;

import com.fasterxml.jackson.databind.JsonNode;

public interface RequestPayload extends Payload {

	public String getMethod();

	public byte[] getBody();

}
