package io.md.dao;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.utils.GRPCRequestPayloadDeserializer;

@JsonDeserialize(using = GRPCRequestPayloadDeserializer.class)
public class GRPCRequestPayload extends  GRPCPayload implements RequestPayload {

	protected GRPCRequestPayload(MultivaluedMap<String, String> hdrs, byte[] body,
		String path) {
		super(hdrs, body, path);
	}

	public GRPCRequestPayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);
	}

	@Override
	boolean isRequest() {
		return true;
	}


}
