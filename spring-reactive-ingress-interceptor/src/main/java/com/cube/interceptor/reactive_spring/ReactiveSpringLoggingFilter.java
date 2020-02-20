package com.cube.interceptor.reactive_spring;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.md.constants.Constants;
import reactor.core.publisher.Mono;

import com.cube.interceptor.reactive_spring.decorators.ReactiveRequestDecorator;
import com.cube.interceptor.reactive_spring.decorators.ReactiveResponseDecorator;
import com.cube.interceptor.utils.Utils;

/**
 * Reference : https://stackoverflow.com/a/47931511/2761431 https://github.com/piomin/spring-boot-logging
 */

@Component
public class ReactiveSpringLoggingFilter implements WebFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveSpringLoggingFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		MultivaluedMap<String, String> requestHeaders = Utils
			.getMultiMap(request.getHeaders().entrySet());

		boolean isSampled = Utils.isSampled(requestHeaders);
		if (isSampled) {

			URI uri = request.getURI();

			//query params
			MultivaluedMap<String, String> queryParams = Utils.getQueryParams(uri);

			//path
			String apiPath = uri.getPath();

			MultivaluedMap<String, String> traceMetaMap = Utils.getTraceInfoMetaMap(request);

			String cRequestId = traceMetaMap.getFirst(Constants.DEFAULT_REQUEST_ID);

			ByteArrayOutputStream reqBaos = new ByteArrayOutputStream();
			ByteArrayOutputStream respBaos = new ByteArrayOutputStream();

			//Spring reactive does not call getBody() if the request type is GET
			//We workaround by checking if the method is GET. We also log
			//if the POST request body length is 0
			List<String> reqBodyLength = request.getHeaders().get("Content-Length");
			if (request.getMethodValue().equalsIgnoreCase("GET") || (reqBodyLength == null
				|| reqBodyLength.get(0).equals("0"))) {
				logRequest(request, reqBaos, apiPath, cRequestId, queryParams);
			}

			ServerWebExchangeDecorator exchangeDecorator = new ServerWebExchangeDecorator(
				exchange) {
				@Override
				public ServerHttpRequest getRequest() {
					return new ReactiveRequestDecorator(super.getRequest(), apiPath, cRequestId,
						queryParams);
				}

				@Override
				public ServerHttpResponse getResponse() {
					return new ReactiveResponseDecorator(super.getResponse(), apiPath,
						traceMetaMap);
				}
			};

			return chain.filter(exchangeDecorator)
				.doOnSuccess(success -> {
					writeToLog(apiPath, traceMetaMap, respBaos, exchangeDecorator);
				})
				.doOnError(throwable -> {
					writeToLog(apiPath, traceMetaMap, respBaos, exchangeDecorator);
				});
		} else {
			return chain.filter(exchange);
		}
	}

	private void writeToLog(String finalUri, MultivaluedMap<String, String> traceMetaMap,
		ByteArrayOutputStream respBaos, ServerWebExchangeDecorator exchangeDecorator) {
		ServerHttpResponse response = exchangeDecorator.getResponse();
		List<String> respbodyLength = response.getHeaders().get("Content-Length");
		if ((respbodyLength == null || respbodyLength.get(0).equals("0"))) {
			logResponse(response, respBaos, finalUri, traceMetaMap);
		}
	}

	public static void logRequest(ServerHttpRequest request, ByteArrayOutputStream reqBaos,
		String apiPath, String cRequestId,
		MultivaluedMap<String, String> queryParams) {
		//hdrs
		MultivaluedMap<String, String> requestHeaders = Utils
			.getMultiMap(request.getHeaders().entrySet());

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getRequestMeta(request.getMethodValue(), cRequestId, Optional.empty());

		//body
		String requestBody = reqBaos.toString(StandardCharsets.UTF_8);

		Utils.createAndLogReqEvent(apiPath, queryParams, requestHeaders, meta,
			requestBody);
	}

	public static Mono<Void> logResponse(ServerHttpResponse response,
		ByteArrayOutputStream respBaos,
		String apiPath, MultivaluedMap<String, String> traceMeta) {

		//hdrs
		MultivaluedMap<String, String> responseHeaders = Utils
			.getMultiMap(response.getHeaders().entrySet());

		//meta
		MultivaluedMap<String, String> meta = Utils
			.getResponseMeta(apiPath, String.valueOf(response.getStatusCode().value()),
				Optional.empty());
		meta.putAll(traceMeta);

		//body
		String responseBody = respBaos.toString(StandardCharsets.UTF_8);

		Utils.createAndLogRespEvent(apiPath, responseHeaders, meta, responseBody);
		return Mono.empty();
	}

}
