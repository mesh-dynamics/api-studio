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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import io.md.utils.CommonUtils;

public class ClientTracingFilter extends ClientFilter {

	private static final Logger LOGGER = LogMgr.getLogger(ClientTracingFilter.class);

	@Override
	public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
		try {
			MultivaluedMap<String, String> mdTraceHeaders =  new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);
			MultivaluedMap<String, Object> clientHeaders = clientRequest.getHeaders();
			for (Map.Entry<String, List<String>> entry : mdTraceHeaders.entrySet()) {
				Iterator valuesItr = entry.getValue().iterator();
				while (valuesItr.hasNext()) {
					clientHeaders.add(entry.getKey(), valuesItr.next());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception occured during logging, proceeding to the application!", e);
		}

		// Call the next client handler in the filter chain
		ClientResponse resp = getNext().handle(clientRequest);
		return resp;
	}
}
