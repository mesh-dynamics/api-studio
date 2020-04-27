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
