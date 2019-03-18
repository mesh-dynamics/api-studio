package com.cube.cache;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ComparisonChain;

import com.cube.core.CompareTemplate;
import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;


/**
 * Cache for retrieving analysis templates from solr
 * Based on cache implementation by google guava library
 */
public class AnalysisTemplateCache {


    /**
     * Key against which the analysis template will be retrieved/cached
     */
    class TemplateKey {
        String customerId;
        String appId;
        String serviceId;
        String path;

        public TemplateKey(String customerId, String appId, String serviceId, String path) {
            this.customerId = customerId;
            this.appId = appId;
            this.serviceId = serviceId;
            this.path = path;
        }


        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("customerId" , customerId).add("appId" , appId)
                    .add("serviceId" , serviceId).add("path" , path).toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.customerId,this.appId,this.serviceId,this.path);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TemplateKey) {
                TemplateKey other = (TemplateKey) o;
                return (ComparisonChain.start().
                        compare(this.customerId , other.customerId).compare(this.appId , other.appId).
                        compare(this.serviceId , other.serviceId).compare(this.path , other.path).result() == 0);
            } else {
                return false;
            }
        }


    }

    private LoadingCache<TemplateKey, CompareTemplate> templateCache;


    private static final Logger LOGGER = LogManager.getLogger(AnalysisTemplateCache.class);

    /**
     *
     * @param rrStore
     */
    public AnalysisTemplateCache(ReqRespStore rrStore) {
        templateCache = CacheBuilder.newBuilder().maximumSize(10).build(
                new CacheLoader<>() {
                    @Override
                    public CompareTemplate load(TemplateKey key) throws Exception {
                        Optional<CompareTemplate> template =
                                rrStore.getCompareTemplate(key.customerId,key.appId,key.serviceId,key.path);
                        if (template.isEmpty()) {
                            throw new Exception("Couldn't find template corresponding to " + key);
                        }
                        return template.get();
                    }
                }
        );
    }


    public CompareTemplate fetchCompareTemplate(String customerId, String appId,
                                                          String serviceId, String path) throws CacheException {
        TemplateKey key = new TemplateKey(customerId, appId, serviceId, path);
        try {
            return templateCache.get(key);
        }  catch (ExecutionException e) {
            // wrapping all exceptions in CacheException class
            throw new CacheException("Error while fetching template for :".concat(key.toString()) , e);

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
