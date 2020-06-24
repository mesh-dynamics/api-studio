package io.cube.apachecxf.egress;

import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;

/**
 * Priority is to specify in which order the filters are to be executed.
 * Lower the order, early the filter is executed.
 * We want Client filter to execute before Tracing Filter.
 **/
@Provider
@Priority(value = 4502)
public class MDClientTracingFilter implements ClientRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MDClientTracingFilter.class);

	@Override
	public void filter(ClientRequestContext clientRequestContext) {
		try {
			LOGGER.info("Inside Egress tracing filter");
			MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
			CommonUtils.injectContext(mdTraceHeaders);
			MultivaluedMap<String, Object> clientHeaders = clientRequestContext.getHeaders();
			for (Map.Entry<String, List<String>> entry : mdTraceHeaders.entrySet()) {
				for (String entValue : entry.getValue()) {
					clientHeaders.add(entry.getKey(), entValue);
				}
			}
		} catch (Exception e) {
			LOGGER.error(
					io.md.constants.Constants.MESSAGE + ":Error occurred in Mocking filter\n" +
					Constants.EXCEPTION_STACK, e
				);
		}
	}
}