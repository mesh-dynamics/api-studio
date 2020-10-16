package io.md.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class ServerUtils {

  public static JsonNode convertStringToNode(String value, ObjectMapper jsonMapper)
  {
    try {
      return  jsonMapper.readTree(value);
    } catch (IOException e) {
      return new TextNode(value);
    }
  }

}
