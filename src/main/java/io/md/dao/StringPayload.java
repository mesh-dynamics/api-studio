package io.md.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

public class StringPayload extends AbstractRawPayload {

	@JsonSerialize(using = StringSerializer.class)
	@JsonDeserialize(as = String.class)
	public final String payload;

	// for Jackson
	private StringPayload() {
		this.payload = "";
	}

	public StringPayload(String payload) {
		this.payload = payload;
	}

	@Override
	public String payloadAsString() {
		return payload;
	}

	@Override
	public String payloadAsString(ObjectMapper mapper) {
		return payloadAsString();
	}
}
