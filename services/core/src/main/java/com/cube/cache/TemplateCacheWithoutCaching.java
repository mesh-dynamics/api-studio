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
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.utils.Constants;

import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;


/**
 * This is a dummy cache that directly queries solr. Used for test purposes
 * Cache for retrieving analysis templates from solr
 * Based on cache implementation by google guava library
 */
public class TemplateCacheWithoutCaching implements TemplateCache {

    private static final Logger LOGGER = LogManager.getLogger(TemplateCacheWithoutCaching.class);
    private final ReqRespStore reqRespStore;
    /**
     *
     * @param rrStore
     */
    public TemplateCacheWithoutCaching(ReqRespStore rrStore) {
        this.reqRespStore = rrStore;
    }

    @Override
    public CompareTemplate fetchCompareTemplate(TemplateKey key) throws CacheException {

        try {
            CompareTemplate toReturn = null;
                toReturn = reqRespStore.getCompareTemplate(key).orElseThrow(() ->
                    new IOException("Template not found in solr " + key.toString()));
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully stored in redis"
                    , "key" , key.toString())));
            return toReturn;
        }  catch (Throwable e) {
            // wrapping all exceptions in CacheException class
            CacheException ce = new CacheException("Error while fetching template for :".concat(key.toString()) , e);
            throw ce;
        }
    }

    @Override
    public void invalidateKey(TemplateKey key) {
    }

    @Override
    public void invalidateAll() {
    }

}
