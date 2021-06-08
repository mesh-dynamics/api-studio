package io.md.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.md.dao.GRPCResponsePayload;
import io.md.dao.HTTPRequestPayload;

public class GPRCResponsePayloadDeserialzer extends StdDeserializer<GRPCResponsePayload> {

	public GPRCResponsePayloadDeserialzer() {
		super(GRPCResponsePayload.class);
	}

	@Override
	public GRPCResponsePayload deserialize(JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		JsonNode node = mapper.readerFor(GRPCResponsePayload.class).readTree(jsonParser);
		return new GRPCResponsePayload(node);
	}

}
