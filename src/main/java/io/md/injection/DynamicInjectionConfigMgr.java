package io.md.injection;

import io.md.services.DataStore;
import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

class DynamicInjectionConfigMgr {

    private final DataStore dataStore;
    private static PassiveExpiringMap<String , Optional<DynamicInjectionConfig>> configs = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);

    DynamicInjectionConfigMgr(DataStore dataStore){
        this.dataStore = dataStore;
    }

    Optional<DynamicInjectionConfig> getConfig(String customerId , String app , Optional<String> version){

        if(!version.isPresent()) return Optional.empty();

        final String key = String.format("%s-%s-%s", customerId,  app, version.get());
        Optional<DynamicInjectionConfig> cfg = configs.get(key);
        if(cfg !=null) return cfg;

        cfg = dataStore.getDynamicInjectionConfig(customerId , app , version.get());
        configs.put(key , cfg);

        return cfg;
    }


}
