package com.cube.interceptor.apachecxf.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cube.agent.CommonConfig;

import io.cube.agent.Constants;
import io.md.utils.CommonUtils;

@Provider
@Priority(4500)
public class MockingClientFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MockingClientFilter.class);


	@Override
	public void filter(ClientRequestContext clientRequestContext) {
		try {
			URI originalUri = clientRequestContext.getUri();
			CommonConfig commonConfig = CommonConfig.getInstance();
			String serviceName = CommonUtils.getEgressServiceName(originalUri);
			commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {
				commonConfig.authToken.ifPresentOrElse(auth -> {
					MultivaluedMap<String, Object> clientHeaders = clientRequestContext
						.getHeaders();
					clientHeaders.put(Constants.AUTHORIZATION_HEADER, List.of(auth));
				}, ()-> {
					LOGGER.info("Auth token not present for Mocking service");
				});
				clientRequestContext.setUri(mockURI);
			});
		} catch (Exception e) {
			LOGGER.error(String.valueOf(
				Map.of(
					io.md.constants.Constants.MESSAGE, "Error occurred in Mocking filter",
					io.md.constants.Constants.REASON, e.getMessage()
				)));
		}
	}
}
