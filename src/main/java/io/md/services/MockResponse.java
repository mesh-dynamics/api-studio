package io.md.services;

import java.util.Optional;

import io.md.dao.Event;

/*
 * Created by IntelliJ IDEA.
 * Date: 2020-05-27
 */
public class MockResponse {

	final public Optional<Event> response;

	final public long numResults;


	public MockResponse(Optional<Event> response, long numResults) {
		this.response = response;
		this.numResults = numResults;
	}

	// default constructor for jackson
	public MockResponse() {
		response = Optional.empty();
		numResults = 0;
	}
}
