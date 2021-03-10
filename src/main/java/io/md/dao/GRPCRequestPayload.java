package io.md.dao;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.constants.Constants;
import io.md.utils.GRPCRequestPayloadDeserializer;

@JsonDeserialize(using = GRPCRequestPayloadDeserializer.class)
public class GRPCRequestPayload extends  GRPCPayload implements RequestPayload {
	@JsonProperty("method")
	private String method; // This is http method which in all likelhood is POST for grpc case

	public GRPCRequestPayload(MultivaluedMap<String, String> hdrs, byte[] body,
		String path, String method) {
		super(hdrs, body, path);
		this.method = method;
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
				return Constants.GRPC_DEFAULT_HTTP_MEHTOD; //grpc default
			}
		} else if(this.method!=null && !this.method.equals(""))
		{
			return this.method;
		}
		else {
			return Constants.GRPC_DEFAULT_HTTP_MEHTOD; //grpc default
		}
	}

	// Ideally there would be no queryparams for grpc case
	@Override
	public MultivaluedMap<String, String> getQueryParams() {
		return new MultivaluedHashMap<String, String>();
	}

}
