package io.md.utils;

import java.net.http.HttpRequest;
import java.util.Iterator;
import java.util.Map.Entry;

import io.opentracing.propagation.TextMap;

public class RequestCarrier implements TextMap {

	private final HttpRequest.Builder requestBuilder;

	public RequestCarrier(HttpRequest.Builder requestBuilder) {
		this.requestBuilder = requestBuilder;
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("carrier is write-only");
	}


	@Override
	public void put(String key, String value) {
		requestBuilder.setHeader(key, value);
	}
}
