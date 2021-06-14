package com.cubeio.logcollector.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
