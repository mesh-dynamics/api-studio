package com.cube.interceptor.apachecxf.ingress;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedHashMap;
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
 * Priority is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Logging filter to execute after Tracing Filter during Ingress
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
		String spanKey = Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);
		Span span = CommonUtils.startServerSpan(requestHeaders, spanKey);
		Scope scope = CommonUtils.activateSpan(span);

		Optional<String> fieldCategory = config.commonConfig.sampler.getFieldCategory();
		String sampleBaggageItem = span.getBaggageItem(Constants.MD_IS_SAMPLED);
		if (sampleBaggageItem == null) {
			//root span
			boolean isSampled = runSampling(reqContext, fieldCategory);
			span.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && config.commonConfig.samplerVeto) {
			span.setBaggageItem(Constants.MD_IS_VETOED,
				String.valueOf(runSampling(reqContext, fieldCategory)));
		}

		String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
		reqContext.setProperty(scopeKey, scope);
		reqContext.setProperty(spanKey, span);
	}

	private boolean runSampling(ContainerRequestContext reqContext, Optional<String> fieldCategory) {
		boolean isSampled;
		if (fieldCategory.isEmpty()) {
			isSampled = Utils.isSampled(new MultivaluedHashMap<>());
		} else {
			switch (fieldCategory.get()) {
				case Constants.HEADERS:
					isSampled = Utils.isSampled(reqContext.getHeaders());
					break;
				case Constants.QUERY_PARAMS:
					isSampled = Utils.isSampled(reqContext.getUriInfo().getQueryParameters());
					break;
				case Constants.API_PATH_FIELD:
					String apiPath = reqContext.getUriInfo().getRequestUri().getPath();
					MultivaluedMap<String,String> apiPathMap = new MultivaluedHashMap<>();
					apiPathMap.add(Constants.API_PATH_FIELD, apiPath);
					isSampled = Utils.isSampled(apiPathMap);
					break;
				default:
					isSampled = Utils.isSampled(new MultivaluedHashMap<>());
			}
		}
		return isSampled;
	}

	@Override
	public void filter(ContainerRequestContext reqContext,
		ContainerResponseContext respContext) throws IOException {
		String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
		String spanKey = Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);

		Object obj = reqContext.getProperty(scopeKey);
		if (obj != null) {
			((Scope) obj).close();
			reqContext.removeProperty(scopeKey);
		}

		obj = reqContext.getProperty(spanKey);
		if (obj != null) {
			((Span) obj).finish();
			reqContext.removeProperty(spanKey);
		}
	}
}