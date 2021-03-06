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

package io.cube.jaxrs.egress;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedList;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import io.cube.agent.CommonConfig;
import io.md.utils.CommonUtils;

@Priority(4001)
public class ClientMockingFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LogMgr.getLogger(ClientMockingFilter.class);

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
							new LinkedList((Arrays.asList(auth))));
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
