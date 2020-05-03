package io.cube.spring.ingress;

import java.io.IOException;

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

	private static final Config config = new Config();

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

			String sampleBaggageItem = span.getBaggageItem(Constants.MD_IS_SAMPLED);
			if (sampleBaggageItem == null) {
				//root span
				boolean isSampled = Utils.isSampled(requestHeaders);
				span.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
			} else if (!BooleanUtils.toBoolean(sampleBaggageItem)
				&& Config.commonConfig.samplerVeto) {
				span.setBaggageItem(Constants.MD_IS_VETOED,
					String.valueOf(Utils.isSampled(requestHeaders)));
			}

			MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);
			//cannot directly inject into httpservletrequest
			for (String value : mdTraceHeaders.keySet()) {
				wrappedRequest.putHeader(value, mdTraceHeaders.get(value));
			}
		} catch (Exception ex) {
			LOGGER.error("Exception occured while running Tracing filter!", ex);
		}

		if (wrappedRequest != null) {
			filterChain.doFilter(wrappedRequest, httpServletResponse);
		} else {
			filterChain.doFilter(httpServletRequest, httpServletResponse);
		}

		if (scope != null) {
			scope.close();
		}

		if (span != null) {
			span.finish();
		}
	}
}

