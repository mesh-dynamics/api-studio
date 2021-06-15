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

package com.cube.core.attribute.rule;

import io.md.core.AttributeRuleMap;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.cube.ws.Config;

public class AttributeMapSaveTest {

    public static void main(String[] args) {
        try {

            Map<String, TemplateEntry> ruleMap = new HashMap<>();

            // public TemplateEntry(@JsonProperty("path") String path, @JsonProperty("dt") DataType dt
            // , @JsonProperty("pt") PresenceType pt, @JsonProperty("ct") ComparisonType ct
            // , @JsonProperty("em") ExtractionMethod em
            // , @JsonProperty("customization") Optional<String> customization) {
            //
            ruleMap.put("/timestamp" , new TemplateEntry("/timestamp", CompareTemplate.DataType.Default,
                CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Ignore
                , CompareTemplate.ExtractionMethod.Default, Optional.empty(), Optional.empty()));

            AttributeRuleMap attributeRuleMap = new AttributeRuleMap(ruleMap);

            Config config = new Config();

            TemplateKey templateKey = new TemplateKey("Default", "ravivj"
                , "random" , "NA", "NA"
                , TemplateKey.Type.RequestCompare);

            String ruleMapAsString = config.jsonMapper.writeValueAsString(attributeRuleMap);

            String ruleMapId = config.rrstore.saveAttributeRuleMap(templateKey, ruleMapAsString);

            System.out.println("Created Rule Map with Id " + ruleMapId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
