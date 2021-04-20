package io.md.drivers;


import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.core5.http.Header;

import io.md.core.Utils;

public class MDHttp2Response implements MDResponse {

	public static final MultivaluedHashMap<String,String> EMPTY =  new MultivaluedHashMap<>();
	private final SimpleHttpResponse response;
	private final String path;
	private final MultivaluedHashMap<String,String> trailers;
	public MDHttp2Response(SimpleHttpResponse response , String path, MultivaluedHashMap<String,String> trailers){
		this.response = response;
		this.path = path;
		this.trailers = trailers;
	}
	@Override
	public byte[] getBody() {
		Header header = response.getFirstHeader("Content-Encoding");
		byte[] originalBody =  response.getBodyBytes();
		return Utils.decodeResponseBody(originalBody , header!=null ? header.getValue() : "").orElse(originalBody) ;
	}

	@Override
	public MultivaluedMap<String, String> getHeaders() {
		MultivaluedMap<String, String> responseHeaders = new MultivaluedHashMap<>();
		for(Header hdr : response.getHeaders()){
			responseHeaders.putSingle(hdr.getName(), hdr.getValue());
		}
		return responseHeaders;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public Integer statusCode() {
		return response.getCode();
	}

	@Override
	public MultivaluedMap<String, String> getTrailers() {
		return trailers;
	}
}
