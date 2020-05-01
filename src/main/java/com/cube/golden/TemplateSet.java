package com.cube.golden;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.core.CompareTemplateVersioned;
import io.md.core.AttributeRuleMap;

public class TemplateSet {

    // Tagging the template set
    @JsonProperty("version")
    public final String version;
    @JsonProperty("customer")
    public final String customer;
    @JsonProperty("app")
    public final String app;
    @JsonProperty("timestamp")
    public final Instant timestamp;
    @JsonProperty("templates")
    public final List<CompareTemplateVersioned> templates;
    @JsonProperty("attributeRuleMap")
    public final Optional<AttributeRuleMap> appAttributeRuleMap;


    @JsonCreator
    public TemplateSet(@JsonProperty("version") String version, @JsonProperty("customer") String customer,
                       @JsonProperty("app") String app, @JsonProperty("timestamp") Instant timestamp,
                       @JsonProperty("templates") List<CompareTemplateVersioned> compareTemplateVersionedList,
                       @JsonProperty("appAttributeMap") Optional<AttributeRuleMap> appAttributeRuleMap) {
        this.version = version != null ? version : UUID.randomUUID().toString(); ;
        this.customer = customer;
        this.app = app;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.templates = compareTemplateVersionedList;
        this.appAttributeRuleMap = appAttributeRuleMap;
    }

    public static class TemplateSetMetaStoreException extends Exception {
        public TemplateSetMetaStoreException(String message) {
            super(message);
        }

    }

}
