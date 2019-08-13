package com.cube.serialize;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.cache.TemplateKey;
import com.cube.core.Utils;

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
