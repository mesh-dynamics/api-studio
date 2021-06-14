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
