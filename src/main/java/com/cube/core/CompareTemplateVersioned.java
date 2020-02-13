package com.cube.core;

import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.cache.TemplateKey;

public class CompareTemplateVersioned extends CompareTemplate {

    @JsonProperty("service")
    public String service;
    @JsonProperty("requestPath")
    public String requestPath;
    @JsonProperty("type")
    public TemplateKey.Type type;

    public CompareTemplateVersioned() {
        super();
    }

    public CompareTemplateVersioned(Optional<String> service, Optional<String> requestPath, TemplateKey.Type type,
                            CompareTemplate contained) {
        super(contained.prefixpath);
        this.service = service.orElse("");
        this.requestPath = requestPath.map(reqPath -> {
            return CompareTemplate.normaliseAPIPath(reqPath);
        }).orElse("");
        this.type = type;
        setRules(contained.getRules());
    }

    public CompareTemplateVersioned(CompareTemplateVersioned source) {
        this(source, source.getRules());
    }

    public CompareTemplateVersioned(CompareTemplateVersioned source, Collection<TemplateEntry> newRules) {
        super(source.prefixpath);
        this.service = source.service;
        this.requestPath = CompareTemplate.normaliseAPIPath(source.requestPath);
        this.type = source.type;
        setRules(newRules);
    }

    @Override
    public ValidateCompareTemplate validate() {
        ValidateCompareTemplate validateCompareTemplate = super.validate();
        if(!validateCompareTemplate.isValid()) {
            return new ValidateCompareTemplate (validateCompareTemplate.isValid, Optional.of("For requestPath: " + requestPath + " and Type: " + type.toString() +  " - " + validateCompareTemplate.getMessage() ));
        }
        return validateCompareTemplate;
    }
}
