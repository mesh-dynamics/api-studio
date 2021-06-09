package com.cubeui.backend.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class FileRetrievalException extends RuntimeException {
  public FileRetrievalException(String message) {
    super(message);
  }
}
