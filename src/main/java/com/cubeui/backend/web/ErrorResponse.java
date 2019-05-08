package com.cubeui.backend.web;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class ErrorResponse {

    private Date timestamp;
    private String message;
    private String details;

    public ErrorResponse(String message) {
        super();
        this.timestamp = new Date();
        this.message = message;
    }

    public ErrorResponse(String message, String details) {
        super();
        this.timestamp = new Date();
        this.message = message;
        this.details = details;
    }
}
