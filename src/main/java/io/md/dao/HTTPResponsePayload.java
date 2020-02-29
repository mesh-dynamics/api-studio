/*
 *
 *    Copyright Cube I O
 *
 */
package io.md.dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.constants.Constants;
import io.md.utils.Utils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-10-01
 */
public class HTTPResponsePayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogManager.getLogger(HTTPResponsePayload.class);

	@JsonDeserialize(as=MultivaluedHashMap.class)
	public MultivaluedMap<String, String> hdrs;
	public int status;
	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	public byte[] body;

	static String HEADERS = "hdrs";
	static String STATUS = "method";
	static String BODY = "body";
	/**
	 *
	 * @param hdrs
	 * @param status
	 * @param body
	 */
	@JsonCreator
	public HTTPResponsePayload(@JsonProperty("hdrs") MultivaluedMap<String, String> hdrs,
		@JsonProperty("status") int status,
		@JsonProperty("body") byte[] body) {
		if (hdrs != null) this.hdrs = Utils.setLowerCaseKeys(hdrs);
		this.status = status;
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
	public void parseIfRequired() {
		if (this.dataObj == null) {
			try {
				Map<String, JsonNode> properties = new HashMap<>();
				properties.put(HEADERS, mapper.valueToTree(hdrs));
				properties.put(STATUS, new IntNode(status));
				if (hdrs != null && Utils.isJsonMimeType(hdrs)) {
					properties.put(BODY, mapper.readTree(new String(body, StandardCharsets.UTF_8)));
				} else {
					properties.put(BODY, new TextNode(new String(body, StandardCharsets.UTF_8)));
				}
				final JsonNodeFactory factory = JsonNodeFactory.instance;
				ObjectNode rootNode = factory.objectNode();
				rootNode.setAll(properties);
				this.dataObj = new JsonDataObj(rootNode, mapper);
			} catch (IOException e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Error while "
					+ "creating json data obj for http request payload")), e);
				this.dataObj = (JsonDataObj.createEmptyObject(mapper));
			}
		}
	}

	@Override
	public void syncFromDataObj() throws PathNotFoundException, DataObjProcessingException {
		if (!isDataObjEmpty()) {
			hdrs = dataObj.getValAsObject("/".concat(HEADERS), MultivaluedHashMap.class);
			status = dataObj.getValAsObject("/".concat(STATUS) , Integer.class);
			body = (dataObj.getValAsString("/".concat(BODY))).getBytes();
		}
	}
}
