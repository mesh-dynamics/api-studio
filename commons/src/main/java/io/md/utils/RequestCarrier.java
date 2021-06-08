package io.md.utils;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.http.HttpRequest;

import io.opentracing.propagation.TextMap;

public class RequestCarrier implements TextMap {

	private final HttpRequest requestBuilder;

	public RequestCarrier(HttpRequest requestBuilder) {
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
