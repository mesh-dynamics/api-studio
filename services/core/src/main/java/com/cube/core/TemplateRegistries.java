package com.cube.core;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TemplateRegistries {

    public TemplateRegistries() {
        super();
    }


    @JsonProperty("registries")
    private
    List<TemplateRegistry> templateRegistryList;

    public List<TemplateRegistry> getTemplateRegistryList() {
        return templateRegistryList;
    }

    public void setTemplateRegistryList(List<TemplateRegistry> templateRegistryList) {
        this.templateRegistryList = templateRegistryList;
    }


}
