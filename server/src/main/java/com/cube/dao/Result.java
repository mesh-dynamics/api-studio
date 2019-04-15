/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.stream.Stream;

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
	
	final Stream<T> objects;
	final long numResults;
	final long numFound;

	/**
	 * @return
	 */
	public Stream<T> getObjects() {
		return objects;
	}

	/**
	 * @return
	 */
	public long numResults() {
		return numResults;
	}
}
