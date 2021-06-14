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

package io.md.logger;

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

  public LogStoreDTO(String app , String instance , String service , String customerId,  String version , String sourceType, String logMessage , Level level){
    this.app = app;
    this.instance = instance;
    this.service = service;
    this.customerId = customerId;
    this.version = version;
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
