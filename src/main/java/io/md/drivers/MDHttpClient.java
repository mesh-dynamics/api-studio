package io.md.drivers;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.dao.RequestDetails;


public abstract class MDHttpClient {
	public final URI uri;

	public final String method;

	public final byte[] body;

	public final MultivaluedMap<String , String> headers;


	public MDHttpClient(RequestDetails details){
		this(details.uri , details.method, details.body, details.headers);
	}

	public MDHttpClient(URI uri , String method, byte[] body , MultivaluedMap<String , String> headers){
		this.uri = uri;
		this.method = method.toUpperCase();
		this.body = body;
		this.headers = headers!=null ? headers : new MultivaluedHashMap<>();
	}

	public abstract MDResponse makeRequest() throws Exception;

	public abstract CompletableFuture<MDResponse> makeRequestAsync();
}

