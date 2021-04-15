package io.md.drivers;

import javax.ws.rs.core.MultivaluedMap;

public interface MDResponse {

	public byte[] getBody();

	public MultivaluedMap<String, String> getHeaders();

	public String getPath();

	public Integer statusCode();

	public MultivaluedMap<String , String> getTrailers();

}
