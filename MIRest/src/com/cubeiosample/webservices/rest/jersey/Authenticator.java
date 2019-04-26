package com.cubeiosample.webservices.rest.jersey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.log4j.Logger;


public class Authenticator {
  
  final static Logger LOGGER = Logger.getLogger(Authenticator.class);
  // TODO: read key from a conf parameter/file
  final static Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  final static long TIME_LAX = 260000;  // 6 months for testing/demo
  
  // TODO: figure out how we can get this value either from conf file or from ISTIO flags
  final static boolean DUMMY_AUTHENTICATION = true;

  public static boolean authenticate(String username, String password) throws Exception {
    // TODO: validate against db and then return true/false; needs to be done along with /createuser api
    return true;
  }
   
  public static String issueToken(String username) {
    LocalDateTime ldt = LocalDateTime.now().plusMinutes(60L);
    Instant expiry = ldt.atZone(ZoneId.systemDefault()).toInstant();
    String jws = Jwts.builder()
        .setSubject(username)
        //.setIssuer(uriInfo.getAbsolutePath().toString())
        .setIssuedAt(new Date())
        .setExpiration(Date.from(expiry))
        .signWith(key)
        .compact();
    LOGGER.debug("Key is " + key.toString() + " " + key.hashCode());
    LOGGER.debug("Token for " + username + " is " + jws);    
    return jws;
  }
  
  
  public static String validateToken(String jws) {
    // key is needed to parse jwt. If successful, this token is valid.
    Jws<Claims> claims = Jwts.parser().setSigningKey(key).parseClaimsJws(jws);
    Date dt = claims.getBody().getExpiration();
    
    if (DUMMY_AUTHENTICATION || dt.after(Date.from(LocalDateTime.now().plusMinutes(TIME_LAX).atZone(ZoneId.systemDefault()).toInstant()))) { 
      String subject = claims.getBody().getSubject();
      return subject;
    }
    LOGGER.debug(String.format("Token validatation failed: %s", jws));
    return null;
  }
}
