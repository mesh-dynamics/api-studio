package io.md.utils;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import io.md.constants.Constants;
import io.md.dao.FnReqRespPayload;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.JsonByteArrayPayload;
import io.md.dao.JsonPayload;

public class PayloadSerializerModifier extends BeanSerializerModifier {

	private static final Logger LOGGER = LogManager.getLogger(PayloadSerializerModifier.class);

	//NOTE as we implement more classes which implement payload, we'll need to add
	//them here
	Set<Class> payloadClasses = Set.of(HTTPResponsePayload.class, HTTPRequestPayload.class,
		JsonByteArrayPayload.class , JsonPayload.class);

	@Override
	public JsonSerializer<?> modifySerializer(
		final SerializationConfig serializationConfig,
		final BeanDescription beanDescription,
		final JsonSerializer<?> jsonSerializer) {

		if (payloadClasses.contains(beanDescription.getBeanClass())) {
			LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
				, "Delegating to payload serializer", "className"
				, beanDescription.getBeanClass().getName())));
			return new PayloadSerializer((JsonSerializer<Object>) jsonSerializer);
		} else  if (beanDescription.getBeanClass() == FnReqRespPayload.class) {
			return new FnReqRespPayloadSerializer();
		}

		return jsonSerializer;
	}
}
