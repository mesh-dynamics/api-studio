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
