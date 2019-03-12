/**
 * Copyright Cube I O
 */
package com.cube.core;

import com.cube.dao.Response;

/**
 * @author prasad
 *
 */
public interface ResponseComparator {

	static ResponseComparator EQUALITYCOMPARATOR = new EqualityResponseComparator();

	/**
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	Comparator.Match compare(Response lhs, Response rhs);

}
