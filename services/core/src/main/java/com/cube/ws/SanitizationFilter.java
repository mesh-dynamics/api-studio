package com.cube.ws;

import java.util.Set;

import io.md.dao.Event;

public interface SanitizationFilter {

	public boolean consume(Event event);

	public Set<String> getBadReqIds();
}
