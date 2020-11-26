package io.md.dao;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.utils.GRPCRequestPayloadDeserializer;

@JsonDeserialize(using = GRPCRequestPayloadDeserializer.class)
public class GRPCRequestPayload extends  GRPCPayload implements RequestPayload {

	public GRPCRequestPayload(MultivaluedMap<String, String> hdrs, byte[] body,
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

	@JsonIgnore
	public String getMethod() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			try {
				return this.dataObj.getValAsString("/".concat("method"));
			} catch (PathNotFoundException e) {
				return null;
			}
		}
		return methodName;
	}

	// Ideally there would be no queryparams for grpc case
	@Override
	public MultivaluedMap<String, String> getQueryParams() {
		return new MultivaluedHashMap<String, String>();
	}


}
