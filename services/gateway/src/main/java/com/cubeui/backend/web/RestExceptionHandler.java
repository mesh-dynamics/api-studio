/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.web;

import com.cubeui.backend.security.jwt.InvalidJwtAuthenticationException;
import com.cubeui.backend.service.exception.FileRetrievalException;
import com.cubeui.backend.service.exception.FileStorageException;
import com.cubeui.backend.web.exception.AppServiceMappingException;
import com.cubeui.backend.web.exception.ConfigExistsException;
import com.cubeui.backend.web.exception.CustomerIdException;
import com.cubeui.backend.web.exception.DuplicateRecordException;
import com.cubeui.backend.web.exception.EnvironmentNameExistsException;
import com.cubeui.backend.web.exception.EnvironmentNotFoundException;
import com.cubeui.backend.web.exception.InvalidDataException;
import com.cubeui.backend.web.exception.OldPasswordException;
import com.cubeui.backend.web.exception.RecordNotFoundException;
import com.cubeui.backend.web.exception.RequiredFieldException;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import com.cubeui.backend.web.exception.ResetPasswordException;
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

    @ExceptionHandler(value = {EnvironmentNameExistsException.class})
    public ResponseEntity invalidData(EnvironmentNameExistsException ex, WebRequest request) {
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

    @ExceptionHandler(value = {InvalidEventException.class})
    public ResponseEntity invalidData(InvalidEventException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Invalid Event ", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }
    
    @ExceptionHandler(value = {ResetPasswordException.class})
    public ResponseEntity invalidData(ResetPasswordException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Reset Password Exception", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(value = {OldPasswordException.class})
    public ResponseEntity invalidData(OldPasswordException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Password Matches with old passwords", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(value = {RequiredFieldException.class})
    public ResponseEntity invalidData(RequiredFieldException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Mandatory fields are missing", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(value = {FileStorageException.class})
    public ResponseEntity invalidData(FileStorageException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Error while saving the file", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(value = {FileRetrievalException.class})
    public ResponseEntity invalidData(FileRetrievalException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Error while retrieving the file", ex.getMessage(), INTERNAL_SERVER_ERROR.value());
        return status(INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(value = {AppServiceMappingException.class})
    public ResponseEntity appServiceMappingException(AppServiceMappingException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse<String>("Service app mapping error", ex.getMessage(), BAD_REQUEST.value());
        return status(BAD_REQUEST).body(errorResponse);
    }
}