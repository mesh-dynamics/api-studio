package com.cube.cache;

import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import io.md.core.CompareTemplate;

import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;
import com.cube.utils.Constants;


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
