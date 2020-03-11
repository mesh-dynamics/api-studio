package com.cube.interceptor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

@Component
@Order(3000)
public class TracingFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LogManager.getLogger(TracingFilter.class);

	private static final Config config;

	static {
		config = new Config();
	}

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws ServletException, IOException {
		//start a md-child-span
		MultivaluedMap<String, String> requestHeaders = Utils.getHeaders(httpServletRequest);
		String spanKey = Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);
		Span span = CommonUtils.startServerSpan(requestHeaders, spanKey);
		Scope scope = CommonUtils.activateSpan(span);

		Optional<String> fieldCategory = config.commonConfig.sampler.getFieldCategory();
		String sampleBaggageItem = span.getBaggageItem(Constants.MD_IS_SAMPLED);
		if (sampleBaggageItem == null) {
			//root span
			boolean isSampled = runSampling(httpServletRequest, fieldCategory);
			span.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && config.commonConfig.samplerVeto) {
			span.setBaggageItem(Constants.MD_IS_VETOED,
				String.valueOf(runSampling(httpServletRequest, fieldCategory)));
		}

		filterChain.doFilter(httpServletRequest, httpServletResponse);

		if (scope != null) {
			scope.close();
		}

		if (span != null) {
			span.finish();
		}
	}

	private boolean runSampling(HttpServletRequest httpServletRequest,
		Optional<String> fieldCategory) {
		boolean isSampled;
		if (fieldCategory.isEmpty()) {
			isSampled = Utils.isSampled(new MultivaluedHashMap<>());
		} else {
			switch (fieldCategory.get()) {
				case Constants.HEADERS:
					isSampled = Utils.isSampled(Utils.getHeaders(httpServletRequest));
					break;
				case Constants.QUERY_PARAMS:
					MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
					try {
						queryParams = Utils
							.getQueryParams(new URI(httpServletRequest.getRequestURL().toString()));
					} catch (URISyntaxException e) {
						LOGGER.error(new ObjectMessage(
							Map.of(Constants.MESSAGE,
								"URI formation failed,  query params ignored!")));
					}
					isSampled = Utils.isSampled(queryParams);
					break;
				case Constants.API_PATH_FIELD:
					MultivaluedMap<String, String> apiPathMap = new MultivaluedHashMap<>();
					apiPathMap.add(Constants.API_PATH_FIELD, httpServletRequest.getContextPath());
					isSampled = Utils.isSampled(apiPathMap);
					break;
				default:
					isSampled = Utils.isSampled(new MultivaluedHashMap<>());
			}
		}
		return isSampled;
	}
}
