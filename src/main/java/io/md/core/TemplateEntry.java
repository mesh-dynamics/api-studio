package io.md.core;

import static io.md.core.Comparator.Resolution.ERR_InvalidExtractionMethod;
import static io.md.core.Comparator.Resolution.ERR_NewField;
import static io.md.core.Comparator.Resolution.ERR_Required;
import static io.md.core.Comparator.Resolution.ERR_RequiredGolden;
import static io.md.core.Comparator.Resolution.ERR_ValMismatch;
import static io.md.core.Comparator.Resolution.ERR_ValTypeMismatch;
import static io.md.core.Comparator.Resolution.OK;
import static io.md.core.Comparator.Resolution.OK_CustomMatch;
import static io.md.core.Comparator.Resolution.OK_DefaultCT;
import static io.md.core.Comparator.Resolution.OK_DefaultPT;
import static io.md.core.Comparator.Resolution.OK_Ignore;
import static io.md.core.Comparator.Resolution.OK_Optional;
import static io.md.core.Comparator.Resolution.OK_OptionalMismatch;
import static io.md.core.Comparator.Resolution.OK_OtherValInvalid;
import static io.md.core.CompareTemplate.ComparisonType.Equal;
import static io.md.core.CompareTemplate.DataType.Default;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;

import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.ExtractionMethod;
import io.md.core.CompareTemplate.PresenceType;
import io.md.utils.Utils;

public class TemplateEntry {

	private static final Logger LOGGER = LogManager.getLogger(TemplateEntry.class);

	/**
	 * @param path
	 * @param dt
	 * @param pt
	 * @param ct
	 * @param em
	 * @param customization
	 */
	// Adding appropriate annotations for json serialization/deserialization
	@JsonCreator
	public TemplateEntry(@JsonProperty("path") String path,
		@JsonProperty("dt") CompareTemplate.DataType dt,
		@JsonProperty("pt") CompareTemplate.PresenceType pt,
		@JsonProperty("ct") CompareTemplate.ComparisonType ct,
		@JsonProperty("em") CompareTemplate.ExtractionMethod em,
		@JsonProperty("customization") Optional<String> customization) {
		super();
		this.path = path;
		this.dt = (dt != null) ? dt : Default;
		this.pt = (pt != null) ? pt : CompareTemplate.PresenceType.Default;
		this.ct = (ct != null) ? ct : CompareTemplate.ComparisonType.Default;
		this.em = (em != null) ? em : CompareTemplate.ExtractionMethod.Default;
		this.customization = customization;
		this.pathptr = JsonPointer.valueOf(path);
		if (em == CompareTemplate.ExtractionMethod.Regex) {
			// default pattern is to match everything
			regex = Optional.ofNullable(Pattern.compile(customization.orElse(".*")));
		} else {
			regex = Optional.empty();
		}
	}

	/**
	 * @param path
	 * @param dt
	 * @param pt
	 * @param ct
	 * @param em
	 */
	public TemplateEntry(String path, CompareTemplate.DataType dt, CompareTemplate.PresenceType pt, CompareTemplate.ComparisonType ct, CompareTemplate.ExtractionMethod em) {
		this(path, dt, pt, ct, em, Optional.empty());
	}

	/**
	 * @param path
	 * @param dt
	 * @param pt
	 * @param ct
	 */
	public TemplateEntry(String path, CompareTemplate.DataType dt, CompareTemplate.PresenceType pt, CompareTemplate.ComparisonType ct) {
		this(path, dt, pt, ct, CompareTemplate.ExtractionMethod.Default, Optional.empty());
	}


	@JsonProperty("path")
	public String path;
	@JsonProperty("dt")
	public CompareTemplate.DataType dt;
	@JsonProperty("pt")
	public CompareTemplate.PresenceType pt;
	@JsonProperty("ct")
	public CompareTemplate.ComparisonType ct;
	@JsonProperty("em")
	public CompareTemplate.ExtractionMethod em;
	@JsonProperty("customization")
	public Optional<String> customization; // metadata for fuzzy match. For e.g. this could be the regex
	@JsonIgnore
	public JsonPointer pathptr; // compiled form of path
	@JsonIgnore
	public Optional<Pattern> regex; // compiled form of regex if ct == CustomRegex
	@JsonIgnore
	public boolean isParentArray = false;

	/*
	 * Assuming compare type is not ignore or default
	 */
	public Comparator.Resolution rhsmissing() {
		switch (pt) {
			case Required:
				return ERR_Required;
			case Optional:
				return OK_Optional;
			case Default:
				return OK_DefaultPT;
		}
		return OK;
	}

	/*
	 * Assuming rhs is present
	 * Assuming compare type is not ignore or default
	 */
	public Comparator.Resolution lhsmissing() {
		if (pt == CompareTemplate.PresenceType.Default && !isParentArray) {
			return ERR_NewField;
		} else if(pt == CompareTemplate.PresenceType.Required && !isParentArray) {
			return ERR_RequiredGolden;
		}
		switch (ct) {
			case Ignore:
				return OK_Ignore;
			case Default:
				return OK_DefaultCT;
			default:
				return OK_OtherValInvalid;
		}
		// return OK_OtherValInvalid;
	}


	// This function is designed on the premise that it checks matches over the actual found diffs and then return the resolution.
	// If the resolution is taken first and then the diffs are added then this may generate spurious diffs for example - In current case of Response/Request match.
	public Comparator.Resolution checkMatchStr(Optional<String> lhs, Optional<String> rhs) {
		Comparator.Resolution resolution = checkTypeAndPresence(CompareTemplate.DataType.Str, rhs);
		if (resolution.isErr()) {
			return resolution;
		}

		boolean isCustomMatch = false;
		switch (em) {
			case Regex:
				isCustomMatch = true;
				break;
			case Default:
				// regular string comparison
				break;
			case Round:
			case Floor:
			case Ceil:
				// not valid for strings
				return ERR_InvalidExtractionMethod;
		}

		switch (ct) {
			case Equal:
			case EqualOptional:
				if (isCustomMatch) {
					// extract regex and compare
					Pattern pattern = regex.orElseGet(() -> {
						LOGGER.error("Internal logical error - compiled pattern missing for regex");
						return Pattern.compile(customization.orElse(".*"));
					});
					return rhs.map(rval -> {
						Matcher rhsmatcher = pattern.matcher(rval);
						if (!rhsmatcher.matches()) {
							return Comparator.Resolution.ERR_ValFormatMismatch;
						}
						return lhs.map(lval -> {
							Matcher lhsmatcher = pattern.matcher(lval);
							if (!lhsmatcher.matches()) {
								return OK_OtherValInvalid;
							}
							if (rhsmatcher.groupCount() != lhsmatcher.groupCount()) {
								return (ct == Equal) ? ERR_ValMismatch
									: OK_OptionalMismatch;
							}
							for (int i = 0; i < rhsmatcher.groupCount(); ++i) {
								if (!rhsmatcher.group(i).equals(lhsmatcher.group(i))) {
									return (ct == Equal) ? ERR_ValMismatch
										: OK_OptionalMismatch;
								}
							}
							return OK_CustomMatch;
						}).orElse(lhsmissing());
					}).orElse(rhsmissing());
				} else {
					return checkEqual(lhs, rhs,
						ct == CompareTemplate.ComparisonType.EqualOptional, isCustomMatch);
				}
			case Ignore:
				return OK_Ignore;
			case Default:
				return OK_DefaultCT;
			default:
				return ERR_ValTypeMismatch; // could be CustomRound, Floor, Ceil
		}

	}

	public void checkMatchStr(Optional<String> lhs, Optional<String> rhs, Comparator.Match match,
		boolean needDiff, String prefixpath) {

		Comparator.Resolution resolution = checkMatchStr(lhs, rhs);
		match.mergeStr(resolution, path, needDiff, prefixpath, rhs, lhs);
	}

	public void checkMatchStr(Optional<String> lhs, Optional<String> rhs, Comparator.Match match,
		boolean needDiff) {

		checkMatchStr(lhs, rhs, match, needDiff, "");
	}


	public void checkMatchStr(String lhs, String rhs, Comparator.Match match, boolean needDiff) {
		checkMatchStr(Optional.ofNullable(lhs), Optional.ofNullable(rhs), match, needDiff, "");
	}

	public Comparator.Resolution checkMatchInt(Optional<Integer> lhs, Optional<Integer> rhs) {
		Comparator.Resolution resolution = checkTypeAndPresence(CompareTemplate.DataType.Int, rhs);
		if (resolution.isErr()) {
			return resolution;
		}

		// if resolution is not error and compare type is null ... a null point
		// exception will be thrown
		switch (ct) {
			case Equal:
			case EqualOptional:
				return checkEqual(lhs, rhs,
					ct == CompareTemplate.ComparisonType.EqualOptional, false);
			case Ignore:
				return OK_Ignore;
			case Default:
				return OK_DefaultCT;
			default:
				return ERR_ValTypeMismatch; // could be CustomRound, Floor, Ceil, CustomReqex

		}
	}

	public  void checkMatchInt(Optional<Integer> lhs, Optional<Integer> rhs, Comparator.Match match, boolean needDiff) {

		Comparator.Resolution resolution = checkMatchInt(lhs, rhs);
		match.mergeInt(resolution, path, needDiff, "", rhs, lhs);
	}

	public  void checkMatchInt(int lhs, int rhs, Comparator.Match match, boolean needDiff) {
		checkMatchInt(Optional.ofNullable(lhs), Optional.ofNullable(rhs), match, needDiff);
	}

	public  <T> Comparator.Resolution checkEqual(Optional<T> lhs, Optional<T> rhs, boolean isEqualOptional, boolean isCustomMatch) {
		return rhs.map(rval -> {
			return lhs.map(lval -> {
				if (rval.equals(lval)) {
					if (isCustomMatch) {
						return OK_CustomMatch;
					}
					return OK;
				} else {
					return isEqualOptional ? OK_OptionalMismatch : ERR_ValMismatch;
				}
			}).orElse(lhsmissing());
		}).orElse(rhsmissing());
	}

	private <T> Comparator.Resolution checkTypeAndPresence(CompareTemplate.DataType expectedType, Optional<T> val) {
		// check type
		if (dt != expectedType && dt != Default) {
			return ERR_ValTypeMismatch;
		}
		if (pt == CompareTemplate.PresenceType.Required && val.isEmpty()) {
			return ERR_Required;
		}
		return OK;
	}

	public Comparator.Resolution checkMatchDbl(Optional<Double> lhs, Optional<Double> rhs) {
		Comparator.Resolution resolution = checkTypeAndPresence(CompareTemplate.DataType.Float, rhs);
		if (resolution.isErr()) {
			return resolution;
		}

		boolean isCustomMatch = false;
		switch (em) {
			case Round:
			case Ceil:
			case Floor:
				lhs = lhs.map(this::adjustDblVal);
				rhs = rhs.map(this::adjustDblVal);
				isCustomMatch = true;
				break;
			case Regex:
				// invalid extraction method for double
				return ERR_InvalidExtractionMethod;
			case Default:
				// do nothing
				break;
		}

		if (lhs.isEmpty()) {
			return lhsmissing();
		}
		if (rhs.isEmpty()) {
			return rhsmissing();
		}


		switch (ct) {
			case Equal:
			case EqualOptional:
				return checkEqual(lhs, rhs,
					ct == CompareTemplate.ComparisonType.EqualOptional, isCustomMatch);
			case Ignore:
				return OK_Ignore;
			case Default:
				return OK_DefaultCT;
			default:
				return ERR_ValTypeMismatch; // could be CustomRegex

		}

	}

	private Double adjustDblVal(double val) {
		int numdecimal = customization.flatMap(Utils::strToInt).orElse(0);
		double multiplier = Math.pow(10, numdecimal);
		switch(em) {
			case Ceil:
				return Math.ceil(val * multiplier)/multiplier;
			case Floor:
				return Math.floor(val * multiplier)/multiplier;
			case Round:
				return Math.round(val * multiplier)/multiplier;
			default:
				return val;
		}
	}

	@JsonIgnore
	public String getPath() {
		return this.path;
	}

	@JsonIgnore
	public DataType getDataType() {
		return dt;
	}

	@JsonIgnore
	public PresenceType getPresenceType() {
		return pt;
	}

	@JsonIgnore
	public CompareTemplate.ComparisonType getCompareType() {
		return ct;
	}

	@JsonIgnore
	public ExtractionMethod getExtractionMethod() {
		return em;
	}

	@JsonIgnore
	public Optional<String> getCustomization() {
		return customization;
	}

	@JsonIgnore
	public int getPathLength() {
		return path.length();
	}


}
