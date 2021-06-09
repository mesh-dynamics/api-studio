package com.cubeui.backend.security.jwt;

import com.cubeui.backend.domain.User;
import com.cubeui.backend.web.exception.ResetPasswordException;
import java.io.IOException;

import java.time.Instant;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtTokenFilter extends GenericFilterBean {

    private JwtTokenValidator jwtTokenValidator;

    public JwtTokenFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
        throws IOException, ServletException {

        try {
            jwtTokenValidator.resolveAndValidateToken((HttpServletRequest) req);
        } catch(InvalidJwtAuthenticationException e) {
            ((HttpServletResponse) res).sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid authorization token");
            return;
        }
        filterChain.doFilter(req, res);
    }
}
