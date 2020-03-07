package io.md.utils;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import io.md.dao.LazyParseAbstractPayload;
import io.md.dao.Payload;

public class PayloadSerializer extends JsonSerializer<Payload> {

	private static final Logger LOGGER = LogManager.getLogger(PayloadSerializer.class);

	private final JsonSerializer<Object> serializer;

	public PayloadSerializer(JsonSerializer<Object> jsonSerializer) {
		this.serializer = jsonSerializer;
	}

	@Override
	public void serialize(final Payload o, final JsonGenerator jsonGenerator,
		final SerializerProvider serializerProvider)
		throws IOException {
		// delegating to the default serializer, if data obj is empty
		// otherwise just serializer the data object root
		LazyParseAbstractPayload asAbstractPayload = (LazyParseAbstractPayload) o;
		if (asAbstractPayload.dataObj != null && !asAbstractPayload.dataObj.isDataObjEmpty()) {
			jsonGenerator.writeObject(asAbstractPayload.dataObj.getRoot());
		} else {
			serializer.serialize(o, jsonGenerator, serializerProvider);
		}
	}


	@Override
	public void serializeWithType(Payload value, JsonGenerator gen,
		SerializerProvider provider, TypeSerializer typeSerializer)
		throws IOException {
		WritableTypeId typeId = typeSerializer.typeId(value, JsonToken.VALUE_STRING);
		typeSerializer.getPropertyName();
		typeSerializer.writeTypePrefix(gen, typeId);
		serialize(value, gen, provider); // call your customized serialize method
		typeSerializer.writeTypeSuffix(gen, typeId);
	}
}
