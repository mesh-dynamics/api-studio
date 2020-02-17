package com.cube.interceptor.jersey;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.utils.Utils;

@Provider
@Priority(Priorities.HEADER_DECORATOR + 9)
public class TracingFilter implements ContainerRequestFilter {

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	public void filter(ContainerRequestContext reqContext) throws IOException {
		//start a md-child-span
		MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();
		Scope scope = CommonUtils.startServerSpan(requestHeaders,
			Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN));
		Span currentSpan = scope.span();

		if (currentSpan.getBaggageItem(Constants.MD_IS_SAMPLED) == null) {
			//root span
			boolean isSampled = Utils.isSampled(requestHeaders);
			currentSpan.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		}

		String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
		reqContext.setProperty(scopeKey, scope);
	}

}