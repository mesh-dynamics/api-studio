package io.cube.apachecxf.egress;


import static io.cube.apachecxf.egress.Utils.closeSpanAndScope;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import io.md.logger.LogMgr;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.cxf.jaxrs.client.spec.ClientRequestContextImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.WriterInterceptorContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.md.constants.Constants;
import io.md.dao.MDTraceInfo;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;


/**
 * Priority is to specify in which order the filters are to be executed. Lower the order, early the
 * filter is executed. We want Client filter to execute before Mock and Tracing Filter.
 **/
@Provider
@Priority(value = 4500)
public class MDClientLoggingFilter implements WriterInterceptor, ClientRequestFilter,
	ClientResponseFilter {

	private static final Logger LOGGER = LogMgr.getLogger(MDClientLoggingFilter.class);

	@Override
	public void aroundWriteTo(WriterInterceptorContext context) throws IOException {

		MutableBoolean didContextProceed = new MutableBoolean(false);

		try {
			LOGGER.debug("aroundWriteTo : Inside Egress Logging request filter");
			ClientRequestContext reqContext = null;
			Message message = null;
			//get the request context
			if (context instanceof WriterInterceptorContextImpl) {
				message = PhaseInterceptorChain.getCurrentMessage();
				reqContext = new ClientRequestContextImpl(message.getExchange().getOutMessage(),
					true);
			}

			if (reqContext != null) {
				recordRequest(message, context, reqContext, didContextProceed);
			} else {
				LOGGER.debug("Unable to fetch request context!");
			}
		} catch (Exception e) {
			LOGGER.error(
				Constants.MESSAGE + ":Exception occurred in interceptor\n" +
					Constants.EXCEPTION_STACK, e);
		} finally {
			if (didContextProceed.isFalse()) {
				LOGGER.debug(
					io.md.constants.Constants.MESSAGE
						+ ":Proceeding context in aroundWriteTo finally"
				);
				context.proceed();
			}
		}
	}

	private void logRequest(WriterInterceptorContext writerInterceptorContext,
		ClientRequestContext clientRequestContext, String apiPath,
		String cRequestId, MultivaluedMap<String, String> queryParams, MDTraceInfo mdTraceInfo,
		String serviceName, MutableBoolean didContextProceed)
		throws IOException {
		Span span = io.cube.agent.Utils.createPerformanceSpan(Constants.PROCESS_REQUEST_EGRESS);
		didContextProceed.setFalse();
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			//hdrs
			MultivaluedMap<String, String> requestHeaders = getStrHeaders(clientRequestContext);

			//meta
			MultivaluedMap<String, String> meta = Utils
				.getRequestMeta(clientRequestContext.getMethod(), cRequestId,
					Optional.ofNullable(serviceName));

			//body
			byte[] requestBody = new byte[0];

			//we pass null for GET requests
			if (writerInterceptorContext != null) {
				requestBody = getRequestBody(writerInterceptorContext, didContextProceed);
			}

			Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta, mdTraceInfo,
				requestBody);
		} finally {
			span.finish();
		}
	}

	private byte[] getRequestBody(WriterInterceptorContext interceptorContext,
		MutableBoolean didContextProceed) throws IOException {
		final Span span = io.cube.agent.Utils.createPerformanceSpan(
			Constants.COPY_REQUEST_BODY_EGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			OutputStream originalStream = interceptorContext.getOutputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] reqBody;
			interceptorContext.setOutputStream(baos);
			try {
				didContextProceed.setTrue();
				interceptorContext.proceed();
			} catch (Exception e) {
				LOGGER.error(
					io.md.constants.Constants.MESSAGE + ":Error occurred in context proceed\n" +
						Constants.EXCEPTION_STACK, e
				);
			} finally {
				reqBody = baos.toByteArray();
				baos.writeTo(originalStream);
				baos.close();
				interceptorContext.setOutputStream(originalStream);
			}
			return reqBody;
		} finally {
			span.finish();
		}
	}

	@Override
	public void filter(ClientRequestContext clientRequestContext,
		ClientResponseContext clientResponseContext) throws IOException {
		Span span = null;
		Scope scope = null;
		Message message = null;

		try {
			LOGGER.debug("Inside Egress Logging response filter");
			message = PhaseInterceptorChain.getCurrentMessage();
			span = (Span) message.getExchange().get(Constants.MD_CHILD_SPAN);
			scope = (Scope) message.getExchange().get(Constants.MD_SCOPE);
			if (message.getExchange().get(Constants.MD_SAMPLE_REQUEST) != null) {
				// Do not log response in case the egress serivce is to be mocked
				Object requestURI = message.getExchange().get(Message.REQUEST_URI);

				String service = null;
				if (requestURI != null) {
					service = CommonUtils.getEgressServiceName(new URI(requestURI.toString()));
				} else {
					LOGGER.error("Request URI is not available!");
				}

				CommonConfig commonConfig = CommonConfig.getInstance();
				if (commonConfig.shouldMockService(service)) {
					return;
				}
				Object apiPathObj = message.getExchange().get(Constants.MD_API_PATH_PROP);
				Object serviceNameObj = message.getExchange().get(Constants.MD_SERVICE_PROP);
				Object traceMetaMapObj = message.getExchange()
					.get(Constants.MD_TRACE_META_MAP_PROP);
				Object traceInfo = message.getExchange().get(Constants.MD_TRACE_INFO);

				String apiPath = apiPathObj != null ? apiPathObj.toString() : "";
				String serviceName = serviceNameObj != null ? serviceNameObj.toString() : "";

				ObjectMapper mapper = new ObjectMapper();
				MultivaluedMap<String, String> traceMetaMap = traceMetaMapObj != null ? mapper
					.convertValue(traceMetaMapObj, MetadataMap.class)
					: Utils.createEmptyMultivaluedMap();

				MDTraceInfo mdTraceInfo =
					traceInfo != null ? (MDTraceInfo) traceInfo : new MDTraceInfo();

				logResponse(clientResponseContext, apiPath, traceMetaMap, mdTraceInfo, serviceName);
			}
		} catch (Exception e) {
			LOGGER.error(
				Constants.MESSAGE + ":Error occured in intercepting the response\n" +
					Constants.EXCEPTION_STACK, e
			);
		} finally {
			closeSpanAndScope(span, scope);
		}
	}

	private void logResponse(ClientResponseContext responseContext, String apiPath,
		MultivaluedMap<String, String> traceMeta, MDTraceInfo mdTraceInfo, String serviceName)
		throws IOException {
		Span span = io.cube.agent.Utils.createPerformanceSpan(Constants.PROCESS_RESPONSE_EGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			//hdrs
			MultivaluedMap<String, String> responseHeaders = responseContext.getHeaders();

			//meta
			MultivaluedMap<String, String> meta = Utils
				.getResponseMeta(apiPath, String.valueOf(responseContext.getStatus()),
					Optional.ofNullable(serviceName));
			meta.putAll(traceMeta);

			//body
			byte[] responseBody = getResponseBody(responseContext);

			Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, mdTraceInfo, responseBody);
		} finally {
			span.finish();
		}
	}

	private byte[] getResponseBody(ClientResponseContext respContext) {
		Span span = io.cube.agent.Utils.createPerformanceSpan(Constants.COPY_RESPONSE_BODY_EGRESS);
		try (Scope scope = io.cube.agent.Utils.activatePerformanceSpan(span)) {
			try (InputStream entityStream = respContext.getEntityStream()) {
				if (entityStream != null) {
					byte[] respBody = IOUtils.toByteArray(entityStream);
					InputStream in = new ByteArrayInputStream(respBody);
					respContext.setEntityStream(in);
					return respBody;
				}
			} catch (IOException e) {
				LOGGER.error(
					Constants.MESSAGE + ":Failure during reading the response body\n" +
						Constants.REASON, e
				);
				respContext.setEntityStream(new ByteArrayInputStream(new byte[0]));
			}
		} finally {
			span.finish();
		}

		return new byte[0];
	}

	@Override
	public void filter(ClientRequestContext clientRequestContext) throws IOException {
		try {
			LOGGER.debug("Inside Egress Logging request filter : " + clientRequestContext.getUri());
			Message message = PhaseInterceptorChain.getCurrentMessage();
			if (clientRequestContext.getEntity() == null) {
				//aroundWriteTo will not be called, as there will be no body to write.
				//hence have to log the request here. WebClient does not have a provision
				//to create a get request with body, so double logging is not an issue.
				recordRequest(message, null, clientRequestContext, new MutableBoolean(false));
			}
		} catch (Exception e) {
			LOGGER.error(
				Constants.MESSAGE + " Error occurred in intercepting the request\n" +
					Constants.EXCEPTION_STACK, e);
		}
	}

	private void recordRequest(Message message, WriterInterceptorContext writerInterceptorContext,
		ClientRequestContext clientRequestContext, MutableBoolean didContextProceed)
		throws IOException {

		Span newClientSpan = null;
		Scope newClientScope = null;
		try {
			if (isMockingON(message, didContextProceed)) {
				return;
			}

			Optional<Span> ingressSpan = CommonUtils.getCurrentSpan();
			if (!ingressSpan.isPresent()) {
				LOGGER.debug(
					Constants.MESSAGE + ":Ingress span not set. Creating default Span context"
				);
			}

			SpanContext spanContext = ingressSpan.map(Span::context).orElse(null);

			MultivaluedMap<String, String> strHeaders = getStrHeaders(clientRequestContext);

			String externalIdField = strHeaders.getFirst(CommonConfig.externalIdField);
			if (externalIdField != null) {
				newClientSpan = CommonUtils.externalIdToSpan.get(externalIdField);
			}

			LOGGER.debug("Client span is null!");

			if (newClientSpan == null) {
				newClientSpan = CommonUtils
					.startClientSpan(Constants.MD_CHILD_SPAN, spanContext, false);
			}

			newClientScope = CommonUtils.activateSpan(newClientSpan);

			//Ingress span not passed, hence have to run sampling separately for egress
			getOrRunSampling(clientRequestContext, newClientSpan);

			//Either baggage has sampling set to true or this service uses its veto power to sample.
			boolean isSampled = BooleanUtils
				.toBoolean(newClientSpan.getBaggageItem(Constants.MD_IS_SAMPLED));
			boolean isVetoed = BooleanUtils
				.toBoolean(newClientSpan.getBaggageItem(Constants.MD_IS_VETOED));

			if (isSampled || isVetoed) {
				gatherDataAndLogRequest(message, writerInterceptorContext, clientRequestContext,
					didContextProceed, newClientSpan, newClientScope);
			} else {
				LOGGER.debug(
					Constants.MESSAGE + ": Sampling is false!"
				);
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	private void getOrRunSampling(ClientRequestContext reqContext, Span span) {
		Optional<String> fieldCategory = CommonConfig.getInstance().sampler.getFieldCategory();
		String sampleBaggageItem = span.getBaggageItem(Constants.MD_IS_SAMPLED);
		if (sampleBaggageItem == null) {
			//root span
			boolean isSampled = runSampling(reqContext, fieldCategory);
			span.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
		} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && CommonConfig.getInstance().samplerVeto) {
			span.setBaggageItem(Constants.MD_IS_VETOED,
				String.valueOf(runSampling(reqContext, fieldCategory)));
		}
	}

	private boolean runSampling(ClientRequestContext reqContext, Optional<String> fieldCategory) {
		boolean isSampled;
		if (!fieldCategory.isPresent()) {
			isSampled = Utils.isSampled(new MultivaluedHashMap<>());
		} else {
			switch (fieldCategory.get()) {
				case Constants.HEADERS:
					isSampled = Utils.isSampled(reqContext.getStringHeaders());
					break;
				case Constants.QUERY_PARAMS:
					isSampled = Utils.isSampled(Utils.getQueryParams(reqContext.getUri()));
					break;
				case Constants.API_PATH_FIELD:
					String apiPath = reqContext.getUri().getPath();
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

	private void gatherDataAndLogRequest(Message message,
		WriterInterceptorContext writerInterceptorContext,
		ClientRequestContext clientRequestContext, MutableBoolean didContextProceed,
		Span newClientSpan, Scope newClientScope)
		throws IOException {
		newClientSpan.setBaggageItem(Constants.MD_IS_VETOED, null);

		MultivaluedMap<String, String> strHeaders = getStrHeaders(clientRequestContext);

		URI uri = clientRequestContext.getUri();

		//query params
		MultivaluedMap<String, String> queryParams = Utils.getQueryParams(uri);

		//path
		String apiPath = uri.getPath();

		//serviceName to be host+port for outgoing calls
		String serviceName = CommonUtils.getEgressServiceName(uri);

		MDTraceInfo mdTraceInfo = io.md.utils.Utils.getTraceInfo(newClientSpan);

		String xRequestId = strHeaders.getFirst(Constants.X_REQUEST_ID);
		MultivaluedMap<String, String> traceMetaMap = Utils
			.buildTraceInfoMap(mdTraceInfo, xRequestId);

		//one Exchange per request created. So, no need to clear.
		message.getExchange().put(Constants.MD_SERVICE_PROP, serviceName);
		message
			.getExchange().put(Constants.MD_TRACE_META_MAP_PROP, traceMetaMap);
		message.getExchange().put(Constants.MD_API_PATH_PROP, apiPath);
		message.getExchange().put(Constants.MD_SAMPLE_REQUEST, true);
		message.getExchange().put(Constants.MD_TRACE_INFO, mdTraceInfo);
		message.getExchange().put(Constants.MD_CHILD_SPAN, newClientSpan);
		message.getExchange().put(Constants.MD_SCOPE, newClientScope);

		logRequest(writerInterceptorContext, clientRequestContext, apiPath,
			traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID), queryParams,
			mdTraceInfo, serviceName, didContextProceed);

		newClientSpan
			.setBaggageItem(Constants.MD_PARENT_SPAN, newClientSpan.context().toSpanId());
	}

	private boolean isMockingON(Message message, MutableBoolean didContextProceed)
		throws URISyntaxException {
		Object uriValue = message.get(Message.REQUEST_URI);
		String service = uriValue == null ? null
			: CommonUtils.getEgressServiceName(URI.create(uriValue.toString()));
		CommonConfig commonConfig = CommonConfig.getInstance();
		if (commonConfig.shouldMockService(service) || (service != null && uriValue.toString()
			.startsWith(new URI(commonConfig.CUBE_MOCK_SERVICE_URI).toString()))) {
			didContextProceed.setFalse();
			return true;
		}
		return false;
	}

	private MultivaluedMap<String, String> getStrHeaders(
		ClientRequestContext clientRequestContext) {
		MultivaluedMap<String, String> strHeaders = new MultivaluedHashMap<>();
		//Don't use getStringHeaders() as the lib code is buggy if the header has empty/null value
		MultivaluedMap<String, Object> headers = clientRequestContext.getHeaders();
		headers.entrySet().forEach(entry -> {
			String key = entry.getKey();
			List<Object> values = entry.getValue();
			if (values != null) {
				for (Object value : values) {
					if (value != null) {
						strHeaders.add(key, value.toString());
					}
				}
			}
		});
		return strHeaders;
	}

}