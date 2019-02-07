package io.cube.utils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import io.opentracing.Tracer;


public class RestUtils {
  final static Logger LOGGER = Logger.getLogger(RestUtils.class);
  final static boolean addHeaders = false;

    
  public static Response callWithRetries(Tracer tracer, Builder req, JSONObject body, String requestType, int numRetries) {
    int numAttempts = 0;
    // inject headers
    if (addHeaders) {
    	Tracing.addTraceHeaders(tracer, req, requestType);
    }
    while (numAttempts < numRetries) {
      try {
        if (requestType.equalsIgnoreCase("GET")) {
          return req.get();
        }
        // assuming body is not null.
        return req.post(Entity.entity(body.toString(), MediaType.APPLICATION_JSON));
      } catch (Exception e) {
        LOGGER.error("request attempt " + numAttempts + ": " + req.toString() + "; exception: " + e.toString());
        ++numAttempts;
      }
    }
    LOGGER.debug("call with retries failed: " + req.toString());
    return null;
  }
 
}
