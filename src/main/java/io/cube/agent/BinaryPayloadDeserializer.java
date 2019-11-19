package io.cube.agent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class BinaryPayloadDeserializer extends StdDeserializer<byte[]> {

  protected BinaryPayloadDeserializer() {
    this(null);
  }

  private BinaryPayloadDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public byte[] deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    String nodeValue = jsonParser.readValueAs(String.class);
    try {
      return Hex.decodeHex(nodeValue.toCharArray());
    } catch (DecoderException e) {
      return new byte[0];
    }
  }
}
