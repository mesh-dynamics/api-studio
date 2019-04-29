package com.cube.cache;

import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.*;

import com.cube.core.CompareTemplate;
import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;


/**
 * Cache for retrieving analysis templates from solr
 * Based on cache implementation by google guava library
 */
public class TemplateCache {


    private LoadingCache<TemplateKey, CompareTemplate> templateCache;


    private static final Logger LOGGER = LogManager.getLogger(TemplateCache.class);

    /**
     *
     * @param rrStore
     */
    public TemplateCache(ReqRespStore rrStore) {
        templateCache = CacheBuilder.newBuilder().maximumSize(200).removalListener(
                new RemovalListener<>() {
                    @Override
                    public void onRemoval(RemovalNotification<Object, Object> removalNotification) {
                        LOGGER.info("Removed key ".concat(removalNotification.getKey().toString()));
                    }
                }).build(
                new CacheLoader<>() {
                    @Override
                    public CompareTemplate load(TemplateKey key) throws Exception {
                        return rrStore.getCompareTemplate(key).orElseThrow(() -> {
                            return new Exception("Couldn't find template corresponding to " + key);
                        });
                    }
                }
        );
    }


    public CompareTemplate fetchCompareTemplate(TemplateKey key) throws CacheException {
        try {
            return templateCache.get(key);
        }  catch (ExecutionException e) {
            // wrapping all exceptions in CacheException class
            throw new CacheException("Error while fetching template for :".concat(key.toString()) , e);

        }
    }

    public void invalidateKey(TemplateKey key) {
            templateCache.invalidate(key);
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
