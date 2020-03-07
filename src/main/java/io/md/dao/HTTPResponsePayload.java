/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonCreator;
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
public class HTTPResponsePayload extends LazyParseAbstractPayload {

	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> hdrs;
	public int status;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	private byte[] body;

	static String BODY = "body";

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
		if (hdrs != null) this.hdrs = Utils.setLowerCaseKeys(hdrs);
		this.status = status;
		this.body = body;
	}

	// Once the payload has been serialized, only this constructor will be called
	// from within the deserializer
	public HTTPResponsePayload(JsonNode node) {
		super(node);
		this.hdrs =  this.dataObj.getValAsObject("/".concat("hdrs"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		this.status =  this.dataObj.getValAsObject("/".concat("status"),
			Integer.class).orElse(-1);
		//this.body = null;
		postParse();
	}

	public byte[] getBody() {
		return body;
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException {
		throw new NotImplementedException("Payload can be accessed as a json string");
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadProcessingException {
		try {
			return this.dataObj.isDataObjEmpty() ? mapper.writeValueAsString(this) :
				dataObj.serializeDataObj();
		} catch (Exception e) {
			throw  new RawPayloadProcessingException(e);
		}
	}

	@Override
	public void postParse() {
		if (!this.dataObj.isDataObjEmpty()) {
			this.dataObj.unwrapAsJson("/".concat(BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
		}
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return false;
	}

}
