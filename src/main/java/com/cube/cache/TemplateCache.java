package com.cube.cache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;

import io.cube.agent.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse.RetStatus;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import redis.clients.jedis.Jedis;

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


    //private LoadingCache<TemplateKey, CompareTemplate> templateCache;
    private Config config;
    private static final Logger LOGGER = LogManager.getLogger(TemplateCache.class);
    private ReqRespStore reqRespStore;
    /**
     *
     * @param rrStore
     */
    public TemplateCache(ReqRespStore rrStore, Config config) {
        this.config = config;
        this.reqRespStore = rrStore;
    }

    private FnKey cacheFnKey;

    public CompareTemplate fetchCompareTemplate(TemplateKey key) throws CacheException {
        if (cacheFnKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            cacheFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(cacheFnKey,  CommonUtils.getCurrentTraceId(),
                CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.empty(), key);
            if (ret.retStatus == RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            return (CompareTemplate) ret.retVal;
        }


        try (Jedis jedis = config.jedisPool.getResource()) {
            CompareTemplate toReturn = null;
            if (jedis.exists(key.toString())) {
                String comparatorJson = jedis.get(key.toString());
                LOGGER.info("Successfully retrieved from redis key :: " + key.toString());
                toReturn = config.jsonMapper.readValue(comparatorJson, CompareTemplate.class);
            } else {
                toReturn = reqRespStore.getCompareTemplate(key).orElseThrow(() ->
                    new IOException("Template not found in solr " + key.toString()));
                jedis.set(key.toString() , config.jsonMapper.writeValueAsString(toReturn));
                LOGGER.info("Successfully stored in redis key :: " + key.toString());
            }
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(cacheFnKey,  CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn, RetStatus.Success,
                    Optional.empty(), key);
            }
            return toReturn;
        }  catch (Throwable e) {
            // wrapping all exceptions in CacheException class
            CacheException ce = new CacheException("Error while fetching template for :".concat(key.toString()) , e);
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(cacheFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(),
                    CommonUtils.getParentSpanId(),
                    ce, RetStatus.Exception, Optional.of(ce.getClass().getName()), key);
            }
            throw ce;
        }
    }

    public void invalidateKey(TemplateKey key) {
            try (Jedis jedis = config.jedisPool.getResource()) {
                // no need to remove the key if it doesn't exist
                if (jedis.exists(key.toString())) {
                    jedis.del(key.toString());
                    LOGGER.info("Successfully removed from redis , key :: " + key.toString());
                }
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
