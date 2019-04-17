package com.cube.cache;

import java.util.ArrayList;
import java.util.List;
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
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.core.RequestComparator;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplatedRequestComparator;
import com.cube.ws.Config;

public class RequestComparatorCache {
    private static final Logger LOGGER = LogManager.getLogger(RequestComparatorCache.class);

    private TemplateCache templateCache;

    private LoadingCache<TemplateKey , RequestComparator> requestComparatorCache;

    private RequestComparator defaultRequestComparatorWithoutReqId;
    private RequestComparator defaultRequestComparatorWithReqId;


    public RequestComparatorCache(TemplateCache cache , ObjectMapper jsonMapper) {
        this.templateCache = cache;

        CompareTemplate defaultTemplateWithoutReqId = new CompareTemplate();
        CompareTemplate defaultTemplateWithReqId = new CompareTemplate();
        List<TemplateEntry> defaultRules = new ArrayList<>();
        defaultRules.add(new TemplateEntry(PATHPATH,DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(QPARAMPATH, DataType.Obj, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(FPARAMPATH, DataType.Obj, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(RRTYPEPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(CUSTOMERIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(APPPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(COLLECTIONPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(HDRPATH+"/"+Config.DEFAULT_TRACE_FIELD, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
        defaultRules.forEach(rule -> {
            defaultTemplateWithoutReqId.addRule(rule);
            defaultTemplateWithReqId.addRule(rule);
        });
        defaultTemplateWithReqId.addRule(new TemplateEntry(REQIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));


        defaultRequestComparatorWithoutReqId = new TemplatedRequestComparator(defaultTemplateWithoutReqId
                , jsonMapper);
        defaultRequestComparatorWithReqId = new TemplatedRequestComparator(defaultTemplateWithReqId
                , jsonMapper);

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


    public RequestComparator getRequestComparator(TemplateKey key , boolean reqIdInDefault)  {
        try {
            return requestComparatorCache.get(key);
        } catch (ExecutionException e) {
            LOGGER.debug("Unable to find template key :: " + key
                    + " in Request Comparator Cache , sending default key");
            if (reqIdInDefault) {
                return defaultRequestComparatorWithReqId;
            }
            return defaultRequestComparatorWithoutReqId;
            //throw new CacheException("Error while fetching request comparator for key " + key , e);
        }
    }

    public void invalidateKey(TemplateKey key)  {
        requestComparatorCache.invalidate(key);
        templateCache.invalidateKey(key);
    }

}
