package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogStoreDTO {
  public String app;
  public String instance;
  public String service;
  public String sourceType;
  public String version;
  public String logMessage;

  //for jackson serialization
  private LogStoreDTO() {
    this.app = "";
    this.instance = "";
    this.service = "";
    this.sourceType = "";
    this.version = "";
    this.logMessage = "";
  }
}
