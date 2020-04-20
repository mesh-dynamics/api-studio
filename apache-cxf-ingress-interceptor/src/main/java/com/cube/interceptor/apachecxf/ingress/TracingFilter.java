package com.cube.interceptor.apachecxf.ingress;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

import com.cube.interceptor.apachecxf.ingress.config.Config;
import com.cube.interceptor.apachecxf.ingress.utils.Utils;

/**
 * Priority is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Logging filter to execute after Tracing Filter during Ingress
 **/
@Provider
@Priority(1000)
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {
	// config not used but required to ensure commonConfig initailised properly before
	// filter execution
	public static final Config config = Utils.config;
	private static final Logger LOGGER = LoggerFactory.getLogger(TracingFilter.class);

	@Override
	public void filter(ContainerRequestContext reqContext) {
		try {
			//start a md-child-span
			MultivaluedMap<String, String> requestHeaders = reqContext.getHeaders();
			String spanKey = Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);
			Span span = CommonUtils.startServerSpan(requestHeaders, spanKey);
			Scope scope = CommonUtils.activateSpan(span);

			Optional<String> fieldCategory = Utils.config.commonConfig.sampler.getFieldCategory();
			String sampleBaggageItem = span.getBaggageItem(Constants.MD_IS_SAMPLED);
			if (sampleBaggageItem == null) {
				//root span
				boolean isSampled = runSampling(reqContext, fieldCategory);
				span.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
			} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && Utils.config.commonConfig.samplerVeto) {
				span.setBaggageItem(Constants.MD_IS_VETOED,
					String.valueOf(runSampling(reqContext, fieldCategory)));
			}

			String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
			reqContext.setProperty(scopeKey, scope);
			reqContext.setProperty(spanKey, span);
			CommonUtils.injectContext(requestHeaders);
		} catch (Exception e) {
			LOGGER.error(String.valueOf(
				Map.of(
					Constants.MESSAGE, "Exception occurred in interceptor",
					Constants.REASON, e.getMessage()
				)));
		}
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
		ContainerResponseContext respContext) {
		try {
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
		} catch (Exception e) {
			LOGGER.error(String.valueOf(
				Map.of(
					Constants.MESSAGE, "Exception occurred in interceptor",
					Constants.REASON, e.getMessage()
				)));
		}
	}
}