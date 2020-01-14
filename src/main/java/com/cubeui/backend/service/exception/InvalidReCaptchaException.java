package com.cubeui.backend.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class InvalidReCaptchaException extends Throwable {
  public InvalidReCaptchaException(String e) {
    super(e);
  }
}
