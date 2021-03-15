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

        final String key = String.format("%s-%s-%s", customerId,  app, version.get());
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
