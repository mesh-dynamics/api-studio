package io.md.dao;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

public class RequestDetails {
	public final URI uri;
	public final byte[] body;
	public final MultivaluedMap<String,String> headers;
	public final String method;

	public RequestDetails(URI uri , byte[] body , String method , MultivaluedMap<String,String> headers){
		this.uri = uri;
		this.body = body;
		this.method = method;
		this.headers = headers;
	}

}
