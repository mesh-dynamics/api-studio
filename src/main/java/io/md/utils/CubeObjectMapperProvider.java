/**
 * Copyright Cube I O
 */
package io.md.utils;

import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
        final ObjectMapper result = new ObjectMapper();
        result.registerModule(new Jdk8Module());
        result.registerModule(new JavaTimeModule());
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        result.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        SimpleModule module = new SimpleModule();
        module.setSerializerModifier(new PayloadSerializerModifier());
        result.registerModule(module);
        return result;
    }
}
