package com.cube.golden;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.TemplateEntry;
import io.md.dao.ReqRespUpdateOperation.OperationType;

public class TemplateEntryOperation {

    @JsonProperty("type")
    OperationType type;
    @JsonProperty("path")
    String path;
    @JsonProperty("newRule")
    Optional<TemplateEntry> newRule;

    /**
     * Default or serialization
     */
    public TemplateEntryOperation() {

    }

    public TemplateEntryOperation(OperationType type, String path, Optional<TemplateEntry> newRule) {
        this.type = type;
        this.path = path;
        this.newRule = newRule;
    }

    public Optional<TemplateEntry> getNewRule() {
        return newRule;
    }

    public String getPath() {
        return path;
    }

    public OperationType getType() {
        return type;
    }

}
