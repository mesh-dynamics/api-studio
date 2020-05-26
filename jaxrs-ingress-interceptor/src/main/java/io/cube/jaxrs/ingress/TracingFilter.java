package io.cube.jaxrs.ingress;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;


@Provider
@Priority(1000)
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(TracingFilter.class);
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
		try {
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
			} else if (!BooleanUtils.toBoolean(sampleBaggageItem)
				&& CommonConfig.getInstance().samplerVeto) {
				currentSpan.setBaggageItem(Constants.MD_IS_VETOED,
					String.valueOf(Utils.isSampled(requestHeaders)));
			}
			reqContext.setProperty(scopeKey, scope);
			reqContext.setProperty(spanKey, currentSpan);
			CommonUtils.injectContext(requestHeaders);
		} catch (Exception ex) {
			LOGGER.error(String.valueOf(Map.of(Constants.MESSAGE,
				"Exception occured during setting up span!")), ex);
		}
	}

	@Override
	public void filter(ContainerRequestContext reqContext,
		ContainerResponseContext respContext) throws IOException {

		try {
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
		} catch (Exception ex) {
			LOGGER.error(String.valueOf(Map.of(Constants.MESSAGE,
				"Exception occured during closing span!")), ex);
		}
	}
}