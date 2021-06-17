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

package io.md.dao.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.md.dao.Config;
import io.md.utils.AgentConfigDeserializer;

@JsonDeserialize(using = AgentConfigDeserializer.class)
public class AgentConfig implements Config<String> {

  private String config;

  public AgentConfig() {
    this.config = null;
  }

  public AgentConfig(String config) {
    this.config = config;
  }

  @Override
  public String getConfig() {
    return config;
  }

  @Override
  @JsonIgnore
  public String getType() {
    return ConfigType.AgentConfig.toString();
  }

}
