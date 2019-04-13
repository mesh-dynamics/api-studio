/**
 * Copyright Cube I O
 */
package com.cube.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.Op;

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
		Default,
		ExactMatch,
		FuzzyMatch,
		NoMatch,
		Exception;

		public MatchType And(MatchType other) {
			switch (this) {
				case NoMatch:
				case Exception:
					return this;
				case ExactMatch:
					return other;
				default:
					return (other == NoMatch || other == Exception) ? other : this;
			}
		}

		/**
		 * Here other is the best so far, we need to decide
		 * if the current analysis match result should replace
		 * the best result
	         * ExactMatch > FuzzyMatch > NoMatch > Exception > Default
		 * @param other
		 * @return
		 */
		public boolean isBetter(MatchType other) {
			switch (this) {
				case ExactMatch: return (other != ExactMatch); // ExactMatch will not override Exact Match, but everything else
				case FuzzyMatch: return !(other == ExactMatch || other == FuzzyMatch); // Partial Match will not override Partial Match
				case NoMatch: return (other == Exception || other == Default); // NoMatch overrides Default and Exception
				case Exception: return (other == Default); // Exception only overrides default
				case Default: return false; // the default is only the starting condition and worse than anything
				default: return false;
			}
		}

		/**
		 * @param other
		 * @return true if this is better or equal to other match
		 */
		public boolean isBetterOrEqual(MatchType other) {
			switch (this) {
				case NoMatch: return (other == NoMatch);
				case ExactMatch: return true;
				default: return (other != ExactMatch); // PartialMatch is better only if other is not ExactMatch
			}
		}

	}

	enum Resolution {
		OK,
		OK_Optional,
		OK_Ignore,
		OK_CustomMatch,
		OK_OtherValInvalid, // the val to compare against does not exist or is not of right type
		OK_OptionalMismatch, // vals mismatched but comparison type was EqualOptional (used in scoring case for prioritizing)
		ERR_NotExpected, // This indicates that presence type is required and the old object does not have the value
		ERR_Required,
		ERR_ValMismatch,
		ERR_ValTypeMismatch,
		ERR_ValFormatMismatch,
		ERR;
		/**
		 * @return
		 */
		public boolean isErr() {
			return this == Resolution.ERR_NotExpected || this == ERR_Required ||
					this == ERR_ValMismatch || this == ERR_ValTypeMismatch ||
					this == ERR_ValFormatMismatch || this == ERR;
		}

		public MatchType toMatchType() {
			if (isErr()) {
				return MatchType.NoMatch;
			} else if (this == OK) {
				return MatchType.ExactMatch;
			} else return MatchType.FuzzyMatch;
		}

	}

	class Match {

		private static final Logger LOGGER = LogManager.getLogger(Match.class);


		/**
		 * @param mt
		 * @param matchmeta
		 * @param diffs
		 */
		public Match(MatchType mt, String matchmeta, List<Diff> diffs) {
			super();
			this.mt = mt;
			this.matchmeta = matchmeta;
			this.diffs = diffs;
		}
		
		public MatchType mt;
		final public String matchmeta;
		final public List<Diff> diffs;

		public void merge(Match other, boolean needDiff, String prefixpath) {
			mt = mt.And(other.mt);
			if (needDiff) {
				other.diffs.forEach(diff -> {
					Diff newdiff = prefixpath.isEmpty() ? diff : new Diff(diff.op, prefixpath + diff.path, diff.value, diff.from,
							diff.fromValue, diff.resolution);
					diffs.add(newdiff);
				});
			}
		}

		public void mergeInt(Resolution resolution, String path, boolean needDiff, String prefixpath, Optional<Integer> val,
							 Optional<Integer> fromVal) {
			merge(resolution, path, needDiff, prefixpath, val.map(Utils::intToJson), fromVal.map(Utils::intToJson));
		}

		public void mergeStr(Resolution resolution, String path, boolean needDiff, String prefixpath, Optional<String> val,
							 Optional<String> fromVal) {
			merge(resolution, path, needDiff, prefixpath, val.map(Utils::strToJson), fromVal.map(Utils::strToJson));
		}

		public void merge(Resolution resolution, String path, boolean needDiff, String prefixpath,
						  Optional<JsonNode> val, Optional<JsonNode> fromVal) {
			if (needDiff && resolution != Resolution.OK) {
				String op = Diff.NOOP;
				if (val.isEmpty()) {
					if (!fromVal.isEmpty()) {
						op = Diff.REMOVE;
					}
				} else if (fromVal.isEmpty()) {
					op = Diff.ADD;
				} else {
					op = Diff.REPLACE;
				}
				Comparator.Diff diff = new Comparator.Diff(op,prefixpath + path, val,
						Optional.empty(), fromVal, resolution);
				diffs.add(diff);
			}
			mt = mt.And(resolution.toMatchType());
		}

		public static Match NOMATCH = new Match(MatchType.NoMatch, "", Collections.emptyList());
		public static Match DEFAULT = new Match(MatchType.Default, "", Collections.emptyList());


		public String getDiffAsJsonStr(ObjectMapper jsonmapper) {
			try {
				return jsonmapper.writeValueAsString(diffs);
			} catch (JsonProcessingException e) {
				LOGGER.error("Error in converting diffs to json: ", e);
				return "";
			}
		}
	}

	/**
	 * This class captures the information that the json diff library produces
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	class Diff {

		static final String ADD = "add";
		static final String REMOVE = "remove";
		static final String REPLACE = "replace";
		static final String MOVE = "move";
		static final String COPY = "copy";
		static final String TEST = "test";
		static final String NOOP = "noop"; // used for validation errors

		/**
		 * Needed for jackson
		 */
		@SuppressWarnings("unused")
		private Diff() {
			super();
			value = null;
			path = null;
			op = null;
			fromValue = Optional.empty();
			from = Optional.empty();
			resolution = Resolution.ERR;
		}



		/**
		 * @param op
		 * @param path
		 * @param value
		 * @param resolution
		 */
		Diff(String op, String path, JsonNode value, Resolution resolution) {
			this(op, path, Optional.of(value), resolution);
		}

		/**
		 * @param op
		 * @param path
		 * @param value
		 * @param resolution
		 */
		Diff(String op, String path, Optional<JsonNode> value, Resolution resolution) {
			this(op, path, value, Optional.empty(), Optional.empty(), resolution);
		}


		Diff(String op, String path, Optional<JsonNode> value, Optional<String> from,
					Optional<JsonNode> fromValue, Resolution resolution) {
			super();
			this.op = op;
			this.path = path;
			this.value = value;
			this.from = from;
			this.fromValue = fromValue;
			this.resolution = resolution;
		}


		@NotNull
		public final String op;
		public final String path;
		public final Optional<JsonNode> value;
		public final Optional<String> from; //only to be used in move operation
		public final Optional<JsonNode> fromValue; // only used in replace operation
		public Resolution resolution;
	}

}
