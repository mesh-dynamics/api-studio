package com.cubeui.backend.domain.DTO;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Level;

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
  }
}
