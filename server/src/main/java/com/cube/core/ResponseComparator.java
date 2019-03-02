/**
 * Copyright Cube I O
 */
package com.cube.core;

import com.cube.dao.Response;
import com.cube.drivers.Analysis.RespMatch;

/**
 * @author prasad
 *
 */
public interface ResponseComparator {

	static ResponseComparator EQUALITYCOMPARATOR = new EqualityResponseComparator();

	/**
	 * @param recordedr
	 * @param replayr
	 * @return
	 */
	RespMatch compare(Response lhs, Response rhs);

}
