/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.md.services.DSResult;

/**
 * @author prasad
 *
 */
public class Result<T> implements DSResult<T> {

	/**
	 * @param objects
	 * @param numresults
	 * @param numFound
	 */
	public Result(Stream<T> objects, long numresults, long numFound, QueryResponse solrResponse) {
		super();
		this.objects = objects;
		this.numResults = numresults;
		this.numFound = numFound;
		this.solrResponse = solrResponse;
	}

	@JsonSerialize(using = StreamToListSerializer.class)
	final Stream<T> objects;
	public final long numResults;  // number of results
	public final long numFound; // number of results possible in no limit is passed
	final QueryResponse solrResponse;

	/**
	 * @return
	 */
	public Stream<T> getObjects() {
		return objects;
	}

    @Override
    public long getNumResults() {
        return numResults;
    }

    @Override
    public long getNumFound() {
        return numFound;
    }

    /**
	 *
	 * @param args - variable number of string arguments for recursive search
	 * @return
	 */
	public ArrayList getFacets(String... args) {
		ArrayList facetsNamedList = (ArrayList) solrResponse.getResponse().
			findRecursive(args);

		return solrNamedPairToMap(facetsNamedList);
	}

	public ArrayList solrNamedPairToMap(ArrayList facetsNamedList) {
		ArrayList facets = new ArrayList();
		if(facetsNamedList==null) return facets;

		// Required for Jackson serialization. Jackson is failing to serialize namedList directly
		facetsNamedList.forEach(diffResFacet -> {
			facets.add((((NamedList) diffResFacet).asMap(2)));
		});

		return facets;
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
