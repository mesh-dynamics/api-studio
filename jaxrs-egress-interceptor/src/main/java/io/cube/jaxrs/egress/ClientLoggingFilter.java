package io.cube.jaxrs.egress;

import static io.md.utils.Utils.getTraceInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;

@Priority(4000)
public class ClientLoggingFilter implements ClientRequestFilter, ClientResponseFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClientLoggingFilter.class);

	private static final Config config = new Config();

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		try {
			Optional<Span> ingressSpan = CommonUtils.getCurrentSpan();

			//Empty ingress span pertains to DB initialization scenarios.
			SpanContext spanContext = ingressSpan.map(Span::context)
				.orElse(CommonUtils.createDefSpanContext());

			Span clientSpan = CommonUtils
				.startClientSpan(Constants.MD_CHILD_SPAN, spanContext, false);

			Scope clientScope = CommonUtils.activateSpan(clientSpan);

			// Do not log request in case the egress service is to be mocked
			String service = CommonUtils.getEgressServiceName(requestContext.getUri());
			CommonConfig commonConfig = CommonConfig.getInstance();
			if (commonConfig.shouldMockService(service)) {
				LOGGER.info("Mocking in progress, not logging the request!");
				return;
			}

			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
				.toBoolean(clientSpan.getBaggageItem(Constants.MD_IS_SAMPLED));
			boolean isVetoed = BooleanUtils
				.toBoolean(clientSpan.getBaggageItem(Constants.MD_IS_VETOED));

			//Empty ingress span pertains to DB initialization scenarios.
			//So need to record all calls as these will not be driven by replay driver.
			if (isSampled || isVetoed || !ingressSpan.isPresent()) {
				//this is local baggage item
				clientSpan.setBaggageItem(Constants.MD_IS_VETOED, null);

				//hdrs
				MultivaluedMap<String, String> requestHeaders = requestContext
					.getStringHeaders();

				URI uri = requestContext.getUri();

				//query params
				MultivaluedMap<String, String> queryParams = Utils.getQueryParams(uri);

				//path
				String apiPath = uri.getPath();

				//serviceName to be host+port for outgoing calls
				String serviceName = CommonUtils.getEgressServiceName(uri);

				MDTraceInfo mdTraceInfo = getTraceInfo(clientSpan);

				MultivaluedMap<String, String> traceMetaMap = CommonUtils
					.buildTraceInfoMap(serviceName, mdTraceInfo,
						requestHeaders.getFirst(Constants.X_REQUEST_ID));

				requestContext.setProperty(Constants.MD_SERVICE_PROP, serviceName);
				requestContext.setProperty(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
				requestContext.setProperty(Constants.MD_API_PATH_PROP, apiPath);
				requestContext.setProperty(Constants.MD_SAMPLE_REQUEST, true);
				requestContext.setProperty(Constants.MD_TRACE_INFO, mdTraceInfo);
				requestContext.setProperty(Constants.MD_CHILD_SPAN, clientSpan);
				requestContext.setProperty(Constants.MD_SCOPE, clientScope);

				final OutputStream stream = new ClientLoggingStream(
					requestContext.getEntityStream());
				requestContext.setEntityStream(stream);
				requestContext.setProperty(Constants.MD_LOG_STREAM_PROP, stream);

				logRequest(requestContext, apiPath,
					traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), requestHeaders,
					queryParams, mdTraceInfo, serviceName);

				//Setting the current span id as parent span id value
				//This is intentionally set after the capture as it should be parent for subsequent capture
				//condiitional to avoid setting span for DB initialization scenarios
				clientSpan
					.setBaggageItem(Constants.MD_PARENT_SPAN, clientSpan.context().toSpanId());
			}

		} catch (Exception ex) {
			LOGGER.error("Exception occured during logging request!", ex);
		}
	}

	@Override
	public void filter(ClientRequestContext requestContext,
		ClientResponseContext respContext) throws IOException {
		Span span = null;
		Scope scope = null;
		try {
			if (requestContext.getProperty(Constants.MD_SAMPLE_REQUEST) != null) {
				Object apiPathObj = requestContext.getProperty(Constants.MD_API_PATH_PROP);
				Object traceMetaMapObj = requestContext
					.getProperty(Constants.MD_TRACE_META_MAP_PROP);
				Object serviceNameObj = requestContext.getProperty(Constants.MD_SERVICE_PROP);
				Object traceInfo = requestContext.getProperty(Constants.MD_TRACE_INFO);
				span = (Span) requestContext.getProperty(Constants.MD_CHILD_SPAN);
				scope = (Scope) requestContext.getProperty(Constants.MD_SCOPE);

				String apiPath = apiPathObj != null ? apiPathObj.toString() : "";
				String serviceName = serviceNameObj != null ? serviceNameObj.toString() : "";
				ObjectMapper mapper = new ObjectMapper();
				MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
					.convertValue(traceMetaMapObj, MultivaluedMap.class)
					: new MultivaluedHashMap<>();
				MDTraceInfo mdTraceInfo =
					traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();
				MultivaluedMap<String, String> responseHeaders = respContext.getHeaders();

				//meta
				MultivaluedMap<String, String> meta = Utils
					.getResponseMeta(apiPath, String.valueOf(respContext.getStatus()),
						Optional.ofNullable(serviceName));
				meta.putAll(traceMetaMap);

				//body
				byte[] responseBody = getResponseBody(respContext);

				Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo,
					responseBody);
				removeSetContextProperty(requestContext);
			}
		} catch (Exception e) {
			LOGGER.error("Exception occured during logging the response!", e);
		} finally {
			if (span != null) {
				span.finish();
			}
			if (scope != null) {
				scope.close();
			}
		}
	}

	private void logRequest(ClientRequestContext requestContext, String apiPath,
		String cRequestId, MultivaluedMap<String, String> requestHeaders,
		MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo,
		String serviceName)
		throws IOException {
		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(requestContext.getMethod(), cRequestId,
				Optional.ofNullable(serviceName));

		//body
		byte[] requestBody = getRequestBody(requestContext);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
			requestBody);
	}

	private byte[] getRequestBody(ClientRequestContext requestContext) {
		if (requestContext.hasEntity()) {
			final ClientLoggingStream stream = (ClientLoggingStream) requestContext
				.getProperty(Constants.MD_LOG_STREAM_PROP);
			return stream.getbytes();
		}
		return new byte[0];
	}

	private void removeSetContextProperty(ClientRequestContext context) {
		context.removeProperty(Constants.MD_API_PATH_PROP);
		context.removeProperty(Constants.MD_TRACE_META_MAP_PROP);
		context.removeProperty(Constants.MD_SERVICE_PROP);
		context.removeProperty(Constants.MD_LOG_STREAM_PROP);
		context.removeProperty(Constants.MD_SAMPLE_REQUEST);
		context.removeProperty(Constants.MD_TRACE_INFO);
	}

	private byte[] getResponseBody(ClientResponseContext respContext) throws IOException {

		byte[] respBody = IOUtils.toByteArray(respContext.getEntityStream());
		InputStream in = new ByteArrayInputStream(respBody);
		respContext.setEntityStream(in);

		return respBody;
	}

}
