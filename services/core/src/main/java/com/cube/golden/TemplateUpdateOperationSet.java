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

package com.cube.golden;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.TemplateKey;

public class TemplateUpdateOperationSet {

    @JsonProperty("templateUpdates")
    private final Map<TemplateKey, SingleTemplateUpdateOperation> templateUpdates;

    @JsonProperty("templateUpdateSetId")
    private final String templateUpdateSetId;

    /**
     * For Json Deserialization
     */
    public TemplateUpdateOperationSet() {
        super();
        this.templateUpdateSetId = "";
        this.templateUpdates = new HashMap<>();
    }

    public TemplateUpdateOperationSet(String templateUpdateOperationSetId, Map<TemplateKey, SingleTemplateUpdateOperation> updates) {
        this.templateUpdateSetId = templateUpdateOperationSetId;
        this.templateUpdates = updates;
    }

    @JsonIgnore
    public Map<TemplateKey, SingleTemplateUpdateOperation> getTemplateUpdates() {
        return this.templateUpdates;
    }

    @JsonIgnore
    public String getTemplateUpdateOperationSetId() {return this.templateUpdateSetId; }

}
