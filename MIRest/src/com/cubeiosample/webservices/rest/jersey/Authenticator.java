package com.cubeiosample.webservices.rest.jersey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

import org.apache.log4j.Logger;


public class Authenticator {
  
  final static Logger LOGGER = Logger.getLogger(Authenticator.class);
  // TODO: read key from a conf parameter/file
  final static Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  final static long TIME_LAX = 260000;  // 6 months for testing/demo
  
  // TODO: figure out how we can get this value either from conf file or from ISTIO flags
  
  public static boolean authenticate(String username, String password, MovieRentals mv) throws Exception {
    if(mv.validateUserAndPassword(username, password)) {
      return true;
    }
    throw new Exception("Invalid username/password");
  }
   
  public static String issueToken(String username) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + 60*60*24* 1000);
    String jws = Jwts.builder()
        .setSubject(username)
        //.setIssuer(uriInfo.getAbsolutePath().toString())
        .setIssuedAt(new Date())
        .setExpiration(validity)
        .signWith(key)
        .compact();
    LOGGER.debug("Token for " + username + " is " + jws);
    return jws;
  }
  
  
  public static String validateToken(String jws) {
    // key is needed to parse jwt. If successful, this token is valid.
    try {
      Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jws);
      if (Config.DUMMY_AUTHENTICATION || !claims.getBody().getExpiration().before(new Date())) {
        String subject = claims.getBody().getSubject();
        return subject;
      }
    } catch  (JwtException | IllegalArgumentException e) {
      LOGGER.error("Expired or invalid authentication token, message=" + e.getMessage());
    }
    LOGGER.debug(String.format("Token validatation failed: %s", jws));
    return null;
  }
}
