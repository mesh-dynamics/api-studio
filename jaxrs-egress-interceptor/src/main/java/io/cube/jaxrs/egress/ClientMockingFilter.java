package io.cube.jaxrs.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.utils.CommonUtils;

@Priority(4001)
public class ClientMockingFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientMockingFilter.class);

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		try {
			URI originalUri = requestContext.getUri();
			CommonConfig commonConfig = CommonConfig.getInstance();
			String serviceName = CommonUtils.getEgressServiceName(originalUri);

			commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {
				commonConfig.authToken.ifPresent(auth -> {
					MultivaluedMap<String, Object> clientHeaders = requestContext
						.getHeaders();
					clientHeaders.put(io.cube.agent.Constants.AUTHORIZATION_HEADER,
						Arrays.asList(auth));
				});

				if (!commonConfig.authToken.isPresent()) {
					LOGGER.info("Auth token not present for Mocking service");
				}

				requestContext.setUri(mockURI);
			});
		} catch (URISyntaxException e) {
			LOGGER.error("Mocking filter issue, exception during setting URI!", e);
		} catch (Exception ex) {
			LOGGER.error("Mocking filter issue, exception occured!", ex);
		}
	}
}
