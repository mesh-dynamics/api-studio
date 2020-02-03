package com.cubeui.backend.service.jwt;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNAUTHORIZED)
public class InvalidJwtActivationException extends AuthenticationException {
    public InvalidJwtActivationException(String e) {
        super(e);
    }
}
