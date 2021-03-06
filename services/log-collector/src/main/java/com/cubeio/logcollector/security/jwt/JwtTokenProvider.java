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

package com.cubeio.logcollector.security.jwt;

import com.cubeio.logcollector.domain.User;
import com.cubeio.logcollector.domain.ApiAccessToken;
import com.cubeio.logcollector.repository.ApiAccessTokenRepository;
import com.cubeio.logcollector.repository.UserRepository;

import com.cubeio.logcollector.security.CustomUserDetailsService;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${security.jwt.token.secret-key}")
    private String secretKey = "secret";

    @Value("${security.jwt.refresh.token.secret-key}")
    private String refreshSecretKey = "refresh-secret";

    @Value("${security.jwt.token.expire-length}")
    private long validityInSeconds = 60 * 60 * 24; // 24 hours

    @Value("${security.jwt.refresh.token.expire-length}")
    private long validityInSecondsForRefresh = 60 * 60 * 24 * 14; // 2 weeks

    private UserDetailsService userDetailsService;

    private ApiAccessTokenRepository apiAccessTokenRepository;

    private UserRepository userRepository;

    public JwtTokenProvider(CustomUserDetailsService customUserDetailsService, ApiAccessTokenRepository apiAccessTokenRepository, UserRepository userRepository) {
        this.userDetailsService = customUserDetailsService;
        this.userRepository = userRepository;
        this.apiAccessTokenRepository = apiAccessTokenRepository;
    }

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(String username, List<String> roles) {

        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInSeconds * 1000);

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

    public String createRefreshToken(User user){
        Claims claims = Jwts.claims().setSubject(user.getUsername());
        claims.put("scopes", user.getAuthorities().stream().map(s -> s.toString()).collect(
            Collectors.toList()));
        Date now = new Date();
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + validityInSecondsForRefresh * 1000))
            .signWith(SignatureAlgorithm.HS256, refreshSecretKey)
            .compact();
    }

    Authentication getAuthentication(String token) {
        UserDetails userDetails = this.userDetailsService.loadUserByUsername(getUsername(token, secretKey));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    String getUsername(String token, String secretKey) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody().getSubject();
    }

    List<String> resolveToken(HttpServletRequest req) {
        List<String> bearerTokens = Collections.list(req.getHeaders("Authorization"));
        List<String> tokens = new ArrayList<>();
        bearerTokens.forEach(bearerToken -> {
            String tokenArray[] = bearerToken.split(",");
            for(String token : tokenArray) {
                if (token != null && token.startsWith("Bearer ")) {
                    tokens.add(token.substring(7));
                }
            }
        });

        return tokens;
    }

    public UserDetails getUser(HttpServletRequest request) {
        final List<String> tokens = resolveToken((HttpServletRequest) request);
        Pair<String, Boolean> token = validateToken(tokens);
        return this.userDetailsService.loadUserByUsername(getUsername(token.getFirst(), secretKey));
    }

    public UserDetails getUserFromRefreshToken(String token) {
        return this.userDetailsService.loadUserByUsername(getUsername(token, refreshSecretKey));
    }

    Pair<String, Boolean> validateToken(List<String> tokens) {
        return  validate(tokens, secretKey);
    }

    public boolean validateRefreshToken(String token) {
        return validate(List.of(token), refreshSecretKey).getSecond();
    }

     Pair<String, Boolean> validate(List<String> tokens, String secretKey) {
        for(String token : tokens) {
            try {
                boolean value = false;
                Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
                log.trace("validate token is called ");
                if ("pat".equalsIgnoreCase(claims.getBody().get("type", String.class))) {
                    log.trace("Found that the token is of type API token");
                    long customerId = claims.getBody().get("customer_id", Long.class);
                    //The token is of type personal access token, so check the DB to confirm that it is not revoked
                    Optional<List<ApiAccessToken>> accessToken = userRepository.findByUsernameAndCustomerId(getUsername(token, secretKey), customerId)
                        .map(User::getId).flatMap(apiAccessTokenRepository::findByUserId);
                    value =  accessToken.flatMap(list -> list.stream().findFirst())
                        .map(ApiAccessToken::getToken)
                        .filter(token::equals)
                        .isPresent();
                } else {
                    log.trace("It is a normal token");
                    value = !claims.getBody().getExpiration().before(new Date());
                }
                if(value) {
                    return Pair.of(token, true);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.error("Expired or invalid authentication token, message=" + e.getMessage());
            }
        }
        return Pair.of("The token is not valid", false);
    }

    public long getValidity() {
        return validityInSeconds;
    }

}
