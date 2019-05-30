package com.cubeui.backend.web;

import com.cubeui.backend.security.jwt.InvalidJwtAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.ResponseEntity.status;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {

    @ExceptionHandler(value = {InvalidJwtAuthenticationException.class})
    public ResponseEntity invalidJwtAuthentication(InvalidJwtAuthenticationException ex, WebRequest request) {
        log.debug("handling InvalidJwtAuthenticationException...");
        ErrorResponse errorResponse = new ErrorResponse("Unauthorized", ex.getMessage(), UNAUTHORIZED.value());
        return status(UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(value = {RecordFoundException.class})
    public ResponseEntity recordNotFound(RecordFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Record Not Found", ex.getMessage(), NOT_FOUND.value());
        return status(NOT_FOUND).body(errorResponse);
    }
}