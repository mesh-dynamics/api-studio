package com.cube.cache;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

import redis.clients.jedis.Jedis;

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

    private Pattern topLevelKeyPattern = Pattern.compile("ReplayResultCache\\{cust=(\\S+), app=(\\S+), instance=(\\S+)\\}");
    private Pattern secondaryKeyPattern = Pattern.compile("ReplayResultCache\\{service=(\\S+), path=(\\S+), matchOrNoMatch=(\\S+)\\}");

    public class ReplayPathStatistic {

        @JsonIgnore
        public transient String customer;
        @JsonIgnore
        public transient String app;
        @JsonIgnore
        public transient String service;
        @JsonIgnore
        public transient String instanceId;
        @JsonProperty("path")
        public String path;
        @JsonProperty("totalReq")
        public Integer reqCount = 0;
        @JsonProperty("matchReq")
        public Integer matchCount = 0 ;
        @JsonProperty("noMatchReq")
        public Integer nonMatchCount = 0;

        public ReplayPathStatistic(String customer, String app, String service, String path, String instanceId) {
            this.customer = customer;
            this.app = app;
            this.service = service;
            this.path = path;
            this.instanceId = instanceId;
        }

        public void incrementMatchCount(Integer matchCount){
            this.matchCount += matchCount;
            reqCount += matchCount;
        }


        public void incrementNonMatchCount(Integer nonMatchCount){
            this.nonMatchCount += nonMatchCount;
            reqCount += nonMatchCount;
        }

        public String toString() {
            return MoreObjects.toStringHelper(this).add("cust" , customer).add("app" , app)
                .add("instance" , instanceId).add("service" , service).add("path", path).toString();
        }

    }

    public ReplayResultCache(ReqRespStore reqRespStore, Config config) {
        //replayStatisticsMap = new ConcurrentHashMap<>();
        //currentReplayId = new ConcurrentHashMap<>();
        this.reqRespStore = reqRespStore;
        this.config = config;
    }

    //private ConcurrentHashMap<Integer , ConcurrentHashMap<Integer , ReplayPathStatistic>> replayStatisticsMap;
    //private ConcurrentHashMap<Integer , String> currentReplayId;
    private ReqRespStore reqRespStore;
    private Config config;

    private String topLevelKey(String customer, String app, String instanceId) {
        return MoreObjects.toStringHelper(this).add("cust" , customer).add("app" , app)
            .add("instance" , instanceId).toString();
    }
    private String replayIdKey(String customer, String app, String instanceId) {
        return MoreObjects.toStringHelper("ReplayId").add("cust" , customer).add("app" , app)
            .add("instance" , instanceId).toString();
    }

    private String secondaryKey(String service, String path, boolean matchOrNotMatch) {
        return MoreObjects.toStringHelper(this).add("service" , service).add("path", path)
            .add("matchOrNoMatch" , matchOrNotMatch).toString();
    }




    /*private ReplayPathStatistic getPathStatistic(String customer, String app, String service, String path, String instanceId) {
        Integer topLevelKey = Objects.hash(customer,app,instanceId);
        Integer secondaryKey = Objects.hash(service,path);
        return replayStatisticsMap.computeIfAbsent(topLevelKey , k -> new ConcurrentHashMap<>())
                .computeIfAbsent(secondaryKey , k -> new ReplayPathStatistic(customer,app,service,path,instanceId));
    }*/

    /**
     * Increment the counter for request match (from MockService)
     * @param customer
     * @param app
     * @param service
     * @param path
     */
    public void incrementReqMatchCounter(String customer, String app, String service, String path, String instanceId) {
        if (config.intentResolver.isIntentToMock()) return;
        try (Jedis jedis = config.jedisPool.getResource()) {
            String topKey = topLevelKey(customer, app,instanceId);
            String secondaryKey = secondaryKey(service,path,true);
            jedis.hincrBy(topKey , secondaryKey , 1);
        }
    }

    /**
     * Increment the counter for request no match (from MockService)
     * @param customer
     * @param app
     * @param service
     * @param path
     */
    public void incrementReqNotMatchCounter(String customer, String app, String service, String path, String instanceId){
        if (config.intentResolver.isIntentToMock()) return;
        try (Jedis jedis = config.jedisPool.getResource()) {
            String topKey = topLevelKey(customer , app, instanceId);
            String secondaryKey = secondaryKey(service,path,false);
            jedis.hincrBy(topKey , secondaryKey, 1);
        }
    }

    /**
     * Once the replay is over, store the results in the backend. The results are stored path wise
     * @param key
     * @param replayId
     */
    private void materializeResults(Integer key, String replayId) {
        if (config.intentResolver.isIntentToMock()) return;
        Map<String,List<ReplayPathStatistic>> serviceVsReplayPathStatistic = new HashMap<>();
        try (Jedis jedis = config.jedisPool.getResource()) {
            String replayKey = MoreObjects.toStringHelper(this).add("replay" , replayId).toString();
            Map<String, Map<String, ReplayPathStatistic>> servicePathStatisticMap = new HashMap<>();
            String topLevelKey = jedis.get(replayKey);
            Matcher topLevelKeyMatcher = topLevelKeyPattern.matcher(topLevelKey);
            String customer = topLevelKeyMatcher.group(1);
            String app = topLevelKeyMatcher.group(2);
            String instance = topLevelKeyMatcher.group(3);
            Map<String, String> serviceLevelMap = jedis.hgetAll(topLevelKey);
            serviceLevelMap.forEach((k,v) -> {
                Matcher secondaryKeyMatcher = secondaryKeyPattern.matcher(k);
                String service = secondaryKeyMatcher.group(1);
                String path = secondaryKeyMatcher.group(2);
                boolean matchOrNoMatch = Boolean.valueOf(secondaryKeyMatcher.group(3));
                int count = Integer.valueOf(v);
                ReplayPathStatistic statistic = servicePathStatisticMap.computeIfAbsent(service , s -> new HashMap<>())
                    .computeIfAbsent(path , p -> new ReplayPathStatistic(customer ,app , service, path, instance));
                if (matchOrNoMatch) {
                    statistic.incrementMatchCount(count);
                } else {
                    statistic.incrementNonMatchCount(count);
                }
            });

            servicePathStatisticMap.forEach((service , map) -> {
                List<ReplayPathStatistic> list = new ArrayList<>();
                list.addAll(map.values());
                serviceVsReplayPathStatistic.put(service , list);
            });

            reqRespStore.saveReplayResult(serviceVsReplayPathStatistic , replayId);
            jedis.hdel(topLevelKey);
            jedis.del(replayKey);
        } catch (Exception e) {
            LOGGER.error("Materializing results for replay id :: "
                + replayId + " resulted in exception " + e.getMessage());
        }
    }

    public Optional<String> getCurrentReplayId(String customer, String app, String instanceId) {
        Optional<String> result = Optional.empty();
        try (Jedis jedis = config.jedisPool.getResource()) {
            String relpayKey = replayIdKey(customer, app, instanceId);
            if (jedis.exists(relpayKey)) {
                result = Optional.of(jedis.get(relpayKey));
            }
        } catch (Exception e) {
            LOGGER.error("Error while retieving current replay id from cache :: " + e.getMessage());
        }
        return result;
    }


    /**
     * Invalidate the keys once the replay is over
     * @param key
     */
    private void invalidateCache(Integer key){
        try (Jedis jedis = config.jedisPool.getResource()) {
            jedis.del(String.valueOf(key));
        }
        //currentReplayId.remove(key);
        //replayStatisticsMap.remove(key);
    }


    /**
     * Stop recording stats for the given replay (from ReplayDriver)
     * @param customer
     * @param app
     * @param replayId
     */
    public void stopReplay(String customer, String app, String instanceId, String replayId) {
        Integer key = Objects.hash(customer, app, instanceId);
        invalidateCache(key);
        if (config.intentResolver.isIntentToMock()) return;
        materializeResults(key , replayId);
    }

    /**
     * Start recording stats for the given replay (from ReplayDriver)
     * @param customer
     * @param app
     * @param replayId
     */
    public void startReplay(String customer, String app, String instanceId , String replayId) {
        if (config.intentResolver.isIntentToMock()) return;
        String topLevelKey = topLevelKey(customer , app , instanceId);
        String replayKey = MoreObjects.toStringHelper(this).add("replay" , replayId).toString();
        String replayIdKey = replayIdKey(customer, app, instanceId);
        try (Jedis jedis = config.jedisPool.getResource()) {
            jedis.set(replayKey , topLevelKey);
            jedis.set(replayIdKey , replayId);
        }
    }

}
