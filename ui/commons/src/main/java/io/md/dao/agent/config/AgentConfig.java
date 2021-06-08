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
