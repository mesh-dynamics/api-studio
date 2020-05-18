package io.md.utils;

import java.io.IOException;
import java.util.Arrays;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.google.gson.Gson;

import io.md.dao.FnReqRespPayload;

public class FnReqRespPayloadSerializer extends JsonSerializer<FnReqRespPayload> {

	static Logger LOGGER = LoggerFactory.getLogger(FnReqRespPayloadSerializer.class);
	private Gson gson = MeshDGsonProvider.getInstance();

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
			//TODO: Add it as a gson serializer
			if (fnReqRespPayload.retOrExceptionVal instanceof SerialClob) {
				serializeClob(fnReqRespPayload, jsonGenerator);
			} else if (fnReqRespPayload.retOrExceptionVal instanceof SerialBlob) {
				serializeBlob(fnReqRespPayload, jsonGenerator);
			} else if (fnReqRespPayload.retOrExceptionVal instanceof String) {
				jsonGenerator.writeString("\"" + fnReqRespPayload.retOrExceptionVal + "\"");
			} else {
				jsonGenerator.writeRawValue(gson.toJson(fnReqRespPayload.retOrExceptionVal));
			}
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

	private void serializeBlob(FnReqRespPayload fnReqRespPayload, JsonGenerator jsonGenerator)
		throws IOException {
		try {
			SerialBlob blob = (SerialBlob) fnReqRespPayload.retOrExceptionVal;
			jsonGenerator.writeBinary(blob.getBinaryStream(), (int)blob.length());
		} catch (SerialException e) {
			LOGGER.error("serialize java.sql.Blob error : {}", e.getMessage(), e);
		}
	}

	private void serializeClob(FnReqRespPayload fnReqRespPayload, JsonGenerator jsonGenerator)
		throws IOException {
		try {
			SerialClob clob = (SerialClob) fnReqRespPayload.retOrExceptionVal;
			jsonGenerator.writeString(clob.getCharacterStream(), (int)clob.length());
		} catch (SerialException e) {
			LOGGER.error("serialize java.sql.Clob error : {}", e.getMessage(), e);
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
