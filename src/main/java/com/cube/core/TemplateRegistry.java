package com.cube.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.md.core.CompareTemplate;

import com.cube.cache.TemplateKey;

public class TemplateRegistry {

    @JsonProperty("path")
    private  String path;
    @JsonProperty("service")
    private  String service;
    @JsonProperty("template")
    private CompareTemplate template;
    /*
    @JsonProperty("id")
    private String id;
    */
    @JsonProperty("type")
    private TemplateKey.Type type;

    // for jackson
    public TemplateRegistry() {
        super();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public CompareTemplate getTemplate() {
        return template;
    }

    public void setTemplate(CompareTemplate template) {
        this.template = template;
    }

    /*
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    */

    public void setType(TemplateKey.Type type) { this.type = type; }

    public TemplateKey.Type getType() { return type; }
}
