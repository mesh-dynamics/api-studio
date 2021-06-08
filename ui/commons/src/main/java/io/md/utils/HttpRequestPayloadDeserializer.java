package io.md.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.md.dao.HTTPRequestPayload;

public class HttpRequestPayloadDeserializer extends StdDeserializer<HTTPRequestPayload> {

	public HttpRequestPayloadDeserializer() {
		super(HTTPRequestPayload.class);
	}

	@Override
	public HTTPRequestPayload deserialize(JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		JsonNode node = mapper.readerFor(HTTPRequestPayload.class).readTree(jsonParser);
		return new HTTPRequestPayload(node);
	}
}
