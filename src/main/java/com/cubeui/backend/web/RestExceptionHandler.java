package com.cubeui.backend.web;

import com.cubeui.backend.security.jwt.InvalidJwtAuthenticationException;
import com.cubeui.backend.web.exception.ConfigExistsException;
import com.cubeui.backend.web.exception.CustomerIdException;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.EnvironmentNameExitsException;
import com.cubeui.backend.web.exception.EnvironmentNotFoundException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import static org.springframework.http.HttpStatus.*;
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

    @ExceptionHandler(value = {RecordNotFoundException.class})
    public ResponseEntity recordNotFound(RecordNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Record Not Found", ex.getMessage(), NOT_FOUND.value());
        return status(NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(value = {InvalidDataException.class})
    public ResponseEntity invalidData(InvalidDataException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Invalid Data Received", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(value = {DuplicateRecordException.class})
    public ResponseEntity invalidData(DuplicateRecordException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Record Already Exists", ex.getMessage(), FORBIDDEN.value());
        return status(FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(value = {CustomerIdException.class})
    public ResponseEntity invalidData(CustomerIdException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse("Invalid Customer Id", ex.getMessage(), UNAUTHORIZED.value());
        return status(UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(value = {EnvironmentNameExitsException.class})
    public ResponseEntity invalidData(EnvironmentNameExitsException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Environment already exists", ex.getMessage(), CONFLICT.value());
        return status(CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(value = {EnvironmentNotFoundException.class})
    public ResponseEntity invalidData(EnvironmentNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Environment not found", ex.getMessage(), NOT_FOUND.value());
        return status(NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(value = {ConfigExistsException.class})
    public ResponseEntity invalidData(ConfigExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Config already exists", ex.getMessage(), CONFLICT.value());
        return status(CONFLICT).body(errorResponse);
    }
}