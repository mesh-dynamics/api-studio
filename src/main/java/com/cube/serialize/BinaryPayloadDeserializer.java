package com.cube.serialize;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import com.cube.utils.Constants;
import com.cube.ws.AnalyzeWS;

public class BinaryPayloadDeserializer extends JsonDeserializer<byte[]> {
    private static final Logger LOGGER = LogManager.getLogger(BinaryPayloadDeserializer.class);

    @Override
    public byte[] deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String hexString = jsonParser.getValueAsString();
        try {
            return Hex.decodeHex(hexString.toCharArray());
        } catch (DecoderException e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.DECODING_EXCEPTION , e.getMessage() ,
                Constants.INVALID_HEX_PAYLOAD , hexString)));
        }
        return new byte[0];
    }
}
