package io.cube.spring.ingress;

import static io.cube.spring.ingress.Utils.getHeaders;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Tracing filter to execute before Logging Filter.
 **/
@Component
@Order(3000)
public class TracingFilter extends OncePerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(TracingFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws ServletException, IOException {
		HeaderWrapper wrappedRequest = null;
		Span span = null;
		Scope scope = null;
		try {
			//start a md-child-span
			wrappedRequest = new HeaderWrapper(httpServletRequest);
			MultivaluedMap<String, String> requestHeaders = wrappedRequest.headersToMultiMap();
			String spanKey = Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);
			span = CommonUtils.startServerSpan(requestHeaders, spanKey);
			scope = CommonUtils.activateSpan(span);

			getorRunSampling(httpServletRequest, span);

			MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);
			//cannot directly inject into httpservletrequest
			for (String value : mdTraceHeaders.keySet()) {
				wrappedRequest.putHeader(value, mdTraceHeaders.get(value));
			}
		} catch (Exception ex) {
			LOGGER.error("Exception occured while running Tracing filter!", ex);
		}

		try {
			if (wrappedRequest != null) {
				filterChain.doFilter(wrappedRequest, httpServletResponse);
			} else {
				filterChain.doFilter(httpServletRequest, httpServletResponse);
			}
		} finally {
			if (scope != null) {
				scope.close();
			}

			if (span != null) {
				span.finish();
			}
		}
	}

	private void getorRunSampling(HttpServletRequest httpServletRequest, Span currentSpan) {
		Optional<String> fieldCategory = CommonConfig.getInstance().sampler.getFieldCategory();
		String sampleBaggageItem = currentSpan.getBaggageItem(Constants.MD_IS_SAMPLED);
		if (sampleBaggageItem == null) {
			//root span
			boolean isSampled = runSampling(httpServletRequest, fieldCategory);
			currentSpan.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		} else if (!BooleanUtils.toBoolean(sampleBaggageItem)
			&& CommonConfig.getInstance().samplerVeto) {
			currentSpan.setBaggageItem(Constants.MD_IS_VETOED,
				String.valueOf(runSampling(httpServletRequest, fieldCategory)));
		}
	}

	private boolean runSampling(
		HttpServletRequest httpServletRequest, Optional<String> fieldCategory) {
		boolean isSampled;
		if (!fieldCategory.isPresent()) {
			isSampled = Utils.isSampled(new MultivaluedHashMap<>());
		} else {
			switch (fieldCategory.get()) {
				case Constants.HEADERS:
					isSampled = Utils.isSampled(getHeaders(httpServletRequest));
					break;
				case Constants.QUERY_PARAMS:
					isSampled = Utils.isSampled(Utils.getQueryParameters(
						httpServletRequest.getQueryString()));
					break;
				case Constants.API_PATH_FIELD:
					String apiPath = httpServletRequest.getRequestURI();
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
}

