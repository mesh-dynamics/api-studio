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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
public class HTTPRequestPayload extends LazyParseAbstractPayload {

	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> hdrs;
	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> queryParams; // query params
	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> formParams; // form params
	public String method;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	public byte[] body;

	static String BODY = "body";

    /**
     *
     * @param hdrs
     * @param queryParams
     * @param formParams
     * @param method
     * @param body
     */
    @JsonCreator
    public HTTPRequestPayload(@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
	    @JsonProperty("queryParams") MultivaluedMap<String, String> queryParams,
	    @JsonProperty("formParams") MultivaluedMap<String, String> formParams,
	    @JsonProperty("method") String method,
	    @JsonProperty("body") byte[] body) {
	    this.hdrs = Utils.setLowerCaseKeys(hdrs);
	    this.queryParams = queryParams;
	    this.formParams = formParams;
	    this.method = method;
	    this.body = body;
    }

	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException {
		throw new NotImplementedException("Payload can be accessed as a json string");
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadProcessingException {
		try {
			return mapper.writeValueAsString(this);
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
		if (!this.dataObj.isDataObjEmpty()) {
			this.dataObj.unwrapAsJson("/".concat(BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
		}
	}

	@Override
	public void syncFromDataObj() throws PathNotFoundException, DataObjProcessingException {
		if (!isDataObjEmpty()) {
			this.dataObj.wrapAsByteArray("/".concat(BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
			HTTPRequestPayload requestPayload = this.dataObj.convertToType(HTTPRequestPayload.class);
			this.hdrs = requestPayload.hdrs;
			this.formParams = requestPayload.formParams;
			this.queryParams = requestPayload.queryParams;
			this.body = requestPayload.body;
			this.method = requestPayload.method;
		}
	}
}
