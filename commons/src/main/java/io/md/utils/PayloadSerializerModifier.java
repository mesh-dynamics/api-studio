package io.md.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import io.md.dao.FnReqRespPayload;
import io.md.dao.GRPCRequestPayload;
import io.md.dao.GRPCResponsePayload;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.JsonByteArrayPayload;
import io.md.dao.JsonPayload;

public class PayloadSerializerModifier extends BeanSerializerModifier {

	private static final Logger LOGGER = LogMgr.getLogger(PayloadSerializerModifier.class);

	//NOTE as we implement more classes which implement payload, we'll need to add
	//them here
	Set<Class> payloadClasses = new HashSet<>(
	Arrays.asList(HTTPResponsePayload.class, HTTPRequestPayload.class,
		JsonByteArrayPayload.class , JsonPayload.class, GRPCRequestPayload.class,
		GRPCResponsePayload.class));

	@Override
	public JsonSerializer<?> modifySerializer(
		final SerializationConfig serializationConfig,
		final BeanDescription beanDescription,
		final JsonSerializer<?> jsonSerializer) {

		if (payloadClasses.contains(beanDescription.getBeanClass())) {
			LOGGER.debug( "Delegating to payload serializer : className : "
				.concat( beanDescription.getBeanClass().getName()));
			return new PayloadSerializer((JsonSerializer<Object>) jsonSerializer);
		} else  if (beanDescription.getBeanClass() == FnReqRespPayload.class) {
			return new FnReqRespPayloadSerializer();
		}

		return jsonSerializer;
	}
}
