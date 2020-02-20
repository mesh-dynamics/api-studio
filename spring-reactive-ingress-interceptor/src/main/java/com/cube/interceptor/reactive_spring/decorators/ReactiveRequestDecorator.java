package com.cube.interceptor.reactive_spring.decorators;

import static com.cube.interceptor.reactive_spring.ReactiveSpringLoggingFilter.logRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;

import reactor.core.publisher.Flux;

public class ReactiveRequestDecorator extends ServerHttpRequestDecorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveRequestDecorator.class);
	private String finalUri;
	private String cRequestId;
	private String serviceName;
	private MultivaluedMap<String, String> queryParams;

	public ReactiveRequestDecorator(ServerHttpRequest delegate, String finalUri, String cRequestId,
		MultivaluedMap<String, String> queryParams) {
		super(delegate);
		this.finalUri = finalUri;
		this.cRequestId = cRequestId;
		this.serviceName = serviceName;
		this.queryParams = queryParams;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		return super.getBody().doOnNext(dataBuffer -> {
			try {
				Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
				logRequest(getDelegate(), baos, finalUri, cRequestId, queryParams);
			} catch (IOException e) {
				LOGGER.error("Unable to log input request due to an error", e);
			} finally {
				try {
					baos.close();
				} catch (IOException e) {
					LOGGER.error("Unable to log input request due to an error", e);
				}
			}
		});
	}

}
