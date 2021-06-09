package com.cube.golden;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SingleTemplateUpdateOperation {

    @JsonProperty("operations")
    final Collection<TemplateEntryOperation> operationList;

    /**
     * For Json Deserialization
     */
    public SingleTemplateUpdateOperation() {
        operationList = Collections.emptyList();
    }

    public SingleTemplateUpdateOperation(Collection<TemplateEntryOperation> operationList) {
        this.operationList = operationList;
    }

    public Collection<TemplateEntryOperation> getOperationList() {
        return operationList;
    }



}
