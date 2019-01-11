/**
 * Copyright Cube I O
 */
package com.cube.core;

import com.cube.dao.Response;
import com.cube.drivers.Analysis.RespMatch;
import com.cube.drivers.Analysis.RespMatchType;

/**
 * @author prasad
 *
 */
public class EqualityResponseComparator implements ResponseComparator {

	/* (non-Javadoc)
	 * @see com.cube.core.ResponseComparator#compare(com.cube.dao.Response, com.cube.dao.Response)
	 */
	@Override
	public RespMatch compare(Response lhs, Response rhs) {
		
		RespMatchType res = (lhs.body.equals(rhs.body)) ? 
				RespMatchType.ExactMatch : RespMatchType.NoMatch;
		
		return new RespMatch(res, "");
		
	}

}
