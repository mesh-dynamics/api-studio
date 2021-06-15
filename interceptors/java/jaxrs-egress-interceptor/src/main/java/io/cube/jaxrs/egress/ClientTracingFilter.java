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
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.slf4j.Logger;
import io.md.utils.CommonUtils;

@Priority(4002)
public class ClientTracingFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LogMgr.getLogger(ClientTracingFilter.class);

	@Override
	public void filter(ClientRequestContext clientRequestContext) throws IOException {
		try {
			MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);
			MultivaluedMap<String, Object> clientHeaders = clientRequestContext.getHeaders();
			for (Map.Entry<String, List<String>> entry : mdTraceHeaders.entrySet()) {
				for (String entValue : entry.getValue()) {
					clientHeaders.add(entry.getKey(), entValue);
				}
			}
		} catch (Exception ex) {
			LOGGER.error(
				"Exception occured during logging, proceeding to the application!", ex);
		}
	}
}
