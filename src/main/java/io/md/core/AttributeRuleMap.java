package io.md.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

public class AttributeRuleMap {

	@JsonProperty("attributeRuleMap")
	private Map<String, TemplateEntry> attributeNameVsRule;

	@JsonCreator
	public AttributeRuleMap(@JsonProperty("attributeRuleMap")Map<String, TemplateEntry> preDefinedRules) {
		this.attributeNameVsRule = preDefinedRules;
	}


	public Optional<TemplateEntry> getRule(String attributeName) {
		return Optional.ofNullable(attributeNameVsRule.get(attributeName));
	}

	public Map<String, TemplateEntry> getAttributeNameVsRule() {
		return attributeNameVsRule;
	}

}
