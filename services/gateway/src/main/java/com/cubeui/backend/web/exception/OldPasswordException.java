package com.cubeui.backend.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OldPasswordException extends RuntimeException {

  public OldPasswordException(String message) {
    super(message);
  }

}
