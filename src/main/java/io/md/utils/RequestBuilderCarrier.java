package io.md.utils;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.client.Invocation;

public class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {
	private final Invocation.Builder builder;

	RequestBuilderCarrier(Invocation.Builder builder) {
		this.builder = builder;
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("carrier is write-only");
	}

	@Override
	public void put(String key, String value) {
		builder.header(key, value);
	}
}
