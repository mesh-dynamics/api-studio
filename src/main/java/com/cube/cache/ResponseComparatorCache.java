package com.cube.cache;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.cube.core.*;
import com.cube.exception.CacheException;

/**
 * Returns ResponseComparator registered in database against the template key
 * If key not registered , returns a default ResponseComparator
 */
public class ResponseComparatorCache {

    private TemplateCache templateCache;

    private LoadingCache<TemplateKey , ResponseComparator> responseComparatorCache;

    private static final Logger LOGGER = LogManager.getLogger(ResponseComparatorCache.class);

    private ResponseComparator defaultResponseComparator;

    public ResponseComparatorCache(TemplateCache cache , ObjectMapper jsonMapper) {
        this.templateCache = cache;
        TemplateEntry equalityRule = new TemplateEntry("/body", CompareTemplate.DataType.Str,
                CompareTemplate.PresenceType.Required, CompareTemplate.ComparisonType.Equal);
        CompareTemplate defaultTemplate = new CompareTemplate();
        defaultTemplate.addRule(equalityRule);
        defaultResponseComparator = new TemplatedResponseComparator(defaultTemplate , jsonMapper);
        this.responseComparatorCache = CacheBuilder.newBuilder().maximumSize(100).build(
                new CacheLoader<>() {
                    @Override
                    public ResponseComparator load(TemplateKey templateKey) throws Exception {
                        CompareTemplate template = templateCache.fetchCompareTemplate(templateKey);
                        LOGGER.info("Successfully loaded into cache response comparator for key :: " + templateKey);
                        return new TemplatedResponseComparator(template , jsonMapper);
                    }
                }
        );
    }


    public ResponseComparator getResponseComparator(TemplateKey key) {
        try {
            return responseComparatorCache.get(key);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to find template key :: " + key +
                " in Response Comparator Cache , sending default key " + e.getMessage() );
            return defaultResponseComparator;
        }
    }

    public void invalidateKey(TemplateKey key) {
        responseComparatorCache.invalidate(key);
        templateCache.invalidateKey(key);
    }


}
