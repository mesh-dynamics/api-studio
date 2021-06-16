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

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.TemplateEntry;
import io.md.dao.ReqRespUpdateOperation.OperationType;

public class TemplateEntryOperation {

    @JsonProperty("type")
    OperationType operationType;
    @JsonProperty("path")
    String path;
    @JsonProperty("newRule")
    Optional<TemplateEntry> newRule;
    @JsonProperty("ruleType")
    RuleType ruleType;

    /**
     * Default or serialization
     */
    public TemplateEntryOperation() {
        this.ruleType = RuleType.TEMPLATERULE;
    }

    public TemplateEntryOperation(OperationType operationType, String path, Optional<TemplateEntry> newRule, RuleType ruleType) {
        this.operationType = operationType;
        this.path = path;
        this.newRule = newRule;
        this.ruleType = ruleType;
    }

    public Optional<TemplateEntry> getNewRule() {
        return newRule;
    }

    public String getPath() {
        return path;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public RuleType getRuleType() {
        return ruleType;
    }


    public static enum RuleType {
        TEMPLATERULE,
        ATTRIBUTERULE;

        private RuleType() {
        }
    }
}
