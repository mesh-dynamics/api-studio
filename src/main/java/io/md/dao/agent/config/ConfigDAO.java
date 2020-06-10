package io.md.dao.agent.config;

import io.md.dao.Config;

public class ConfigDAO {
  public Integer version;
  public final String customerId;
  public final String app;
  public final String service;
  public final String instanceId;
  public String tag;
  public Config configJson;

  public ConfigDAO() {
    this.customerId = null;
    this.version = null;
    this.app = null;
    this.service = null;
    this.instanceId = null;
    this.configJson = null;
  }

  public ConfigDAO(String customerId, String app, String service,
      String instanceId, String tag) {
    this.customerId = customerId;
    this.app = app;
    this.service = service;
    this.instanceId = instanceId;
    this.tag = tag;
  }

  public ConfigDAO setConfigJson(Config configJson) {
    this.configJson = configJson;
    return this;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

}
