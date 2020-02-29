package io.md.dao;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import io.md.constants.Constants;

public class StringPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogManager.getLogger(StringPayload.class);

	@JsonSerialize(using = StringSerializer.class)
	public String payload;

	@JsonCreator
	public StringPayload(@JsonProperty("payload") String payload) {
		this.payload = payload;
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws RawPayloadEmptyException {
		if (isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Payload String Empty/Null");
		}
		return payload.getBytes();
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadEmptyException {
		if (isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Payload String Empty/Null");
		}
		return this.payload;
	}

	@Override
	public void parseIfRequired() {
		if (this.dataObj == null) {
			try {
				dataObj = new JsonDataObj(rawPayloadAsString(), this.mapper);
			} catch (RawPayloadEmptyException e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Error while "
					+ "creating json data obj for http request payload")), e);
				this.dataObj = (JsonDataObj.createEmptyObject(mapper));
			}
		}
	}

	@Override
	public void syncFromDataObj() throws DataObjProcessingException {
		if (!isDataObjEmpty()) {
			payload = this.dataObj.serializeDataObj();
		}
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return payload == null || payload.isEmpty();
	}
}
