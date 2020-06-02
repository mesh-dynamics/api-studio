package io.md.core;

import io.md.dao.AgentConfig;
import io.md.dao.ConfigStore;
import org.apache.commons.lang3.Validate;

public class ValidateAgentStore {

  public static void validate(ConfigStore store) {
    Validate.notBlank(store.version);
    Validate.notBlank(store.customerId);
    Validate.notBlank(store.app);
    Validate.notBlank(store.service);
    Validate.notBlank(store.instanceId);
    Validate.isInstanceOf(AgentConfig.class, store.configJson);
    AgentConfig config = (AgentConfig) store.configJson;
    Validate.notBlank(config.getConfig());
  }

}
