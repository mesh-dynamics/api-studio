package io.md.dao;

import java.util.Optional;

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
	static final String PAYLOADSTATEPATH = "/payloadState";

	public enum HTTPPayloadState {
		WrappedEncoded,
	    WrappedDecoded,
	    UnwrappedDecoded
	}

	@JsonDeserialize(as= MultivaluedHashMap.class)
	@JsonProperty("hdrs")
	protected MultivaluedMap<String, String> hdrs;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	@JsonProperty("body")
	protected byte[] body;

	// in case of bodies that need to be interpreted as string, there is no way to distinguish
	// between the case where the body is a base64 encoded body or decoded body. So we keep this
	// additional state to keep track of whether the body has already been unwrapped
	public HTTPPayloadState payloadState = HTTPPayloadState.WrappedEncoded;

	protected HTTPPayload(
		@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
		@JsonProperty("body") byte[] body) {
		if (hdrs != null) this.hdrs = Utils.setLowerCaseKeys(hdrs);
		this.body = body;
		this.payloadState = HTTPPayloadState.WrappedEncoded;
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
				String mimeType = Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN);
				if (wrapForDisplay && !Utils.startsWithIgnoreCase(mimeType,
					MediaType.MULTIPART_FORM_DATA)) {
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
		this.payloadState = HTTPPayloadState.WrappedEncoded;
		if (!this.dataObj.isDataObjEmpty()) {
			this.dataObj.getValAsObject(PAYLOADSTATEPATH, HTTPPayloadState.class)
				.ifPresent(v -> payloadState=v);
			unWrapBody();
		}
	}

	@JsonIgnore
	public byte[] getBody() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			try {
				wrapBody();
				return this.dataObj.getValAsByteArray("/".concat(BODY));
			} catch (PathNotFoundException e) {
				//do nothing
			}
		} else if (this.body != null && !(this.body.length == 0)) {
			return body;
		}
		return new byte[]{};
	}


	@JsonIgnore
	public MultivaluedMap<String, String> getHdrs() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			return this.dataObj.getValAsObject("/".concat("hdrs"),
				MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		}
		return hdrs;
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
		if (payloadState == HTTPPayloadState.UnwrappedDecoded) {
			this.dataObj.wrapAsString("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN) , Optional.empty());
			setPayloadState(HTTPPayloadState.WrappedDecoded);
		}
	}

	protected void unWrapBody() {
		// Currently unwrapAsJson does both decoding and unwrapping.
		// Will cleanup and separate the functions later
		if (payloadState == HTTPPayloadState.WrappedDecoded || payloadState == HTTPPayloadState.WrappedEncoded) {
			this.dataObj.unwrapAsJson("/".concat(HTTPRequestPayload.BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN), Optional.empty());
			setPayloadState(HTTPPayloadState.UnwrappedDecoded);
		}
	}

	/*
	 * this will update state both in this object and the parsed dataObj
	 */
	void setPayloadState(HTTPPayloadState payloadState) {
		this.payloadState = payloadState;
		try {
			dataObj.put(PAYLOADSTATEPATH, new JsonDataObj(payloadState, mapper));
		} catch (PathNotFoundException e) {
			LOGGER.error("Payload not an object, should not happen");
		}
	}

}
