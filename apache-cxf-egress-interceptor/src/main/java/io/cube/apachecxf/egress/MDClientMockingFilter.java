package io.cube.apachecxf.egress;

import static io.cube.apachecxf.egress.Utils.closeSpanAndScope;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cube.agent.CommonConfig;
import io.cube.agent.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

/**
 * Order is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed.
 * Mock Filter should execute after Data Filter, so that in Mock mode, Data Filter can just
 * return without recording the request. Otherwise Mock Filter would change the URI and
 * the shouldMockService() will return false, as this would be evaluated on the changed URI.
 **/

@Provider
@Priority(value = 4501)
public class MDClientMockingFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MDClientMockingFilter.class);

	@Override
	public void filter(ClientRequestContext clientRequestContext) {
		Span[] clientSpan = {null};
		Scope[] clientScope = {null};
		try {
			LOGGER.info("Inside Egress Mocking filter");
			URI originalUri = clientRequestContext.getUri();
			CommonConfig commonConfig = CommonConfig.getInstance();
			String serviceName = CommonUtils.getEgressServiceName(originalUri);

			commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {
				Optional<Span> ingressSpan = CommonUtils.getCurrentSpan();

				//Empty ingress span pertains to DB initialization scenarios.
				SpanContext spanContext = ingressSpan.map(Span::context)
					.orElse(CommonUtils.createDefSpanContext());

				clientSpan[0] = CommonUtils
					.startClientSpan(io.md.constants.Constants.MD_CHILD_SPAN, spanContext, false);

				clientScope[0] = CommonUtils.activateSpan(clientSpan[0]);

				setAuthTokenAndMockURI(clientRequestContext, commonConfig, mockURI);
			});
		} catch (Exception e) {
			LOGGER.error(
				io.md.constants.Constants.MESSAGE + ":Error occurred in Mocking filter\n" +
				io.md.constants.Constants.EXCEPTION_STACK, e
			);

			closeSpanAndScope(clientSpan[0], clientScope[0]);
		}

		closeSpanAndScope(clientSpan[0], clientScope[0]);
	}

	private void setAuthTokenAndMockURI(ClientRequestContext clientRequestContext,
		CommonConfig commonConfig, URI mockURI) {
		if (commonConfig.authToken.isPresent()) {
			String auth = commonConfig.authToken.get();
			MultivaluedMap<String, Object> clientHeaders = clientRequestContext
				.getHeaders();

			clientHeaders.put(Constants.AUTHORIZATION_HEADER, Collections.singletonList(auth));
		} else {
			LOGGER.info("Auth token not present for Mocking service");
		}
		clientRequestContext.setUri(mockURI);
	}

}
