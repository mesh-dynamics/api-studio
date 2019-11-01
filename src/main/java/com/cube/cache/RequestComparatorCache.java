package com.cube.cache;

import com.cube.utils.Constants;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.cube.core.JsonComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.cube.core.CompareTemplate;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.core.RequestComparator;
import com.cube.core.TemplateEntry;
import com.cube.core.TemplatedRequestComparator;

// TODO: Event redesign: This and ResponseComparatorCache can be replaced by a single ComparatorCache
public class RequestComparatorCache {
    private static final Logger LOGGER = LogManager.getLogger(RequestComparatorCache.class);

    private TemplateCache templateCache;

    private LoadingCache<TemplateKey , RequestComparator> requestComparatorCache;
    private LoadingCache<TemplateKey, JsonComparator> functionComparatorCache;

    private RequestComparator defaultRequestComparatorWithoutReqId;
    private RequestComparator defaultRequestComparatorWithReqId;
    private JsonComparator defaultFunctionComparator;


    public RequestComparatorCache(TemplateCache cache , ObjectMapper jsonMapper) {
        this.templateCache = cache;

        CompareTemplate defaultTemplateWithoutReqId = new CompareTemplate();
        CompareTemplate defaultTemplateWithReqId = new CompareTemplate();
        CompareTemplate defaultFunctionTemplate = new CompareTemplate();
        List<TemplateEntry> defaultRules = new ArrayList<>();
        //TODO: Event redesign: cleanup the commented rules
        //defaultRules.add(new TemplateEntry(PATHPATH,DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(Constants.QUERY_PARAMS_PATH, DataType.Obj, PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(Constants.FORM_PARAMS_PATH, DataType.Obj, PresenceType.Optional, ComparisonType.Equal));
        //defaultRules.add(new TemplateEntry(RUNTYPEPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        //defaultRules.add(new TemplateEntry(CUSTOMERIDPATH, DataType.Str, PresenceType.Optional, ComparisonType
        // .Equal));
        //defaultRules.add(new TemplateEntry(APPPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        //defaultRules.add(new TemplateEntry(COLLECTIONPATH, DataType.Str, PresenceType.Optional, ComparisonType
        // .Equal));
        //defaultRules.add(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, DataType.Str, PresenceType.Optional,
        //    ComparisonType.Equal));
        //defaultRules.add(new TemplateEntry(HDRPATH+"/"+Config.DEFAULT_TRACE_FIELD, DataType.Str,q/para
        //    PresenceType.Optional, ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(Constants.BODY_PATH, CompareTemplate.DataType.Default,
            CompareTemplate.PresenceType.Required, CompareTemplate.ComparisonType.Equal));
        defaultRules.add(new TemplateEntry(Constants.METHOD_PATH, DataType.Str, CompareTemplate.PresenceType.Required,
            CompareTemplate.ComparisonType.Equal));
        defaultRules.forEach(rule -> {
            defaultTemplateWithoutReqId.addRule(rule);
            defaultTemplateWithReqId.addRule(rule);
        });
        defaultTemplateWithReqId.addRule(new TemplateEntry(Constants.REQ_ID_PATH, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));

        //Function Template
        defaultFunctionTemplate.addRule(new TemplateEntry(Constants.ARGS_PATH, DataType.NrptArray, PresenceType.Required, ComparisonType.Equal));


        defaultRequestComparatorWithoutReqId = new TemplatedRequestComparator(defaultTemplateWithoutReqId
                , jsonMapper);
        defaultRequestComparatorWithReqId = new TemplatedRequestComparator(defaultTemplateWithReqId
                , jsonMapper);
        defaultFunctionComparator = new JsonComparator(defaultFunctionTemplate, jsonMapper);

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

        this.functionComparatorCache = CacheBuilder.newBuilder().maximumSize(100).build(
            new CacheLoader<>() {
                @Override
                public JsonComparator load(TemplateKey templateKey) throws Exception {
                    CompareTemplate template = templateCache.fetchCompareTemplate(templateKey);
                    LOGGER.info("Successfully loaded into cache request comparator for key :: " + templateKey);
                    return new JsonComparator(template , jsonMapper);
                }
            }
        );
    }


    public RequestComparator getRequestComparator(TemplateKey key,
                                                  boolean reqIdInDefault)  {
        try {
            return requestComparatorCache.get(key);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to find template key :: " + key
                    + " in Request Comparator Cache , sending default key " + e.getMessage());
            if (reqIdInDefault) {
                return defaultRequestComparatorWithReqId;
            }
            return defaultRequestComparatorWithoutReqId;
            //throw new CacheException("Error while fetching request comparator for key " + key , e);
        } catch (Throwable e) {
            LOGGER.error("Unhandled exception occured (re-throwing) :: " + e.getMessage());
            throw e;
        }
    }

    public JsonComparator getFunctionComparator(TemplateKey key) {
        try {
            return functionComparatorCache.get(key);
        } catch (ExecutionException e) {
            LOGGER.error("Unable to find template key :: " + key
                + " in Request Comparator Cache , sending default key " + e.getMessage());
            return defaultFunctionComparator;
        } catch (Throwable e) {
            LOGGER.error("Unhandled exception occured (re-throwing) :: " + e.getMessage());
            throw e;
        }
    }

    public void invalidateKey(TemplateKey key)  {
        requestComparatorCache.invalidate(key);
        functionComparatorCache.invalidate(key);
        templateCache.invalidateKey(key);
    }

    public void invalidateAll() {
        requestComparatorCache.invalidateAll();
        functionComparatorCache.invalidateAll();
    }

}
