package com.cube.cache;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static com.cube.dao.RRBase.*;
import static com.cube.dao.Request.*;

import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplatedRequestComparator;
import com.cube.exception.CacheException;
import com.cube.ws.Config;

public class RequestComparatorCache {
    private static final Logger LOGGER = LogManager.getLogger(RequestComparatorCache.class);

    private TemplateCache templateCache;

    private LoadingCache<TemplateKey , RequestComparator> requestComparatorCache;

    private RequestComparator defaultRequestComparator;

    public RequestComparatorCache(TemplateCache cache , ObjectMapper jsonMapper) {
        this.templateCache = cache;

        CompareTemplate defaultTemplate = new CompareTemplate();
        defaultTemplate.addRule(new TemplateEntry(PATHPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(QPARAMPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(FPARAMPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(RRTYPEPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(CUSTOMERIDPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(APPPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(REQIDPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.EqualOptional));
        defaultTemplate.addRule(new TemplateEntry(COLLECTIONPATH, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, CompareTemplate.DataType.Str
                , CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Equal));
        defaultTemplate.addRule(new TemplateEntry(HDRPATH+"/"+Config.DEFAULT_TRACE_FIELD
                , CompareTemplate.DataType.Str, CompareTemplate.PresenceType.Optional
                , CompareTemplate.ComparisonType.EqualOptional));
        defaultRequestComparator = new TemplatedRequestComparator(defaultTemplate , jsonMapper);

        this.requestComparatorCache = CacheBuilder.newBuilder().maximumSize(100).build(
                new CacheLoader<>() {
                    @Override
                    public RequestComparator load(TemplateKey templateKey) throws Exception {
                        CompareTemplate template = templateCache.fetchCompareTemplate(templateKey);
                        LOGGER.info("Successfully loaded into cache request comparator for key :: " + templateKey);
                        return new TemplatedRequestComparator(template , jsonMapper);
                    }
                }
        );
    }


    public RequestComparator getRequestComparator(TemplateKey key)  {
        try {
            return requestComparatorCache.get(key);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to find template key :: " + key
                    + " in Request Comparator Cache , sending default key");
            return defaultRequestComparator;
            //throw new CacheException("Error while fetching request comparator for key " + key , e);
        }
    }

    public void invalidateKey(TemplateKey key)  {
        requestComparatorCache.invalidate(key);
        templateCache.invalidateKey(key);
    }

}
