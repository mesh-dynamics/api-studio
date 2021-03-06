/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.core;

import static io.md.core.Comparator.Resolution.ERR_ValMismatch;
import static io.md.core.Comparator.Resolution.OK;
import static io.md.core.Comparator.Resolution.OK_OptionalMismatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import io.md.logger.LogMgr;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonPointer;

import io.md.utils.CommonUtils;

/**
 * @author prasad
 *
 * Semantics:
 * Presence Type:
 * Required =>            if value missing in rhs -> ERR_Required
 *                        if value missing in lhs -> ERR_RequiredGolden
 *
 * Optional =>            If value missing in rhs -> OK_Optional
 *                        If value missing in lhs -> OK_Optional
 *
 * RequiredIfInGolden =>  If value missing in rhs -> ERR_Req
 *                        If value missing in lhs -> OK_Optional
 *
 * RequiredIdentical =>   If value missing in rhs -> Err_Required
 *                        If value missing in lhs -> Err_NewField
 *
 * ComparisonType and other types checked only for keys present in both lhs and rhs.
 *
 */
public class CompareTemplate {

	private static final Logger LOGGER = LogMgr.getLogger(CompareTemplate.class);
	private Map<String, TemplateEntry> rules;

	//Adding appropriate annotations for json serialization and deserialization
	@JsonProperty("prefixPath")
	public final String prefixpath;

	@JsonIgnore
	private Optional<AttributeRuleMap> appLevelAttributeRuleMap = Optional.empty();

	public enum DataType {
		Str,
		Int,
		Float,
		RptArray, // array having same structure for all its elements
		NrptArray, // array with different types for each element
		Set , // Repeating Array where elements have to compared irrespective of order
		Obj, // object type
		Default; // not specified

		public boolean isObj() {
			return this == RptArray || this == NrptArray || this == Set || this == Obj;
		}
	}

	public enum PresenceType {
		Required, // Key should be present in both LHS and RHS
		RequiredIdentical, // RHS should have exactly the same keys as LHS
		RequiredIfInGolden, // RHS should at least have the keys in LHS
		Optional
	}

	public enum ComparisonType {
		Equal,
		EqualOptional, // this is for cases where equality is desired, but not required.
		// In retrieval scenario, objects satisfying equality should scored higher
		Ignore
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

	public CompareTemplate(String prefixpath, AttributeRuleMap attributeRuleMap) {
		super();
		rules = new HashMap<>();
		this.prefixpath = prefixpath;
		this.appLevelAttributeRuleMap = Optional.ofNullable(attributeRuleMap);
	}

	/**
	 * @param path
	 * @return The rule corresponding to the path. Search from the path upwards, till a matching rule
	 * is found. Never returns null. Will return default rule if nothing is found.
	 */
	public TemplateEntry getRule(String path) {
		JsonPointer normalisedPathPointer = getNormalisedPath(path);
		TemplateEntry toReturn = get(normalisedPathPointer)
			.orElseGet(() -> getInheritedRule(normalisedPathPointer, path));
		// TODO maybe it's better to precompute these values
		toReturn.isParentArray = isParentArray(path);
		return toReturn;
	}

	public boolean isParentArray(String path) {
		Boolean toReturn = false;
		int index = path.lastIndexOf('/');
		if (index != -1) {
			String subPath = path.substring(0, index);
			toReturn = get(JsonPointer.valueOf(subPath)).
				map(entry -> entry.dt == DataType.RptArray || entry.dt == DataType.NrptArray
					|| entry.dt == DataType.Set).orElse(false);
		}
		return toReturn;
	}

	@JsonGetter("rules")
	public Collection<TemplateEntry> getRules() {
		return rules.values();
	}


	@JsonSetter("rules")
	public void setRules(Collection<TemplateEntry> rules) {

		// Here sorting the rules based on the length of path.
		// This is needed because let's say there is rule for /body
		// marked as RptArray, so all descendant paths should be
		// normalised and have "*" at the child level of body.
		List<TemplateEntry> rulesList = new ArrayList(rules);
		rulesList.sort(java.util.Comparator.comparing(TemplateEntry::getPathLength));

		for (TemplateEntry rule : rulesList) {
			addRule(rule);
		}
	}

	@JsonIgnore
	public void setAppLevelAttributeRuleMap(AttributeRuleMap ruleMap) {
		this.appLevelAttributeRuleMap = Optional.of(ruleMap);
	}

	@JsonIgnore
	public Optional<AttributeRuleMap> getAppLevelAttributeRuleMap() {
		return this.appLevelAttributeRuleMap;
	}

	public CompareTemplate subsetWithPrefix(String prefix) {
		CompareTemplate ret = new CompareTemplate(prefix);

		getRules().forEach(rule -> {
			if (rule.path.startsWith(prefix)) {
				// strip the prefix from the path, this is needed other paths will not match while doing json comparison
				LOGGER.debug("Found a rule for " + prefix +  "::  " + rule.path + " " + rule.dt + " " + rule.pt + " " + rule.ct + " " + rule.em);
				TemplateEntry newrule = new
					TemplateEntry(rule.path.substring(prefix.length())
					, rule.dt, rule.pt, rule.ct, rule.em, rule.customization, rule.arrayComparisionKeyPath);
				ret.addRule(newrule);
			}
		});

		return ret;
	}

	public JsonPointer getNormalisedPath(String rootPath) {
		if(!rootPath.equals("") && !rootPath.startsWith("/")) rootPath = "/".concat(rootPath);
		JsonPointer rootPointer = JsonPointer.valueOf(rootPath);
		JsonPointer pointer = rootPointer;

		// Using array for changing variable inside the lambda function
		final JsonPointer[] returnPointer = {JsonPointer.valueOf("")};
		// TODO: Change comparison to JsonPointer.empty() method once we upgrade Jackson to 2.10
		while (!pointer.toString().isEmpty()) {
			String currentProperty = pointer.getMatchingProperty();

			returnPointer[0] = get(returnPointer[0]).map(rule -> {
					if (rule.dt == DataType.RptArray || rule.dt == DataType.Set) {
						return returnPointer[0].append(JsonPointer.valueOf("/*"));
					} else {
						return returnPointer[0].append(JsonPointer.valueOf("/" + currentProperty));
					}
				}).orElse(
					returnPointer[0].append(JsonPointer.valueOf("/" + currentProperty)));
			pointer = pointer.tail();
		}
		return returnPointer[0];
	}

	public CompareTemplate cloneWithAdditionalRules(Collection<TemplateEntry> newRules) {
		CompareTemplate clonedCompareTemplate = new CompareTemplate(this.prefixpath);
		clonedCompareTemplate.rules = new HashMap<>(this.rules);
		// this will merge the new rules properly
		clonedCompareTemplate.setRules(newRules);
		return clonedCompareTemplate;
	}

	/*
	 * Equality and Ignore compare rules can be inherited from the nearest ancestor
	 */
	private TemplateEntryAsRule getInheritedRule(JsonPointer pathPointer, String origPath) {
		JsonPointer parentPointer = pathPointer.head();
		if (parentPointer!=null) {
			return get(parentPointer).flatMap(rule -> {
				return Optional
					.of(new TemplateEntryAsRule(origPath, DataType.Default, rule.ptInheritance,
						rule.ct, CompareTemplate.ExtractionMethod.Default, Optional.empty(),
						Optional.empty()
						, Optional.of(parentPointer.toString()), false));

			}).orElseGet(() -> getInheritedRule(parentPointer, origPath));
		} else {
			return new TemplateEntryAsRule(new TemplateEntry(origPath, DataType.Default, PresenceType.Optional, ComparisonType.Ignore)
				, Optional.empty(), false) ;
		}
	}

	/**
	 *
	 * @return ValidTemplate
	 */
	public ValidateCompareTemplate validate() {
		boolean isValid = true;
		String message = "";
		ValidateCompareTemplate validateCompareTemplate = new ValidateCompareTemplate(isValid, Optional.of(message));
		return validateCompareTemplate;
	}

	public Optional<TemplateEntry> get(JsonPointer path) {
		Optional<TemplateEntry> templateEntry = Optional.ofNullable(rules.get(path.toString()));
		if (!templateEntry.isPresent()) {
			// path.last() returns null for empty path
			templateEntry = Optional.ofNullable(path.last()).flatMap(pathv -> {
				return appLevelAttributeRuleMap.flatMap(map ->
					map.getRule(pathv.toString()));
			}).map(templateEntry1 -> {
				return new TemplateEntryAsRule(templateEntry1, Optional.empty(), true);
			});
		}
		return templateEntry;
	}


	/**
	 * @param rule
	 */
	public void addRule(TemplateEntry rule) {
		TemplateEntry normalisedRule = new TemplateEntry(getNormalisedPath(rule.path).toString(),
			rule.dt, rule.pt, null, rule.ct, rule.em, rule.customization, rule.arrayComparisionKeyPath);
		rules.put(normalisedRule.path, normalisedRule);
	}

	public static String normaliseAPIPath(String apiPath) {
		return apiPath.equals("/") ? apiPath : 
			 StringUtils.stripStart(apiPath/*StringUtils.stripEnd(apiPath,"/")*/, "/");
	}

	public static class CompareTemplateStoreException extends Exception {
		public CompareTemplateStoreException(String message) {
			super(message);
		}

	}

}