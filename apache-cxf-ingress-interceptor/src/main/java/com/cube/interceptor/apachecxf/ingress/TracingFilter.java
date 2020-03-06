package com.cube.interceptor.apachecxf.ingress;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.BooleanUtils;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

import com.cube.interceptor.config.Config;
import com.cube.interceptor.utils.Utils;

/**
 * Priority is to specify in which order the filters are to be executed.
 * Lower the order, early the filter is executed.
 * We want Logging filter to execute after Tracing Filter during Ingress
 **/
@Provider
@Priority(1000)
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

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

		String sampleBaggageItem = currentSpan.getBaggageItem(Constants.MD_IS_SAMPLED);
		if (sampleBaggageItem == null) {
			//root span
			boolean isSampled = Utils.isSampled(requestHeaders);
			currentSpan.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && config.commonConfig.samplerVeto) {
			currentSpan.setBaggageItem(Constants.MD_IS_VETOED,
				String.valueOf(Utils.isSampled(requestHeaders)));
		}

		String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
		reqContext.setProperty(scopeKey, scope);
	}

	@Override
	public void filter(ContainerRequestContext reqContext,
		ContainerResponseContext respContext) throws IOException {
		String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);

		Object obj = reqContext.getProperty(scopeKey);
		if (obj != null) {
			((Scope) obj).close();
			reqContext.removeProperty(scopeKey);
		}
	}
}