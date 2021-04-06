package io.md.core;

import static io.md.core.Comparator.Resolution.ERR_InvalidExtractionMethod;
import static io.md.core.Comparator.Resolution.ERR_NewField;
import static io.md.core.Comparator.Resolution.ERR_Required;
import static io.md.core.Comparator.Resolution.ERR_RequiredGolden;
import static io.md.core.Comparator.Resolution.ERR_ValMismatch;
import static io.md.core.Comparator.Resolution.ERR_ValTypeMismatch;
import static io.md.core.Comparator.Resolution.OK;
import static io.md.core.Comparator.Resolution.OK_CustomMatch;
import static io.md.core.Comparator.Resolution.OK_Ignore;
import static io.md.core.Comparator.Resolution.OK_Optional;
import static io.md.core.Comparator.Resolution.OK_OptionalMismatch;
import static io.md.core.Comparator.Resolution.OK_OtherValInvalid;
import static io.md.core.CompareTemplate.ComparisonType.Equal;
import static io.md.core.CompareTemplate.ComparisonType.Ignore;
import static io.md.core.CompareTemplate.DataType.Default;

import io.md.core.CompareTemplate.ComparisonType;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;

import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.ExtractionMethod;
import io.md.core.CompareTemplate.PresenceType;
import io.md.utils.Utils;

public class TemplateEntry {

	private static final Logger LOGGER = LogMgr.getLogger(TemplateEntry.class);

	/**
	 * @param path
	 * @param dt
	 * @param pt
	 * @param ptInheritance
	 * @param ct
	 * @param em
	 * @param customization
	 */
	// Adding appropriate annotations for json serialization/deserialization
	@JsonCreator
	public TemplateEntry(@JsonProperty("path") String path,
		@JsonProperty("dt") CompareTemplate.DataType dt,
		@JsonProperty("pt") CompareTemplate.PresenceType pt,
		@JsonProperty("ptInheritance") CompareTemplate.PresenceType ptInheritance,
		@JsonProperty("ct") CompareTemplate.ComparisonType ct,
		@JsonProperty("em") CompareTemplate.ExtractionMethod em,
		@JsonProperty("customization") Optional<String> customization,
		@JsonProperty("arrayCompKeyPath") Optional<String> arrayComparisionKeyPath) {
		super();
		this.path = path;
		this.dt = (dt != null) ? dt : Default;
		this.pt = (pt != null) ? pt : PresenceType.Optional;
		this.ptInheritance = (ptInheritance != null) ? ptInheritance : getInheritancePt(pt);
		this.ct = (ct != null) ? ct : ComparisonType.Ignore;
		this.em = (em != null) ? em : CompareTemplate.ExtractionMethod.Default;
		this.customization = customization;
		this.pathptr = JsonPointer.valueOf(path);
		if (em == CompareTemplate.ExtractionMethod.Regex) {
			// default pattern is to match everything
			regex = Optional.ofNullable(Pattern.compile(customization.orElse(".*")));
		} else {
			regex = Optional.empty();
		}
		this.arrayComparisionKeyPath = arrayComparisionKeyPath;
	}

	public TemplateEntry(String path,
		CompareTemplate.DataType dt, CompareTemplate.PresenceType pt,
		CompareTemplate.ComparisonType ct,
		CompareTemplate.ExtractionMethod em,
		Optional<String> customization,
		Optional<String> arrayComparisionKeyPath) {

		this(path, dt, pt, null, ct, em, customization, arrayComparisionKeyPath);

	}

	/**
	 * @param path
	 * @param dt
	 * @param pt
	 * @param ptInheritance
	 * @param ct
	 * @param em
	 */
	public TemplateEntry(String path, DataType dt, PresenceType pt,
		PresenceType ptInheritance, ComparisonType ct,
		ExtractionMethod em) {
		this(path, dt, pt, ptInheritance, ct, em, Optional.empty(), Optional.empty());
	}

	/**
	 * @param path
	 * @param dt
	 * @param pt
	 * @param ptInheritance
	 * @param ct
	 */
	public TemplateEntry(String path, DataType dt, PresenceType pt,
		PresenceType ptInheritance, ComparisonType ct) {
		this(path, dt, pt, ptInheritance, ct, CompareTemplate.ExtractionMethod.Default, Optional.empty(), Optional.empty());
	}

	public TemplateEntry(String path, DataType dt, PresenceType pt, ComparisonType ct) {
		this(path, dt, pt, null, ct, CompareTemplate.ExtractionMethod.Default, Optional.empty(), Optional.empty());
	}

	@JsonProperty("path")
	public String path;
	@JsonProperty("dt")
	public CompareTemplate.DataType dt;
	@JsonProperty("pt")
	public CompareTemplate.PresenceType pt;
	@JsonProperty("ptInheritance")
	public CompareTemplate.PresenceType ptInheritance;
	@JsonProperty("ct")
	public CompareTemplate.ComparisonType ct;
	@JsonProperty("em")
	public CompareTemplate.ExtractionMethod em;
	@JsonProperty("customization")
	public Optional<String> customization; // metadata for fuzzy match. For e.g. this could be the regex
	@JsonProperty("arrayCompKeyPath")
	public Optional<String> arrayComparisionKeyPath;
	@JsonIgnore
	public JsonPointer pathptr; // compiled form of path
	@JsonIgnore
	public Optional<Pattern> regex; // compiled form of regex if ct == CustomRegex
	@JsonIgnore
	public boolean isParentArray = false;

	public Comparator.Resolution rhsmissing() {
		switch (pt) {
			case Optional:
				return OK_Optional;
			default:
				return ERR_Required;
		}
	}

	public Comparator.Resolution lhsmissing() {
		switch (pt){
			case Required:
				return ERR_RequiredGolden;
			case RequiredIdentical:
				return ERR_NewField;
			default:
				return OK_Optional;
		}
	}

	public Comparator.Resolution checkMatch(Optional<?> lhs, Optional<?> rhs,
		CompareTemplate.DataType expectedDt) {
		// lhs can be empty if we cannot convert it to rhs datatype

		if (dt != Default && dt != expectedDt) {
			return ERR_ValTypeMismatch;
		} else if (ct != Ignore || em != ExtractionMethod.Default) {
			// Both lhs and rhs should be present for comparison or extraction
			if (!lhs.isPresent() || !rhs.isPresent()) {
				return ERR_ValTypeMismatch;
			} else {
				switch (expectedDt) {
					case Str:
						return checkMatchStr((String) lhs.get(), (String) rhs.get());
					case Float:
						return checkMatchDbl((Double) lhs.get(), (Double) rhs.get());
					case Int:
						return checkMatchInt((Integer) lhs.get(), (Integer) rhs.get());
					default:
						return ERR_ValTypeMismatch;
				}
			}
		} else {
			// ct is ignore and em is default - hence no need for further comparisons
			return OK_Ignore;
		}
	}



	// This function is designed on the premise that it checks matches over the actual found diffs and then return the resolution.
	// If the resolution is taken first and then the diffs are added then this may generate spurious diffs for example - In current case of Response/Request match.
	public Comparator.Resolution checkMatchStr(String lhs, String rhs) {
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

					Matcher rhsmatcher = pattern.matcher(rhs);
					if (!rhsmatcher.matches()) {
						return Comparator.Resolution.ERR_ValFormatMismatch;
					}

					Matcher lhsmatcher = pattern.matcher(lhs);
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
				} else {
					return checkEqual(lhs, rhs,
						ct == CompareTemplate.ComparisonType.EqualOptional, isCustomMatch);
				}
			case Ignore:
				return OK_Ignore;
			default:
				return ERR_ValTypeMismatch; // could be CustomRound, Floor, Ceil
		}

	}

	public Comparator.Resolution checkMatchInt(Integer lhs, Integer rhs) {
		// if resolution is not error and compare type is null ... a null point
		// exception will be thrown
		switch (ct) {
			case Equal:
			case EqualOptional:
				return checkEqual(lhs, rhs,
					ct == CompareTemplate.ComparisonType.EqualOptional, false);
			case Ignore:
				return OK_Ignore;
			default:
				return ERR_ValTypeMismatch; // could be CustomRound, Floor, Ceil, CustomReqex

		}
	}

	public  void checkMatchInt(Integer lhs, Integer rhs, Comparator.Match match, boolean needDiff) {

		Comparator.Resolution resolution = checkMatchInt(lhs, rhs);
		match.mergeInt(resolution, path, needDiff, "", Optional.ofNullable(rhs), Optional.ofNullable(lhs));
	}

	public  void checkMatchInt(int lhs, int rhs, Comparator.Match match, boolean needDiff) {
		checkMatchInt(lhs, rhs, match, needDiff);
	}

	public <T> Comparator.Resolution checkEqual(Object lval, Object rval, boolean isEqualOptional,
		boolean isCustomMatch) {

		if (rval.equals(lval)) {
			if (isCustomMatch) {
				return OK_CustomMatch;
			}
			return OK;
		} else {
			return isEqualOptional ? OK_OptionalMismatch : ERR_ValMismatch;
		}
	}

	public Comparator.Resolution checkMatchDbl(Double lhs, Double rhs) {
		boolean isCustomMatch = false;
		switch (em) {
			case Round:
			case Ceil:
			case Floor:
				lhs = this.adjustDblVal(lhs);
				rhs = this.adjustDblVal(rhs);
				isCustomMatch = true;
				break;
			case Regex:
				// invalid extraction method for double
				return ERR_InvalidExtractionMethod;
			case Default:
				// do nothing
				break;
		}

		switch (ct) {
			case Equal:
			case EqualOptional:
				return checkEqual(lhs, rhs,
					ct == CompareTemplate.ComparisonType.EqualOptional, isCustomMatch);
			case Ignore:
				return OK_Ignore;
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

	private static PresenceType getInheritancePt(PresenceType pt) {
		if (pt == PresenceType.Required) {
			// For inherited rules, keys in RHS should be exactly as lhs
			return PresenceType.RequiredIdentical;
		}
		return pt;
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
