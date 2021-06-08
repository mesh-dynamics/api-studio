package io.md.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.md.dao.JsonByteArrayPayload;

public class JsonByteArrayPayloadDeserializer extends StdDeserializer<JsonByteArrayPayload> {

	public JsonByteArrayPayloadDeserializer() {
		super(JsonByteArrayPayload.class);
	}

	@Override
	public JsonByteArrayPayload deserialize(JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		JsonNode node = mapper.readTree(jsonParser);
		if (node != null && node.isObject() && node.get("jsonBinary") != null) {
			byte[] actualJsonBinary = node.get("jsonBinary").binaryValue();
			node = mapper.readTree(actualJsonBinary);
		}
		return new JsonByteArrayPayload(node);
	}
}
