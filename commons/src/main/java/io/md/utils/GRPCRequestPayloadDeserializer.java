package io.md.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.md.dao.GRPCRequestPayload;
import io.md.dao.HTTPRequestPayload;

public class GRPCRequestPayloadDeserializer extends StdDeserializer<GRPCRequestPayload> {
	public GRPCRequestPayloadDeserializer() {
		super(GRPCRequestPayload.class);
	}

	@Override
	public GRPCRequestPayload deserialize(JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		JsonNode node = mapper.readerFor(GRPCRequestPayload.class).readTree(jsonParser);
		return new GRPCRequestPayload(node);
	}
}
