package io.md.injection;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.dao.DataObj;
import io.md.services.DataStore;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class DynamicInjectorFactory {

    private final DataStore dataStore;
    private final ObjectMapper jsonMapper;
    private final DynamicInjectionConfigMgr diCfgMgr;

    public DynamicInjectorFactory(DataStore datastore , ObjectMapper mapper){
        this.dataStore = datastore;
        this.jsonMapper = mapper;
        this.diCfgMgr = new DynamicInjectionConfigMgr(datastore);
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
