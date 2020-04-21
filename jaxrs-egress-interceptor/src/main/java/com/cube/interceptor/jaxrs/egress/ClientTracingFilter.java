package com.cube.interceptor.jaxrs.egress;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;

@Priority(4002)
public class ClientTracingFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientTracingFilter.class);

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
		} catch (Exception e) {
			LOGGER.error(String.valueOf(Map.of(Constants.MESSAGE,
				"Exception occured during logging, proceeding to the application!")),
				e.getMessage());
		}
	}
}
