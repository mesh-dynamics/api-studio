package com.cubeui.backend.web.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class InvalidDataException extends RuntimeException {

    public InvalidDataException(String exception) {
        super(exception);
    }

}
