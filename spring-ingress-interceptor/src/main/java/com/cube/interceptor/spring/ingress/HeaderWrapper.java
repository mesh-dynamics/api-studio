package com.cube.interceptor.spring.ingress;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class HeaderWrapper extends HttpServletRequestWrapper {

	private final MultivaluedMap<String, String> customHeaders;

	public HeaderWrapper(HttpServletRequest request) {
		super(request);
		this.customHeaders = new MultivaluedHashMap<>();
	}

	public void putHeader(String name, List<String> value) {
		this.customHeaders.put(name, value);
	}

	@Override
	public String getHeader(String name) {
		String headerValue = customHeaders.getFirst(name);

		if (headerValue != null) {
			return headerValue;
		}

		return super.getHeader(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		List<String> names = Collections.list(super.getHeaderNames());
		names.addAll(customHeaders.keySet());
		return Collections.enumeration(names);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		Set<String> headerValues = new HashSet<>();
		List<String> customHeaderValues = this.customHeaders.get(name);
		if (customHeaderValues != null) {
			headerValues.addAll(customHeaderValues);
		}

		headerValues.addAll(Collections.list(((HttpServletRequest) getRequest()).getHeaders(name)));

		return Collections.enumeration(headerValues);
	}

	public MultivaluedMap<String, String> headersToMultiMap() {
		MultivaluedMap<String, String> headerMap = new MultivaluedHashMap<>();
		Enumeration<String> headerNames = getHeaderNames();
		Collections.list(headerNames).stream().forEach(headerName -> {
			Enumeration<String> headerValues = this.getHeaders(headerName);
			headerMap.addAll(headerName, Collections.list(headerValues));
		});

		return headerMap;
	}
}
