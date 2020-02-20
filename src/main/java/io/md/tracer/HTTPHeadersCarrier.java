package io.md.tracer;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import io.opentracing.propagation.TextMap;

public class HTTPHeadersCarrier implements TextMap {

	private MultivaluedMap<String, String> headers;

	public HTTPHeadersCarrier(MultivaluedMap<String, String> headers) {
		this.headers = headers;
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("Should be used only with tracer#inject()");
	}

	@Override
	public void put(String key, String value) {
		headers.add(key, value);
	}
}
