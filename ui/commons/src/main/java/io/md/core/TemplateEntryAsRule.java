package io.md.core;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.ExtractionMethod;
import io.md.core.CompareTemplate.PresenceType;

public class TemplateEntryAsRule extends TemplateEntry {

	@JsonProperty("parentPath")
	public Optional<String> parentPath;
	@JsonProperty("isGlobalRule")
	public boolean isGlobalRule;

	public TemplateEntryAsRule(TemplateEntry te,
		Optional<String> parentPath, boolean isGlobalRule) {
		super(te.path, te.dt, te.pt, te.ct, te.em, te.customization, te.arrayComparisionKeyPath);
		this.parentPath = parentPath;
		this.isGlobalRule = isGlobalRule;
	}


	public TemplateEntryAsRule(String path, DataType dt,
		PresenceType pt, ComparisonType ct,
		ExtractionMethod em, Optional<String> customization,
		Optional<String> arrayComparisionKeyPath,
		Optional<String> parentPath, boolean isGlobalRule) {
		super(path, dt, pt, ct, em, customization, arrayComparisionKeyPath);
		this.parentPath = parentPath;
		this.isGlobalRule = isGlobalRule;
	}

}
