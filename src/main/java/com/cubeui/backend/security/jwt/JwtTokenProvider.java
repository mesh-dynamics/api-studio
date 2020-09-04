package com.cubeui.backend.security.jwt;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.cubeui.backend.domain.Customer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.cubeui.backend.domain.ApiAccessToken;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.ApiAccessTokenRepository;
import com.cubeui.backend.repository.UserRepository;
import com.cubeui.backend.security.CustomUserDetailsService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

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

    String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public Customer getCustomer(HttpServletRequest req) {
        final UserDetails userDetails = getUser(req);
        return ((User)userDetails).getCustomer();
    }

    public UserDetails getUser(HttpServletRequest request) {
        final String token = resolveToken((HttpServletRequest) request);
        return this.userDetailsService.loadUserByUsername(getUsername(token, secretKey));
    }

    public UserDetails getUserFromRefreshToken(String token) {
        return this.userDetailsService.loadUserByUsername(getUsername(token, refreshSecretKey));
    }

    boolean validateToken(String token) {
        return  validate(token, secretKey);
    }

    public boolean validateRefreshToken(String token) {
        return validate(token, refreshSecretKey);
    }

     boolean validate(String token, String secretKey) {
        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            log.trace("validate token is called ");
            if ("pat".equalsIgnoreCase(claims.getBody().get("type", String.class))) {
                log.trace("Found that the token is of type API token");
                long customerId = claims.getBody().get("customer_id", Long.class);
                //The token is of type personal access token, so check the DB to confirm that it is not revoked
                Optional<List<ApiAccessToken>> accessToken = userRepository.findByUsernameAndCustomerId(getUsername(token, secretKey), customerId)
                    .map(User::getId).flatMap(apiAccessTokenRepository::findByUserId);
                return accessToken.flatMap(list -> list.stream().findFirst())
                    .map(ApiAccessToken::getToken)
                    .filter(token::equals)
                    .isPresent();
            } else {
                log.trace("It is a normal token");
                return !claims.getBody().getExpiration().before(new Date());
            }

        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidJwtAuthenticationException("Expired or invalid authentication token");
        }
    }

    public long getValidity() {
        return validityInSeconds;
    }

}
