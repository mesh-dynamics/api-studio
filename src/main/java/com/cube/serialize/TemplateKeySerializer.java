package com.cube.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.cube.cache.TemplateKey;

public class TemplateKeySerializer extends JsonSerializer<TemplateKey> {

    private ObjectMapper objectMapper;

    public TemplateKeySerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void serialize(TemplateKey templateKey, JsonGenerator jGen, SerializerProvider serializerProvider) throws IOException {
         jGen.writeFieldName(objectMapper.writeValueAsString(templateKey));
    }
}
