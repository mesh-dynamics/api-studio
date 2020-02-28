package io.md.utils;

import java.util.Set;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.LazyParseAbstractPayload;
import io.md.dao.StringAsByteArrayPayload;
import io.md.dao.StringPayload;

public class LazyParseAbstractPayloadSerializerModifier extends BeanSerializerModifier {

	Set<Class> payloadClasses = Set.of(HTTPResponsePayload.class, HTTPRequestPayload.class,
		StringAsByteArrayPayload.class , StringPayload.class);

	@Override
	public JsonSerializer<?> modifySerializer(
		final SerializationConfig serializationConfig,
		final BeanDescription beanDescription,
		final JsonSerializer<?> jsonSerializer) {

		if (payloadClasses.contains(beanDescription.getBeanClass())) {
			System.out.println("delegated");
			return new ModifyingSerializer((JsonSerializer<Object>) jsonSerializer);
		}

		return jsonSerializer;
	}
}
	/*
	@Override
	public JsonSerializer<?> modifySerializer(
		SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
		System.out.println(beanDesc.getBeanClass().getName());
		if (this.isSubClassOf(beanDesc.getBeanClass().LazyParseAbstractPayload.class)) {
			System.out.println("delegated");
			return new LazyParseAbstractPayloadSerializer((JsonSerializer<Object>) serializer);
		}

		return serializer;
	}*/
