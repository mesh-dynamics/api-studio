package com.cube.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.dao.ReqRespStore;

/**
 * TODO in case of a distributed deployment of cube service, these caches need to be separated
 * as a service, possibly redis
 */
public class ReplayResultCache {

    private static final Logger LOGGER = LogManager.getLogger(ReplayResultCache.class);

    public class ReplayPathStatistic {

        @JsonIgnore
        public String customer;
        @JsonIgnore
        public String app;
        @JsonIgnore
        public String service;
        @JsonProperty("path")
        public String path;
        @JsonProperty("totalReq")
        public Integer reqCount = 0;
        @JsonProperty("matchReq")
        public Integer matchCount = 0 ;
        @JsonProperty("noMatchReq")
        public Integer nonMatchCount = 0;

        public ReplayPathStatistic(String customer, String app, String service, String path) {
            this.customer = customer;
            this.app = app;
            this.service = service;
            this.path = path;
        }

        public void incrementMatchCount() {
            reqCount++;
            matchCount++;
        }

        public void incrementNonMatchCount(){
            reqCount++;
            nonMatchCount++;
        }

    }

    public ReplayResultCache(ReqRespStore reqRespStore) {
        replayStatisticsMap = new ConcurrentHashMap<>();
        currentReplayId = new ConcurrentHashMap<>();
        this.reqRespStore = reqRespStore;
    }

    private ConcurrentHashMap<Integer , ConcurrentHashMap<Integer , ReplayPathStatistic>> replayStatisticsMap;
    private ConcurrentHashMap<Integer , String> currentReplayId;
    private ReqRespStore reqRespStore;

    public ReplayPathStatistic getPathStatistic(String customer, String app, String service, String path) {
        Integer topLevelKey = Objects.hash(customer,app);
        Integer secondaryKey = Objects.hash(service,path);
        return replayStatisticsMap.computeIfAbsent(topLevelKey , k -> new ConcurrentHashMap<>())
                .computeIfAbsent(secondaryKey , k -> new ReplayPathStatistic(customer,app,service,path));
    }

    public void incrementReqMatchCounter(String customer, String app, String service, String path) {
        getPathStatistic(customer,app,service,path).incrementMatchCount();
    }


    public void incrementReqNotMatchCounter(String customer, String app, String service, String path) {
        getPathStatistic(customer,app,service,path).incrementNonMatchCount();
    }

    private void materializeResults(Integer key, String replayId) {
        Map<String,List<ReplayPathStatistic>> serviceVsReplayPathStatistic = new HashMap<>();
        replayStatisticsMap.getOrDefault(key , new ConcurrentHashMap<>())
                .entrySet().stream().map(Map.Entry::getValue).forEach(pathStatistic -> {
                    serviceVsReplayPathStatistic.computeIfAbsent(pathStatistic.service
                            , service -> new ArrayList<>()).add(pathStatistic);
                });
        reqRespStore.saveReplayResult(serviceVsReplayPathStatistic , replayId);
    }

    private void invalidateCache(Integer key){
        currentReplayId.remove(key);
        replayStatisticsMap.remove(key);
    }

    public void stopReplay(String customer, String app, String replayId) {
        Integer key = Objects.hash(customer , app);
        materializeResults(key , replayId);
        invalidateCache(key);
    }

    public void startReplay(String customer, String app, String replayId) {
        Integer key = Objects.hash(customer , app);
        currentReplayId.put(key , replayId);
        replayStatisticsMap.remove(key);
    }

}
