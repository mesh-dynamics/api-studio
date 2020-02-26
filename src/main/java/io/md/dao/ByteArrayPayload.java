package io.md.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ByteArraySerializer;

public class ByteArrayPayload extends AbstractRawPayload {

	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(as = byte[].class)
	public final byte[] payload;

	// for Jackson
	private ByteArrayPayload() {
		this.payload = new byte[]{};
	}

	public ByteArrayPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public String payloadAsString() {
		return new String(payload);
	}

	@Override
	public String payloadAsString(ObjectMapper mapper) {
		return payloadAsString();
	}
}
