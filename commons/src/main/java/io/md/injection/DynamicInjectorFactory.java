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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.dao.DataObj;
import io.md.services.DataStore;

import java.util.Map;
import java.util.Optional;

public class DynamicInjectorFactory {

    private final DataStore dataStore;
    private final ObjectMapper jsonMapper;
    private final DynamicInjectionConfigMgr diCfgMgr;

    public DynamicInjectorFactory(DataStore datastore , ObjectMapper mapper){
        this.dataStore = datastore;
        this.jsonMapper = mapper;
        this.diCfgMgr = DynamicInjectionConfigMgr.getInstance(dataStore);
    }

    public DynamicInjector getMgr(String customerId , String app , Optional<String> dynamicCfgVersion){
        Optional<DynamicInjectionConfig> cfg = diCfgMgr.getConfig(customerId , app , dynamicCfgVersion);

        return new DynamicInjector(cfg , dataStore , jsonMapper);
    }

    public DynamicInjector getMgr(String customerId , String app , Optional<String> dynamicCfgVersion , Map<String, DataObj> extractionMap){
        Optional<DynamicInjectionConfig> cfg = diCfgMgr.getConfig(customerId , app , dynamicCfgVersion);

        return new DynamicInjector(cfg , dataStore , jsonMapper , extractionMap);
    }

    public DynamicInjector getMgrFromStrMap(String customerId , String app , Optional<String> dynamicCfgVersion , Map<String, String> extractionMap) {
        Optional<DynamicInjectionConfig> cfg = diCfgMgr.getConfig(customerId , app , dynamicCfgVersion);

        return new DynamicInjector(cfg , dataStore , jsonMapper , DynamicInjector.convert(extractionMap , jsonMapper));
    }

}
