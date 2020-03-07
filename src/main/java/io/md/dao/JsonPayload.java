package io.md.dao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import io.md.utils.JsonPayloadDeserializer;

@JsonDeserialize(using = JsonPayloadDeserializer.class)
public class JsonPayload extends LazyParseAbstractPayload {

	@JsonSerialize(using = StringSerializer.class)
	public String json;


	public JsonPayload(@JsonProperty("json") String payload) {
		this.json = payload;
	}

	public JsonPayload(JsonNode dataObjRoot) {
		super(dataObjRoot);
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws RawPayloadEmptyException {
		if (isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Payload String Empty/Null");
		}
		return json.getBytes();
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadEmptyException {
		if (isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Payload String Empty/Null");
		}
		return this.json;
	}

	@Override
	public void parseIfRequired(){
		if (this.dataObj == null) {
			this.dataObj = new JsonDataObj(json, mapper);
		}
	}

	@Override
	public void postParse() {
		// DO NOTHING (No unwrapping required)
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return (json == null || json.isEmpty()) && this.dataObj.isDataObjEmpty();
	}
}
