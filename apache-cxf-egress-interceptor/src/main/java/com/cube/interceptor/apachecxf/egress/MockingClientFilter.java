package com.cube.interceptor.apachecxf.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.http.client.utils.URIBuilder;

import io.cube.agent.CommonConfig;


@Provider
public class MockingClientFilter implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext clientRequestContext) {
		URI originalUri = clientRequestContext.getUri();
		CommonConfig commonConfig = CommonConfig.getInstance();
		if (commonConfig.toMockService(originalUri.toString())) {
			try {
				URI mockURI = commonConfig.getMockingURI(originalUri);
				clientRequestContext.setUri(mockURI);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

}


