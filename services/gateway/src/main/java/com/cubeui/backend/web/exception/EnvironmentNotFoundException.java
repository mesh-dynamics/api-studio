package com.cubeui.backend.web.exception;

import javax.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EnvironmentNotFoundException extends RuntimeException {

  public EnvironmentNotFoundException(@NotEmpty Long id) {
    super("Environment with id '" + id + "' not found for this user");
  }
}
