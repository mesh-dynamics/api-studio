/**
 * Copyright Cube I O
 */
package io.md.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.md.core.TemplateKey;

import java.util.HashMap;
import java.util.Map;

/**
 * @author prasad
 * This was needed for json support for java 8 objects in jackson
 */
public class CubeObjectMapperProvider  {

    private static final Map<String , ObjectMapper> mapperMap = new HashMap<>();
    private static final JsonFactory defaultJsonFactory = new JsonFactory();

    private static ObjectMapper singleInstance = createDefaultMapper();

    public static ObjectMapper getInstance() {
        return singleInstance;
    }

    private static ObjectMapper createDefaultMapper() {
        return createMapper(defaultJsonFactory);
    }

    /*
       Singleton Object Mapper creation method for given Json Factory
     */
    public static ObjectMapper createMapper(JsonFactory jsonFactory) {
        String jsonFactoryId = jsonFactory.getClass().getName();
        if(mapperMap.containsKey(jsonFactoryId)){
            return mapperMap.get(jsonFactoryId);
        }
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        final ObjectMapper result = new ObjectMapper(jsonFactory);
        result.registerModule(new Jdk8Module());
        result.registerModule(new JavaTimeModule());
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        result.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        SimpleModule module = new SimpleModule();
        module.addKeySerializer(TemplateKey.class, new TemplateKeySerializer(result));
        module.addKeyDeserializer(TemplateKey.class, new TemplateKeyDeserializer(result));
        module.setSerializerModifier(new PayloadSerializerModifier());
        result.registerModule(module);

        mapperMap.put(jsonFactoryId , result);

        return result;
    }
}
