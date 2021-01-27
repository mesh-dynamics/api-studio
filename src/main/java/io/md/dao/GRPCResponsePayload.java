package io.md.dao;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.md.logger.LogMgr;
import io.md.utils.GPRCResponsePayloadDeserialzer;

@JsonDeserialize(using = GPRCResponsePayloadDeserialzer.class)
public class GRPCResponsePayload extends GRPCPayload implements ResponsePayload {

	private static final Logger LOGGER = LogMgr.getLogger(GRPCResponsePayload.class);

	@JsonProperty("status")
	private Integer status;

	protected GRPCResponsePayload(MultivaluedMap<String, String> hdrs, byte[] body,
		 String path) {
		super(hdrs, body, path);
		this.status = 200;
	}

	public GRPCResponsePayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);

		// TODO Hot fix to set status as 200 everytime for any GRPC Response
		// this will need to be fixed later to set status corresponding to the actual
		// GPRC return code.
		try {
			((ObjectNode)this.dataObj.getRoot()).set("status"
				, JsonNodeFactory.instance.numberNode(200));
		}  catch (Exception e) {
			LOGGER.error("Unable to set status in GRPC Response payload " , e);
		}

	}

	@Override
	boolean isRequest() {
		return false;
	}

	// Grpc status code is always 200
	@Override
	public String getStatusCode() {
		return "200";
	}
}
