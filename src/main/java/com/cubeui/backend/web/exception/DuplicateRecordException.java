package com.cubeui.backend.web.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.FORBIDDEN;


@ResponseStatus(FORBIDDEN)
public class DuplicateRecordException extends RuntimeException {

    public DuplicateRecordException(String exception) {
        super(exception);
    }

}
