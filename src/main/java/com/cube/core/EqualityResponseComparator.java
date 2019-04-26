/**
 * Copyright Cube I O
 */
package com.cube.core;

import com.cube.dao.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Collections;
import java.util.List;

/**
 * @author prasad
 *
 */
public class EqualityResponseComparator implements ResponseComparator {

	/* (non-Javadoc)
	 * @see com.cube.core.ResponseComparator#compare(com.cube.dao.Response, com.cube.dao.Response)
	 */
	@Override
	public Comparator.Match compare(Response lhs, Response rhs) {

		if (lhs.body.equals(rhs.body)) {
			return new Comparator.Match(Comparator.MatchType.ExactMatch, "EqualityMatch", Collections.emptyList());
		} else {
			return new Comparator.Match(Comparator.MatchType.NoMatch, "EqualityMatch",
					List.of(new Comparator.Diff(Comparator.Diff.REPLACE, "/body", TextNode.valueOf(rhs.body), Comparator.Resolution.ERR_ValMismatch)));
		}
		
	}

}
