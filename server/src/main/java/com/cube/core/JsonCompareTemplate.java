/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonPointer;

/**
 * @author prasad
 *
 */
public class JsonCompareTemplate {

	Map<String, TemplateEntry> rules;
	
	enum DataType {
		Str,
		Int,
		Float,
		RptArray, // array having same structure for all its elements
		NrptArray, // array with different types for each element
		Obj, // object type
		Default // not specified
	}
	
	enum PresenceType {
		Required,
		Optional,
		Default // if not specified
	}
	
	public enum ComparisonType {
		Equal,
		Ignore,
		CustomRegex, // for strings
		CustomRound, // for floats
		CustomFloor, // for floats
		CustomCeil, // for floats
		Default // if not specified
	}
	
	static class TemplateEntry {
		
		
		/**
		 * @param path
		 * @param dt
		 * @param pt
		 * @param ct
		 * @param customization
		 */
		private TemplateEntry(String path, DataType dt, PresenceType pt, ComparisonType ct, String customization) {
			super();
			this.path = path;
			this.dt = dt;
			this.pt = pt;
			this.ct = ct;
			this.customization = customization;	
			this.pathptr = JsonPointer.valueOf(path);
			if (ct == ComparisonType.CustomRegex) {
				regex = Optional.ofNullable(Pattern.compile(customization));
			} else {
				regex = Optional.empty();
			}
		}
		
		String path;
		DataType dt;
		PresenceType pt;
		ComparisonType ct;
		String customization; // metadata for fuzzy match. For e.g. this could be the regex
		JsonPointer pathptr; // compiled form of path
		Optional<Pattern> regex; // compiled form of regex if ct == CustomRegex
	}

	
	
	
	/**
	 * 
	 */
	public JsonCompareTemplate() {
		super();
		rules = new HashMap<String, TemplateEntry>();
	}

	/**
	 * @param path
	 * @return The rule corresponding to the path. Search from the path upwards, till a matching rule 
	 * is found.
	 */
	public TemplateEntry getRule(String path) {
		
		return get(path).orElse(getInheritedRule(path));
	}
	
	public Collection<TemplateEntry> getRules() {
		return rules.values();
	}
	
	/*
	 * Equality and Ignore compare rules can be inherited from the nearest ancestor
	 */
	private TemplateEntry getInheritedRule(String path) {
		int index = path.lastIndexOf('/');
		if (index != -1) {
			String subpath = path.substring(0, index);
			return get(subpath).flatMap(rule -> {
				if (rule.ct == ComparisonType.Equal) {
					return Optional.of(DEFAULT_RULE_EQUALITY);
				} else if (rule.ct == ComparisonType.Ignore) {
					return Optional.of(DEFAULT_RULE_IGNORE);
				} else {
					return Optional.empty();
				}
			}).orElse(getInheritedRule(subpath));
		} else {
			return DEFAULT_RULE;
		}
	}
	
	private Optional<TemplateEntry> get(String path) {
		return Optional.ofNullable(rules.get(path));
	}
	
	private static final TemplateEntry DEFAULT_RULE = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.Default, "");
	private static final TemplateEntry DEFAULT_RULE_EQUALITY = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.Equal, "");
	private static final TemplateEntry DEFAULT_RULE_IGNORE = new TemplateEntry("/", DataType.Default, PresenceType.Default, ComparisonType.Ignore, "");
	
}
