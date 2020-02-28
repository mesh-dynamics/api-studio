package io.md.utils;

import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;

import io.md.constants.Constants;
import io.md.dao.DataObj.DataObjProcessingException;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.LazyParseAbstractPayload;

public class LazyParseAbstractPayloadSerializer extends StdSerializer<LazyParseAbstractPayload> {

	private static final Logger LOGGER = LogManager
		.getLogger(LazyParseAbstractPayloadSerializer.class);

	protected LazyParseAbstractPayloadSerializer(Class<LazyParseAbstractPayload> t) {
		super(t);
	}

	/*public LazyParseAbstractPayloadSerializer() {
		super();
		//super();
	}*/
/*	private final JsonSerializer<Object> defaultSerializer;

	public LazyParseAbstractPayloadSerializer() {
		super(LazyParseAbstractPayload.class);
		defaultSerializer = null;
	}

	public LazyParseAbstractPayloadSerializer(JsonSerializer<Object> defaultSerializer) {
		super(LazyParseAbstractPayload.class);
		this.defaultSerializer = defaultSerializer;
	}*/


	@Override
	public void serialize(LazyParseAbstractPayload lazyParseAbstractPayload,
		JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

		/*try {
			lazyParseAbstractPayload.syncFromDataObj();
		} catch (PathNotFoundException | DataObjProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Sync from data obj failed before serialization")), e);
		}*/

		//This is the crazy one-liner that will save someone a very long time
		BeanSerializerFactory.instance.createSerializer(serializerProvider,
			SimpleType.construct(LazyParseAbstractPayload.class))
			.serialize(lazyParseAbstractPayload, jsonGenerator, serializerProvider);

	}

	@Override
	public void serializeWithType(LazyParseAbstractPayload value, JsonGenerator gen,
		SerializerProvider provider, TypeSerializer typeSerializer)
		throws IOException, JsonProcessingException {
		try {
			value.syncFromDataObj();
		} catch (PathNotFoundException | DataObjProcessingException e) {
			LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
				"Sync from data obj failed before serialization")), e);
		}
		//WritableTypeId typeId = typeSerializer.typeId(value, );
		//typeSerializer.writeTypePrefix(gen, typeId);
		serialize(value, gen, provider); // call your customized serialize method
		//typeSerializer.writeTypeSuffix(gen, typeId);
	}
}
