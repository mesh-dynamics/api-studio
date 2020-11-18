package com.cubeio.logcollector.domain.DTO;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;

import java.time.Instant;

@Getter
@Setter
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
}
