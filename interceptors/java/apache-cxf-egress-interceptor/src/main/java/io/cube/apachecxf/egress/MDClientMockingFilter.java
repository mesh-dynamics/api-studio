package io.cube.apachecxf.egress;

import static io.cube.apachecxf.egress.Utils.closeSpanAndScope;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.md.logger.LogMgr;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.slf4j.Logger;

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
public class MDClientMockingFilter implements ClientRequestFilter, ClientResponseFilter {

	private static final Logger LOGGER = LogMgr.getLogger(MDClientMockingFilter.class);
	private static final String MOCK_SPAN = "md-mock-span";
	private static final String MOCK_SCOPE = "md-mock-scope";

	@Override
	public void filter(ClientRequestContext clientRequestContext) {
		Span[] clientSpan = {null};
		Scope[] clientScope = {null};
		try {
			LOGGER.debug("Inside Egress Mocking Request filter");
			URI originalUri = clientRequestContext.getUri();
			CommonConfig commonConfig = CommonConfig.getInstance();
			Message message = PhaseInterceptorChain.getCurrentMessage();
//			String serviceName = CommonUtils.getEgressServiceName(originalUri);

//			commonConfig.getMockingURI(originalUri, serviceName).ifPresent(mockURI -> {

			//mockURI is already set through app. Not able to set the URI in interceptor
			//for apache cxf 2.7.7
			LOGGER.debug("MOCK URI : " + originalUri);
			if (originalUri != null && originalUri.toString()
				.startsWith(new URI(commonConfig.CUBE_MOCK_SERVICE_URI).toString())) {
				Optional<Span> ingressSpan = CommonUtils.getCurrentSpan();

				//Empty ingress span pertains to DB initialization scenarios.
				SpanContext spanContext = ingressSpan.map(Span::context)
					.orElse(CommonUtils.createDefSpanContext());

				clientSpan[0] = CommonUtils
					.startClientSpan(io.md.constants.Constants.MD_CHILD_SPAN, spanContext, false);

				clientScope[0] = CommonUtils.activateSpan(clientSpan[0]);

				setAuthToken(clientRequestContext, commonConfig);

				if (message != null) {
					message.getExchange().put(MOCK_SPAN, clientSpan[0]);
					message.getExchange().put(MOCK_SCOPE, clientScope[0]);
				}

			}
			//);
		} catch (Exception e) {
			LOGGER.error(
				io.md.constants.Constants.MESSAGE + ":Error occurred in Mocking filter\n" +
				io.md.constants.Constants.EXCEPTION_STACK, e
			);
		}
	}

	private void setAuthToken(ClientRequestContext clientRequestContext,
		CommonConfig commonConfig) {
		if (commonConfig.authToken.isPresent()) {
			String auth = commonConfig.authToken.get();
			MultivaluedMap<String, Object> clientHeaders = clientRequestContext
				.getHeaders();

			clientHeaders.put(Constants.AUTHORIZATION_HEADER, Collections.singletonList(auth));
		} else {
			LOGGER.info("Auth token not present for Mocking service");
		}
	}

	@Override
	public void filter(ClientRequestContext clientRequestContext,
		ClientResponseContext clientResponseContext) {
		LOGGER.debug("Inside Egress Mocking Response filter");
		Message message = PhaseInterceptorChain.getCurrentMessage();
		if (message != null) {
			Object span = message.getExchange().get(MOCK_SPAN);
			Object scope = message.getExchange().get(MOCK_SCOPE);

			if (span != null && scope != null) {
				closeSpanAndScope((Span) span, (Scope) scope);
			}
		}

	}
}
