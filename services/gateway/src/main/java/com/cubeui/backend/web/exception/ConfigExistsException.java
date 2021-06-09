package com.cubeui.backend.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConfigExistsException extends RuntimeException {
  public ConfigExistsException(String config) {
    super("Config{" + config + "} already exists for the user");
  }
}
