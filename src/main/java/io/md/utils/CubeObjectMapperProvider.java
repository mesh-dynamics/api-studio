/**
 * Copyright Cube I O
 */
package io.md.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.core.TemplateKey;

/**
 * @author prasad
 * This was needed for json support for java 8 objects in jackson
 */
public class CubeObjectMapperProvider  {


    private static ObjectMapper singleInstance = createDefaultMapper();

    public static ObjectMapper getInstance() {
        return singleInstance;
    }

    private static ObjectMapper createDefaultMapper() {
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        final ObjectMapper result = new ObjectMapper(jsonFactory);
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
