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

package io.md.drivers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.md.core.Utils;

public class MDHttpResponse implements MDResponse {

	private static final Logger LOGGER = LogManager.getLogger(MDHttpResponse.class);
	public static final MultivaluedHashMap<String,String> EMPTY =  new MultivaluedHashMap<>(0);
	private final HttpResponse<byte[]> response;

	public MDHttpResponse(HttpResponse<byte[]> resp) {
		this.response = resp;
	}

	public static String determineContentEncoding(
		HttpHeaders headers) {
		return headers.firstValue("Content-Encoding").orElse("");
	}

	@Override
	public byte[] getBody() {
		byte[] originalBody = response.body();
		return  Utils.decodeResponseBody(originalBody , determineContentEncoding(response.headers())).orElse(originalBody);
	}

	@Override
	public MultivaluedMap<String, String> getHeaders() {
		MultivaluedMap<String, String> responseHeaders = new MultivaluedHashMap<>();
		response.headers().map().forEach((k, v) -> {
			responseHeaders.addAll(k, v);
		});
		return responseHeaders;
	}

	@Override
	public String getPath() {
		return response.uri().getPath();
	}

	@Override
	public Integer statusCode() {
		return response.statusCode();
	}

	@Override
	public MultivaluedMap<String, String> getTrailers() {
		return EMPTY;
	}
}
