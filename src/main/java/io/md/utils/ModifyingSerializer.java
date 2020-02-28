package io.md.utils;

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;

import java.io.IOException;

import javax.print.DocFlavor.STRING;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import io.md.dao.LazyParseAbstractPayload;

public class ModifyingSerializer extends JsonSerializer<LazyParseAbstractPayload> {

	private final JsonSerializer<Object> serializer;

	public ModifyingSerializer(JsonSerializer<Object> jsonSerializer) {
		this.serializer = jsonSerializer;
	}

	@Override
	public void serialize(final LazyParseAbstractPayload o, final JsonGenerator jsonGenerator,
		final SerializerProvider serializerProvider)
		throws IOException {

			try {
				o.syncFromDataObj();
			} catch (Exception e) {
				e.printStackTrace();
			}

		serializer.serialize(o, jsonGenerator, serializerProvider);
	}


	@Override
	public void serializeWithType(LazyParseAbstractPayload value, JsonGenerator gen,
		SerializerProvider provider, TypeSerializer typeSerializer)
		throws IOException, JsonProcessingException {
		//WritableTypeId typeId = typeSerializer.typeId(value, As.PROPERTY);
		WritableTypeId typeId = typeSerializer.typeId(value, JsonToken.VALUE_STRING);
		typeSerializer.writeTypePrefix(gen, typeId);
		serialize(value, gen, provider); // call your customized serialize method
		typeSerializer.writeTypeSuffix(gen, typeId);
		//typeSerializer.writeTypeSuffix(gen, typeId);
	}
}
