package io.md.dao;

import java.util.Arrays;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 11/08/20
 * Common parts of HTTPRequest and HTTPResponse
 */
public class HTTPPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPPayload.class);
	static final String BODY = "body";


	@JsonDeserialize(as= MultivaluedHashMap.class)
	public MultivaluedMap<String, String> hdrs;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	protected byte[] body;

	// in case of bodies that need to be interpreted as string, there is no way to distinguish
	// between the case where the body is a base64 encoded body or decoded body. So we keep this
	// additional flag to keep track of whether the body has already been unwrapped
	public boolean isBodyWrapped;

	protected HTTPPayload(
		@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
		@JsonProperty("body") byte[] body) {
		if (hdrs != null) this.hdrs = Utils.setLowerCaseKeys(hdrs);
		this.body = body;
		this.isBodyWrapped = true;
	}

	protected HTTPPayload(JsonNode deserializedJsonTree) {
		super(deserializedJsonTree);
		this.hdrs =  this.dataObj.getValAsObject("/".concat("hdrs"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
	}


	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException {
		throw new NotImplementedException("Payload can be accessed as a json string");
	}

	@Override
	public String rawPayloadAsString()
		throws RawPayloadProcessingException {
		parseIfRequired();
		return this.rawPayloadAsString(false);
	}

	@Override
	public long size() {
		return this.body != null ? this.body.length : 0;
	}

	public String rawPayloadAsString(boolean wrapForDisplay) throws
		NotImplementedException, RawPayloadProcessingException {
		try {
			if (this.dataObj.isDataObjEmpty()) {
				return mapper.writeValueAsString(this);
			} else {
				if (wrapForDisplay) {
					wrapBody();
				}
				return dataObj.serializeDataObj();
			}
		} catch (Exception e) {
			throw  new RawPayloadProcessingException(e);
		}
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return false;
	}

	@Override
	public void postParse() {
		this.isBodyWrapped = true;
		if (!this.dataObj.isDataObjEmpty()) {
			this.dataObj.getValAsObject("/isBodyWrapped", Boolean.class)
				.ifPresent(v -> isBodyWrapped=v);
			unWrapBody();
		}
	}

	@JsonIgnore
	public byte[] getBody() {
		if (this.body != null && !(this.body.length == 0)) {
			return body;
		} else if (this.dataObj!= null && !this.dataObj.isDataObjEmpty()) {
			try {
				wrapBody();
				return this.dataObj.getValAsByteArray("/".concat(BODY));
			} catch (PathNotFoundException e) {
				//do nothing
			}
		}
		return new byte[]{};
	}

	@Override
	public void updatePayloadBody() throws PathNotFoundException  {
		if (this.dataObj.isDataObjEmpty()) {
			return;
		} else {
			this.body = this.dataObj.getValAsByteArray("/".concat(BODY));
		}
	}

	protected void wrapBody() {
		if (!isBodyWrapped) {
			this.dataObj.wrapAsString("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
			isBodyWrapped = true;
		}
	}

	private void unWrapBody() {
		if (isBodyWrapped) {
			this.dataObj.unwrapAsJson("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
			isBodyWrapped = false;
		}
	}
}
