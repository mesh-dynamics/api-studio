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
