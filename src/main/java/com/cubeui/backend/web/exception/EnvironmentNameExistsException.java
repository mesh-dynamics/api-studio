package com.cubeui.backend.web.exception;

import javax.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EnvironmentNameExistsException extends RuntimeException {

  public EnvironmentNameExistsException(@NotEmpty String name) {
    super("Environment " + name + " already exists for this user/app");
  }
}
