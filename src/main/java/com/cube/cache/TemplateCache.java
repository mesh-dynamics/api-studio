package com.cube.cache;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import io.cube.agent.FnKey;

import com.cube.core.CompareTemplate;
import com.cube.core.Utils;
import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;
import com.cube.ws.Config;


/**
 * Cache for retrieving analysis templates from solr
 * Based on cache implementation by google guava library
 */
public class TemplateCache {


    private LoadingCache<TemplateKey, CompareTemplate> templateCache;
    private Config config;
    private static final Logger LOGGER = LogManager.getLogger(TemplateCache.class);

    /**
     *
     * @param rrStore
     */
    public TemplateCache(ReqRespStore rrStore, Config config) {
        this.config = config;
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

    private FnKey cacheFnKey;

    public CompareTemplate fetchCompareTemplate(TemplateKey key) throws CacheException {
        if (cacheFnKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            cacheFnKey = new FnKey(config.customerId, config.app, config.instance,
                config.serviceName, method);
        }

        if (Utils.isIntentToMock()) {
            return (CompareTemplate) config.mocker.mock(cacheFnKey,  Utils.getCurrentTraceId(),
                Utils.getCurrentSpanId(), Utils.getParentSpanId(), Optional.empty(), key).retVal;
        }


        try {
            CompareTemplate toReturn = templateCache.get(key);
            if (Utils.isIntentToRecord()) {
                config.recorder.record(cacheFnKey,  Utils.getCurrentTraceId(),
                    Utils.getCurrentSpanId(), Utils.getParentSpanId(), toReturn, key);
            }
            return toReturn;
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
