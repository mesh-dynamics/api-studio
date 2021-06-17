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

package com.cube.cache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.utils.FnKey;
import io.md.utils.Constants;
import redis.clients.jedis.Jedis;

import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;
import com.cube.ws.Config;


/**
 * Cache for retrieving analysis templates from solr
 * Based on cache implementation by google guava library
 */
public class TemplateCacheRedis implements TemplateCache {


    //private LoadingCache<TemplateKey, CompareTemplate> templateCache;
    private Config config;
    private static final Logger LOGGER = LogManager.getLogger(TemplateCacheRedis.class);
    private ReqRespStore reqRespStore;
    /**
     *
     * @param rrStore
     */
    public TemplateCacheRedis(ReqRespStore rrStore, Config config) {
        this.config = config;
        this.reqRespStore = rrStore;
    }

    private FnKey cacheFnKey;

    @Override
    public CompareTemplate fetchCompareTemplate(TemplateKey key) throws CacheException {
        if (cacheFnKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            cacheFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                config.commonConfig.serviceName, method);
        }
        String runId = Instant.now().toString();

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(cacheFnKey, Optional.empty(), Optional.empty(), runId, key);
            if (ret.retStatus == RetStatus.Exception) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Throwing exception as a result of mocking function")));
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            return (CompareTemplate) ret.retVal;
        }


        try (Jedis jedis = config.jedisPool.getResource()) {
            CompareTemplate toReturn = null;
            if (jedis.exists(key.toString())) {
                String comparatorJson = jedis.get(key.toString());
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Successfully retrieved from redis",  "key" ,  key.toString())));
                toReturn = config.jsonMapper.readValue(comparatorJson, CompareTemplate.class);
            } else {
                toReturn = reqRespStore.getCompareTemplate(key).orElseThrow(() ->
                    new IOException("Template not found in solr " + key.toString()));
                jedis.set(key.toString() , config.jsonMapper.writeValueAsString(toReturn));
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully stored in redis"
                    , "key" , key.toString())));
            }
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(cacheFnKey, toReturn, RetStatus.Success,
                    Optional.empty(),runId, key);
            }
            return toReturn;
        }  catch (Throwable e) {
            // wrapping all exceptions in CacheException class
            CacheException ce = new CacheException("Error while fetching template for :".concat(key.toString()) , e);
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(cacheFnKey,
                    ce, RetStatus.Exception, Optional.of(ce.getClass().getName()), runId, key);
            }
            throw ce;
        }
    }

    @Override
    public void invalidateKey(TemplateKey key) {
            try (Jedis jedis = config.jedisPool.getResource()) {
                // no need to remove the key if it doesn't exist
                if (jedis.exists(key.toString())) {
                    jedis.del(key.toString());
                    LOGGER.debug(new ObjectMessage(
                        Map.of(Constants.MESSAGE ,
                            "Successfully removed from redis", "key" , key.toString())));
                }
            }
    }

    @Override
    public void invalidateAll() {
        try (Jedis jedis = config.jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

}
