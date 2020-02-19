package com.cube.interceptor.jaxrs;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import io.md.utils.CommonUtils;

public class TracingFilter implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext clientRequestContext) throws IOException {
		CommonUtils.injectContext(clientRequestContext.getStringHeaders());
	}
}
