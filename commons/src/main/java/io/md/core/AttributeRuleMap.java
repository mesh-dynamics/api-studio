package io.md.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.md.utils.Utils;

public class AttributeRuleMap {

	@JsonProperty("attributeRuleMap")
	private Map<String, TemplateEntry> attributeNameVsRule;

	@JsonCreator
	public AttributeRuleMap(@JsonProperty("attributeRuleMap")Map<String, TemplateEntry> preDefinedRules) throws Exception {
		for (Map.Entry<String, TemplateEntry> entry : preDefinedRules.entrySet()) {
			String path = entry.getKey();
			TemplateEntry rule = entry.getValue();
			if ((!path.equals("") && !path.startsWith("/")) || (!rule.path.equals("") && !rule.path
				.startsWith("/"))) {
				throw new Exception("Path not appended by leading slash / in path or rule");
			}
//			if(!path.equals("") && !path.startsWith("/")) path = "/".concat(path);
//			if(!rule.path.equals("") && !rule.path.startsWith("/")) rule.path = "/".concat(path);
		};
		this.attributeNameVsRule = preDefinedRules;
	}


	public Optional<TemplateEntry> getRule(String attributeName) {
		return Optional.ofNullable(attributeNameVsRule.get(attributeName));
	}

	public Map<String, TemplateEntry> getAttributeNameVsRule() {
		return attributeNameVsRule;
	}

}
