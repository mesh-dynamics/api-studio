package com.cube.interceptor.jaxrs;

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

//import javax.ws.rs.Priorities;

@Provider
//@Priority(Priorities.HEADER_DECORATOR + 9)
@Priority(1000)
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final Config config;

	public static final String scopeKey
		= Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
	public static final String spanKey
		= Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);

	static {
		config = new Config();
	}

	@Override
	public void filter(ContainerRequestContext reqContext) throws IOException {
		//start a md-child-span
		MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();
		Span currentSpan = CommonUtils.startServerSpan(requestHeaders,
			Constants.SERVICE_FIELD.concat("-").concat(Constants.MD_CHILD_SPAN));
		Scope scope = CommonUtils.activateSpan(currentSpan);

		String sampleBaggageItem = currentSpan.getBaggageItem(Constants.MD_IS_SAMPLED);
		if (sampleBaggageItem == null) {
			//root span
			boolean isSampled = Utils.isSampled(requestHeaders);
			currentSpan.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && config.commonConfig.samplerVeto) {
			currentSpan.setBaggageItem(Constants.MD_IS_VETOED,
				String.valueOf(Utils.isSampled(requestHeaders)));
		}
		reqContext.setProperty(scopeKey, scope);
		reqContext.setProperty(spanKey, currentSpan);
	}

	@Override
	public void filter(ContainerRequestContext reqContext,
		ContainerResponseContext respContext) throws IOException {

		Object obj = reqContext.getProperty(scopeKey);
		if (obj != null) {
			((Scope) obj).close();
			//reqContext.removeProperty(scopeKey);
		}

		Object spanObj = reqContext.getProperty(spanKey);
		if (spanObj != null) {
			((Span) spanObj).finish();
			//reqContext.removeProperty(spanKey);
		}
	}
}