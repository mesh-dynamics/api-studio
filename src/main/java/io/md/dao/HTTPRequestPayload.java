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
public class HTTPRequestPayload extends HTTPPayload implements RequestPayload {

	private static final Logger LOGGER = LoggerFactory.getLogger(HTTPRequestPayload.class);

	@JsonDeserialize(as=MultivaluedHashMap.class)
	@JsonProperty("queryParams")
	private MultivaluedMap<String, String> queryParams; // query params
	@JsonProperty("formParams")
	@JsonDeserialize(as=MultivaluedHashMap.class)
	private MultivaluedMap<String, String> formParams; // form params
	@JsonProperty("method")
	private String method;
	@JsonProperty("path")
	private String path;

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
	    super(hdrs, body);
	    this.queryParams = queryParams;
	    this.formParams = formParams;
	    this.method = method;
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
		try {
			this.method = this.dataObj.getValAsString("/".concat("method"));
		} catch (PathNotFoundException e) {
			this.method = "GET";
		}
		// to unwrap the body if not already
		postParse();
	}

	@JsonIgnore
	public MultivaluedMap<String, String> getQueryParams() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			return this.dataObj.getValAsObject("/".concat("queryParams"),
				MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		}
		return queryParams;
	}

	@JsonIgnore
	public MultivaluedMap<String, String> getFormParams() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			return this.dataObj.getValAsObject("/".concat("formParams"),
				MultivaluedHashMap.class).orElse(new MultivaluedHashMap<>());
		}
		return formParams;
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
		return method;
	}

	@JsonIgnore
	public String getPath() {
		if (this.dataObj != null && !this.dataObj.isDataObjEmpty()) {
			try {
				return this.dataObj.getValAsString("/path");
			} catch (PathNotFoundException e) {
				return null;
			}
		}
		return path;
	}

	@Override
	public void postParse() {
    	super.postParse();
		if (!this.dataObj.isDataObjEmpty()) {
			try {
				String[] pathSplits = this.dataObj.getValAsString("/path").split("/" , -1);
				ObjectNode root = (ObjectNode) this.dataObj.objRoot;
				ArrayNode pathArrayNode = JsonNodeFactory.instance.arrayNode();
				Arrays.stream(pathSplits).forEach(pathSegment ->
					pathArrayNode.add(JsonNodeFactory.instance.textNode(pathSegment)));
				root.set(HTTPRequestPayload.PATH_SEGMENTS, pathArrayNode);
			} catch (PathNotFoundException e) {
				LOGGER.error("Unable to split api path into segments " + e.getMessage());
			}
		}
	}



}
