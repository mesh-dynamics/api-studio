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

package io.md.injection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjectionConfig.StaticValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class StaticInjection{


    public final String customerId;
    public final String app;
    public final String version;

    private final static String staticValPrefix = "staticVal_";

    public StaticInjection(String customerId, String app, String version) {
        this.version = version;
        this.customerId = customerId;
        this.app = app;
    }

    public DynamicInjectionConfig convertStaticMetasToDynamicConfig(List<StaticInjectionMeta> staticConfigList){
        final List<StaticValue> staticValueList = new ArrayList<>();
        final List<InjectionMeta> injectionMetaList = new ArrayList<>();
        staticConfigList.forEach(config -> {
            final String varName = staticValPrefix + config.value;
            staticValueList.add(new StaticValue(varName, config.value));
            injectionMetaList.add(
                new InjectionMeta(config.apiPaths, config.jsonPath, false,
                    varName, Optional.empty(), Optional.empty(), Optional.empty(), config.method,
                    Optional.empty(), Optional.empty()));
        });
        return new DynamicInjectionConfig(version, customerId, app, Optional.empty(),
            Collections.emptyList(), injectionMetaList, staticValueList, Collections.emptyList());
    }

    public static List<StaticInjectionMeta> getStaticMetasFromDynamicConfig(
        DynamicInjectionConfig diConfig) {
        HashMap<String, String> varNameToValueMap = new HashMap<>();
        List<StaticInjectionMeta> staticInjectionMetas = new ArrayList<>();
        diConfig.staticValues.forEach(staticValue -> {
            varNameToValueMap.put(staticValue.name, staticValue.value);
        });
        diConfig.injectionMetas.forEach(injectionMeta -> {
            Optional<String> value = Optional.ofNullable(varNameToValueMap.get(injectionMeta.name));
            value.ifPresent(val -> {
                staticInjectionMetas.add(new StaticInjectionMeta(val,
                    injectionMeta.apiPaths, injectionMeta.jsonPath, injectionMeta.method));
            });

        });
        return staticInjectionMetas;
    }
    @JsonPropertyOrder({"Value", "APIPaths", "JSONPath", "Method"})
    public static class StaticInjectionMeta {
        @JsonProperty("Value")
        final String value;
        @JsonProperty("APIPaths")
        final List<String> apiPaths;
        @JsonProperty("JSONPath")
        final String jsonPath;
        @JsonProperty("Method")
        final HTTPMethodType method;

        @JsonCreator
        public StaticInjectionMeta(@JsonProperty("Value") String value,
            @JsonProperty("APIPaths") List<String> apiPaths, @JsonProperty("JSONPath") String jsonPath,
            @JsonProperty("Method") HTTPMethodType method) {
            this.value = value;
            this.apiPaths = apiPaths;
            this.jsonPath = jsonPath;
            this.method = method;
        }
    }

}



