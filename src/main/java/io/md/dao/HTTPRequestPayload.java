/*
 *
 *    Copyright Cube I O
 *
 */
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

import io.md.utils.HttpRequestPayloadDeserializer;
import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
@JsonDeserialize(using = HttpRequestPayloadDeserializer.class)
public class HTTPRequestPayload extends LazyParseAbstractPayload implements RequestPayload {

	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPRequestPayload.class);

	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> hdrs;
	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> queryParams; // query params
	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> formParams; // form params
	public String method;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	private byte[] body;
	static final String BODY = "body";
	public String path;
	static final String PATH_SEGMENTS = "pathSegments";

    /**
     *
     * @param hdrs
     * @param queryParams
     * @param formParams
     * @param method
     * @param body
     */
    // NOTE this constructor will be used only in the agent,
    // when creating the payload initially
    public HTTPRequestPayload(@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
	    @JsonProperty("queryParams") MultivaluedMap<String, String> queryParams,
	    @JsonProperty("formParams") MultivaluedMap<String, String> formParams,
	    @JsonProperty("method") String method,
	    @JsonProperty("body") byte[] body, @JsonProperty("path") String path) {
	    this.hdrs = Utils.setLowerCaseKeys(hdrs);
	    this.queryParams = queryParams;
	    this.formParams = formParams;
	    this.method = method;
	    this.body = body;
	    this.path = path;
    }


    // Once the payload has been serialized, only this constructor will be called
    // from within the deserializer
	public HTTPRequestPayload(JsonNode deserializedJsonTree) {
    	super(deserializedJsonTree);
		this.queryParams = this.dataObj.getValAsObject("/".concat("queryParams"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		this.formParams =  this.dataObj.getValAsObject("/".concat("formParams"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		this.hdrs =  this.dataObj.getValAsObject("/".concat("hdrs"),
			MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		try {
			this.method = this.dataObj.getValAsString("/".concat("method"));
		} catch (PathNotFoundException e) {
			this.method = "GET";
		}
		// to unwrap the body if not already
		postParse();
	}

	@JsonIgnore
	public byte[] getBody() {
		if (this.body != null && !(this.body.length == 0)) {
			return body;
		} else if (!this.dataObj.isDataObjEmpty()) {
			try {
				this.dataObj.wrapAsString("/".concat(BODY),
					Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
				return this.dataObj.getValAsByteArray("/".concat(BODY));
			} catch (PathNotFoundException e) {
				//do nothing
			}
		}
		return new byte[]{};
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws NotImplementedException {
		throw new NotImplementedException("Payload can be accessed as a json string");
	}

	@Override
	public String rawPayloadAsString()
		throws RawPayloadProcessingException {
		return this.rawPayloadAsString(false);
	}

	public String rawPayloadAsString(boolean wrapForDisplay) throws
		NotImplementedException, RawPayloadProcessingException {
		try {
			if (this.dataObj.isDataObjEmpty()) {
				return mapper.writeValueAsString(this);
			} else {
				if (wrapForDisplay) this.dataObj.wrapAsString("/".concat(BODY),
					Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
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
		if (!this.dataObj.isDataObjEmpty()) {
			this.dataObj.unwrapAsJson("/".concat(BODY),
				Utils.getMimeType(hdrs).orElse(MediaType.TEXT_PLAIN));
			try {
				String[] pathSplits = this.dataObj.getValAsString("/path").split("/");
				ObjectNode root = (ObjectNode) this.dataObj.objRoot;
				ArrayNode pathArrayNode = JsonNodeFactory.instance.arrayNode();
				Arrays.stream(pathSplits).forEach(pathSegment ->
					pathArrayNode.add(JsonNodeFactory.instance.textNode(pathSegment)));
				root.set(PATH_SEGMENTS , pathArrayNode);
			} catch (PathNotFoundException e) {
				LOGGER.error("Unable to split api path into segments " + e.getMessage());
			}
		}
	}
}
