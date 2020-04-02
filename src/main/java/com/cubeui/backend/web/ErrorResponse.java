package com.cubeui.backend.web;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ErrorResponse<T>{

    private Date timestamp;
    private T error;
    private int status;
    private String message;

    public ErrorResponse(T error) {
        this.timestamp = new Date();
        this.error = error;
    }

    public ErrorResponse(T error, String message) {
        this(error);
        this.message = message;
    }

    public ErrorResponse(T error, String message, int status) {
        this(error, message);
        this.status = status;
    }
}
