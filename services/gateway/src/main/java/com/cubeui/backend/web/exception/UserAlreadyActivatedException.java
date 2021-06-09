package com.cubeui.backend.web.exception;

import static org.springframework.http.HttpStatus.CONFLICT;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(CONFLICT)
public class UserAlreadyActivatedException extends RuntimeException {

    public UserAlreadyActivatedException(String exception) {
        super(exception);
    }
}
