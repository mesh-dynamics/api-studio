package io.md.utils;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.md.dao.FnReqRespPayload;

public class FnReqRespPayloadSerializer extends JsonSerializer<FnReqRespPayload> {

	private Gson gson = (new GsonBuilder()).create();

	@Override
	public void serialize(FnReqRespPayload fnReqRespPayload, JsonGenerator jsonGenerator,
		SerializerProvider serializerProvider) throws IOException {
		// delegating to the default serializer, if data obj is empty
		// otherwise just serializer the data object root
		if (fnReqRespPayload.dataObj != null &&
			!fnReqRespPayload.dataObj.isDataObjEmpty()) {
			jsonGenerator.writeObject(fnReqRespPayload.dataObj.getRoot());
		} else {
			jsonGenerator.writeStartObject();
			jsonGenerator.writeObjectField("respTS", fnReqRespPayload.respTS);
			jsonGenerator.writeObjectField("exceptionType", fnReqRespPayload.exceptionType);
			jsonGenerator.writeObjectField("retStatus", fnReqRespPayload.retStatus);
			jsonGenerator.writeFieldName("retOrExceptionVal");
			jsonGenerator.writeRawValue(gson.toJson(fnReqRespPayload.retOrExceptionVal));
			jsonGenerator.writeArrayFieldStart("argVals");
			if (fnReqRespPayload.argVals != null) {
				Arrays.stream(fnReqRespPayload.argVals).forEach(
					UtilException.rethrowConsumer(argVal ->
						jsonGenerator.writeRawValue(gson.toJson(argVal))));
			}
			jsonGenerator.writeEndArray();
			jsonGenerator.writeEndObject();
		}
	}

	@Override
	public void serializeWithType(FnReqRespPayload value, JsonGenerator gen,
		SerializerProvider provider, TypeSerializer typeSerializer)
		throws IOException {
		WritableTypeId typeId = typeSerializer.typeId(value, JsonToken.VALUE_STRING);
		typeSerializer.getPropertyName();
		typeSerializer.writeTypePrefix(gen, typeId);
		serialize(value, gen, provider); // call your customized serialize method
		typeSerializer.writeTypeSuffix(gen, typeId);
	}
}
