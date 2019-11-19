package io.cube.agent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.apache.commons.codec.binary.Hex;

public class BinaryPayloadSerializer extends StdSerializer<byte[]> {

  protected  BinaryPayloadSerializer() {
    this(null);
  }

  private BinaryPayloadSerializer(Class<byte[]> t) {
    super(t);
  }

  @Override
  public void serialize(byte[] bytes, JsonGenerator jsonGenerator,
      SerializerProvider serializerProvider) throws IOException {
      jsonGenerator.writeString(Hex.encodeHexString(bytes));
  }
}
