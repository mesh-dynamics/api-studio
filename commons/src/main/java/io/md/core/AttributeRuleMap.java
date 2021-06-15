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
