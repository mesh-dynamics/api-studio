package com.cube.interceptor.jaxrs;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;

public class TracingFilter implements ClientResponseFilter {

	public static final String CLIENT_SPAN = "md-client-span";

	@Override
	public void filter(ClientRequestContext clientRequestContext,
		ClientResponseContext clientResponseContext) throws IOException {
		//CommonUtils.startClientSpan(Constants.SERVICE_FIELD.concat(CLIENT_SPAN));
		CommonUtils.injectContext(clientRequestContext.getStringHeaders());
	}
}
