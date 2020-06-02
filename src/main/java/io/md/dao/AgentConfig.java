package io.md.dao;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.md.utils.AgentConfigDeserializer;

@JsonDeserialize(using = AgentConfigDeserializer.class)
public class AgentConfig implements StoreConfig<String> {

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
  public String getType() {
    return ConfigType.AgentConfig.toString();
  }

}
