package io.cube.interceptor.jersey_1x.egress;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;

public class ClientTracingFilter extends ClientFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientTracingFilter.class);

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
