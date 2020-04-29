package io.md.core;

import java.util.Collections;
import java.util.List;
import java.util.Optional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.DataObj;
import io.md.utils.Utils;

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

	Match compare(DataObj lhs, DataObj rhs);

	CompareTemplate getCompareTemplate();

	public enum MatchType {
		Default,
		ExactMatch,
		FuzzyMatch,
		NoMatch,
		RecReqNoMatch,
		ReplayReqNoMatch,
		MockReqNoMatch,
		DontCare,
		Exception;

		public boolean isNoMatch() {
			return this == NoMatch || this == RecReqNoMatch || this == ReplayReqNoMatch || this == MockReqNoMatch;
		}


		public MatchType And(MatchType other) {
			switch (this) {
				case NoMatch:
				case RecReqNoMatch:
				case ReplayReqNoMatch:
				case MockReqNoMatch:
				case Exception:
					return this;
				case ExactMatch:
					return other;
				default:
					return (other.isNoMatch() || other == Exception) ? other : this;
			}
		}

		/**
		 * Exact Match is better than everything else (except Exact Match itself)
		 * Fuzzy Match is better than everything else (except Exact Match and Fuzzy Match)
		 * No Match is better than Exception , Don't Care and Default
		 * Exception is better than Don't Care and Default
		 * Don't Care is better than Default
		 * Default is not better than anything (including Default, we override)
		 * (Default is just the starting condition, no document pair should finally have
		 * this Match Type visible in Solr)
		 * @param other MatchType to compare with
		 * @return if this matchtype is strictly better than the other Match Type
		 * Here other is the best so far, we need to decide
		 * if the current analysis match result should replace
		 * ExactMatch > FuzzyMatch > NoMatch > Exception > DontCare > Default
		 */
		public boolean isBetter(MatchType other) {
			switch(this) {
				case ExactMatch: return (other != ExactMatch); // ExactMatch will not override Exact Match, but everything else
				case FuzzyMatch: return !(other == ExactMatch || other == FuzzyMatch);
				case NoMatch: case RecReqNoMatch: case ReplayReqNoMatch: case MockReqNoMatch:
					return (other == Exception || other == Default || other == DontCare);
				case Exception: return (other == Default || other == DontCare);
				case DontCare: return (other == Default);
				case Default:
				default: return false;
			}
		}


		/**
		 * @param other
		 * @return true if this is better or equal to other match
		 */
		public boolean isBetterOrEqual(MatchType other) {
			switch (this) {
				case NoMatch: case RecReqNoMatch: case ReplayReqNoMatch: case MockReqNoMatch: return (other.isNoMatch());
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
		OK_DefaultPT, // Return when presence type is default.
		OK_DefaultCT, // Return when both are present and ComparisonType is set to Default.
		ERR_Required,
		ERR_RequiredGolden,
		ERR_ValMismatch,
		ERR_ValTypeMismatch,
		ERR_ValFormatMismatch,
		ERR_NewField, // This indicates that presence type is required/default and the old object does not have the
		// value
		ERR_InvalidExtractionMethod, // extraction method does not match type (e.g. regex for double)
		ERR;
		/**
		 * @return
		 */
		public boolean isErr() {
			return  this == ERR_Required ||
				this == ERR_ValMismatch || this == ERR_ValTypeMismatch ||
				this == ERR_ValFormatMismatch || this == ERR || this == ERR_NewField ||
				this == ERR_RequiredGolden || this == ERR_InvalidExtractionMethod;

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

		private static final Logger LOGGER = LoggerFactory.getLogger(Match.class);


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
				if (!val.isPresent()) {
					if (fromVal.isPresent()) {
						op = Diff.REMOVE;
					}
				} else if (!fromVal.isPresent()) {
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
		public static Match DONT_CARE = new Match(MatchType.DontCare, "", Collections.emptyList());
		public static Match STATUSNOMATCH = new Match(MatchType.NoMatch, "Status not matching",
			Collections.emptyList());


		public String getDiffAsJsonStr(ObjectMapper jsonMapper) {
			try {
				return jsonMapper.writeValueAsString(diffs);
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

		public static final String ADD = "add";
		public static final String REMOVE = "remove";
		public static final String REPLACE = "replace";
		public static final String MOVE = "move";
		public static final String COPY = "copy";
		public static final String TEST = "test";
		public static final String NOOP = "noop"; // used for validation errors

		/**
		 * Needed for jackson
		 */
		@SuppressWarnings("unused")
		private Diff() {
			super();
			value = Optional.empty();
			path = "";
			op = NOOP;
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
		public Diff(String op, String path, JsonNode value, Resolution resolution) {
			this(op, path, Optional.of(value), resolution);
		}

		/**
		 * @param op
		 * @param path
		 * @param value
		 * @param resolution
		 */
		public Diff(String op, String path, Optional<JsonNode> value, Resolution resolution) {
			this(op, path, value, Optional.empty(), Optional.empty(), resolution);
		}


		public Diff(String op, String path, Optional<JsonNode> value, Optional<String> from,
			Optional<JsonNode> fromValue, Resolution resolution) {
			super();
			this.op = op;
			this.path = path;
			this.value = value;
			this.from = from;
			this.fromValue = fromValue;
			this.resolution = resolution;
		}



		public final String op;
		public final String path;
		public final Optional<JsonNode> value;
		public final Optional<String> from; //only to be used in move operation
		public final Optional<JsonNode> fromValue; // only used in replace operation
		public Resolution resolution;
	}

}
