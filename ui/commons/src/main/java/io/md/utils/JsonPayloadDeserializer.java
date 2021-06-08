package io.md.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.md.dao.JsonPayload;

public class JsonPayloadDeserializer extends StdDeserializer<JsonPayload> {

	public JsonPayloadDeserializer() {
		super(JsonPayload.class);
	}

	@Override
	public JsonPayload deserialize(JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		JsonNode node = mapper.readTree(jsonParser);
		if (node != null && node.isObject() && node.get("json") != null) {
			String actualJson = node.get("json").textValue();
			node = mapper.readTree(actualJson);
		}
		return new JsonPayload(node);
	}
}
