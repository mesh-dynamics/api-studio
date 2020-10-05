package com.cubeui.backend.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RequiredFieldException extends RuntimeException {
  public RequiredFieldException(String message) {
    super(message);
  }
}
