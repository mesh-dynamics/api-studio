package com.cube.interceptor.reactive_spring.decorators;

import static com.cube.interceptor.reactive_spring.ReactiveSpringLoggingFilter.logResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.function.Function;

import javax.ws.rs.core.MultivaluedMap;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveResponseDecorator extends ServerHttpResponseDecorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReactiveResponseDecorator.class);
	private String finalUri;
	private MultivaluedMap<String, String> traceMetaMap;

	public ReactiveResponseDecorator(ServerHttpResponse delegate, String finalUri,
		MultivaluedMap<String, String> traceMetaMap) {
		super(delegate);
		this.finalUri = finalUri;
		this.traceMetaMap = traceMetaMap;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		return super.writeWith(Flux.from(body).map(writeToLog()));
	}

	@Override
	public Mono<Void> writeAndFlushWith(
		Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return super.writeAndFlushWith(Flux.from(body).map(x ->
			Flux.from(x).map(writeToLog())
		));
	}

	private Function<DataBuffer, DataBuffer> writeToLog() {
		return dataBuffer -> {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				Channels.newChannel(baos).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
				logResponse(getDelegate(), baos, finalUri, traceMetaMap);
			} catch (IOException e) {
				LOGGER.error("Unable to log input response due to an error", e);
			} finally {
				try {
					baos.close();
				} catch (IOException e) {
					LOGGER.error("Unable to log input response due to an error", e);
				}
			}
			return dataBuffer;
		};
	}
}
