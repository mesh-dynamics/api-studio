package io.md.dao;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

import io.md.constants.Constants;

public class StringAsByteArrayPayload extends LazyParseAbstractPayload {

	private static final Logger LOGGER = LogManager.getLogger(StringAsByteArrayPayload.class);

	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	@JsonProperty("payload")
	public byte[] payload;

	// for Jackson
	private StringAsByteArrayPayload() {
		super();
		this.payload = new byte[]{};
	}

	public StringAsByteArrayPayload(byte[] payload) {
		super();
		this.payload = payload;
	}

	@Override
	public byte[] rawPayloadAsByteArray() throws RawPayloadEmptyException  {
		if (this.isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Byte Array is Empty/Null");
		}
		return payload;
	}

	@Override
	public String rawPayloadAsString() throws RawPayloadEmptyException {
		if (this.isRawPayloadEmpty()) {
			throw new RawPayloadEmptyException("Byte Array is Empty/Null");
		}
		return new String(payload, StandardCharsets.UTF_8);
	}

	@Override
	public boolean isRawPayloadEmpty() {
		return this.payload == null || this.payload.length == 0;
	}

	@Override
	public void parseIfRequired() {
		if (this.dataObj == null) {
			try {
				dataObj = new JsonDataObj(rawPayloadAsString(), this.mapper);
			} catch (RawPayloadEmptyException | NotImplementedException e) {
				LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Error while "
					+ "creating json data obj for http request payload")), e);
				this.dataObj = (JsonDataObj.createEmptyObject(mapper));
			}
		}
	}

	@Override
	public void syncFromDataObj() throws DataObjProcessingException {
		if (!this.isDataObjEmpty()) {
			payload = (dataObj.serializeDataObj()).getBytes();
		}
	}
}
