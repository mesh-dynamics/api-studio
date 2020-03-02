package com.cube.interceptor.jersey.egress;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import io.md.utils.CommonUtils;

import com.cube.interceptor.jersey.egress.utils.Utils;

public class ClientTracingFilter extends ClientFilter {

	@Override
	public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
		MultivaluedMap<String, String> transformedHeaders = Utils.transformHeaders(clientRequest.getHeaders());
		CommonUtils.injectContext(transformedHeaders);
		MultivaluedMap<String, Object> clientHeaders = clientRequest.getHeaders();
		for (Map.Entry<String, List<String>> entry : transformedHeaders.entrySet()) {
				if (!clientHeaders.containsKey(entry.getKey())) {
					clientHeaders.add(entry.getKey(), entry.getValue());
			}
		}
		// Call the next client handler in the filter chain
		ClientResponse resp = getNext().handle(clientRequest);
		return resp;
	}
}
