package io.md.dao;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.utils.GPRCResponsePayloadDeserialzer;

@JsonDeserialize(using = GPRCResponsePayloadDeserialzer.class)
public class GRPCResponsePayload extends GRPCPayload {


	protected GRPCResponsePayload(MultivaluedMap<String, String> hdrs, byte[] body,
		 String path) {
		super(hdrs, body, path);
	}

	public GRPCResponsePayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);
	}

	@Override
	boolean isRequest() {
		return false;
	}
}
