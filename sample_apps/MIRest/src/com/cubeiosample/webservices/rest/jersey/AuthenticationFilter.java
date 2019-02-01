package com.cubeiosample.webservices.rest.jersey;


import java.io.IOException;
import java.security.Principal;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

// Based on the post https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey/26778123#26778123
@Secured
@Provider
//@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

  final static Logger LOGGER = Logger.getLogger(AuthenticationFilter.class);
  private static final String REALM = "MIRest";
  private static final String AUTHENTICATION_SCHEME = "Bearer";

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    // Get the Authorization header from the request
    String authorizationHeader =
        requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

    // Validate the Authorization header
    if (!isTokenBasedAuthentication(authorizationHeader)) {
      abortWithUnauthorized(requestContext);
      return;
    }

    // Extract the token from the Authorization header
    String token = authorizationHeader
        .substring(AUTHENTICATION_SCHEME.length()).trim();
    LOGGER.debug("Token is " + token);

    // Validate the token and extract username
    // has to be "final" for the anonymous class SecurityContext to work
    final String username = Authenticator.validateToken(token);  
    if (username == null) {
      abortWithUnauthorized(requestContext);
    }
    final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
    requestContext.setSecurityContext(new SecurityContext() {
      @Override
      public Principal getUserPrincipal() {
        return new Principal() {
          @Override
          public String getName() {
            return username;
          }
        };
      }

      @Override
      public boolean isUserInRole(String role) {
        return true;
      }

      @Override
      public boolean isSecure() {
        return currentSecurityContext.isSecure();
      }

      @Override
      public String getAuthenticationScheme() {
        return AUTHENTICATION_SCHEME;
      }
    });

  }

  private boolean isTokenBasedAuthentication(String authorizationHeader) {

    // Check if the Authorization header is valid
    // It must not be null and must be prefixed with "CubeIO" plus a whitespace
    // The authentication scheme comparison must be case-insensitive
    return authorizationHeader != null && authorizationHeader.toLowerCase()
        .startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
  }

  private void abortWithUnauthorized(ContainerRequestContext requestContext) {

    // Abort the filter chain with a 401 status code response
    // The WWW-Authenticate header is sent along with the response
    requestContext.abortWith(
        Response.status(Response.Status.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE, 
            AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
        .build());
    }

//    private String ValidateToken(String token) throws Exception {
//      // Check if the token was issued by the server and if it's not expired
//      // Throw an Exception if the token is invalid
//      String username = Authenticator.ValidateToken(token);
//      if (username == null) {
//        throw new Exception();
//      }
//      return username;
//    }
}