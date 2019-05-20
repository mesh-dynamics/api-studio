package com.cube.cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.cube.dao.ReqRespStore;
import com.cube.ws.Config;

/**
 * TODO in case of a distributed deployment of cube service, these caches need to be separated
 * as a service, possibly redis
 * This Cache serves the purpose of storing request match / not match counts for a Mock(Virtual)
 * Service during replay. The initiation/stopping of replay is done via the replay driver. Once the replay
 * is in action, the mock service gives directions to increment appropriate counters. The linking
 * is done via the customer/app/service composite key. So the assumption is that only one replay
 * will be running for every such combination at a given time.
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
        @JsonIgnore
        public String instanceId;
        @JsonProperty("path")
        public String path;
        @JsonProperty("totalReq")
        public AtomicInteger reqCount = new AtomicInteger(0);
        @JsonProperty("matchReq")
        public AtomicInteger matchCount = new AtomicInteger(0) ;
        @JsonProperty("noMatchReq")
        public AtomicInteger nonMatchCount = new AtomicInteger(0);

        public ReplayPathStatistic(String customer, String app, String service, String path, String instanceId) {
            this.customer = customer;
            this.app = app;
            this.service = service;
            this.path = path;
            this.instanceId = instanceId;
        }

        public void incrementMatchCount() {
            reqCount.incrementAndGet();
            matchCount.incrementAndGet();
        }

        public void incrementNonMatchCount(){
            reqCount.incrementAndGet();
            nonMatchCount.incrementAndGet();
        }

    }

    public ReplayResultCache(ReqRespStore reqRespStore, Config config) {
        replayStatisticsMap = new ConcurrentHashMap<>();
        currentReplayId = new ConcurrentHashMap<>();
        this.reqRespStore = reqRespStore;
        this.config = config;
    }

    private ConcurrentHashMap<Integer , ConcurrentHashMap<Integer , ReplayPathStatistic>> replayStatisticsMap;
    private ConcurrentHashMap<Integer , String> currentReplayId;
    private ReqRespStore reqRespStore;
    private Config config;

    private ReplayPathStatistic getPathStatistic(String customer, String app, String service, String path, String instanceId) {
        Integer topLevelKey = Objects.hash(customer,app,instanceId);
        Integer secondaryKey = Objects.hash(service,path);
        return replayStatisticsMap.computeIfAbsent(topLevelKey , k -> new ConcurrentHashMap<>())
                .computeIfAbsent(secondaryKey , k -> new ReplayPathStatistic(customer,app,service,path,instanceId));
    }

    /**
     * Increment the counter for request match (from MockService)
     * @param customer
     * @param app
     * @param service
     * @param path
     */
    public void incrementReqMatchCounter(String customer, String app, String service, String path, String instanceId) {
        if (config.getState() == Config.AppState.Mock) return;
        getPathStatistic(customer,app,service,path,instanceId).incrementMatchCount();
    }

    /**
     * Increment the counter for request no match (from MockService)
     * @param customer
     * @param app
     * @param service
     * @param path
     */
    public void incrementReqNotMatchCounter(String customer, String app, String service, String path, String instaceId) {
        if (config.getState() == Config.AppState.Mock) return;
        getPathStatistic(customer,app,service,path,instaceId).incrementNonMatchCount();
    }

    /**
     * Once the replay is over, store the results in the backend. The results are stored path wise
     * @param key
     * @param replayId
     */
    private void materializeResults(Integer key, String replayId) {
        if (config.getState() == Config.AppState.Mock) return;
        Map<String,List<ReplayPathStatistic>> serviceVsReplayPathStatistic = new HashMap<>();
        replayStatisticsMap.getOrDefault(key , new ConcurrentHashMap<>())
                .entrySet().stream().map(Map.Entry::getValue).forEach(pathStatistic -> {
                    serviceVsReplayPathStatistic.computeIfAbsent(pathStatistic.service
                            , service -> new ArrayList<>()).add(pathStatistic);
                });
        reqRespStore.saveReplayResult(serviceVsReplayPathStatistic , replayId);
    }

    public Optional<String> getCurrentReplayId(String customer, String app, String instanceId) {
        return Optional.ofNullable(currentReplayId.get(Objects.hash(customer,app,instanceId)));
    }


    /**
     * Invalidate the keys once the replay is over
     * @param key
     */
    private void invalidateCache(Integer key){
        if (config.getState() == Config.AppState.Mock) return;
        currentReplayId.remove(key);
        replayStatisticsMap.remove(key);
    }

    /**
     * Stop recording stats for the given replay (from ReplayDriver)
     * @param customer
     * @param app
     * @param replayId
     */
    public void stopReplay(String customer, String app, String instanceId, String replayId) {
        if (config.getState() == Config.AppState.Mock) return;
        Integer key = Objects.hash(customer, app, instanceId);
        materializeResults(key , replayId);
        invalidateCache(key);
    }

    /**
     * Start recording stats for the given replay (from ReplayDriver)
     * @param customer
     * @param app
     * @param replayId
     */
    public void startReplay(String customer, String app, String instanceId , String replayId) {
        if (config.getState() == Config.AppState.Mock) return;
        Integer key = Objects.hash(customer , app, instanceId);
        currentReplayId.put(key , replayId);
        replayStatisticsMap.remove(key);
    }

}
