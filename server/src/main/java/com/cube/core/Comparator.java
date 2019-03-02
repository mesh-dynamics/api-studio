/**
 * Copyright Cube I O
 */
package com.cube.core;

/**
 * @author prasad
 *
 */
public interface Comparator {

	
	/**
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	Match compare(String lhs, String rhs);
	
	public enum MatchType {
		ExactMatch,
		FuzzyMatch,
		NoMatch,
		Exception
	}
	
	public static class Match {
		
		/**
		 * @param mt
		 * @param matchmeta
		 */
		public Match(MatchType mt, String matchmeta) {
			super();
			this.mt = mt;
			this.matchmeta = matchmeta;
		}
		
		final public MatchType mt;
		final public String matchmeta;		
	}
	
}
