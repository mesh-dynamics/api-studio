package com.cubeui.backend.web.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ResponseStatus(UNAUTHORIZED)
public class CustomerIdException extends RuntimeException{
    public CustomerIdException(String exception) {
        super(exception);
    }
}
