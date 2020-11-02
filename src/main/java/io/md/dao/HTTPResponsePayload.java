/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.constants.Constants;
import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.utils.HttpResponsePayloadDeserializer;
import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
@JsonDeserialize(using = HttpResponsePayloadDeserializer.class)
public class HTTPResponsePayload extends HTTPPayload implements ResponsePayload {

	@JsonProperty("status")
	private Integer status;

	/**
	 *
	 * @param hdrs
	 * @param status
	 * @param body
	 */
	// NOTE this constructor will be used only in the agent,
	// when creating the payload initially
	public HTTPResponsePayload(@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
		@JsonProperty("status") int status,
		@JsonProperty("body") byte[] body) {
		super(hdrs, body);
		this.status = status;
	}

	// Once the payload has been serialized, only this constructor will be called
	// from within the deserializer
	public HTTPResponsePayload(JsonNode node) {
		super(node);
		this.status =  this.dataObj.getValAsObject("/".concat("status"),
			Integer.class).orElse(-1);
		//this.body = null;
		postParse();
	}


	@Override
	@JsonIgnore
	public String getStatusCode() {
		return String.valueOf(status);
	}


	@JsonIgnore
	public Integer getStatus() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			return this.dataObj.getValAsObject("/".concat("status"),
				Integer.class).orElse(-1);
		}
		return status;
	}

	@Override
	public List<String> getPayloadFields() {
		return Arrays.asList(String.format("%s:%s", Constants.STATUS, getStatusCode()));
	}

}
