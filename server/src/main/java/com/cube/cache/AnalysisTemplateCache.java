package com.cube.cache;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.cube.core.CompareTemplate;
import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;


/**
 * Cache for retrieving analysis templates from solr
 * Based on cache implementation by google guava library
 */
public class AnalysisTemplateCache {


    /**
     * Key against which the analysis template will be retrieved/cached
     */
    class TemplateKey {
        String customerId;
        String appId;
        String serviceId;
        String path;

        public TemplateKey(String customerId, String appId, String serviceId, String path) {
            this.customerId = customerId;
            this.appId = appId;
            this.serviceId = serviceId;
            this.path = path;
        }

        public String toString(){
            return customerId.concat("-").concat(appId).concat("-").concat(serviceId).concat("-").concat(path);
        }

    }

    private LoadingCache<TemplateKey, CompareTemplate> templateCache;


    private static final Logger LOGGER = LogManager.getLogger(AnalysisTemplateCache.class);

    /**
     *
     * @param rrStore
     */
    public AnalysisTemplateCache(ReqRespStore rrStore) {
        templateCache = CacheBuilder.newBuilder().maximumSize(10).build(
                new CacheLoader<>() {
                    @Override
                    public CompareTemplate load(TemplateKey key) throws Exception {
                        Optional<CompareTemplate> template =
                                rrStore.getCompareTemplate(key.customerId,key.appId,key.serviceId,key.path);
                        if (template.isEmpty()) {
                            throw new Exception("Couldn't find template corresponding to " + key);
                        }
                        return template.get();
                    }
                }
        );
    }


    public CompareTemplate fetchCompareTemplate(String customerId, String appId,
                                                          String serviceId, String path) throws CacheException {
        TemplateKey key = new TemplateKey(customerId, appId, serviceId, path);
        try {
            return templateCache.get(key);
        }  catch (ExecutionException e) {
            // wrapping all templates in CacheException class
            throw new CacheException("Error while fetching template for :".concat(key.toString()) , e);

        }
    }


    /*private Integer createKey(String customerId, String appId, String serviceId, String path){
        int prime = 31;
        int result = 1;
        result = result*prime + customerId.hashCode();
        result = result*prime + appId.hashCode();
        result = result*prime + serviceId.hashCode();
        result = result*prime + path.hashCode();
        return result;
    }*/


}
