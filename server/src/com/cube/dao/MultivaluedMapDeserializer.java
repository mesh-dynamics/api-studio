/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author prasad
 *
 */
public class MultivaluedMapDeserializer extends JsonDeserializer<MultivaluedMap<String, String>> {

	/* (non-Javadoc)
	 * @see com.fasterxml.jackson.databind.JsonDeserializer#deserialize(com.fasterxml.jackson.core.JsonParser, com.fasterxml.jackson.databind.DeserializationContext)
	 */
	@Override
	public MultivaluedMap<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectMapper m = new ObjectMapper();
		TypeReference<MultivaluedHashMap<String, String>> typeRef 
		  = new TypeReference<MultivaluedHashMap<String, String>>() {};
		return m.readValue(p, typeRef);
	}

}
