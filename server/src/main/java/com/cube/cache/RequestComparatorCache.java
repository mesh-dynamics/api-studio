package com.cube.cache;

import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import com.cube.core.CompareTemplate;
import com.cube.core.RequestComparator;
import com.cube.core.ResponseComparator;
import com.cube.core.TemplatedRequestComparator;
import com.cube.exception.CacheException;

public class RequestComparatorCache {


    private TemplateCache templateCache;

    private LoadingCache<TemplateKey , RequestComparator> requestComparatorCache;

    public RequestComparatorCache(TemplateCache cache , ObjectMapper jsonMapper) {
        this.templateCache = cache;
        this.requestComparatorCache = CacheBuilder.newBuilder().maximumSize(100).build(
                new CacheLoader<>() {
                    @Override
                    public RequestComparator load(TemplateKey templateKey) throws Exception {
                        CompareTemplate template = templateCache.fetchCompareTemplate(templateKey);
                        return new TemplatedRequestComparator(template , jsonMapper);
                    }
                }
        );
    }


    public RequestComparator getRequestComparator(TemplateKey key) throws CacheException  {
        try {
            return requestComparatorCache.get(key);
        } catch (ExecutionException e) {
            throw new CacheException("Error while fetching request comparator for key " + key , e);
        }
    }

    public void invalidateKey(TemplateKey key) throws CacheException {
        requestComparatorCache.invalidate(key);
        templateCache.invalidateKey(key);
    }

}
