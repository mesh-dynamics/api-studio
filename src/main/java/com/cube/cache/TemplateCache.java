package com.cube.cache;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.*;

import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse.RetStatus;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;

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

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(cacheFnKey,  Utils.getCurrentTraceId(),
                Utils.getCurrentSpanId(), Utils.getParentSpanId(), Optional.empty(), key);
            if (ret.retStatus == RetStatus.Exception) {
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            return (CompareTemplate) ret.retVal;
        }


        try {
            CompareTemplate toReturn = templateCache.get(key);
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(cacheFnKey,  Utils.getCurrentTraceId(),
                    Utils.getCurrentSpanId(), Utils.getParentSpanId(), toReturn, RetStatus.Success,
                    Optional.empty(), key);
            }
            return toReturn;
        }  catch (Throwable e) {
            // wrapping all exceptions in CacheException class
            CacheException ce = new CacheException("Error while fetching template for :".concat(key.toString()) , e);
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(cacheFnKey, Utils.getCurrentTraceId(),
                    Utils.getCurrentSpanId(),
                    Utils.getParentSpanId(),
                    ce, RetStatus.Exception, Optional.of(ce.getClass().getName()), key);
            }
            throw ce;
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
