package com.cubeui.backend.service.exception;

public class ReCaptchaInvalidException extends RuntimeException {

  public ReCaptchaInvalidException(String e) {
    super(e);
  }
}
