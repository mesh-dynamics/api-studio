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
	 */
	public Result(Stream<T> objects, long numresults) {
		super();
		this.objects = objects;
		this.numResults = numresults;
	}
	
	final Stream<T> objects;
	final long numResults;
	
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
