package com.cube.serialize;

import java.io.IOException;

import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class BinaryPayloadSerializer extends JsonSerializer<byte[]> {
    @Override
    public void serialize(byte[] bytes, JsonGenerator jsonGenerator
        , SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(Hex.toHexString(bytes));
    }
}
