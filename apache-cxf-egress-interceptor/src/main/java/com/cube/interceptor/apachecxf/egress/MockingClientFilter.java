package com.cube.interceptor.apachecxf.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cube.agent.CommonConfig;

import io.cube.agent.Constants;
import io.md.utils.CommonUtils;

@Provider
public class MockingClientFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LogManager.getLogger(MockingClientFilter.class);


	@Override
	public void filter(ClientRequestContext clientRequestContext) {
		URI originalUri = clientRequestContext.getUri();
		CommonConfig commonConfig = CommonConfig.getInstance();
		String serviceName = CommonUtils.getEgressServiceName(originalUri);
		try {
			commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {
				commonConfig.authToken.ifPresentOrElse(auth -> {
					MultivaluedMap<String, String> authHeaders = new MultivaluedHashMap<>();
					authHeaders.put(Constants.AUTHORIZATION_HEADER, List.of(auth));
					MultivaluedMap<String, Object> clientHeaders = clientRequestContext
						.getHeaders();
					for (Map.Entry<String, List<String>> entry : authHeaders.entrySet()) {
						for (String entVal : entry.getValue()) {
							clientHeaders.add(entry.getKey(), entVal);
						}
					}
				}, ()-> {
					LOGGER.info("Auth token not present for Mocking service");
				});
				clientRequestContext.setUri(mockURI);
			});
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
