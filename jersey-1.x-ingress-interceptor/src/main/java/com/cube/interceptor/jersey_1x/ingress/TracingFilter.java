package com.cube.interceptor.jersey_1x.ingress;


import javax.annotation.Priority;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;


@Provider
@Priority(1000)
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static Config config;
	private static final Logger LOGGER = LoggerFactory.getLogger(TracingFilter.class);


	static {
			config = new Config();
	}

	@Override
	public ContainerRequest filter(ContainerRequest containerRequest) {
		try {
			//start a md-child-span
			MultivaluedMap<String, String> requestHeaders = containerRequest.getRequestHeaders();
			Span currentSpan = CommonUtils.startServerSpan(requestHeaders,
					Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN));
			Scope scope = CommonUtils.activateSpan(currentSpan);


			String sampleBaggageItem = currentSpan.getBaggageItem(Constants.MD_IS_SAMPLED);
			if (sampleBaggageItem == null) {
				//root span
				boolean isSampled = Utils.isSampled(requestHeaders);
				currentSpan.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
			} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && Config.commonConfig.samplerVeto) {
				currentSpan.setBaggageItem(Constants.MD_IS_VETOED,
						String.valueOf(Utils.isSampled(requestHeaders)));
			}

			CommonUtils.injectContext(requestHeaders);

			String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
			containerRequest.getProperties().put(scopeKey, scope);
		} catch (Exception e) {
			LOGGER.error("Error while running TracingFilter ", e);
		}
		return containerRequest;
	}

	@Override
	public ContainerResponse filter(ContainerRequest containerRequest,
		ContainerResponse containerResponse) {
		String scopeKey = Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);

		Object obj = containerRequest.getProperties().get(scopeKey);
		if (obj != null) {
			((Scope) obj).close();
			containerRequest.getProperties().remove(scopeKey);
		}
		return containerResponse;
	}
}