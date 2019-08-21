/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import io.cube.agent.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import static com.cube.core.Comparator.Resolution.*;
import com.cube.core.RequestComparator.PathCT;

/**
 * @author prasad
 *
 * Semantics:
 * Presence Type:
 * Required => if value missing in rhs -> ERR_Required
 * 		       if value missing in lhs, but present in rhs
 * 					if (comparison type is Ignore or Default)
 * 			    		-> OK_Ignore or OK
 *                  else
 * 			    		-> OK_OtherValInvalid
 *
 * Optional => If value missing in rhs -> OK_Optional
 *             If value missing in lhs -> OK_OtherValInvalid
 *
 * Default => If value missing in rhs -> OK
 *            If value missing in lhs, but present in rhs
 * 				if (comparison type is Ignore or Default)
 * 			    	-> OK_Ignore or OK
 *              else
 * 			    	-> OK_OtherValInvalid
 */
public class CompareTemplate {

	private static final Logger LOGGER = LogManager.getLogger(CompareTemplate.class);
	private Map<String, TemplateEntry> rules;

	//Adding appropriate annotations for json serialization and deserialization
	@JsonProperty("prefixPath")
 	final String prefixpath;

	public enum DataType {
		Str,
		Int,
		Float,
		RptArray, // array having same structure for all its elements
		NrptArray, // array with different types for each element
		Obj, // object type
		Default; // not specified

        boolean isObj() {
            return this == RptArray || this == NrptArray || this == Obj;
        }
	}
	
	public enum PresenceType {
		Required,
		Optional,
		Default // if not specified
	}
	
	public enum ComparisonType {
		Equal,
		EqualOptional, // this is for cases where equality is desired, but not required.
		// In retrieval scenario, objects satisfying equality should scored higher
		Ignore,
		Default // if not specified
	}

	public enum ExtractionMethod {
        Regex, // for strings
        Round, // for floats
        Floor, // for floats
        Ceil, // for floats
        Default, // if not specified
    }

    /**
	 *
	 */
	public CompareTemplate() {
		this("");
	}

	public CompareTemplate(String prefixpath) {
		super();
		rules = new HashMap<>();
		this.prefixpath = prefixpath;
	}

	/**
	 * @param path
	 * @return The rule corresponding to the path. Search from the path upwards, till a matching rule 
	 * is found. Never returns null. Will return default rule if nothing is found.
	 */
	public TemplateEntry getRule(String path) {
		return get(path).orElse(getInheritedRule(path));
	}

	@JsonGetter("rules")
	public Collection<TemplateEntry> getRules() {
		return rules.values();
	}


	List<PathCT> getPathCTs() {
		return getRules().stream().map(rule -> new PathCT(rule.path, rule.ct)).collect(Collectors.toList());
	}

	@JsonSetter("rules")
	public void setRules(Collection<TemplateEntry> rules) {
		for (TemplateEntry rule : rules) {
			addRule(rule);
		}
	}

	public CompareTemplate subsetWithPrefix(String prefix) {
		CompareTemplate ret = new CompareTemplate(prefix);

		getRules().forEach(rule -> {
			if (rule.path.startsWith(prefix)) {
				// strip the prefix from the path, this is needed other paths will not match while doing json comparison
				LOGGER.debug("Found a rule for " + prefix +  "::  " + rule.path + " " + rule.dt + " " + rule.pt + " " + rule.ct + " " + rule.em);
				TemplateEntry newrule = new TemplateEntry(rule.path.substring(prefix.length()), rule.dt, rule.pt, rule.ct, rule.em, rule.customization);
				ret.addRule(newrule);
			}
		});

		return ret;
	}

	/*
	 * Equality and Ignore compare rules can be inherited from the nearest ancestor
	 */
	private TemplateEntry getInheritedRule(String path) {
		int index = path.lastIndexOf('/');
		if (index != -1) {
			String subPath = path.substring(0, index);
			return get(subPath).flatMap(rule -> {
			    if (rule.dt == DataType.RptArray) {
                    Optional<TemplateEntry> starRule = get(subPath + "/*");
                    if (starRule.isPresent()) {
                        return starRule;
                    }
                }
				if (rule.ct == ComparisonType.Equal) {
					return Optional.of(DEFAULT_RULE_EQUALITY);
				} else if (rule.ct == ComparisonType.EqualOptional) {
                    return Optional.of(DEFAULT_RULE_EQUAL_OPTIONAL);
                } else if (rule.ct == ComparisonType.Ignore) {
                    return Optional.of(DEFAULT_RULE_IGNORE);
                } else {
					return Optional.empty();
				}
			}).orElse(getInheritedRule(subPath));
		} else {
			return DEFAULT_RULE;
		}
	}

	public void checkMatch(MultivaluedMap<String, String> lhsfmap, MultivaluedMap<String, String> rhsfmap,
						   Comparator.Match match, boolean needDiff) {

		for (TemplateEntry rule: getRules()) {
			List<String> lvals = CommonUtils.getCaseInsensitiveMatches(lhsfmap , rule.path);
			List<String> rvals = CommonUtils.getCaseInsensitiveMatches(rhsfmap , rule.path);
			if (rule.ct == ComparisonType.Equal || rule.ct == ComparisonType.EqualOptional) {
				Comparator.Resolution resolution = OK;
				Set<String> lset = new HashSet<>(lvals);
				Set<String> rset = new HashSet<>(rvals);
				// check if all values match
				if (!lset.equals(rset)) {
					if (rule.ct == ComparisonType.EqualOptional) { // for soft match, its ok to not match on the field val
						resolution = OK_OptionalMismatch;
					} else {
						resolution = ERR_ValMismatch;
					}
				}
				match.mergeStr(resolution, rule.path, needDiff, prefixpath, Optional.of(rset.toString()), Optional.of(lset.toString()));
			} else {
				// consider only the first val of the multivals
				// TODO: mav have to revisit this later
				Optional<String> lval = lvals.stream().findFirst();
				Optional<String> rval = rvals.stream().findFirst();
				rule.checkMatchStr(lval, rval, match, needDiff, prefixpath);
			}
			if ((match.mt == Comparator.MatchType.NoMatch) && !needDiff) {
				break; // short circuit
			}
		}
	}


	public Optional<TemplateEntry> get(String path) {
		return Optional.ofNullable(rules.get(path));
	}
	
	private static final TemplateEntry DEFAULT_RULE = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.Default);
	private static final TemplateEntry DEFAULT_RULE_EQUALITY = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.Equal);
	private static final TemplateEntry DEFAULT_RULE_EQUAL_OPTIONAL = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.EqualOptional);
	private static final TemplateEntry DEFAULT_RULE_IGNORE = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.Ignore);
	/**
	 * @param rule
	 */
	public void addRule(TemplateEntry rule) {
		rules.put(rule.path, rule);
	}

}
