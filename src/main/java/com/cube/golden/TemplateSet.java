package com.cube.golden;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.AttributeRuleMap;
import io.md.utils.Utils;

import com.cube.core.CompareTemplateVersioned;
import com.cube.core.ServerUtils;

public class TemplateSet {

	// Tagging the template set
	@JsonProperty("version")
	public String version;
	@JsonProperty("customer")
	public final String customer;
	@JsonProperty("app")
	public final String app;
	@JsonProperty("timestamp")
	public final Instant timestamp;
	@JsonProperty("templates")
	public final List<CompareTemplateVersioned> templates;
	@JsonProperty("attributeRuleMap")
	public final Optional<AttributeRuleMap> appAttributeRuleMap;
	@JsonProperty("name")
	public final String name;
	@JsonProperty("label")
	public final String label;


	@JsonCreator
	public TemplateSet(@JsonProperty("customer") String customer,
		@JsonProperty("app") String app, @JsonProperty("timestamp") Instant timestamp,
		@JsonProperty("templates") List<CompareTemplateVersioned> compareTemplateVersionedList,
		@JsonProperty("attributeRuleMap") Optional<AttributeRuleMap> appAttributeRuleMap,
		@JsonProperty("name") String name, @JsonProperty("label") String  label) {
		this.name = name;
		this.label = label;
		this.version = ServerUtils.createTemplateSetVersion(name, label);
		this.customer = customer;
		this.app = app;
		this.timestamp = timestamp != null ? timestamp : Instant.now();
		this.templates = compareTemplateVersionedList;
		this.appAttributeRuleMap = appAttributeRuleMap;
	}

	public static class TemplateSetMetaStoreException extends Exception {
		public TemplateSetMetaStoreException(String message) {
			super(message);
		}

	}

}
