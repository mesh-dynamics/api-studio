package io.cube.interceptor.jersey_1x.egress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
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
    try {
      URI originalUri = clientRequest.getURI();
      CommonConfig commonConfig = CommonConfig.getInstance();
      String serviceName = CommonUtils.getEgressServiceName(originalUri);

      commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {
        LOGGER.debug("Mocking URI Present :" + mockURI);
        if (commonConfig.authToken.isPresent()) {
          String auth = commonConfig.authToken.get();
          MultivaluedMap<String, Object> clientHeaders = clientRequest
                .getHeaders();
          clientHeaders.add(io.cube.agent.Constants.AUTHORIZATION_HEADER, "Bearer "+auth);
          LOGGER.debug("Setting auth token in the header");
         } else {
          LOGGER.info("Auth token not present for Mocking service");
        }
        clientRequest.setURI(mockURI);
      });
    } catch (URISyntaxException e) {
      LOGGER.error("Mocking filter issue, exception during setting URI!", e);
    } catch (Exception ex) {
      LOGGER.error("Mocking filter issue, exception occured!", ex);
    }
    return clientRequest;
  }
}
