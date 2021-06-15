/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.interceptor.reactive_spring.decorators;

import static com.cube.interceptor.reactive_spring.ReactiveSpringLoggingFilter.logRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;

import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.slf4j.Logger;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;

import reactor.core.publisher.Flux;

public class ReactiveRequestDecorator extends ServerHttpRequestDecorator {

	private static final Logger LOGGER = LogMgr.getLogger(ReactiveRequestDecorator.class);
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
