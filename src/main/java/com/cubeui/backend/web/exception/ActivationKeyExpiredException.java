package com.cubeui.backend.web.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(UNAUTHORIZED)
public class ActivationKeyExpiredException extends RuntimeException {

    public ActivationKeyExpiredException(String exception) {
        super(exception);
    }
}
