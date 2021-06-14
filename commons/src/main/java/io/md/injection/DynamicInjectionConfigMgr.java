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

import static io.md.cache.Constants.APP;
import static io.md.cache.Constants.CUSTOMER_ID;
import static io.md.cache.Constants.CACHE_TO_LIVE_DUR;
import static io.md.cache.Constants.VERSION;

import io.md.cache.AbstractMDCache;
import io.md.cache.Constants;
import io.md.logger.LogMgr;
import io.md.services.CustAppConfigCache;
import io.md.services.DataStore;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class DynamicInjectionConfigMgr extends AbstractMDCache {

    private static final Logger LOGGER = LogMgr.getLogger(DynamicInjectionConfigMgr.class);
    private final DataStore dataStore;
    private static PassiveExpiringMap<String , Optional<DynamicInjectionConfig>> configs = new PassiveExpiringMap<>(
        CACHE_TO_LIVE_DUR.toMillis());
    private List<Pair<PassiveExpiringMap<String, ?>, String[]>> cacheAndKeys = new ArrayList<>(1);
    {
        cacheAndKeys.add(Pair.of(configs, new String[]{CUSTOMER_ID, APP, VERSION}));
    }

    private static DynamicInjectionConfigMgr singleton;
    private DynamicInjectionConfigMgr(DataStore dataStore){
        this.dataStore = dataStore;
    }

    public static DynamicInjectionConfigMgr getInstance(DataStore dataStore){

        if(singleton!=null) return singleton;
        synchronized (CustAppConfigCache.class){
            if(singleton==null){
                singleton = new DynamicInjectionConfigMgr(dataStore);
            }
        }
        return singleton;
    }

    Optional<DynamicInjectionConfig> getConfig(String customerId , String app , Optional<String> version){

        if(!version.isPresent()) return Optional.empty();

        final String key = createFieldsKey(customerId,  app, version.get());
        Optional<DynamicInjectionConfig> cfg = configs.get(key);
        if(cfg !=null) return cfg;

        cfg = dataStore.getDynamicInjectionConfig(customerId , app , version.get());
        configs.put(key , cfg);

        return cfg;
    }


    @Override
    public String getName() {
        return Constants.DYNAMIC_INJECTION;
    }

    @Override
    public List<Pair<PassiveExpiringMap<String, ?>, String[]>> getCacheAndKeys() {
        return cacheAndKeys;
    }

}
