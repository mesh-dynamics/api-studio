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

package io.cube.interceptor.jersey_1x.egress;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import io.cube.agent.CommonConfig;
import io.md.utils.CommonUtils;

public class ClientMockingFilter extends ClientFilter {

  private static final Logger LOGGER = LogMgr.getLogger(ClientMockingFilter.class);

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
