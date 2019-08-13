package com.cube.golden;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.cache.TemplateKey;

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
