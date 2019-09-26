/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * @author prasad
 *
 */
public class Result<T> {
			
	/**
	 * @param objects
	 * @param numresults
	 * @param numFound
	 */
	public Result(Stream<T> objects, long numresults, long numFound) {
		super();
		this.objects = objects;
		this.numResults = numresults;
		this.numFound = numFound;
	}

	@JsonSerialize(using = StreamToListSerializer.class)
	final Stream<T> objects;
	public final long numResults;  // number of results
	public final long numFound; // number of results possible in no limit is passed

	/**
	 * @return
	 */
	public Stream<T> getObjects() {
		return objects;
	}

    static public class StreamToListSerializer extends JsonSerializer<Stream<Object>> {
        public StreamToListSerializer() {
        }

        @Override
        public void serialize(Stream<Object> objStream, JsonGenerator jsonGenerator,
                               SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeObject(objStream.collect(Collectors.toList()));
        }
    }

}
