/**
 * Copyright Cube I O
 */
package com.cube.ws;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.utils.PayloadSerializerModifier;

import com.cube.cache.TemplateKey;
import com.cube.serialize.TemplateKeyDeserializer;
import com.cube.serialize.TemplateKeySerializer;

/**
 * @author prasad
 * This was needed for json support for java 8 objects in jackson
 */
@Provider
public class CubeObjectMapperProvider implements ContextResolver<ObjectMapper> {

    final ObjectMapper defaultObjectMapper;

    public CubeObjectMapperProvider() {
        defaultObjectMapper = createDefaultMapper();
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
            return defaultObjectMapper;
    }

    static ObjectMapper createDefaultMapper() {
        final ObjectMapper result = new ObjectMapper();
        result.registerModule(new Jdk8Module());
        result.registerModule(new JavaTimeModule());
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        SimpleModule module = new SimpleModule();
        module.addKeySerializer(TemplateKey.class, new TemplateKeySerializer(result));
        module.addKeyDeserializer(TemplateKey.class, new TemplateKeyDeserializer(result));
        module.setSerializerModifier(new PayloadSerializerModifier());
        result.registerModule(module);
        return result;
    }
}
