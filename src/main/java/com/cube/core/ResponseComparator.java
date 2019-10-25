/**
 * Copyright Cube I O
 */
package com.cube.core;

import com.cube.dao.DataObj;
import com.cube.dao.Response;

/**
 * @author prasad
 *
 */
// TODO: Event redesign: This can be removed
public interface ResponseComparator {

	static ResponseComparator EQUALITYCOMPARATOR = new EqualityResponseComparator();

	/**
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	Comparator.Match compare(Response lhs, Response rhs);

    CompareTemplate getCompareTemplate();

    Comparator.Match compare(DataObj lhs, DataObj rhs);

}
