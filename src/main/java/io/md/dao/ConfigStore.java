package io.md.dao;

public class ConfigStore {
  public final String version;
  public final String customerId;
  public final String app;
  public final String service;
  public final String instanceId;
  public StoreConfig configJson;

  public ConfigStore() {
    this.version = null;
    this.customerId = null;
    this.app = null;
    this.service = null;
    this.instanceId = null;
    this.configJson = null;
  }

  public ConfigStore(String version, String customerId, String app, String service,
      String instanceId) {
    this.version = version;
    this.customerId = customerId;
    this.app = app;
    this.service = service;
    this.instanceId = instanceId;
  }

  public ConfigStore(String version, String customerId, String app, String service,
      String instanceId, StoreConfig configJson) {
    this.version = version;
    this.customerId = customerId;
    this.app = app;
    this.service = service;
    this.instanceId = instanceId;
    this.configJson = configJson;
  }

  public ConfigStore setConfigJson(StoreConfig configJson) {
    this.configJson = configJson;
    return this;
  }

}
