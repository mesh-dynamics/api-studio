package com.cube.serialize;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.cache.TemplateKey;

public class TemplateKeyDeserializer extends KeyDeserializer {

    private ObjectMapper objectMapper;

    public TemplateKeyDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Object deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
        return objectMapper.readValue(s, TemplateKey.class);
    }

}
