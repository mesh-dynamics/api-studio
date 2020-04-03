package com.cubeui.backend.web;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ErrorResponse {

    private Date timestamp;
    private String error;
    private int status;
    private String message;

    public ErrorResponse(String error) {
        this.timestamp = new Date();
        this.error = error;
    }

    public ErrorResponse(String error, String message) {
        this(error);
        this.message = message;
    }

    public ErrorResponse(String error, String message, int status) {
        this(error, message);
        this.status = status;
    }
}
