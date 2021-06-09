package com.cubeui.backend.web.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class AppServiceMappingException extends RuntimeException {
  public AppServiceMappingException(String exception) {
    super(exception);
  }
}
