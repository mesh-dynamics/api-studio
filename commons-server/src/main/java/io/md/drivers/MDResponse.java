package io.md.drivers;

import javax.ws.rs.core.MultivaluedMap;

public interface MDResponse {

	byte[] getBody();

	MultivaluedMap<String, String> getHeaders();

	String getPath();

	Integer statusCode();

	MultivaluedMap<String , String> getTrailers();

}
