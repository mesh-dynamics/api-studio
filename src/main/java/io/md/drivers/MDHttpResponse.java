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
