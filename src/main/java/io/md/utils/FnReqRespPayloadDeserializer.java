package io.md.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import io.md.dao.FnReqRespPayload;

public class FnReqRespPayloadDeserializer extends StdDeserializer<FnReqRespPayload> {

	public FnReqRespPayloadDeserializer() {
		super(FnReqRespPayload.class);
	}


	@Override
	public FnReqRespPayload deserialize(JsonParser jsonParser,
		DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		JsonNode node = mapper.readerFor(FnReqRespPayload.class).readTree(jsonParser);
		return new FnReqRespPayload(node);
	}

}