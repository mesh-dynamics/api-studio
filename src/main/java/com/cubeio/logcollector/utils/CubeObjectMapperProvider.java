/**
 * Copyright Cube I O
 */
package com.cubeio.logcollector.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.msgpack.jackson.dataformat.MessagePackFactory;

public class CubeObjectMapperProvider {

    private static ObjectMapper singleInstance = createMapper(null);
    public  static ObjectMapper msgPacker = createMapper(new MessagePackFactory());

    public static ObjectMapper getInstance() {
        return singleInstance;
    }

    private static ObjectMapper createMapper(JsonFactory jf) {
        JsonFactory jsonFactory = (jf==null) ? new JsonFactory() : jf;
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        final ObjectMapper result = new ObjectMapper(jsonFactory);
        result.registerModule(new Jdk8Module());
        result.registerModule(new JavaTimeModule());
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        result.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        return result;
    }
}
