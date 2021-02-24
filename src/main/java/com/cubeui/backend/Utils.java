package com.cubeui.backend;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.springframework.http.ResponseEntity;

import io.md.constants.Constants;

public class Utils {

	public static MultivaluedMap<String, String> extractTrailerDataFromHeaders(
		ResponseEntity responseEntity) {
		MultivaluedMap<String, String> trailersMultiValuedMap = new MultivaluedHashMap<>();
		responseEntity.getHeaders().entrySet().stream().filter(entry -> entry.getKey().startsWith(
			Constants.MD_TRAILER_HEADER_PREFIX)).forEach(entry -> {
			String realTrailerKey = entry.getKey()
				.substring(Constants.MD_TRAILER_HEADER_PREFIX.length());
			entry.getValue().forEach(
				value -> trailersMultiValuedMap.add(realTrailerKey, String.valueOf(value)));
		});
		return trailersMultiValuedMap;
	}

	public static void addTrailers(ResponseEntity responseEntity, HttpServletResponse response) {
		MultivaluedMap<String, String> trailersMultiValuedMap = extractTrailerDataFromHeaders(
			responseEntity);
		//Add trailers for GRPC handling
		if (response != null) {
			// "Trailer" headers already set in cubeio
			// https://javaee.github.io/tutorial/servlets014b.html
			response.setTrailerFields(() -> {
				Map<String, String> trailersMap = new HashMap<>();
				for (String key : trailersMultiValuedMap.keySet()) {
					trailersMap.put(key, trailersMultiValuedMap.getFirst(key));
				}
				return trailersMap;
			});
		}
	}

}
