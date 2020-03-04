package io.md.dao;

import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

public class JsonPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogManager.getLogger(JsonPayload.class);

	@JsonSerialize(using = StringSerializer.class)
	public String json;

	@JsonCreator
	public JsonPayload(@JsonProperty("json") String payload) {
		this.json = payload;
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
		// DO NOTHING
	}

	@Override
	public void syncFromDataObj()  {
		if (!isDataObjEmpty()) {
			try {
				json = this.dataObj.serializeDataObj();
			} catch (DataObjProcessingException e) {
				json = null;
			}
		}
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return json == null || json.isEmpty();
	}
}
