package com.cube.interceptor.jersey.egress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import io.cube.agent.CommonConfig;
import io.cube.agent.Constants;
import io.md.utils.CommonUtils;

public class ClientMockingFilter extends ClientFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientMockingFilter.class);

  @Override
  public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {

    // Modify the request
    try {
      clientRequest = filter(clientRequest);
    } catch (Exception e) {
      LOGGER.error("Exception in client request filter ", e);
    }

    // Call the next client handler in the filter chain
    ClientResponse resp = getNext().handle(clientRequest);

    return resp;
  }

  private ClientRequest filter(ClientRequest clientRequest) {
    URI originalUri = clientRequest.getURI();
    CommonConfig commonConfig = CommonConfig.getInstance();
    String serviceName = CommonUtils.getEgressServiceName(originalUri);
    try {
      commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {
        LOGGER.info("Mocking URI Present :" + mockURI);
        commonConfig.authToken.ifPresentOrElse(auth -> {
           MultivaluedMap<String, Object> clientHeaders = clientRequest
                .getHeaders();
           clientHeaders.add(Constants.AUTHORIZATION_HEADER, "Bearer "+auth);
           LOGGER.info("Setting auth token in the header");
         }, ()-> {
          LOGGER.info("Auth token not present for Mocking service");
        });
        clientRequest.setURI(mockURI);
      });
    } catch (URISyntaxException e) {
      LOGGER.error("Exception setting the URI ", e);
    }
    return clientRequest;
  }
}
