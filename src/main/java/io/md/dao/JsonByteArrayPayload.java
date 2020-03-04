package io.md.dao;

import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

public class JsonByteArrayPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogManager.getLogger(JsonByteArrayPayload.class);

	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	public byte[] jsonBinary;

	@JsonCreator
	public JsonByteArrayPayload(@JsonProperty("jsonBinary") byte[] payload) {
		this.jsonBinary = payload;
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws RawPayloadEmptyException  {
		if (this.isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Byte Array is Empty/Null");
		}
		return jsonBinary;
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadEmptyException {
		if (this.isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Byte Array is Empty/Null");
		}
		return new String(jsonBinary, StandardCharsets.UTF_8);
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return this.jsonBinary == null || this.jsonBinary.length == 0;
	}

	@Override
	public void parseIfRequired(){
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(jsonBinary, mapper);
		}
	}

	@Override
	public void postParse() {
		//Do Nothing
	}

	@Override
	public void syncFromDataObj() {
		if (!this.isDataObjEmpty()) {
			try {
				jsonBinary = (dataObj.serializeDataObj()).getBytes();
			} catch (DataObjProcessingException e) {
				jsonBinary = null;
			}
		}
	}
}
