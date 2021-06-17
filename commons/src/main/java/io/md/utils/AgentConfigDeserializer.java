/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.md.dao.agent.config.AgentConfig;
import java.io.IOException;

public class AgentConfigDeserializer extends StdDeserializer<AgentConfig> {

  public static String CONFIG = "config";

  public AgentConfigDeserializer() {
    super(AgentConfig.class);
  }

  @Override
  public AgentConfig deserialize(JsonParser jsonParser,
      DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
    JsonNode node = mapper.readerFor(AgentConfig.class).readTree(jsonParser);
    String config = "";
    if(node.get(CONFIG).textValue() != null) {
      config = node.get(CONFIG).textValue();
    }
    return new AgentConfig(config);
  }
}
