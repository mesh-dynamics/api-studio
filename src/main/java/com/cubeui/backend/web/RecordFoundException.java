package com.cubeui.backend.web;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class RecordFoundException extends RuntimeException {

    public RecordFoundException(String exception) {
        super(exception);
    }

}