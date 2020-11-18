package io.cube.agent.logger;

import org.slf4j.event.Level;

import java.time.Instant;

public class LogStoreDTO {
  public String app;
  public String instance;
  public String service;
  public String sourceType;
  public String version;
  public String logMessage;
  public Level level;
  public Instant clientTimeStamp;
  public String customerId;

  //for jackson serialization
  private LogStoreDTO() {
    this.app = "";
    this.instance = "";
    this.service = "";
    this.sourceType = "";
    this.version = "";
    this.logMessage = "";
    this.level = Level.ERROR;
    this.clientTimeStamp = Instant.now();
    this.customerId = "";
  }

  public LogStoreDTO(CubeDeployment cubeDp , String sourceType, String logMessage , Level level){
    this.app = cubeDp.app;
    this.instance = cubeDp.instance;
    this.service = cubeDp.service;
    this.customerId = cubeDp.customerId;
    this.version = cubeDp.version;
    this.sourceType = sourceType;

    this.logMessage = logMessage;
    this.level = level;
    this.clientTimeStamp = Instant.now();
  }

  public String getApp() {
    return app;
  }

  public void setApp(String app) {
    this.app = app;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public Level getLevel() {
    return level;
  }

  public void setLevel(Level level) {
    this.level = level;
  }

  public Instant getClientTimeStamp() {
    return clientTimeStamp;
  }

  public void setClientTimeStamp(Instant clientTimeStamp) {
    this.clientTimeStamp = clientTimeStamp;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }
}
