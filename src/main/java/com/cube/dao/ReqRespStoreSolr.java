/**
 * Copyright Cube I O
 */
package com.cube.dao;

import static io.md.core.TemplateKey.*;

import io.md.constants.ReplayStatus;
import io.md.core.BatchingIterator;
import io.md.core.ConfigApplicationAcknowledge;
import io.md.core.TemplateKey;
import io.md.core.ValidateAgentStore;
import io.md.core.ValidateProtoDescriptorDAO;
import io.md.dao.ProtoDescriptorDAO;
import io.md.dao.*;
import io.md.dao.agent.config.AgentConfigTagInfo;
import io.md.dao.agent.config.ConfigDAO;
import io.md.dao.agent.config.ConfigType;
import io.md.dao.Recording.RecordingStatus;
import io.md.dao.Recording.RecordingType;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.Pair;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Descriptors.DescriptorValidationException;

import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import io.md.core.AttributeRuleMap;
import io.md.core.Comparator;
import io.md.core.Comparator.Diff;
import io.md.core.Comparator.Match;
import io.md.core.Comparator.Resolution;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.ReplayTypeEnum;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.services.FnResponse;
import io.md.utils.FnKey;
import io.md.injection.DynamicInjectionConfig;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.core.Utils;
import io.md.utils.Constants;

import redis.clients.jedis.Jedis;

import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateCache;
import com.cube.cache.TemplateCacheRedis;
import com.cube.cache.TemplateCacheWithoutCaching;
import com.cube.core.CompareTemplateVersioned;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;

import static io.md.constants.Constants.SCORE_FIELD;

/**
 * @author prasad
 *
 */
public class ReqRespStoreSolr extends ReqRespStoreImplBase implements ReqRespStore {

    private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreSolr.class);

/*
    @Override
    public void invalidateCacheFromTemplateSet(TemplateSet templateSet)
    {
        templateSet.templates.stream().forEach(compareTemplateVersioned -> {
            TemplateKey key =
                new TemplateKey(templateSet.version, templateSet.customer, templateSet.app,
                    compareTemplateVersioned.service,
                    compareTemplateVersioned.prefixpath, compareTemplateVersioned.type);
            comparatorCache.invalidateKey(key);
        });
    }

*/

    @Override
    public void invalidateCache() {
        comparatorCache.invalidateAll();
    }

    @Override
    public boolean save(Event event) {

        SolrInputDocument doc = eventToSolrDoc(event);
        return saveDoc(doc);
    }


    @Override
    void removeCollectionKey(ReqRespStoreImplBase.CollectionKey collectionKey) {
        if (config.intentResolver.isIntentToMock()) return;
        try (Jedis jedis = config.jedisPool.getResource()) {
            //jedis.del(collectionKey.toString());
            //Long result = jedis.expire(collectionKey.toString(), Config.REDIS_DELETE_TTL);
            if (jedis.exists(collectionKey.toString())) {
                String shadowKey = Constants.REDIS_SHADOW_KEY_PREFIX + collectionKey.toString();
                Long result = jedis.expire(shadowKey, com.cube.ws.Config.REDIS_DELETE_TTL);
                LOGGER.info(String.format("Expiring redis key \"%s\" in %d seconds"
                        , shadowKey, com.cube.ws.Config.REDIS_DELETE_TTL));
            }
        } catch (Exception e) {
            LOGGER.error("Unable to remove key from redis cache :: "+ e.getMessage());
        }
    }

    @Override
    void updaterFinalReplayStatusInCache(Replay replay) {
        try (Jedis jedis = config.jedisPool.getResource()) {
            CollectionKey cKey = new CollectionKey(replay.customerId
                , replay.app, replay.instanceId);
            String statusKey = Constants.REDIS_STATUS_KEY_PREFIX + cKey.toString();
            String result = jedis.set(statusKey, replay.status.toString());
            LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE,
                "Successfully set replay status for status key", Constants.REPLAY_ID_FIELD,
                replay.replayId, Constants.STATUS, replay.status.toString())));
        } catch (Exception e) {
            LOGGER.error("Error while updating replay status for status key", e);
        }
    }

    private FnKey recordReplayRetrieveKey;
    private FnKey recordReplayStoreKey;

    @Override
    Optional<RecordOrReplay> retrieveFromCache(CollectionKey key, boolean extendTTL) {
        Optional<RecordOrReplay> toReturn = Optional.empty();
        String runId = Instant.now().toString();
        if (recordReplayRetrieveKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            recordReplayRetrieveKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(recordReplayRetrieveKey, Optional.empty(), Optional.empty(),runId,  key);
            if (ret.retStatus == RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            return (Optional<RecordOrReplay>) ret.retVal;
        }

        try (Jedis jedis = config.jedisPool.getResource()) {
            String keyStr = key.toString();
            String fromCache = jedis.get(keyStr);
            if (fromCache != null) {
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Successfully retrieved from redis",  "key" ,  keyStr)));
                toReturn = Optional.of(config.jsonMapper.readValue(fromCache, RecordOrReplay.class));
                String shadowKey = Constants.REDIS_SHADOW_KEY_PREFIX + keyStr;
                Long ttl = jedis.ttl(shadowKey);
                if (ttl != -1 && extendTTL) {
                    jedis.expire(shadowKey, config.REDIS_DELETE_TTL);
                    LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE,
                        "Extending ttl in redis","key" , shadowKey,"duration"
                        , String.valueOf(config.REDIS_DELETE_TTL))));
                }
            }
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(recordReplayRetrieveKey, toReturn,
                    RetStatus.Success, Optional.empty(), runId, key);
            }
        } catch (Exception e) {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(recordReplayRetrieveKey
                    , e, RetStatus.Exception,
                    Optional.of(e.getClass().getName()), runId, key);
            }
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                "Error while retrieving Record/Replay from cache")) , e);
        }
        return toReturn;
    }

    @Override
    public void populateCache(CollectionKey collectionKey, RecordOrReplay rr) {
        String runId = Instant.now().toString();
        if (recordReplayStoreKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            recordReplayStoreKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(recordReplayStoreKey, Optional.empty(), Optional.empty(), runId, collectionKey , rr);
            if (ret != null && ret.retStatus == RetStatus.Exception) {
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Throwing exception as a result of mocking function")));
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            // do nothing -- return type is void
            return;
        }

        try (Jedis jedis = config.jedisPool.getResource()) {
            String toString = config.jsonMapper.writeValueAsString(rr);
            jedis.set(collectionKey.toString() , toString);
            jedis.set(Constants.REDIS_SHADOW_KEY_PREFIX + collectionKey.toString(), "");
            LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Successfully stored in redis"
                , "key" , collectionKey.toString())));
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                "Error while population RecordOrReplay in cache")) , e);
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(recordReplayStoreKey, e, RetStatus.Exception,
                    Optional.of(e.getClass().getName()), runId, collectionKey , rr);
            }
        }
    }

    @Override
    public boolean forceDeleteInCache(Recording recording) {
        CollectionKey key = new CollectionKey(recording.customerId, recording.app, recording.instanceId);
        try (Jedis jedis = config.jedisPool.getResource()) {
            jedis.del(key.toString());
            jedis.del(Constants.REDIS_SHADOW_KEY_PREFIX + key.toString());
        }
        return true;
    }

    @Override
    public boolean forceDeleteInCache(Replay replay) {
        CollectionKey key = new CollectionKey(replay.customerId, replay.app, replay.instanceId);
        try (Jedis jedis = config.jedisPool.getResource()) {
            jedis.del(key.toString());
            jedis.del(Constants.REDIS_SHADOW_KEY_PREFIX + key.toString());
            jedis.del(Constants.REDIS_STATUS_KEY_PREFIX + key.toString());
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRequests(java.lang.String, java.lang.String, java.lang.String, java.lang.Iterable, com.cube.dao.ReqRespStore.RR, com.cube.dao.ReqRespStore.Types)
     */
    @Override
    public Result<Event> getRequests(String customerId, String app, String collection,
                                       List<String> reqids, List<String> services, List<String> paths, Optional<Event.RunType> runType) {

        // TODO: Event redesign - change this include all event types
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, Event.EventType.HTTPRequest);
        builder.withCollection(collection)
            .withReqIds(reqids)
            .withPaths(paths)
            .withServices(services);

        runType.ifPresent(builder::withRunType);

        return getEvents(builder.build());
    }

    @Override
    public Optional<Event> getSingleResponseEvent(String customerId, String app, String collection,
        List<String> services, List<String> paths, Optional<Event.RunType> runType) {

        // TODO: Event redesign - change this include all event types
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, Event.RESPONSE_EVENT_TYPES);
        builder.withCollection(collection)
            .withPaths(paths)
            .withServices(services)
            .withLimit(1);

            runType.ifPresent(builder::withRunType);

        return getSingleEvent(builder.build());
    }


    @Override
    public Comparator getComparator(TemplateKey key, Optional<EventType> eventType) throws TemplateNotFoundException {
        return comparatorCache.getComparator(key, eventType);
    }

    @Override
    public Result<Event> getEvents(EventQuery eventQuery) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        query.addField("score");

        StringBuffer queryBuff = new StringBuffer();

        addFilter(query, TYPEF, Types.Event.toString());
        addFilter(query, CUSTOMERIDF, eventQuery.getCustomerId());
        addFilter(query, APPF, eventQuery.getApp());

        addToFilterOrQuery(query , queryBuff , SERVICEF , eventQuery.getServices() , true , eventQuery.getServicesWeight());

        if(eventQuery.getCollection().orElse("").equalsIgnoreCase("NA")){
            LOGGER.info(String.format("Solr getEvents Applying the recodingType weightage for NA collection %s  %s" , eventQuery.getCustomerId() , eventQuery.getApp() ) );
            recordingTypeWeights.forEach((type , weight)->{
                addToQryStr(queryBuff , RECORDING_TYPE_F , type.toString() , true, Optional.of(weight) );
            });
        }else{
            addToFilterOrQuery(query , queryBuff , COLLECTIONF , eventQuery.getCollection() , true , eventQuery.getCollectionWeight());
        }

        List<String> traceIds = eventQuery.getTraceIds();
        List<String> filteredTraceIds = traceIds.stream().filter(traceid->!traceid.equalsIgnoreCase("NA")).collect(Collectors.toList());
        if(traceIds.size()!=filteredTraceIds.size()){
            LOGGER.info("Filtered NA traceIds from "+traceIds.size()+" to "+filteredTraceIds.size());
        }
        addToFilterOrQuery(query , queryBuff , TRACEIDF , filteredTraceIds , true , eventQuery.getTraceIdsWeight());

        if(eventQuery.isFromMocker()){
            addNegativeFilter(query ,  RRTYPEF , RunType.Mock.toString()  , false);
        }else{
            addToFilterOrQuery(query , queryBuff , RRTYPEF , eventQuery.getRunType().map(Object::toString) , true , eventQuery.getRunTypeWeight());
        }

        addToFilterOrQuery(query , queryBuff , REQIDF , eventQuery.getReqIds(), true , eventQuery.getReqIdsWeight());

        addToFilterOrQuery(query , queryBuff , PATHF , eventQuery.getPaths(), eventQuery.excludePaths() , true , eventQuery.getPathsWeight());

        addFilter(query, EVENTTYPEF, eventQuery.getEventTypes().stream().map(type -> type.toString()).collect(Collectors.toList()));

        addToFilterIntOrQuery(query , queryBuff , PAYLOADKEYF , eventQuery.getPayloadKey(), true , eventQuery.getPayloadKeyWeight());

        // starting from timestamp, non inclusive
        addRangeFilter(query, TIMESTAMPF, eventQuery.getTimestamp(), Optional.empty(), false, true);

        addSort(query , SCOREF , false);
        addSort(query, TIMESTAMPF, eventQuery.isSortOrderAsc());
        addSort(query, IDF, true);

        if(queryBuff.length()!=0){
            query.setQuery(queryBuff.toString());
        }

        return SolrIterator.getResults(solr, query, eventQuery.getLimit(),
            this::docToEvent, eventQuery.getOffset());
    }


    @Override
    public Stream<ReqRespMatchResult> expandOnTrace(ReqRespMatchResult reqRespMatchResult, boolean recordOrReplay) {
        String replayId = reqRespMatchResult.replayId;
        Optional<String> traceId = (recordOrReplay) ? reqRespMatchResult.recordTraceId : reqRespMatchResult.replayTraceId;

        return traceId.map(trace -> {
            SolrQuery reqRespMatchResultQuery = new SolrQuery("*:*");
            addFilter(reqRespMatchResultQuery, TYPEF, Types.ReqRespMatchResult.toString());
            addFilter(reqRespMatchResultQuery, REPLAYIDF, replayId);
            addFilter(reqRespMatchResultQuery, (recordOrReplay)? RECORDTRACEIDF : REPLAYTRACEIDF , trace);
            return SolrIterator.getStream(solr, reqRespMatchResultQuery,
                Optional.empty()).flatMap(doc -> docToAnalysisMatchResult(doc).stream());

        }).orElse(Stream.of(reqRespMatchResult));
    }

    private static final String CPREFIX = "";
    private static final String CSUFFIX = "";
    private static final String STRINGSET_SUFFIX = "_ss"; // set of strings in Solr
    private static final String STRING_SUFFIX = "_s";
    private static final String INT_SUFFIX = "_i";
    private static final String TEXT_SUFFIX = "_t";
    private static final String DATE_SUFFIX = "_dt";
    private static final String BOOLEAN_SUFFIX = "_b";
    private static final String DOUBLE_SUFFIX = "_d";
    private static final String NOTINDEXED_SUFFIX = "_ni";
    private static final String BIN_SUFFIX = "_bin"; // for binary data
    private static final String FUNC_PREFIX  = "func_";
    private static final String FUNC_NAME = CPREFIX + FUNC_PREFIX + "name"  + STRING_SUFFIX;
    private static final String FUNC_SIG_HASH = CPREFIX + FUNC_PREFIX + "sighash" + INT_SUFFIX;
    private static final String FUNC_ARG_HASH_PREFIX = CPREFIX + FUNC_PREFIX + "arghash_";
    private static final String FUNC_ARG_VAL_PREFIX = CPREFIX + FUNC_PREFIX + "argval_";
    private static final String FUNC_RET_VAL = CPREFIX + FUNC_PREFIX + "retval" + NOTINDEXED_SUFFIX;
    private static final String FUNC_RET_STATUSF = CPREFIX + FUNC_PREFIX + "funcstatus"  + STRING_SUFFIX;
    private static final String FUNC_EXCEPTION_TYPEF = CPREFIX + FUNC_PREFIX + "exceptiontype"  + STRING_SUFFIX;
    private static final String DEFAULT_EMPTY_FIELD_VALUE = "null";
    private static final String AGENT_ID_F = CPREFIX + Constants.AGENT_ID + STRING_SUFFIX;
    private static final String INFO_PREFIX = CPREFIX + Constants.INFO + "_";
    private static final String EVENT_META_DATA_PREFIX = CPREFIX + Constants.EVENT_META_DATA + "_";
    private static final String EVENT_META_DATA_KEYSF = CPREFIX + Constants.EVENT_META_DATA_KEY_FIELD + STRINGSET_SUFFIX;
    private static final String CONFIG_ACK_DATA_KEYSF = CPREFIX + Constants.CONFIG_ACK_DATA_KEY_FIELD + STRINGSET_SUFFIX;

    private static final Map<RecordingType, Float> recordingTypeWeights = Map.of(
        RecordingType.Golden, 3.0f,
        RecordingType.UserGolden, 2.5f,
        RecordingType.Capture, 2.0f,
        RecordingType.Replay, 1.5f,
        RecordingType.History, 1.0f);


    private SolrInputDocument funcReqResponseToSolrDoc(FnReqResponse fnReqResponse, String collection) {
        SolrInputDocument solrDocument = new SolrInputDocument();
        solrDocument.setField(TYPEF, Types.FuncReqResp.toString());
        solrDocument.setField(TIMESTAMPF , Instant.now().toString());
        solrDocument.setField(CUSTOMERIDF , fnReqResponse.customerId);
        solrDocument.setField(APPF , fnReqResponse.app);
        solrDocument.setField(INSTANCEIDF, fnReqResponse.instanceId);
        solrDocument.setField(SERVICEF, fnReqResponse.service);
        solrDocument.setField(COLLECTIONF, collection);
        fnReqResponse.traceId.ifPresent(trace -> solrDocument.setField(HDRTRACEF , trace));
        fnReqResponse.spanId.ifPresent(span -> solrDocument.setField(HDRSPANF , span));
        fnReqResponse.parentSpanId.ifPresent(parentSpanId -> solrDocument.setField(HDRPARENTSPANF, parentSpanId));
        solrDocument.setField(FUNC_NAME , fnReqResponse.name);
        solrDocument.setField(FUNC_SIG_HASH, fnReqResponse.fnSignatureHash);
        var counter = new Object(){int x = 0;};
        Arrays.asList(fnReqResponse.argVals).forEach(argVal -> solrDocument.setField(FUNC_ARG_VAL_PREFIX
            + ++counter.x + TEXT_SUFFIX, argVal));
        counter.x = 0;
        Arrays.asList(fnReqResponse.argsHash).forEach(argHash -> solrDocument.setField(FUNC_ARG_HASH_PREFIX
            + ++counter.x + INT_SUFFIX, argHash));
        solrDocument.setField(FUNC_RET_VAL, fnReqResponse.retOrExceptionVal);
        fnReqResponse.respTS.ifPresent(timestamp -> solrDocument.setField(TIMESTAMPF , timestamp.toString()));
        solrDocument.setField(FUNC_RET_STATUSF, fnReqResponse.retStatus.toString());
        fnReqResponse.exceptionType.ifPresent(etype -> solrDocument.setField(FUNC_EXCEPTION_TYPEF, etype));
        return solrDocument;
    }

    @Override
    public boolean storeFunctionReqResp(FnReqResponse funcReqResponse, String collection) {
        SolrInputDocument doc = funcReqResponseToSolrDoc(funcReqResponse, collection);
        return saveDoc(doc);
    }

    private Optional<FnResponse> solrDocToFnResponse(SolrDocument doc, boolean multipleResults) {
        RetStatus retStatus =
            getStrField(doc, FUNC_RET_STATUSF).flatMap(rs -> Utils.valueOf(RetStatus.class,
            rs)).orElse(RetStatus.Success);
        Optional<String> exceptionType = getStrField(doc, FUNC_EXCEPTION_TYPEF);
        return getStrFieldMVFirst(doc,FUNC_RET_VAL).map(retVal -> new FnResponse(retVal ,getTSField(doc,TIMESTAMPF),
            retStatus, exceptionType, multipleResults));
    }

    @Override
    public Optional<FnResponse> getFunctionReturnValue(FnReqResponse funcReqResponse, String collection) {
        StringBuilder argsQuery = new StringBuilder();
        argsQuery.append("*:*");
        var counter = new Object(){int x =0;};
        //funcReqResponse.traceId.ifPresent(trace ->
          //  argsQuery.append(" OR ").append("(").append(HDRTRACEF).append(":").append(trace).append(")^2"));
        funcReqResponse.respTS.ifPresent(timestamp -> argsQuery.
            append(" OR ").append(TIMESTAMPF).append(":{").append(timestamp.toString()).append(" TO *]"));
        SolrQuery query = new SolrQuery(argsQuery.toString());
        query.setFields("*");
        addFilter(query, TYPEF, Types.FuncReqResp.toString());
        addFilter(query, FUNC_SIG_HASH, funcReqResponse.fnSignatureHash);
        addFilter(query, COLLECTIONF, collection);
        addFilter(query, SERVICEF, funcReqResponse.service);
        addSort(query, TIMESTAMPF, true);
        addSort(query, IDF, true);
        funcReqResponse.traceId.ifPresent(trace -> addFilter(query, HDRTRACEF, trace));
        Arrays.asList(funcReqResponse.argsHash).forEach(argHashVal ->
            addFilter(query, FUNC_ARG_HASH_PREFIX + ++counter.x + INT_SUFFIX, argHashVal));
        Optional<Integer> maxResults = Optional.of(1);
        Result<SolrDocument> solrDocumentResult = SolrIterator.getResults(solr, query, maxResults, x-> Optional.of(x));
        return solrDocumentResult.getObjects().findFirst().flatMap(doc -> solrDocToFnResponse(doc,solrDocumentResult.numFound>1));
    }

    @Override
    public String createTemplateUpdateOperationSet(String customer, String app, String sourceTemplateSetVersion) throws Exception {
        String templateUpdateOperationSetId = Types.TemplateUpdateOperationSet.toString().concat("-").concat(
            UUID.randomUUID().toString());
        TemplateUpdateOperationSet templateSetUpdate = new TemplateUpdateOperationSet(templateUpdateOperationSetId,
            new HashMap<>());
        SolrInputDocument inputDoc = templateUpdateOperationSetToSolrDoc(templateSetUpdate, customer);
        saveDoc(inputDoc);
        softcommit();
        return templateUpdateOperationSetId;
    }

    private static final String OPERATION = CPREFIX + "operation" + STRING_SUFFIX;
    private static final String TEMPLATE_KEY = CPREFIX + "template_key" + STRINGSET_SUFFIX;

    private SolrInputDocument templateUpdateOperationSetToSolrDoc(TemplateUpdateOperationSet operationSet, String customerId) throws Exception {
        SolrInputDocument inputDoc = new SolrInputDocument();
        inputDoc.setField(TYPEF, Types.TemplateUpdateOperationSet.toString());
        inputDoc.setField(IDF,  operationSet.getTemplateUpdateOperationSetId());
        inputDoc.setField(CUSTOMERIDF, customerId);
        if (operationSet.getTemplateUpdates() != null && !operationSet.getTemplateUpdates().isEmpty()) {
            inputDoc.setField(OPERATION, config.jsonMapper.writeValueAsString(operationSet.getTemplateUpdates()));
        }
        return inputDoc;
    }

    @Override
    public boolean saveTemplateUpdateOperationSet(TemplateUpdateOperationSet templateUpdateOperationSet, String customerId) throws Exception {
        return saveDoc(templateUpdateOperationSetToSolrDoc(templateUpdateOperationSet, customerId)) && softcommit();
    }

    private TypeReference<HashMap<TemplateKey, SingleTemplateUpdateOperation>> typeReference =
        new TypeReference<>() {};

    private Optional<TemplateUpdateOperationSet> solrDocToTemplateUpdateOperationSet(SolrDocument solrDocument) {
        Optional<String> id = getStrField(solrDocument , IDF);
        Optional<String> operationsAsString = getStrField(solrDocument, OPERATION);
        Map<TemplateKey, SingleTemplateUpdateOperation> updateMap  = (Map<TemplateKey, SingleTemplateUpdateOperation>)
            operationsAsString.flatMap(operations -> {
            try {
                return Optional.of(config.jsonMapper.readValue(operations , typeReference));
                // note that id will always be present as the filter query is on the id field
            } catch (Exception e) {
                LOGGER.error("Unable to deserialize template set update operations :: " + e.getMessage());
                return Optional.empty();
            }
        }).orElse(new HashMap<>());
        return Optional.of(new TemplateUpdateOperationSet(id.get(), updateMap));
    }

    @Override
    public Optional<TemplateUpdateOperationSet> getTemplateUpdateOperationSet(String templateUpdateOperationSetId) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        addFilter(solrQuery, TYPEF , Types.TemplateUpdateOperationSet.toString());
        addFilter(solrQuery, IDF, templateUpdateOperationSetId);
        return SolrIterator.getStream(solr, solrQuery, Optional.of(1)).findFirst().flatMap(this::solrDocToTemplateUpdateOperationSet);
    }

    @Override
    public boolean storeRecordingOperationSetMeta(RecordingOperationSetMeta recordingOperationSetMeta){
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.setField(IDF, recordingOperationSetMeta.id);
        solrDoc.setField(TYPEF, Types.RecordingOperationSetMeta.toString());
        solrDoc.setField(CUSTOMERIDF,recordingOperationSetMeta.customer);
        solrDoc.setField(APPF, recordingOperationSetMeta.app);
        return saveDoc(solrDoc) && softcommit();
    }

    // get recordingOperationSet for a given operationset id, service and path
    @Override
    public Optional<RecordingOperationSetMeta> getRecordingOperationSetMeta(String recordingOperationSetId) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF, Types.RecordingOperationSetMeta.toString());
        addFilter(query, IDF, recordingOperationSetId);
        Optional<Integer> maxresults = Optional.of(1);
        Stream<SolrDocument> stream = SolrIterator.getStream(solr, query, maxresults);
        return stream.findFirst().flatMap(this::docToRecordingOperationSetMeta);
    }

    private Optional<RecordingOperationSetMeta> docToRecordingOperationSetMeta(SolrDocument doc) {
        Optional<String> id = getStrField(doc, IDF);
        Optional<String> customer = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        if(id.isPresent() && customer.isPresent() && app.isPresent()) {
            return Optional.of(new RecordingOperationSetMeta(id.get(), customer.get(), app.get()));
        } else {
            LOGGER.error("unable to convert Solr doc to RecordingOperationSetMeta");
            return Optional.empty();
        }
    }

    @Override
    public boolean storeRecordingOperationSet(RecordingOperationSetSP recordingOperationSetSP) {
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.setField(IDF, recordingOperationSetSP.id);
        solrDoc.setField(TYPEF, Types.RecordingOperationSet.toString());
        solrDoc.setField(OPERATIONSETIDF, recordingOperationSetSP.operationSetId);
        solrDoc.setField(CUSTOMERIDF, recordingOperationSetSP.customer);
        solrDoc.setField(APPF, recordingOperationSetSP.app);
        solrDoc.setField(SERVICEF, recordingOperationSetSP.service);
        solrDoc.setField(PATHF, recordingOperationSetSP.path);
        recordingOperationSetSP.operationsList.forEach(op -> {
            // convert each operation object into JSON before storing
            String opStr = null;
            try {
                opStr = config.jsonMapper.writeValueAsString(op);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            solrDoc.addField(OPERATIONLIST, opStr);
        });
        return saveDoc(solrDoc) && softcommit();
    }

    // get recordingOperationSet for a given operationset id, service and path
    @Override
    public Optional<RecordingOperationSetSP> getRecordingOperationSetSP(String recordingOperationSetId, String service,
                                                                        String path) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF, Types.RecordingOperationSet.toString());
        addFilter(query, OPERATIONSETIDF, recordingOperationSetId);
        addFilter(query, SERVICEF, service);
        addFilter(query, PATHF, path);
        Optional<Integer> maxresults = Optional.of(1);
        Stream<SolrDocument> stream = SolrIterator.getStream(solr, query, maxresults);
        return stream.findFirst().flatMap(this::docToRecordingOperationSetSP);
    }

    // get all recordingOperationSets for a given operationset id
    @Override
    public Stream<RecordingOperationSetSP> getRecordingOperationSetSPs(String recordingOperationSetId) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF, Types.RecordingOperationSet.toString());
        addFilter(query, OPERATIONSETIDF, recordingOperationSetId);
        Optional<Integer> maxresults = Optional.empty();
        Stream<SolrDocument> stream = SolrIterator.getStream(solr, query, maxresults);
        return stream.map(this::docToRecordingOperationSetSP).flatMap(Optional::stream);
    }


    private Optional<RecordingOperationSetSP> docToRecordingOperationSetSP(SolrDocument doc) {
        Optional<String> id = getStrField(doc, IDF);
        Optional<String> operationSetId = getStrField(doc, OPERATIONSETIDF);
        Optional<String> customer = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> path = getStrField(doc, PATHF);
        List<ReqRespUpdateOperation> operationList = getOperationList(doc);
        if (operationSetId.isEmpty()) {
            LOGGER.error("RecordingOperationSetSP not found with given operationSet id");
            return Optional.empty();
        }
        if(id.isPresent() && customer.isPresent() && app.isPresent() && service.isPresent() && path.isPresent()) {
            return Optional.of(
                new RecordingOperationSetSP(id.get(), operationSetId.get(),
                    customer.get(), app.get(), service.get(), path.get(), operationList));
        } else {
            LOGGER.error("unable to convert Solr doc to RecordingOperationSetSP");
            return Optional.empty();
        }
    }

    private List<ReqRespUpdateOperation> getOperationList(SolrDocument doc) {
        List<String> operationStrList = getStrFieldMV(doc, OPERATIONLIST);
        return operationStrList.stream()
            .map(op -> {
                ReqRespUpdateOperation reqRespUpdateOperation = null;
                try {
                    reqRespUpdateOperation = config.jsonMapper.readValue(op,
                        ReqRespUpdateOperation.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return reqRespUpdateOperation;
            })
            .collect(Collectors.toList());
    }

    @Override
    public String saveTemplateSet(TemplateSet templateSet) throws Exception {
        List<String> templateIds = new ArrayList<>();
        templateSet.templates.forEach(UtilException.rethrowConsumer(template -> {
            TemplateKey templateKey = new TemplateKey(templateSet.version, templateSet.customer,
                templateSet.app, template.service, template.requestPath, template.type, template.method, DEFAULT_RECORDING);
                templateIds.add(saveCompareTemplate(templateKey, config.jsonMapper.writeValueAsString(template)));
        }));
        Optional<String> ruleMapId = templateSet.appAttributeRuleMap.map(ruleMap ->
            {
                try {
                    String ruleMapJson = this.config.jsonMapper.writeValueAsString(ruleMap);
                    return saveAttributeRuleMap(new
                        TemplateKey(templateSet.version, templateSet.customer, templateSet.app,
                        io.md.constants.Constants.NOT_APPLICABLE, io.md.constants.Constants.NOT_APPLICABLE, Type.DontCare), ruleMapJson);
                } catch (Exception e) {
                    LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                        "Unable to convert rule map to string")), e);
                    return null;
                }
            });
        return storeTemplateSetMetadata(templateSet, templateIds, ruleMapId);
    }

    public SolrInputDocument agentConfigTagInfoToDoc(AgentConfigTagInfo tagInfo) {
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.setField(CUSTOMERIDF, tagInfo.customerId);
        solrInputDocument.setField(APPF, tagInfo.app);
        solrInputDocument.setField(SERVICEF, tagInfo.service);
        solrInputDocument.setField(INSTANCEIDF, tagInfo.instanceId);
        solrInputDocument.setField(TYPEF, Types.AgentConfigTagInfo.toString());
        solrInputDocument.setField(TAG_F, tagInfo.tag);
        solrInputDocument.setField(IDF, Types.AgentConfigTagInfo.toString() + "-"+  Objects.hash(
            tagInfo.customerId, tagInfo.app, tagInfo.service, tagInfo.instanceId));
        return solrInputDocument;
    }

    public Optional<AgentConfigTagInfo> docToAgentConfigTagInfo(SolrDocument doc) {
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> instanceId = getStrField(doc, INSTANCEIDF);
        Optional<String> tag = getStrField(doc, TAG_F);
        Optional<AgentConfigTagInfo> agentConfigTagInfo = Optional.empty();
        if (customerId.isPresent() && app.isPresent() &&
            service.isPresent() && instanceId.isPresent() && tag.isPresent()) {
            AgentConfigTagInfo agentConfig = new AgentConfigTagInfo(customerId.get(), app.get(),
                service.get(), instanceId.get(), tag.get());
            agentConfigTagInfo = Optional.of(agentConfig);
        }
        return agentConfigTagInfo;
    }

    @Override
    public boolean updateAgentConfigTag(AgentConfigTagInfo tagInfo) {
        return saveDoc(agentConfigTagInfoToDoc(tagInfo)) && softcommit();
    }

    @Override
    public Result<AgentConfigTagInfo> getAgentConfigTagInfoResults(String customerId, String app,
        Optional<String> service, String instanceId) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF , Types.AgentConfigTagInfo.toString());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, SERVICEF, service);
        addFilter(query, INSTANCEIDF, instanceId);
        return SolrIterator.getResults(solr, query, Optional.empty(),
            this::docToAgentConfigTagInfo, Optional.empty());
    }

    @Override
    public boolean storeAgentConfig(ConfigDAO store) {

        SolrQuery maxVersionQuery = new SolrQuery("*:*");
        addFilter(maxVersionQuery, TYPEF, Types.AgentConfig.toString());
        addFilter(maxVersionQuery, CUSTOMERIDF,  store.customerId);
        addFilter(maxVersionQuery, APPF, store.app);
        addFilter(maxVersionQuery, SERVICEF, store.service);
        addFilter(maxVersionQuery,INSTANCEIDF, store.instanceId);
        addFilter(maxVersionQuery,TAG_F, store.tag);
        addSort(maxVersionQuery, INT_VERSION_F, false);
        Optional<SolrDocument> currentDoc = SolrIterator.getSingleResult(solr, maxVersionQuery);
        currentDoc.ifPresent(this::updateAgentConfigDoc);

        int maxVersion = currentDoc.flatMap(this::extractVersionFromDoc).orElse(0);
        store.setVersion(maxVersion+1);
        store.setIsLatest(true);
        SolrInputDocument doc = agentToSolrDoc(store);
        return saveDoc(doc) && softcommit();
    }

    private boolean updateAgentConfigDoc(SolrDocument entry) {
        entry.setField(ISLATESTF, false);
        entry.remove("_version_");
        SolrInputDocument doc = new SolrInputDocument();
        entry.forEach((key, val) -> doc.setField(key, val));
        return saveDoc(doc);
    }

    private  Optional<Integer> extractVersionFromDoc(SolrDocument entry) {
        return getIntField(entry, INT_VERSION_F);
    }

    private Optional<String> extractTagFromDoc(SolrDocument entry) {
        return getStrField(entry, TAG_F);
    }

    private SolrQuery getAgentConfigQuery(String customerId, String app, Optional<String> service,
        Optional<String> instanceId, Optional<String> tag) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF, ConfigType.AgentConfig.toString());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, SERVICEF, service);
        addFilter(query, INSTANCEIDF, instanceId);
        addFilter(query, TAG_F, tag);
        return query;
    }

    @Override
    public Optional<ConfigDAO> getAgentConfig(String customerId, String app,
            String service, String instanceId) {
        // first get current tag
        SolrQuery getCurrentTagQuery = new SolrQuery("*:*");
        addFilter(getCurrentTagQuery, TYPEF , Types.AgentConfigTagInfo.toString());
        addFilter(getCurrentTagQuery, CUSTOMERIDF, customerId);
        addFilter(getCurrentTagQuery, APPF, app);
        addFilter(getCurrentTagQuery, SERVICEF, service);
        addFilter(getCurrentTagQuery, INSTANCEIDF, instanceId);
        String currentTag = SolrIterator.getSingleResult(solr, getCurrentTagQuery)
            .flatMap(this::extractTagFromDoc).orElse("NA");

        // find the latest config the current tag
        SolrQuery query = getAgentConfigQuery(customerId, app, Optional.of(service), Optional.of(instanceId), Optional.empty());
        addFilter(query,TAG_F, currentTag);
        addSort(query, INT_VERSION_F, false);
        return SolrIterator.getSingleResult(solr, query).flatMap(this::docToAgent);
    }

    @Override
    public Pair<List, Stream<ConfigDAO>> getAgentConfigWithFacets(String customerId, String app, Optional<String> service,
        Optional<String> instanceId, Optional<Integer> numOfResults, Optional<Integer> start, Optional<String> tag) {
        SolrQuery query = getAgentConfigQuery(customerId, app, service, instanceId, tag);
        addFilter(query, ISLATESTF, true);
        FacetQ instanceIdFacetq = new FacetQ();
        Facet instanceIdf = Facet.createTermFacet(INSTANCEIDF, Optional.empty());

        FacetQ tagFacetq = new FacetQ();
        Facet tagf = Facet.createTermFacet(TAG_F, Optional.empty());

        FacetQ serviceFacetq = new FacetQ();
        Facet servicef = Facet.createTermFacet(SERVICEF, Optional.empty());
        serviceFacetq.addFacet(SERVICEFACET, servicef);

        tagf.addSubFacet(serviceFacetq);
        tagFacetq.addFacet(TAGFACET, tagf);


        instanceIdf.addSubFacet(tagFacetq);
        instanceIdFacetq.addFacet(INSTANCEFACET, instanceIdf);
        query.setFacetMinCount(1);
        String jsonFacets;
        try {
            jsonFacets = config.jsonMapper.writeValueAsString(instanceIdFacetq);
            query.add(SOLRJSONFACETPARAM, jsonFacets);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting facets to json"), e);
        }
        Result<ConfigDAO> result =  SolrIterator.getResults(solr, query, numOfResults,
            this::docToAgent, start);
        ArrayList instanceFacetResults = result.getFacets(FACETSFIELD, INSTANCEFACET, BUCKETFIELD);
        instanceFacetResults.forEach(instanceFacetResult -> {
            HashMap tagFacetMap = (HashMap) ((HashMap) instanceFacetResult).get(TAGFACET);
            ArrayList tagFacetResults = result.solrNamedPairToMap((ArrayList) tagFacetMap.get(BUCKETFIELD));
            tagFacetResults.forEach(tagFacetResult -> {
                HashMap serviceFacetMap = (HashMap) ((HashMap) tagFacetResult).get(SERVICEFACET);
                ((HashMap)tagFacetResult).put(SERVICEFACET,
                    result.solrNamedPairToMap((ArrayList)serviceFacetMap.get(BUCKETFIELD)));
            });
            ((HashMap)instanceFacetResult).put(TAGFACET,tagFacetResults);
        });

        return new Pair(instanceFacetResults, result.getObjects());
    }

    private SolrInputDocument agentToSolrDoc(ConfigDAO store) {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField(TYPEF, ConfigType.AgentConfig.toString());
        doc.setField(INT_VERSION_F, store.version);
        doc.setField(CUSTOMERIDF, store.customerId);
        doc.setField(APPF, store.app);
        doc.setField(SERVICEF, store.service);
        doc.setField(INSTANCEIDF, store.instanceId);
        doc.setField(TAG_F, store.tag);
        doc.setField(ISLATESTF, store.isLatest);
        try {
            doc.setField(CONFIG_JSON_F, config.jsonMapper.writeValueAsString(store.configJson));
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Unable to convert "
                + "store json as string")) , e);
        }
        return doc;
    }

    private Optional<ConfigDAO> docToAgent(SolrDocument doc) {
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<Integer> version = getIntField(doc, INT_VERSION_F);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> instanceId = getStrField(doc, INSTANCEIDF);
        Optional<String> configJson = getStrField(doc, CONFIG_JSON_F);
        Optional<String> tag = getStrField(doc, TAG_F);
        Optional<Boolean> latest = getBoolField(doc, ISLATESTF);
        ConfigDAO agentStore = new ConfigDAO(customerId.orElse(null),
            app.orElse(null), service.orElse(null), instanceId.orElse(null),
            tag.orElse(null));
        agentStore.setVersion(version.orElse(0));
        agentStore.setIsLatest(latest.orElse(false));
        try {
            configJson.ifPresent(UtilException.rethrowConsumer(config ->
                agentStore.setConfigJson(this.config.jsonMapper.readValue(config
                    , Config.class))));
            ValidateAgentStore.validate(agentStore);
            return Optional.of(agentStore);
        }catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Data fields are null or empty")), e);
        } catch (Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Unable to convert json string back to StoreConfig object")), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean saveAgentConfigAcknowledge(ConfigApplicationAcknowledge confApplicationAck) {
        return saveDoc(agentConfigAcknowledgeToSolrDoc(confApplicationAck)) && softcommit();
    }

    private SolrInputDocument agentConfigAcknowledgeToSolrDoc(ConfigApplicationAcknowledge confApplicationAck)  {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField(CUSTOMERIDF, confApplicationAck.customerId);
        doc.setField(APPF, confApplicationAck.app);
        doc.setField(SERVICEF, confApplicationAck.service);
        doc.setField(INSTANCEIDF, confApplicationAck.instanceId);
        doc.setField(TYPEF, Types.AgentConfigAcknowledge.toString());
        doc.setField(TIMESTAMPF, Instant.now().toString());
        confApplicationAck.acknowledgeInfo.forEach((x, y)
            -> doc.setField(INFO_PREFIX + x + STRING_SUFFIX, y));
        // Storing key to later retrieve them into event object.
        confApplicationAck.acknowledgeInfo.keySet().forEach(key -> doc.addField(CONFIG_ACK_DATA_KEYSF, key));
        return doc;
    }

    public Pair<Result<ConfigApplicationAcknowledge> , List> getLatestAgentConfigAcknowledge(
        io.md.dao.CubeMetaInfo cubeMetaInfo, boolean facetOnNodeSelected, int forLastNsec) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF, Types.AgentConfigAcknowledge.toString());
        addFilter(query, CUSTOMERIDF, cubeMetaInfo.customerId);
        addFilter(query, APPF, cubeMetaInfo.appName);
        addFilter(query, INSTANCEIDF, cubeMetaInfo.instance);
        addFilter(query, SERVICEF, cubeMetaInfo.serviceName);
        Instant startTimeStamp = Instant.now().minusSeconds(forLastNsec);
        addRangeFilter(query, TIMESTAMPF, Optional.of(startTimeStamp), Optional.empty(), true, true);
//        addSort(query, TIMESTAMPF, false);

        if(facetOnNodeSelected) {
            FacetQ facetq = new FacetQ();
            Facet samplingFacet = Facet.createTermFacet(
                INFO_PREFIX + io.md.constants.Constants.IS_NODE_SELECTED + STRING_SUFFIX,
                Optional.empty());

            facetq.addFacet(SAMPLINGFACET, samplingFacet);

            String jsonFacets;
            try {
                jsonFacets = config.jsonMapper.writeValueAsString(facetq);
                query.add(SOLRJSONFACETPARAM, jsonFacets);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting facets to json"), e);
            }
        }

        Result<ConfigApplicationAcknowledge> result = SolrIterator.getResults(solr, query, Optional.empty(),
            this::docToAgentConfigAcknowledge, Optional.empty());

        if(facetOnNodeSelected) {
            ArrayList samplingFacetResults = result.getFacets(FACETSFIELD, SAMPLINGFACET, BUCKETFIELD);
            return new Pair(result, samplingFacetResults);
        }

        else {
            return new Pair(result, Collections.EMPTY_LIST);
        }
    }


    private Optional<ConfigApplicationAcknowledge> docToAgentConfigAcknowledge(SolrDocument doc) {

        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> instanceId = getStrField(doc, INSTANCEIDF);
        List<String> agentConfigDataKeys = getStrFieldMV(doc, CONFIG_ACK_DATA_KEYSF);


        Map<String, String> agentConfigDataMap = new HashMap<String, String>();
        agentConfigDataKeys.forEach(key -> {
            Optional<String> val = getStrField(doc, INFO_PREFIX + key + STRING_SUFFIX);
            val.ifPresent(v -> agentConfigDataMap.put(key, v));
        });

        if (customerId.isEmpty() || app.isEmpty() || service.isEmpty() || instanceId.isEmpty()) {
            LOGGER.error("Improper agent acknowledgement stored in solr");
            return Optional.empty();
        }

        return Optional.of(new ConfigApplicationAcknowledge(customerId.get(), app.get(),
            service.get(), instanceId.get(), agentConfigDataMap));
    }


    private static final String TEMPLATE_ID = "template_id" + STRINGSET_SUFFIX;
    private static final String VERSIONF = Constants.VERSION_FIELD + STRING_SUFFIX;
    private static final String INT_VERSION_F = Constants.VERSION_FIELD + INT_SUFFIX;
    private static final String ATTRIBUTE_RULE_MAP_ID = "attribute_rule_map_id" + STRING_SUFFIX;
    private static final String DYNAMIC_INJECTION_CONFIG_VERSIONF =
        Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD + STRING_SUFFIX;
    private static final String STATIC_INJECTION_MAPF =
        Constants.STATIC_INJECTION_MAP_FIELD + STRING_SUFFIX;

    private static final String TAG_F = Constants.TAG_FIELD + STRING_SUFFIX;
    private static final String ISLATESTF = "isLatest" + BOOLEAN_SUFFIX;


    private String storeTemplateSetMetadata(TemplateSet templateSet, List<String> templateIds
        , Optional<String> appAttributeRuleMapId) throws TemplateSet.TemplateSetMetaStoreException {
        SolrInputDocument solrDoc = new SolrInputDocument();
        String id = Types.TemplateSet.toString().concat("-").concat(String.valueOf(Objects.hash(
            templateSet.customer, templateSet.app, templateSet.version)));

        solrDoc.setField(IDF, id);
        solrDoc.setField(TYPEF, Types.TemplateSet.toString());
        solrDoc.setField(VERSIONF, templateSet.version);
        solrDoc.setField(CUSTOMERIDF , templateSet.customer);
        solrDoc.setField(APPF, templateSet.app);
        solrDoc.setField(TIMESTAMPF , templateSet.timestamp.toString());
        templateIds.forEach(templateId -> solrDoc.addField(TEMPLATE_ID, templateId));
        appAttributeRuleMapId.ifPresent(ruleMapId -> solrDoc.setField(ATTRIBUTE_RULE_MAP_ID, ruleMapId));
        boolean success = saveDoc(solrDoc) && softcommit();
        if(!success) {
            throw new TemplateSet.TemplateSetMetaStoreException("Error saving Template Set Meta Data in Solr");
        }
        comparatorCache.invalidateAll();
        return id;
    }



    public Optional<TemplateSet> getTemplateSet(String templateSetId) {
        try {
            SolrQuery query = new SolrQuery("*:*");
            addFilter(query, IDF, templateSetId);
            addFilter(query, TYPEF, Types.TemplateSet.toString());
            Optional<Integer> maxResults = Optional.of(1);

            return SolrIterator.getStream(solr, query, maxResults).findFirst().flatMap(this::solrDocToTemplateSet);
        } catch (Exception e) {
            LOGGER.error("Error occured while fetching template set for version :: " + templateSetId + " :: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<TemplateSet> getTemplateSet(String customerId, String app, String version) {
        try {
            SolrQuery query = new SolrQuery("*:*");
            addFilter(query, TYPEF, Types.TemplateSet.toString());
            addFilter(query, CUSTOMERIDF, customerId);
            addFilter(query, APPF, app);
            addFilter(query, VERSIONF, version);
            Optional<Integer> maxResults = Optional.of(1);

            return SolrIterator.getStream(solr, query, maxResults).findFirst().flatMap(this::solrDocToTemplateSet);
        } catch (Exception e) {
            LOGGER.error("Error occured while fetching template set for customer/app/verion :: " + customerId + "::"
                + app + "::" +  version + "::"  + e.getMessage());
        }
        return Optional.empty();
    }


    public Optional<TemplateSet> getLatestTemplateSet(String customer, String app) {
        try {
            SolrQuery query = new SolrQuery("*:*");
            addFilter(query, APPF, app);
            addFilter(query, CUSTOMERIDF, customer);
            addSort(query, TIMESTAMPF, false); // descending
            addSort(query, IDF, true);
            Optional<Integer> maxResults = Optional.of(1);

            return SolrIterator.getStream(solr, query, maxResults).findFirst().flatMap(this::solrDocToTemplateSet);
        } catch (Exception e) {
            LOGGER.error("Error occured while fetching template set for customer :: " + customer + " :: app :: " + app + " :: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getDefaultEventType(String customer, String app
        , String service, String apiPath) {
        SolrQuery query = new SolrQuery("*:*");
        query.setFields(EVENTTYPEF);
        addFilter(query, TYPEF, Types.Event.toString());
        addFilter(query, CUSTOMERIDF, customer);
        addFilter(query, APPF, app);
        addFilter(query, SERVICEF, service);
        addFilter(query, PATHF, apiPath);
        Optional<Integer> maxResults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxResults).findFirst().map(doc
            -> (String)doc.getFieldValue(EVENTTYPEF));
    }

    private Optional<TemplateSet> solrDocToTemplateSet(SolrDocument doc) {
        Optional<String> version = getStrField(doc, VERSIONF);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<Instant> creationTimestamp = getTSField(doc, TIMESTAMPF);
        List<String> templateIds = getStrFieldMV(doc, TEMPLATE_ID);
        if (version.isEmpty() || customerId.isEmpty() || app.isEmpty() || creationTimestamp.isEmpty()) {
            LOGGER.error("Improper template set stored in solr for template set id :: " + getStrField(doc, IDF).get());
            return Optional.empty();
        }
        Optional<String> appAttributeRuleMapId = getStrField(doc, ATTRIBUTE_RULE_MAP_ID);
        TemplateSet templateSet = new TemplateSet(version.get(), customerId.get(), app.get(),
            creationTimestamp.get(), getVersionedTemplatesFromSolr(templateIds) ,
            getVersionedAttributeRuleMapFromSolr(appAttributeRuleMapId));
        return Optional.of(templateSet);
    }

    private Stream<CompareTemplateVersioned> solrDocToCompareTemplate(SolrDocument doc) {
        Optional<String> version = getStrField(doc, VERSIONF);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, SERVICEF);
        String templateId = getStrField(doc, IDF).get();
        Optional<String> type = getStrField(doc, TYPEF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> compareTemplate = getStrField(doc, COMPARETEMPLATEJSON);
        Optional<String> requestPath = getStrField(doc, PATHF);
        Optional<String> method = getStrField(doc, METHODF);
        if (type.isEmpty() || service.isEmpty() || compareTemplate.isEmpty() || requestPath.isEmpty()) {
            LOGGER.error("Improper compare-template stored in solr :: " + templateId);
            return Stream.empty();
        }

        try {
            CompareTemplate compareTemplateObj = config.jsonMapper.readValue(compareTemplate.get() , CompareTemplate.class);
            Type templateType = Utils.valueOf(Type.class, type.get()).orElseThrow(
                () -> new Exception("Couldn't obtain proper template type from solr doc"));
            CompareTemplateVersioned compareTemplateVersioned = new CompareTemplateVersioned(service , requestPath,
               method ,templateType, compareTemplateObj);
            return Stream.of(compareTemplateVersioned);
        } catch (Exception e) {
            LOGGER.error("Error while deserializing compare-template from solr :: " + templateId + " " + e.getMessage());
            return Stream.empty();
        }
    }

    private List<CompareTemplateVersioned> getVersionedTemplatesFromSolr(List<String> templateIds) {
        if (templateIds.isEmpty()) return Collections.EMPTY_LIST;
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, IDF, templateIds);
        Optional<Integer> maxResults = Optional.of(templateIds.size());
        return SolrIterator.getStream(solr, query, maxResults).flatMap(this::solrDocToCompareTemplate)
            .collect(Collectors.toList());
    }

    private Optional<AttributeRuleMap> getVersionedAttributeRuleMapFromSolr(Optional<String> ruleMapId) {
        if (ruleMapId.isEmpty()) return Optional.empty();
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, IDF, ruleMapId);
        Optional<Integer> maxResults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxResults).
            findFirst().flatMap(this::docToAttributeRuleMap);
    }


    @Override
    public Optional<Event> getRequestEvent(String reqId) {

        EventQuery.Builder builder = new EventQuery.Builder("*", "*", Event.REQUEST_EVENT_TYPES);
        builder.withReqId(reqId).withLimit(1);

        return getSingleEvent(builder.build());
    }


    /**
     * @param solr
     * @param config
     */
    public ReqRespStoreSolr(SolrClient solr, com.cube.ws.Config config) {
        this(solr, config, true);
    }

    /**
     * @param solr
     * @param config
     */
    public ReqRespStoreSolr(SolrClient solr, com.cube.ws.Config config, boolean useTemplateCaching) {
        super();
        this.solr = solr;
        this.config = config;
        SolrIterator.setConfig(config);

        if (useTemplateCaching) {
            this.templateCache = new TemplateCacheRedis(this, config);
        } else {
            this.templateCache = new TemplateCacheWithoutCaching(this);
        }
        this.comparatorCache = new ComparatorCache(templateCache, config.jsonMapper, this);

    }

    private final SolrClient solr;
    private final com.cube.ws.Config config;
    private final TemplateCache templateCache;
    private final ComparatorCache comparatorCache;

    private static final String TYPEF = CPREFIX + "type" + STRING_SUFFIX;

    // field names in Solr
    private static final String PATHF = CPREFIX + Constants.PATH_FIELD + STRING_SUFFIX;
    private static final String RUNIDF = CPREFIX + Constants.RUN_ID_FIELD + STRING_SUFFIX;
    private static final String MOCKSERVICESF = CPREFIX + Constants.MOCK_SERVICES_FIELD + STRINGSET_SUFFIX;
    private static final String REQIDF = CPREFIX + Constants.REQ_ID_FIELD + STRING_SUFFIX;
    private static final String METHODF = CPREFIX + Constants.METHOD_FIELD + STRING_SUFFIX;
    private static final String BODYF = CPREFIX + Constants.BODY + NOTINDEXED_SUFFIX;
    private static final String OLDBODYF = CPREFIX + Constants.BODY + TEXT_SUFFIX;
    private static final String COLLECTIONF = CPREFIX + Constants.COLLECTION_FIELD + STRING_SUFFIX;
    private static final String TIMESTAMPF = CPREFIX + Constants.TIMESTAMP_FIELD + DATE_SUFFIX;
    private static final String RRTYPEF = CPREFIX + Constants.RUN_TYPE_FIELD + STRING_SUFFIX;
    private static final String CUSTOMERIDF = CPREFIX + Constants.CUSTOMER_ID_FIELD + STRING_SUFFIX;
    private static final String USERIDF = CPREFIX + Constants.USER_ID_FIELD + STRING_SUFFIX;
    private static final String TESTCONFIGNAMEF = CPREFIX + Constants.TEST_CONFIG_NAME_FIELD + STRING_SUFFIX;
    private static final String APPF = CPREFIX + Constants.APP_FIELD + STRING_SUFFIX;
    private static final String INSTANCEIDF = CPREFIX + Constants.INSTANCE_ID_FIELD + STRING_SUFFIX;
    private static final String STATUSF = CPREFIX + Constants.STATUS + INT_SUFFIX;
    private static final String CONTENTTYPEF = CPREFIX + "contenttype" + STRING_SUFFIX;
    private static final String OPERATIONSETIDF = CPREFIX + "operationsetid" + STRING_SUFFIX;
    private static final String OPERATIONLIST = CPREFIX + "operationlist" + STRINGSET_SUFFIX;
    private static final String TRACEIDF = CPREFIX + Constants.TRACE_ID_FIELD + STRING_SUFFIX;
    private static final String PAYLOADSTRF = CPREFIX + "payloadStr" + NOTINDEXED_SUFFIX;
    private static final String PAYLOADKEYF = CPREFIX + "payloadKey" + INT_SUFFIX;
    private static final String EVENTTYPEF = CPREFIX + Constants.EVENT_TYPE_FIELD + STRING_SUFFIX;
    private static final String SPAN_ID_F = CPREFIX  + Constants.SPAN_ID_FIELD + STRING_SUFFIX ;
    private static final String PARENT_SPAN_ID_F = CPREFIX  + Constants.PARENT_SPAN_ID_FIELD + STRING_SUFFIX;
    private static final String CONFIG_JSON_F = CPREFIX + Constants.CONFIG_JSON + STRING_SUFFIX;
    private static final String SCOREF = CPREFIX + SCORE_FIELD + CSUFFIX;
    private static final String PROTO_DESCRIPTOR_FILE_F = CPREFIX + Constants.PROTO_DESCRIPTOR_FILE_FIELD + NOTINDEXED_SUFFIX;


    private static String getFieldName(String fname, String fkey) {
        return String.format("%s_%s",fname, fkey);
    }
    private static String getSolrFieldName(String fname, String fkey) {
        return String.format("%s%s%s", CPREFIX, getFieldName(fname, fkey), STRINGSET_SUFFIX);
    }


    // ensure that this pattern is consistent with the prefix and suffixes used above
    private static final String patternStr = "^" + CPREFIX + "([^_]+)_(.*)" + STRINGSET_SUFFIX + "$";
    private static final Pattern pattern = Pattern.compile(patternStr);
    private static final String QPARAMS = "qp";
    private static final String FPARAMS = "fp";
    private static final String META = "meta";
    private static final String HDR = "hdr";
    private static final String HDRTRACEF = HDR + "_"  + Constants.DEFAULT_TRACE_FIELD + STRINGSET_SUFFIX;
    private static final String HDRSPANF = HDR + "_"  + Constants.DEFAULT_SPAN_FIELD + STRINGSET_SUFFIX;
    private static final String HDRPARENTSPANF = HDR + "_"  + Constants.DEFAULT_PARENT_SPAN_FIELD + STRINGSET_SUFFIX;
    private static final String METASERVICEF = META + "_service" + STRINGSET_SUFFIX;
    private static final String METAREQID = META + "_c" + "-request-id" + STRINGSET_SUFFIX;
    private static final String METATRACEID = META + "_" + Constants.DEFAULT_TRACE_FIELD + STRINGSET_SUFFIX;


    private static void removeFilter(SolrQuery query, String fieldname, Optional<String> fval) {
        fval.ifPresent(val -> removeFilter(query, fieldname, val));
    }

    private static void removeFilter(SolrQuery query, String fieldname, String fval, boolean quote) {
        String newfval = quote ? SolrIterator.escapeQueryChars(fval) : fval;
        query.removeFilterQuery(String.format("%s:%s", fieldname, newfval));
    }


    private static void removeFilter(SolrQuery query, String fieldname, String fval) {
        // add quotes by default in case the strings have spaces in them
        removeFilter(query, fieldname, fval, true);
    }

    private static void addFilter(SolrQuery query, String fieldname, String fval, boolean quote) {
        addFilter(query, fieldname, Optional.of(fval), quote, false);
    }

    private static void addNegativeFilter(SolrQuery query, String fieldname, String fval, boolean quote) {
        String newfval = quote ? SolrIterator.escapeQueryChars(fval) : fval;
        query.addFilterQuery(String.format("(*:* NOT %s:%s)", fieldname, newfval));
    }


    private static void addFilter(SolrQuery query, String fieldname, String fval) {
        // add quotes by default in case the strings have spaces in them
        addFilter(query, fieldname, fval, true);
    }

    private static void addToFilterOrQuery(SolrQuery query, StringBuffer queryBuff , String fieldname, Optional<String> fval , boolean isOr, Optional<Float> weight) {
        if(weight.isEmpty()) addFilter(query, fieldname, fval, false);
        else addToQryStr(queryBuff , fieldname , fval , isOr , weight );
    }

    private static void addFilter(SolrQuery query, String fieldname, Optional<String> fval) {
        addFilter(query, fieldname, fval, false);
    }


    private static void addFilter(SolrQuery query, String fieldname, Optional<String> fval, boolean quote, boolean includeEmpty) {
        fval.ifPresent(val -> {
            String newfval = quote ? SolrIterator.escapeQueryChars(val) : val;
            if (newfval.isEmpty()) {
                newfval = "\"\"";
            }
            if (includeEmpty) {
                // empty val should be treated as fval
                query.addFilterQuery(
                    String.format("(*:* AND NOT %s:*) OR %s:%s", fieldname, fieldname, newfval));
            } else {
                query.addFilterQuery(String.format("%s:%s", fieldname, newfval));
            }
        });
    }

    private static void addFilter(SolrQuery query, String fieldname, Optional<String> fval, boolean enforceEmpty) {
        fval.ifPresentOrElse(val -> addFilter(query, fieldname, val),
            () -> {
                if (enforceEmpty) {
                    query.addFilterQuery(String.format("-%s:*", fieldname));
                }
            });
    }

    private static void addFilter(SolrQuery query, String fieldname, MultivaluedMap<String, String> fvalmap, String keytoadd) {
        String f = getSolrFieldName(fieldname, keytoadd);
        Optional.ofNullable(fvalmap.get(keytoadd)).ifPresent(vals -> vals.forEach(v -> {
            addFilter(query, f, v);
        }));
    }

    private static void addFilter(SolrQuery query, String fieldname, boolean fval) {
        /**
         * TODO
         * addNegativeFilter is used to get those replays also which don't have the archive field in it
         * Once all the replays have the archive field, we can remove this condition
         */
        if(fval) {
            addFilter(query, fieldname, String.valueOf(fval));
        }
        else {
            addNegativeFilter(query, fieldname, String.valueOf(!fval), true);
        }
    }

    private static void addFilter(SolrQuery query, String fieldname, Integer fval) {
        addFilter(query, fieldname, String.valueOf(fval));
    }

    private static void addToFilterIntOrQuery(SolrQuery query, StringBuffer queryBuff , String fieldname, Optional<Integer> fvalOpt , boolean isOr, Optional<Float> weight) {
        if(weight.isEmpty()){
            fvalOpt.ifPresent(fval -> {
                addFilter(query, fieldname, fval);
            });
        }
        else {
            fvalOpt.ifPresent(fval -> {
                addToQryStr(queryBuff , fieldname , fval.toString() , isOr , weight );
            });
        }
    }

    private static void addFilterInt(SolrQuery query, String fieldname, Optional<Integer> fvalOpt) {
        fvalOpt.ifPresent(fval -> {
            addFilter(query, fieldname, fval);
        });
    }

    private static void addToFilterOrQuery(SolrQuery query, StringBuffer queryBuff , String fieldname, List<String> orValues , boolean isOr, Optional<Float> weight) {
        if(weight.isEmpty()) addFilter(query, fieldname, orValues, false);
        else addToQryStr(queryBuff , fieldname , orValues , isOr , weight );
    }

    private static void addFilter(SolrQuery query, String fieldname, Collection<String> orValues) {
        addFilter(query, fieldname, orValues, false);
    }

    private static void addToFilterOrQuery(SolrQuery query, StringBuffer queryBuff , String fieldname, List<String> orValues , boolean negate , boolean isOr, Optional<Float> weight) {
        if(weight.isEmpty()) addFilter(query, fieldname, orValues, negate);
        else addToQryStr(queryBuff , fieldname , orValues , isOr , weight );
    }

    private static void addFilter(SolrQuery query, String fieldname, Collection<String> orValues, boolean negate) {
        if(orValues.isEmpty()) return; // No values specified, so no filters
        String prefix = negate ? "NOT " : "";
        String filter = orValues.stream().map(val -> {
            if (val.isBlank()) {
                // if value is a blank string, convert it to field negation predicate since Solr does not store blank
                // fields
                return (String.format("(*:* NOT %s:*)", fieldname));
            } else {
                return String.format("(%s:%s)", fieldname, SolrIterator.escapeQueryChars(val));
            }
        }).collect(Collectors.joining(" OR "));
        query.addFilterQuery(prefix + " ( " + filter + " ) ");
    }

    private static String getRangeFilterString(String fieldname, Optional<Instant> startDate, Optional<Instant> endDate, boolean startInclusive, boolean endInclusive) {
        // epoch millis of 0 is a special case. convert back to * to cover full range
        String startDateVal = startDate.isPresent() && startDate.get().toEpochMilli() > 0 ?
            SolrIterator.escapeQueryChars(startDate.get().toString()) :
            "*";
        String endDateVal = endDate.isPresent() ? SolrIterator.escapeQueryChars(endDate.get().toString()) : "*";
        String queryFmt = "%s:" + (startInclusive ? "[": "{") + "%s TO %s" + (endInclusive ? "]" : "}");
        return String.format(queryFmt, fieldname, startDateVal, endDateVal);
    }

    private static void addOrRangeFilter(SolrQuery query, List<String> fieldname, Optional<Instant> startDate, Optional<Instant> endDate, boolean startInclusive,
            boolean endInclusive) {
        String rangeQuery = fieldname.stream().map(v -> getRangeFilterString(v, startDate, endDate, startInclusive, endInclusive))
            .collect(Collectors.joining(" OR ", "( ", " )"));
        query.addFilterQuery(rangeQuery);
    }

    private static void addRangeFilter(SolrQuery query,String fieldname, Optional<Instant> startDate, Optional<Instant> endDate, boolean startInclusive, boolean endInclusive) {
        query.addFilterQuery(getRangeFilterString(fieldname, startDate, endDate, startInclusive, endInclusive));
    }

    private static void addWeightedPathFilter(SolrQuery query , String fieldName , String originalPath) {
        String[] pathElements = originalPath.split("/",-1);
        StringBuffer pathBuffer = new StringBuffer();
        StringBuffer queryBuffer = new StringBuffer();
        var countWrapper = new Object() {int count = 0;};
        Arrays.asList(pathElements).stream().forEachOrdered(elem ->
        {
            pathBuffer.append(((countWrapper.count != 0)? "/" : "") + elem);
            String escapedPath =  SolrIterator.escapeQueryChars(pathBuffer.toString())
                    /*.concat((countWrapper.count != pathElements.length -1)? SolrIterator.escapeQueryChars("*") : "")*/ ;
            queryBuffer.append((countWrapper.count !=0)? " OR " : "").append(escapedPath)
                    .append("^").append(++countWrapper.count);
        });

        String finalPathQuery = fieldName.concat(":").concat("(").concat(queryBuffer.toString()).concat(")");
        //Sample query
        //path_s:("registerTemplate\/\*"^1 OR "registerTemplate/response\/\*"^2 OR
        // "registerTemplate/response/moveieinfo\/\*"^3 OR "registerTemplate/response/moveieinfo/ravivj\/\*"^4 OR
        // "registerTemplate/response/moveieinfo/ravivj/productpage\/\*"^5 OR
        // "registerTemplate/response/moveieinfo/ravivj/productpage/productpage"^6)
        query.setQuery(finalPathQuery.toString());
    }

    private static void addFilter(SolrQuery query, String fieldname, MultivaluedMap<String, String> fvalmap, List<String> keys) {
        // Empty list of selected keys is treated as if all keys are to be added
        Collection<String> ftoadd = (keys.isEmpty()) ? fvalmap.keySet() : keys;
        ftoadd.forEach(k -> addFilter(query, fieldname, fvalmap, k));
    }

    private static void addSort(SolrQuery query, String fieldname, boolean ascending) {
    	query.addSort(fieldname, ascending ? ORDER.asc : ORDER.desc);
    }

    private static void addSort(SolrQuery query, String fieldname, Optional<Boolean> ascendingOpt) {
        ascendingOpt.ifPresent(ascending -> query.addSort(fieldname, ascending ? ORDER.asc : ORDER.desc));

    }


    // for predicates in the solr q param. Assumes *:* is already there in the buffer
    private static void addToQryStr(StringBuffer qstr, String fieldname, String fval, boolean quote , boolean isOr , Optional<Float> weight) {
        // String newfval = quote ? String.format("\"%s\"", StringEscapeUtils.escapeJava(fval)) : fval;
        String newfval = quote ? SolrIterator.escapeQueryChars(fval) : fval;
        String prefix = qstr.length()==0 ? "" : isOr ? " OR " : " AND ";
        Float wt = weight.orElse(1.0F);
        qstr.append(String.format("%s%s:%s^=%.2f" , prefix , fieldname, newfval , wt ) );
    }


    private static void addToQryStr(StringBuffer qstr, String fieldname, String fval, boolean isOr) {
        // add quotes to field vals by default
        addToQryStr(qstr, fieldname, fval, true, isOr, Optional.empty());
    }

    private static void addToQryStr(StringBuffer qstr, String fieldname, String fval, boolean isOr, Optional<Float> weight) {
        // add quotes to field vals by default
        addToQryStr(qstr, fieldname, fval, true, isOr, weight);
    }
    /*
    private static void addToQryStr(StringBuffer qstr, String fieldname, List<String> fvals , boolean isOr , Optional<Float> weight) {
        // add quotes to field vals by default
        for (String fval : fvals) {
            addToQryStr(qstr, fieldname, fval, true, isOr  , weight);
        }
    }
    */

    private static void addToQryStr(StringBuffer qstr, String fieldname, List<String> fvals, boolean isOr, Optional<Float>  weight) {
        if(fvals.isEmpty()) return;

        if(fvals.size() ==1){
            addToQryStr(qstr , fieldname , fvals.get(0) , isOr , weight );
            return;
        }

        String orValues = fvals.stream().collect(Collectors.joining(" OR "));
        addToQryStr(qstr, fieldname, "("+orValues+")", true, isOr , weight);
    }


    private static void addToQryStr(StringBuffer qstr, String fieldname, Optional<String> fval , boolean isOr, Optional<Float>  weight) {
        fval.ifPresent(val -> addToQryStr(qstr, fieldname, val , isOr , weight ));
    }

    private static void addToQryStr(StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, String key, boolean isOr, Optional<Float> weight) {
        String f = getSolrFieldName(fieldname, key);
        Optional.ofNullable(fvalmap.get(key)).ifPresent(vals -> vals.forEach(v -> {
            addToQryStr(qstr, f, v , isOr, weight);
        }));
    }

    private static void addToQryStr(StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, List<String> keys , boolean isOr , Optional<Float> weight) {
        // Empty list of selected keys is treated as if all keys are to be added
        Collection<String> ftoadd = (keys.isEmpty()) ? fvalmap.keySet() : keys;
        ftoadd.forEach(k -> {
            addToQryStr(qstr, fieldname, fvalmap, k , isOr , weight);
        });
    }

    private static void addMatch(ComparisonType mt, SolrQuery query, StringBuffer qparam, String fieldname, String fval, boolean isOr, Optional<Float> weight) {
        switch (mt) {
            case Equal: addFilter(query, fieldname, fval); break;
            case EqualOptional: addToQryStr(qparam, fieldname, fval , isOr , weight); break;
            default:
        }
    }

    private static void addMatch(ComparisonType mt, SolrQuery query, StringBuffer qstr, String fieldname, Optional<String> fval, boolean isOr, Optional<Float> weight) {
        switch (mt) {
            case Equal: addFilter(query, fieldname, fval); break;
            case EqualOptional: addToQryStr(qstr, fieldname, fval , isOr , weight); break;
            default:
        }
    }

    private static void addToQuery(SolrQuery query, StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, ComparisonType ct, String path, boolean isOr, Optional<Float> weight) {
        if (ct == ComparisonType.Equal) {
            addFilter(query, fieldname, fvalmap, path);
        } else if (ct == ComparisonType.EqualOptional) {
            addToQryStr(qstr, fieldname, fvalmap, path , isOr , weight);
        }
    }


    private SolrInputDocument eventToSolrDoc(Event event) {
        final SolrInputDocument doc = new SolrInputDocument();
        String id = event.eventType.toString().concat("-").concat(event.apiPath).concat("-")
            .concat(event.reqId);

        doc.setField(IDF, id);
        doc.setField(TYPEF, Types.Event.toString());
        doc.setField(CUSTOMERIDF, event.customerId);
        doc.setField(APPF, event.app);
        doc.setField(SERVICEF, event.service);
        doc.setField(INSTANCEIDF, event.instanceId);
        doc.setField(COLLECTIONF, event.getCollection());
        doc.setField(TRACEIDF, event.getTraceId());
        if (event.spanId != null) doc.setField(SPAN_ID_F, event.spanId);
        if (event.parentSpanId != null) doc.setField(PARENT_SPAN_ID_F, event.parentSpanId);
        doc.setField(RRTYPEF, event.getRunType().toString());
        doc.setField(TIMESTAMPF, event.timestamp.toString());
        doc.setField(REQIDF, event.reqId);
        doc.setField(PATHF, event.apiPath);
        doc.setField(EVENTTYPEF, event.eventType.toString());
        doc.setField(RECORDING_TYPE_F, event.recordingType.toString());
        doc.setField(RUNIDF, event.runId);
        try {
            doc.setField(PAYLOADSTRF, config.jsonMapper.writeValueAsString(event.payload));
        } catch (JsonProcessingException e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Unable to convert "
                + "event payload as string")) , e);
        }
        doc.setField(PAYLOADKEYF, event.payloadKey);

        if(event.metaData != null && !event.metaData.isEmpty()) {
            event.metaData.forEach((x, y)
                -> doc.setField(EVENT_META_DATA_PREFIX + x + STRING_SUFFIX, y));
            // Storing key to later retrieve them into event object.
            event.metaData.keySet().forEach(key -> doc.addField(EVENT_META_DATA_KEYSF, key));
        }

        return doc;
    }

    private Optional<Event> docToEvent(SolrDocument doc) {

        Optional<String> docId = getStrField(doc, IDF);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> instanceId = getStrField(doc, INSTANCEIDF);
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<String> traceid = getStrField(doc, TRACEIDF);
        Optional<String> spanId = getStrField(doc, SPAN_ID_F);
        Optional<String> parentSpanId = getStrField(doc, PARENT_SPAN_ID_F);
        Optional<Event.RunType> runType = getStrField(doc, RRTYPEF).flatMap(rrt -> Utils.valueOf(
            Event.RunType.class, rrt));
        Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);
        Optional<String> reqId = getStrField(doc, REQIDF);
        Optional<String> path = getStrField(doc, PATHF);
        Optional<String> eventType = getStrField(doc, EVENTTYPEF);
        Optional<String> payloadStr = getStrFieldMVFirst(doc, PAYLOADSTRF);
        Optional<Integer> payloadKey = getIntField(doc, PAYLOADKEYF);
        List<String> eventMetaDataKeys = getStrFieldMV(doc, EVENT_META_DATA_KEYSF);
        Optional<Double> score = getDblField(doc , SCOREF);

        Map<String, String> eventMetaDataMap = new HashMap<String, String>();
        score.ifPresent(dblScore->{
            eventMetaDataMap.put(SCORE_FIELD , dblScore.toString());
        });
        eventMetaDataKeys.forEach(key -> {
            Optional<String> val = getStrField(doc, EVENT_META_DATA_PREFIX + key + STRING_SUFFIX);
            val.ifPresent(v -> eventMetaDataMap.put(key, v));
        });
        Optional<RecordingType> recordingType = getStrField(doc, RECORDING_TYPE_F)
            .flatMap(r -> Utils.valueOf(RecordingType.class, r));

        Event.EventType eType = Utils.valueOf(Event.EventType.class, eventType.get()).orElse(null);
        Optional<String> runId = getStrField(doc,RUNIDF);

        EventBuilder eventBuilder = new EventBuilder(customerId.orElse(null)
            , app.orElse(null), service.orElse(null), instanceId.orElse(null)
            , collection.orElse(null), new MDTraceInfo(traceid.orElse(null)
            , spanId.orElse(null), parentSpanId.orElse(null)), runType.orElse(null)
            , timestamp, reqId.orElse(null), path.orElse(""), eType, recordingType.orElse(RecordingType.Golden)).withMetaData(eventMetaDataMap);
        runId.ifPresent(eventBuilder::withRunId);
        // TODO revisit this need to construct payload properly from type and json string
        try {
            payloadStr.ifPresent(UtilException.rethrowConsumer(payload ->
                eventBuilder.setPayload(this.config.jsonMapper.readValue(payload
            , Payload.class))));

        } catch (Exception e) {
            try {
                payloadStr.ifPresent(UtilException.rethrowConsumer(payload -> {
                    String finalPayload = "[ \"" + ((eType == EventType.HTTPRequest)
                        ? "HTTPRequestPayload" : "HTTPResponsePayload") + "\" , " + payload + " ] ";
                    eventBuilder
                        .setPayload(this.config.jsonMapper.readValue(finalPayload, Payload.class));
                }));
            } catch (Exception e1) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Unable to convert json string back to payload object")), e1);
            }
        }
        //eventBuilder.setRawPayloadString(payloadStr.orElse(null));
        //eventBuilder.setRawPayloadBinary(payloadBin.orElse(null));
        eventBuilder.setPayloadKey(payloadKey.orElse(0));

        Optional<Event> event = eventBuilder.createEventOpt();

        // TODO: revisit if parsing is needed here or should be done on demand by the consumer
        /*event.ifPresent(e -> {
            if (e.eventType != EventType.ThriftResponse && e.eventType != EventType.ThriftRequest) {
                e.parsePayLoad(config);
            }
        });*/

        return event;
    }

    private static void checkAndAddValues(MultivaluedMap<String, String> cont, String key, Object val) {
        if (val instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> vals = (List<Object>)val;
            vals.forEach(v -> {
                if (v instanceof String)
                    cont.add(key, (String)v);
            });
        }
    }

    private static<T> Optional<T> getField(SolrDocument doc , String fieldName , Class<T> clazz){
        return Optional.ofNullable(doc.get(fieldName)).flatMap(v -> {
            if (clazz.isInstance(v))
                return Optional.of((T)v);
            return Optional.empty();
        });
    }
    private static<T> Optional<T> getField(SolrDocument doc , String fieldName , Function<Object , T> mapper){
        return Optional.ofNullable(doc.get(fieldName)).flatMap(v -> {
            return io.md.utils.Utils.safeFnExecute(v , mapper);
        });
    }

    private static Optional<String> getStrField(SolrDocument doc, String fname) {
        return getField(doc , fname , String.class);
    }

    private static List<String> getStrFieldMV(SolrDocument doc, String fname) {
        return getField(doc, fname, List.class).orElse(new ArrayList());
    }

    private static<T> Optional<T> getFirst(Collection<?> collection , Class<T> clazz){
        return (Optional<T>) collection.stream().findFirst();
    }

    // get first value of a multi-valued field
    private static Optional<String> getStrFieldMVFirst(SolrDocument doc, String fname) {
        return getField(doc, fname , List.class).flatMap(l->getFirst(l , String.class));
    }

    private static Optional<Integer> getIntField(SolrDocument doc, String fname) {
        return getField(doc, fname , Integer.class);
    }

    private static Optional<Double> getDblField(SolrDocument doc, String fname) {
        return getField(doc, fname , obj->Double.valueOf(obj.toString()));
    }

    private static Optional<Instant> getTSField(SolrDocument doc, String fname) {
        return getField(doc, fname , Date.class).map(Date::toInstant);
    }

    private static Optional<Boolean> getBoolField(SolrDocument doc, String fname) {
        return getField(doc, fname, Boolean.class);
    }

    // get binary field
    private static Optional<byte[]> getBinField(SolrDocument doc, String fname) {
        return getField(doc, fname , byte[].class);
    }

    // get first value of a multi-valued field
    private static Optional<byte[]> getBinFieldMVFirst(SolrDocument doc, String fname) {
        return getField(doc, fname , List.class).flatMap(l->getFirst(l , byte[].class));
    }

    private static void addFieldsToDoc(SolrInputDocument doc,
            String ftype, MultivaluedMap<String, String> fields) {

        fields.forEach((f, vl) -> {
            final String fname = getSolrFieldName(ftype, f);
            vl.forEach((v) -> doc.addField(fname, v));
        });
    }

    private FnKey saveFuncKey;

    private boolean saveDoc(SolrInputDocument doc) {
        String runId = Instant.now().toString();
        if (saveFuncKey == null) {
            try {
                Method currentMethod = solr.getClass().getMethod("add", doc.getClass());
                saveFuncKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app,
                    config.commonConfig.instance, config.commonConfig.serviceName, currentMethod);
            } catch (Exception e) {
                LOGGER.error("Couldn't Initiate save function key :: " + e.getMessage());
            }
        }
        // TODO the or else will change to empty string once we correctly set the baggage state through envoy filters
        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(saveFuncKey , Optional.empty(), Optional.empty(), runId, doc);
            if (ret.retStatus == RetStatus.Exception) {
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            UpdateResponse fromSolr = (UpdateResponse) ret.retVal;
            return fromSolr != null;
        }

        UpdateResponse fromSolr = null;
        boolean toReturn = false;
        try {
            fromSolr = solr.add(doc);
            toReturn = true;

        } catch (Exception e) {
            LOGGER.error("Error in saving document to solr of type " +
                Optional.ofNullable(doc.getFieldValue(TYPEF)).map(Object::toString).orElse("NA")
                + ", id = " +
                Optional.ofNullable(doc.getFieldValue(IDF)).map(Object::toString).orElse("NA"), e);
        }
        // TODO the or else will change to empty string once we correctly set the baggage state through envoy filters
        try {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(saveFuncKey, fromSolr,
                    RetStatus.Success, Optional.empty(), runId, doc);
            }
            return toReturn;
        } catch (Throwable e) {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(saveFuncKey,
                    e, RetStatus.Exception, Optional.of(e.getClass().getName()), runId, doc);
            }
            throw e;
        }
    }

    private FnKey deleteFuncKey;

    private boolean deleteDocsByQuery(String query) {
        LOGGER.info("Attempting solr query to delete docs: {" + query + "}");
        if (deleteFuncKey == null) {
            try {
                Method currentMethod = solr.getClass().getMethod("deleteByQuery", query.getClass());
                deleteFuncKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app,
                    config.commonConfig.instance, config.commonConfig.serviceName, currentMethod);
            } catch (Exception e) {
                LOGGER.error("Couldn't Initiate delete function key :: " + e.getMessage());
            }
        }
        // TODO the or else will change to empty string once we correctly set the baggage state through envoy filters
        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(deleteFuncKey , Optional.empty(), Optional.empty(), query);
            if (ret.retStatus == RetStatus.Exception) {
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            UpdateResponse fromSolr = (UpdateResponse) ret.retVal;
            return fromSolr != null;
        }

        UpdateResponse fromSolr = null;
        boolean toReturn = false;
        try {
            fromSolr = solr.deleteByQuery(query);
            toReturn = softcommit();

        } catch (Exception e) {
            LOGGER.error("Error in deleting documents from solr using query " +
                query, e);
        }
        // TODO the or else will change to empty string once we correctly set the baggage state through envoy filters
        try {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(deleteFuncKey, fromSolr,
                    RetStatus.Success, Optional.empty(), query);
            }
            return toReturn;
        } catch (Throwable e) {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(deleteFuncKey,
                    e, RetStatus.Exception, Optional.of(e.getClass().getName()), query);
            }
            throw e;
        }
    }


    // field names in Solr for Replay object
    private static final String IDF = "id";
    private static final String ENDPOINTF = CPREFIX + "endpoint" + STRING_SUFFIX;
    private static final String REQIDSF = CPREFIX + Constants.REQ_ID_FIELD + STRINGSET_SUFFIX;
    private static final String REPLAYIDF = CPREFIX + "replayId" + STRING_SUFFIX;
    private static final String ASYNCF = CPREFIX + "async" + BOOLEAN_SUFFIX;
    private static final String REPLAYSTATUSF = CPREFIX + "status" + STRING_SUFFIX;
    private static final String PATHSF = CPREFIX + Constants.PATH_FIELD + STRINGSET_SUFFIX;
    private static final String EXCLUDEPATHSF = CPREFIX + Constants.EXCLUDE_PATH_FIELD + BOOLEAN_SUFFIX;
    private static final String REQCNTF = CPREFIX + "reqcnt" + INT_SUFFIX;
    private static final String REQSENTF = CPREFIX + "reqsent" + INT_SUFFIX;
    private static final String REQFAILEDF = CPREFIX + "reqfailed" + INT_SUFFIX;
    private static final String CREATIONTIMESTAMPF = CPREFIX + "creationtimestamp" + DATE_SUFFIX;
    private static final String ANALYSISCOMPLETETIMESTAMPF = CPREFIX + "analysiscompletetimestamp" + DATE_SUFFIX;
    private static final String SAMPLERATEF = CPREFIX + "samplerate" + DOUBLE_SUFFIX;
    private static final String INTERMEDIATESERVF = CPREFIX + "intermediateserv" + STRINGSET_SUFFIX;
    private static final String XFMSF = CPREFIX + "transforms" + STRING_SUFFIX;


    // field names in Solr for compare template (stored as json)
    private static final String COMPARETEMPLATEJSON = CPREFIX + "comparetemplate" + STRING_SUFFIX;
    private static final String ATTRIBUTE_RULE_TEMPLATE_JSON = CPREFIX + "attribute_rule_template" + STRING_SUFFIX;
    private static final String EXTRACTION_METAS_JSON = CPREFIX + Constants.EXTRACTION_METAS_JSON_FIELD + STRING_SUFFIX;
    private static final String INJECTION_METAS_JSON = CPREFIX + Constants.INJECTION_METAS_JSON_FIELD + STRING_SUFFIX;
    private static final String PARTIALMATCH = CPREFIX + "partialmatch" + STRING_SUFFIX;


    private static final String TRACERF = CPREFIX + "tracer" + STRING_SUFFIX;

    // DONT use SimpleDateFormat in multi-threaded environment. Each thread should have its own
    // instance. https://www.callicoder.com/java-simpledateformat-thread-safety-issues/
    // private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static SolrInputDocument replayToSolrDoc(Replay replay) {
        final SolrInputDocument doc = new SolrInputDocument();

        String type = Types.ReplayMeta.toString();
        String id = type + '-' + replay.replayId;
        // the id field is set to replay id so that the document can be updated based on id
        doc.setField(IDF, id);
        doc.setField(APPF, replay.app);
        doc.setField(ASYNCF, replay.async);
        doc.setField(COLLECTIONF, replay.collection);
        doc.setField(CUSTOMERIDF, replay.customerId);
        doc.setField(INSTANCEIDF, replay.instanceId);
        doc.setField(ENDPOINTF, replay.endpoint);
        doc.setField(REPLAYIDF, replay.replayId);
        doc.setField(USERIDF, replay.userId);
        replay.testConfigName.ifPresent(testconf -> doc.setField(TESTCONFIGNAMEF, testconf));
        replay.reqIds.forEach(reqId -> doc.addField(REQIDSF, reqId));
        doc.setField(REPLAYSTATUSF, replay.status.toString());
        doc.setField(TYPEF, type);
        replay.paths.forEach(path -> doc.addField(PATHSF, path));
        replay.mockServices.forEach(mockService -> doc.addField(MOCKSERVICESF,mockService));
        doc.setField(EXCLUDEPATHSF, replay.excludePaths);
        doc.setField(REQCNTF, replay.reqcnt);
        doc.setField(REQSENTF, replay.reqsent);
        doc.setField(REQFAILEDF, replay.reqfailed);
        doc.setField(CREATIONTIMESTAMPF, replay.creationTimeStamp.toString());
        doc.setField(VERSIONF, replay.templateVersion);
        replay.intermediateServices.forEach(service -> doc.addField(INTERMEDIATESERVF , service));
        replay.sampleRate.ifPresent(sr -> doc.setField(SAMPLERATEF, sr));
        replay.generatedClassJarPath.ifPresent(jarPath -> doc.setField(GENERATED_CLASS_JAR_PATH, jarPath));
        replay.service.forEach(serv -> doc.addField(SERVICESF, serv));
        doc.setField(REPLAY_TYPE_F, replay.replayType.toString());
        replay.xfms.ifPresent(xfms -> doc.setField(XFMSF, xfms));
        replay.goldenName.ifPresent(goldenName -> doc.setField(GOLDEN_NAMEF, goldenName));
        replay.recordingId.ifPresent(recordingId -> doc.setField(RECORDING_IDF, recordingId));
        doc.setField(ARCHIVEDF,replay.archived);
        doc.setField(ANALYSISCOMPLETETIMESTAMPF,  replay.analysisCompleteTimestamp.toString());
        replay.dynamicInjectionConfigVersion.ifPresent(DIConfVersion -> doc.setField(
            DYNAMIC_INJECTION_CONFIG_VERSIONF, DIConfVersion));
        replay.staticInjectionMap.ifPresent(sim -> doc.setField(
            STATIC_INJECTION_MAPF, sim));
        doc.setField(RUNIDF, replay.runId);

        return doc;
    }

    /**
     * Form a solr storage document for an analysis template (being stored as
     * a json)
     * @param key
     * @param jsonCompareTemplate
     * @return
     */
    private static SolrInputDocument compareTemplateToSolrDoc(TemplateKey key, String jsonCompareTemplate) {
        final SolrInputDocument doc = new SolrInputDocument();
        String type = key.getReqOrResp().name();
        // Sample key in solr ResponseCompareTemplate-1234-bookinfo-getAllBooks--2013106077
        String id = type.concat("-").concat(String.valueOf(Objects.hash(
                key.getCustomerId() , key.getAppId() , key.getServiceId() , key.getPath()
                , key.getReqOrResp().toString() , key.getVersion() , key.getMethod())));
        doc.setField(IDF , id);
        doc.setField(COMPARETEMPLATEJSON, jsonCompareTemplate);
        String path = key.getPath();
        doc.setField(PATHF , path);
        doc.setField(APPF , key.getAppId());
        doc.setField(CUSTOMERIDF , key.getCustomerId());
        doc.setField(SERVICEF , key.getServiceId());
        doc.setField(TYPEF , type);
        doc.setField(VERSIONF, key.getVersion());
        key.getMethod().ifPresent(method -> doc.setField(METHODF, method));
        return doc;
    }


    private static Optional<Replay> docToReplay(SolrDocument doc) {

        Optional<String> app = getStrField(doc, APPF);
        Optional<String> instanceId = getStrField(doc, INSTANCEIDF);
        Optional<Boolean> async = getBoolField(doc, ASYNCF);
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> userId = getStrField(doc, USERIDF);
        Optional<String> endpoint = getStrField(doc, ENDPOINTF);
        Optional<String> replayId = getStrField(doc, REPLAYIDF);
        List<String> reqIds = getStrFieldMV(doc, REQIDSF);
        Optional<ReplayStatus> status = getStrField(doc, REPLAYSTATUSF)
            .flatMap(s -> Utils.valueOf(ReplayStatus.class, s));
        List<String> paths = getStrFieldMV(doc, PATHSF);
        List<String> mockServices = getStrFieldMV(doc, MOCKSERVICESF);
        Optional<Boolean> excludePaths = getBoolField(doc, EXCLUDEPATHSF);
        int reqcnt = getIntField(doc, REQCNTF).orElse(0);
        int reqsent = getIntField(doc, REQSENTF).orElse(0);
        int reqfailed = getIntField(doc, REQFAILEDF).orElse(0);
        Optional<Instant> creationTimestamp = getTSField(doc, CREATIONTIMESTAMPF);
        Optional<Double> sampleRate = getDblField(doc, SAMPLERATEF);
        List<String> intermediateService = getStrFieldMV(doc, INTERMEDIATESERVF);
        Optional<String> templateVersion = getStrField(doc, VERSIONF);
        Optional<String> generatedClassJarPath = getStrField(doc, GENERATED_CLASS_JAR_PATH);
        List<String> services = getStrFieldMV(doc, SERVICESF);
        /**TODO Remove this once old replays are gone*/
        if(services.isEmpty()) {
            Optional<String> service = getStrField(doc, SERVICEF);
            service.map(s-> services.add(s));
        }
        Optional<String> testConfigName = getStrField(doc, TESTCONFIGNAMEF);
        Optional<String> goldenName = getStrField(doc, GOLDEN_NAMEF);
        Optional<String> recordingId = getStrField(doc, RECORDING_IDF);
        ReplayTypeEnum replayType = getStrField(doc, REPLAY_TYPE_F).flatMap(repType ->
            Utils.valueOf(ReplayTypeEnum.class, repType)).orElse(ReplayTypeEnum.HTTP);
        Optional<String> xfms = getStrField(doc, XFMSF);
        Optional<Boolean> archived = getBoolField(doc, ARCHIVEDF);
        Optional<Instant> analysisCompleteTimestamp = getTSField(doc, ANALYSISCOMPLETETIMESTAMPF);
        Optional<String> dynamicInjectionConfigVersion = getStrField(doc,
            DYNAMIC_INJECTION_CONFIG_VERSIONF);
        Optional<String> staticInjectionMap = getStrField(doc,
            STATIC_INJECTION_MAPF);
        Optional<String> runId = getStrField(doc, RUNIDF);

        Optional<Replay> replay = Optional.empty();
        if (endpoint.isPresent() && customerId.isPresent() && app.isPresent() &&
            instanceId.isPresent() && collection.isPresent()
            && replayId.isPresent() && async.isPresent() && status.isPresent() && userId.isPresent()
            && templateVersion.isPresent()) {
            try {
                ReplayBuilder builder = new ReplayBuilder(endpoint.get(),
                    customerId.get(), app.get(), instanceId.get(), collection.get(), userId.get()).withReqIds(reqIds)
                    .withReplayId(replayId.get())
                    .withAsync(async.get()).withTemplateSetVersion(templateVersion.get())
                    .withReplayStatus(status.get()).withPaths(paths)
                    .withMockServices(mockServices)
                    .withIntermediateServices(intermediateService)
                    .withReqCounts(reqcnt, reqsent, reqfailed)
                    .withReplayType(replayType).withCreationTimestamp(
                        creationTimestamp.orElseGet(() -> Instant.now()));
                runId.ifPresent(builder::withRunId);
                excludePaths.ifPresent(builder::withExcludePaths);
                sampleRate.ifPresent(builder::withSampleRate);
                generatedClassJarPath
                    .ifPresent(UtilException.rethrowConsumer(builder::withGeneratedClassJar));
                builder.withServiceToReplay(services);
                xfms.ifPresent(builder::withXfms);
                testConfigName.ifPresent(builder::withTestConfigName);
                goldenName.ifPresent(builder::withGoldenName);
                recordingId.ifPresent(builder::withRecordingId);
                archived.ifPresent(builder::withArchived);
                analysisCompleteTimestamp.ifPresent(builder::withAnalysisCompleteTimestamp);
                dynamicInjectionConfigVersion.ifPresent(builder::withDynamicInjectionConfigVersion);
                staticInjectionMap.ifPresent(builder::withStaticInjectionMap);

                replay = Optional.of(builder.build());
            } catch (Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
                    ,"Not able to convert Solr result to Replay object"
                    , Constants.REPLAY_ID_FIELD , replayId.orElse("NA") )), e);
            }
        } else {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
                ,"Not able to convert Solr result to Replay object",
                Constants.ERROR, "One of the required fields missing"
                , Constants.REPLAY_ID_FIELD , replayId.orElse("NA") )));
        }

        return replay;
    }


    private boolean softcommit() {
        try {
            solr.commit(false, true, true);
        } catch (SolrServerException | IOException e) {
            LOGGER.error("Error in commiting to Solr", e);
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.dao.Replay)
     */
    @Override
    public boolean saveReplay(Replay replay) {
        SolrInputDocument doc = replayToSolrDoc(replay);
        return saveDoc(doc) && softcommit();
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getReplay(java.lang.String)
     */
    @Override
    public Optional<Replay> getReplay(String replayId) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        //query.setRows(1);
        addFilter(query, TYPEF, Types.ReplayMeta.toString());
        addFilter(query, REPLAYIDF, replayId);

        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> docToReplay(doc));

    }

    /**
     * Extract ResponseCompareTemplate from query result
     * @param doc Retrieve Result
     * @return
     */
    private  Optional<Pair<TemplateKey, CompareTemplate>> docToCompareTemplate(SolrDocument doc) {

        return getStrField(doc, COMPARETEMPLATEJSON).flatMap(templateJson -> {
            try {
                String version = getStrField(doc, VERSIONF).orElseThrow(() ->
                    new Exception("Version not present"));
                String customerId = getStrField(doc, CUSTOMERIDF).orElseThrow(() ->
                    new Exception("Customer id not present"));
                String appId = getStrField(doc, APPF).orElseThrow(() ->
                    new Exception("App not present"));
                String serviceId = getStrField(doc, SERVICEF).orElseThrow(() ->
                    new Exception("Service not present"));
                String path = getStrField(doc, PATHF).orElseThrow(() ->
                    new Exception("Path not present"));
                TemplateKey.Type reqOrResp = getStrField(doc, TYPEF).flatMap(res ->
                    Utils.valueOf(TemplateKey.Type.class, res)).orElseThrow(() ->
                    new Exception("Template key type Not Specified"));
                Optional<String> method = getStrField(doc, METHODF);
                Optional<Pair<TemplateKey, CompareTemplate>> ret = Optional.of(new Pair(new TemplateKey(version,
                    customerId, appId, serviceId, path, reqOrResp,
                    method, "NA"), config.jsonMapper.readValue(templateJson, CompareTemplate.class)));
                return ret;
            } catch (Exception e) {
                LOGGER.error("Error while reading template object from json :: " + getIntField(doc , IDF).orElse(-1));
                return Optional.empty();
            }
        });
    }

    /**
     * Save an analysis template as json for the given key parameters in solr
     * @param key
     * @param templateAsJson
     * @return id of the new template just created
     */
    @Override
    public String saveCompareTemplate(TemplateKey key, String templateAsJson) throws CompareTemplate.CompareTemplateStoreException {
        SolrInputDocument solrDoc = compareTemplateToSolrDoc(key ,templateAsJson);
        boolean success =  saveDoc(solrDoc) && softcommit();
        if(!success) {
            throw new CompareTemplate.CompareTemplateStoreException("Error saving Compare Template in Solr");
        }
        comparatorCache.invalidateKey(key);
        return solrDoc.getFieldValue(IDF).toString();
    }

    @Override
    public String saveAttributeRuleMap(TemplateKey key, String ruleMapJson)
        throws CompareTemplate.CompareTemplateStoreException {
        SolrInputDocument solrDoc = attributeRuleMapToSolrDoc(key , ruleMapJson);
        boolean success = saveDoc(solrDoc) && softcommit();
        if(!success) {
            throw new CompareTemplate.CompareTemplateStoreException("Error saving Compare Template in Solr");
        }
        comparatorCache.invalidateKey(key);
        return solrDoc.getFieldValue(IDF).toString();
    }

    public SolrInputDocument attributeRuleMapToSolrDoc(TemplateKey key, String ruleMapJson) {
        final SolrInputDocument doc = new SolrInputDocument();
        String type = Types.AttributeTemplate.name();
        // Sample key in solr ResponseCompareTemplate-1234-bookinfo-getAllBooks--2013106077
        String id = type.concat("-").concat(String.valueOf(Objects.hash(
            key.getCustomerId() , key.getAppId() , key.getVersion())));
        doc.setField(IDF , id);
        doc.setField(ATTRIBUTE_RULE_TEMPLATE_JSON , ruleMapJson);
        doc.setField(APPF , key.getAppId());
        doc.setField(CUSTOMERIDF , key.getCustomerId());
        doc.setField(TYPEF , type);
        doc.setField(VERSIONF, key.getVersion());
        return doc;
    }

/*    public static String getTemplateType(TemplateKey key) {
         if (key.getReqOrResp() == Type.RequestMatch) {
             return  Types.RequestMatchTemplate.toString();
         }
         else if (key.getReqOrResp() == Type.RequestCompare) {
             return  Types.RequestCompareTemplate.toString();
         }
         else {
             return Types.ResponseCompareTemplate.toString();
         }
    }*/


    /**
     * Get compare template from solr for the given key parameters
     * @param key
     * @return
     */
    @Override
    public Optional<CompareTemplate> getCompareTemplate(TemplateKey key) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF,key.getReqOrResp().name());
        addFilter(query, CUSTOMERIDF, key.getCustomerId());
        addFilter(query, APPF, key.getAppId());
        addFilter(query , SERVICEF , key.getServiceId());
        addWeightedPathFilter(query , PATHF , key.getPath());
        addFilter(query, VERSIONF, key.getVersion(), true);
        addFilter(query, METHODF, key.getMethod() , true , true);
        //addFilter(query, PATHF , key.getPath());
        // 2 at max since there may be one with method empty and one with matching method
        Optional<Integer> maxResults = Optional.of(2);
        Collection<Pair<TemplateKey, CompareTemplate>> templates = SolrIterator.getStream(solr , query , maxResults)
            .map(this::docToCompareTemplate)
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
        // give preference to a match that matches on method as well. If method is empty, it gets lower preference
        Optional<CompareTemplate> fromSolr =
            templates.stream().filter(val -> key.getMethod().equals(val.first().getMethod()))
            .findFirst()
            .or(() -> templates.stream().findFirst())
            .map(val -> val.second());

        return fromSolr;
    }

    @Override
    public Optional<AttributeRuleMap> getAttributeRuleMap(TemplateKey key) {
        final SolrQuery appAttributeTemplateQuery = new SolrQuery("*:*");
        appAttributeTemplateQuery.addField("*");
        addFilter(appAttributeTemplateQuery, TYPEF, Types.AttributeTemplate.name());
        addFilter(appAttributeTemplateQuery, CUSTOMERIDF, key.getCustomerId());
        addFilter(appAttributeTemplateQuery, APPF, key.getAppId());
        addFilter(appAttributeTemplateQuery, VERSIONF, key.getVersion(), true);
        return SolrIterator.getSingleResult(solr, appAttributeTemplateQuery)
            .flatMap(this::docToAttributeRuleMap);
    }

    private Optional<AttributeRuleMap> docToAttributeRuleMap(SolrDocument doc) {
        return getStrField(doc, ATTRIBUTE_RULE_TEMPLATE_JSON).flatMap(templateJson -> {
            try {
                return Optional.of(config.jsonMapper.readValue(templateJson, AttributeRuleMap.class));
            } catch (IOException e) {
                LOGGER.error("Error while reading template object from json :: " + getIntField(doc , IDF).orElse(-1));
                return Optional.empty();
            }
        });
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getReplay(java.util.Optional, java.util.Optional, java.util.Optional, com.cube.dao.Replay.ReplayStatus)
     */
    @Override
    public Stream<Replay> getReplay(Optional<String> customerId, Optional<String> app, Optional<String> instanceId,
                                    ReplayStatus status) {
        return getReplay(customerId,app,instanceId,List.of(status),Optional.of(1),Optional.empty());
    }

    @Override
    public Result<Replay> getReplay(Optional<String> customerId, Optional<String> app, List<String> instanceId,
            List<ReplayStatus> status, Optional<String> collection,  Optional<Integer> numOfResults,  Optional<Integer> start,
            Optional<String> userId, Optional<Instant> endDate, Optional<Instant> startDate, Optional<String> testConfigName,
            Optional<String> goldenName, boolean archived) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.ReplayMeta.toString());
        addFilter(query, ARCHIVEDF, archived);
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, INSTANCEIDF, instanceId);
        addFilter(query, REPLAYSTATUSF, status.stream().map(ReplayStatus::toString).collect(Collectors.toList()));
        addFilter(query, COLLECTIONF , collection);
        addFilter(query, USERIDF, userId);
        addFilter(query, TESTCONFIGNAMEF, testConfigName);
        addFilter(query, GOLDEN_NAMEF, goldenName);
        /**TODO
         * the filter is based on two fields(updationTimestamp and creationTimestamp)
         *Once all the replays will be having the updationTimeStamp, we can remove addOrRange and use addRangeFilter with UPDATIONTIMESTAMPF
         */
        addOrRangeFilter(query, Arrays.asList(ANALYSISCOMPLETETIMESTAMPF, CREATIONTIMESTAMPF), startDate, endDate, true, true);
        // Heuristic: getting the latest replayid if there are multiple.
        /**TODO: what happens if there are multiple replays running for the
         * the sort will based on updationTimestamp once each replay will be having it
         */
        // same triple (customer, app, instance)
        addSort(query, CREATIONTIMESTAMPF, false /* desc */);

        //Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getResults(solr, query, numOfResults, ReqRespStoreSolr::docToReplay, start);
    }

    @Override
    public Stream<Replay> getReplay(Optional<String> customerId, Optional<String> app, Optional<String> instanceId,
            List<ReplayStatus> status, Optional<Integer> numofResults, Optional<String> collection) {
        //Reference - https://stackoverflow.com/a/31688505/3918349
        List<String> instanceidList = instanceId.stream().collect(Collectors.toList());
        return getReplay(customerId, app, instanceidList, status, collection, numofResults, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), false).objects;
    }

    private static final String OBJJSONF = CPREFIX + "json" + NOTINDEXED_SUFFIX;



    public boolean saveMatchResultAggregate(MatchResultAggregate resultAggregate, String customerId) {
        SolrInputDocument doc = matchResultAggregateToSolrDoc(resultAggregate, customerId);
        return saveDoc(doc) && softcommit();
    }

    /**
     * @param resultAggregate
     * @return
     */
    private SolrInputDocument matchResultAggregateToSolrDoc(MatchResultAggregate resultAggregate, String customerId) {
        final SolrInputDocument doc = new SolrInputDocument();

        String resultAggregateJson="";
        try {
            resultAggregateJson = config.jsonMapper.writeValueAsString(resultAggregate);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting MatchResultAggregate object into string for replay id %s", resultAggregate.replayId), e);
        }

        String type = Types.MatchResultAggregate.toString();
        // the id field is set using (replayId, service, path) which is unique
        String id = type + "-" + Objects.hash(resultAggregate.replayId, resultAggregate.service.orElse(DEFAULT_EMPTY_FIELD_VALUE), resultAggregate.path.orElse(DEFAULT_EMPTY_FIELD_VALUE));

        doc.setField(TYPEF, type);
        doc.setField(IDF, id);
        doc.setField(APPF, resultAggregate.app);
        doc.setField(REPLAYIDF, resultAggregate.replayId);
        doc.setField(OBJJSONF, resultAggregateJson);

        // Set the fields if present else put the default value
        doc.setField(SERVICEF, resultAggregate.service.orElse(DEFAULT_EMPTY_FIELD_VALUE));
        doc.setField(PATHF, resultAggregate.path.orElse(DEFAULT_EMPTY_FIELD_VALUE));
        doc.setField(CUSTOMERIDF, customerId);

        return doc;
    }


    public Stream<MatchResultAggregate> getResultAggregate(String replayId, Optional<String> service,
                                                        boolean byPath) {

        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, TYPEF, Types.MatchResultAggregate.toString());
        addFilter(query, REPLAYIDF, replayId);
//        addFilter(query, SERVICEF, service.orElse(DEFAULT_EMPTY_FIELD_VALUE));
        service.ifPresent(servicev -> addFilter(query, SERVICEF, servicev));

        if(!byPath) {
            addFilter(query, PATHF, DEFAULT_EMPTY_FIELD_VALUE);
        }

        return SolrIterator.getStream(solr, query, Optional.empty()).flatMap(doc -> docToMatchResultAggregate(doc).stream());
    }

    /**
     * @param doc
     * @return
     */
    private Optional<MatchResultAggregate> docToMatchResultAggregate(SolrDocument doc) {
        Optional<String> json = getStrFieldMVFirst(doc, OBJJSONF);
        Optional<MatchResultAggregate> matchResultAggregate = json.flatMap(j -> {
            try {
                return Optional.ofNullable(config.jsonMapper.readValue(j, MatchResultAggregate.class));
            } catch (IOException e) {
                LOGGER.error(String.format("Not able to parse json into MatchResultAggregate object: %s", j), e);
                return Optional.empty();
            }
        });
        return matchResultAggregate;
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#saveAnalysis(com.cube.dao.Analysis)
     */
    @Override
    public boolean saveAnalysis(Analysis analysis, String customerId) {
        SolrInputDocument doc = analysisToSolrDoc(analysis, customerId);
        return saveDoc(doc) && softcommit();
    }

    /**
     * @param analysis
     * @return
     */
    private SolrInputDocument analysisToSolrDoc(Analysis analysis, String customerId) {
        final SolrInputDocument doc = new SolrInputDocument();

        String json="";
        try {
            json = config.jsonMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Analysis object into string for replay id %s", analysis.replayId), e);
        }

        String type = Types.Analysis.toString();
        String id = type + '-' + analysis.replayId;
        // the id field is set to replay id so that the document can be updated based on id
        doc.setField(IDF, id);
        doc.setField(REPLAYIDF, analysis.replayId);
        doc.setField(OBJJSONF, json);
        doc.setField(TYPEF, type);
        doc.setField(VERSIONF, analysis.templateVersion);
        doc.setField(CUSTOMERIDF, customerId);
        return doc;
    }

    private static final String RECORDREQIDF = CPREFIX + Constants.RECORD_REQ_ID_FIELD + STRING_SUFFIX;
    private static final String REPLAYREQIDF = CPREFIX + Constants.REPLAY_REQ_ID_FIELD + STRING_SUFFIX;
    private static final String REQMTF = CPREFIX + Constants.REQ_MATCH_TYPE + STRING_SUFFIX;
    private static final String NUMMATCHF = CPREFIX + "numMatch" + INT_SUFFIX;
    private static final String RESP_COMP_RES_TYPE_F = CPREFIX + Constants.RESP_MATCH_TYPE + STRING_SUFFIX; // match type
    private static final String RESP_COMP_RES_META_F = CPREFIX + "respMatchMetadata" + STRING_SUFFIX;
    private static final String MODIFIED_REC_RESP_PAYLOAD_F = CPREFIX + "recRespPayload" + STRING_SUFFIX;
    private static final String MODIFIED_REPLAY_RESP_PAYLOAD_F = CPREFIX + "replayRespPayload"  + STRING_SUFFIX;
    private static final String REQ_COMP_RES_TYPE_F = CPREFIX + Constants.REQ_COMP_RES_TYPE + STRING_SUFFIX;
    private static final String REQ_COMP_RES_META_F = CPREFIX + Constants.REQ_COMP_RES_META + STRING_SUFFIX;
    private static final String DIFFF = CPREFIX + "diff" + NOTINDEXED_SUFFIX;
    private static final String SERVICEF = CPREFIX + Constants.SERVICE_FIELD + STRING_SUFFIX;
    private static final String SERVICESF = CPREFIX + Constants.SERVICE_FIELD + STRINGSET_SUFFIX;
    private static final String RECORDTRACEIDF = CPREFIX + "recordtraceid" + STRING_SUFFIX;
    private static final String REPLAYTRACEIDF = CPREFIX + "replaytraceid" + STRING_SUFFIX;
    private static final String RECORD_SPANID_F = CPREFIX + "recordSpanId" + STRING_SUFFIX;
    private static final String REPLAY_SPANID_F = CPREFIX + "replaySpanId" + STRING_SUFFIX;
    private static final String RECORD_PARENT_SPANID_F = CPREFIX + "recordParentSpanId" + STRING_SUFFIX;
    private static final String REPLAY_PARENT_SPANID_F = CPREFIX + "replayParentSpanId" + STRING_SUFFIX;
    private static final String REPLAY_TYPE_F = CPREFIX + Constants.REPLAY_TYPE_FIELD + STRING_SUFFIX;

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#saveResult(com.cube.dao.Analysis.Result)
     */
    @Override
    public boolean saveResult(ReqRespMatchResult res, String customerId) {
        SolrInputDocument doc = resultToSolrDoc(res, customerId);
        return saveDoc(doc);
    }

    /**
     * @param res
     * @return
     */
    private SolrInputDocument resultToSolrDoc(ReqRespMatchResult res, String customerId) {
        final SolrInputDocument doc = new SolrInputDocument();

        // usually result will never be updated. But we set id field uniquely anyway

        String type = Types.ReqRespMatchResult.toString();
        // The recReplayReqIdCombined field is set to (recordReqId, replayReqId, replay) which is unique for each replay.
        String recReplayReqIdCombined =
            res.recordReqId.orElse("None") + '-' + res.replayReqId.orElse("None") + res.replayId;


        String id = type + '-' + Objects.hash(recReplayReqIdCombined);
        doc.setField(TYPEF, type);
        doc.setField(IDF, id);
        res.recordReqId.ifPresent(recordReqId ->  doc.setField(RECORDREQIDF, recordReqId));
        res.replayReqId.ifPresent(replayReqId ->  doc.setField(REPLAYREQIDF, replayReqId));
        doc.setField(SERVICEF, res.service);
        doc.setField(PATHF, res.path);
        doc.setField(REPLAYIDF, res.replayId);
        res.recordTraceId.ifPresent(traceId  -> doc.setField(RECORDTRACEIDF, traceId));
        res.replayTraceId.ifPresent(traceId  -> doc.setField(REPLAYTRACEIDF, traceId));
        res.recordedSpanId.ifPresent(spanId -> doc.setField(RECORD_SPANID_F, spanId));
        res.recordedParentSpanId.ifPresent(pSpanId -> doc.setField(RECORD_PARENT_SPANID_F, pSpanId));
        res.replayedSpanId.ifPresent(spanId -> doc.setField(REPLAY_SPANID_F, spanId));
        res.replayedParentSpanId.ifPresent(rSpanId -> doc.setField(REPLAY_PARENT_SPANID_F, rSpanId));
        doc.setField(REQMTF, res.reqMatchRes.toString());
        doc.setField(NUMMATCHF, res.numMatch);
        doc.setField(RESP_COMP_RES_TYPE_F, res.respCompareRes.mt.toString());
        doc.setField(RESP_COMP_RES_META_F, res.respCompareRes.matchmeta);
        doc.setField(CUSTOMERIDF, customerId);
        res.respCompareRes.recordedResponse.ifPresent(modifiedRecResponse -> {
            try {
                doc.setField(MODIFIED_REC_RESP_PAYLOAD_F
                    , config.jsonMapper.writeValueAsString(modifiedRecResponse));
            } catch(Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Conversion of Modified Recorded Response Payload to String failed")), e);
            }
        });

        res.respCompareRes.replayedResponse.ifPresent(modifiedReplayResponse -> {
            try {
                doc.setField(MODIFIED_REPLAY_RESP_PAYLOAD_F
                    , config.jsonMapper.writeValueAsString(modifiedReplayResponse));
            } catch(Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Conversion of Modified Replayed Response Payload to String failed")), e);
            }
        });
        AtomicInteger counter = new AtomicInteger(0);
        doc.addChildDocuments(res.respCompareRes.diffs.stream().map(diff ->
                diffToSolrDoc(diff, DiffType.Response, recReplayReqIdCombined
                    .concat(res.service).concat(res.path).concat(String
                        .valueOf(counter.getAndIncrement())), res.replayId, customerId))
            .collect(Collectors.toList()));
        counter.getAndSet(0);
        doc.addChildDocuments(res.reqCompareRes.diffs.stream().map(diff ->
        diffToSolrDoc(diff, DiffType.Request, recReplayReqIdCombined
            .concat(res.service).concat(res.path).concat(String
                .valueOf(counter.getAndIncrement())), res.replayId, customerId))
            .collect(Collectors.toList()));
        doc.addField(REQ_COMP_RES_TYPE_F, res.reqCompareRes.mt.toString());
        doc.addField(REQ_COMP_RES_META_F, res.reqCompareRes.matchmeta);
        return doc;
    }

    private static final String DIFF_VALUE_F = CPREFIX + Constants.DIFF_VALUE_FIELD + NOTINDEXED_SUFFIX;
    private static final String DIFF_FROM_STR_F = CPREFIX + Constants.DIFF_FROM_STR_FIELD + NOTINDEXED_SUFFIX;
    private static final String DIFF_FROM_VALUE_F = CPREFIX + Constants.DIFF_FROM_VALUE_FIELD  + NOTINDEXED_SUFFIX;
    private static final String DIFF_OP_F = CPREFIX + Constants.DIFF_OP_FIELD + STRING_SUFFIX;
    private static final String DIFF_PATH_F = CPREFIX + Constants.DIFF_PATH_FIELD + STRING_SUFFIX;
    private static final String DIFF_RESOLUTION_F = CPREFIX + Constants.DIFF_RESOLUTION_FIELD + STRING_SUFFIX;
    private static final String DIFF_TYPE_F = CPREFIX + Constants.DIFF_TYPE_FIELD + STRING_SUFFIX;

    private enum DiffType {
        Request, Response
    }

    private SolrInputDocument diffToSolrDoc(Diff diff, DiffType type, String idPrefix, String replayId, String customerId) {
        SolrInputDocument inputDocument = new SolrInputDocument();
        diff.value.ifPresent(val -> inputDocument
            .setField(DIFF_VALUE_F, val.toString()));
        diff.fromValue.ifPresent(fromVal -> inputDocument
            .setField(DIFF_FROM_VALUE_F, fromVal.toString()));
        diff.from.ifPresent(frm -> inputDocument.setField(DIFF_FROM_STR_F, frm));
        inputDocument.setField(DIFF_OP_F, diff.op);
        inputDocument.setField(DIFF_PATH_F, (diff.path != null)? diff.path.strip(): "");
        inputDocument.setField(DIFF_RESOLUTION_F, diff.resolution.name());
        inputDocument.setField(DIFF_TYPE_F, type.name());
        String id = Types.Diff.toString().concat("-").concat(
            String.valueOf(Objects.hash(idPrefix, diff.path, diff.op, type.name())));
        inputDocument.setField(IDF, id);
        inputDocument.setField(TYPEF, Types.Diff.toString());
        inputDocument.setField(REPLAYIDF, replayId);
        inputDocument.setField(CUSTOMERIDF, customerId);
        return inputDocument;
    }

    private Diff solrDocToDiff(SolrDocument doc) throws Exception {
        String op = getStrField(doc, DIFF_OP_F).orElseThrow(() ->
            new Exception("Operation not present"));
        String path = getStrField(doc, DIFF_PATH_F).orElseThrow(() ->
            new Exception("Path not present"));
        Resolution resolution = getStrField(doc, DIFF_RESOLUTION_F).flatMap(res ->
            Utils.valueOf(Resolution.class, res)).orElseThrow(() ->
            new Exception("Resolution Not Specified"));
        Optional<JsonNode> valNode = getStrFieldMVFirst(doc, DIFF_VALUE_F)
            .map(UtilException.rethrowFunction(config.jsonMapper::readTree));
        Optional<JsonNode> fromValueNode =  getStrFieldMVFirst(doc, DIFF_FROM_VALUE_F)
            .map(UtilException.rethrowFunction(config.jsonMapper::readTree));
        Optional<String> fromVal = getStrField(doc, DIFF_FROM_STR_F);

        return new Diff(op, path, valNode, fromVal, fromValueNode,resolution);
    }

    // TODO combine the next three functions into a single function which takes in
    // a single Analysis Res Query Specification
    /**
     * Get request/response match result (as computed by analysis) for a given recorded request and
     * replay Id combination. The assumption is there will be only one such result  in solr per request/replay.
     * Ideally the analysis should overwrite the result, if we perform analysis for the same replay.
     * @param recordReqId
     * @param replayId
     * @return
     */
    @Override
    public Optional<ReqRespMatchResult> getAnalysisMatchResult(String recordReqId
        , String replayId) {
            SolrQuery query = new SolrQuery("*:*");
            query.setFields("*");
            addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
            addFilter(query, RECORDREQIDF, recordReqId);
            addFilter(query, REPLAYIDF, replayId);
            query.addField(getDiffParentChildFilter());
            return SolrIterator.getSingleResult(solr, query)
                    .flatMap(doc -> docToAnalysisMatchResult(doc));
    }

    @Override
    public Optional<ReqRespMatchResult> getAnalysisMatchResult(Optional<String> recordReqId
        , Optional<String> replayReqId, String replayId) {
        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
        addFilter(query, RECORDREQIDF, recordReqId, true);
        addFilter(query, REPLAYREQIDF, replayReqId, true);
        addFilter(query, REPLAYIDF, replayId);
        query.addField(getDiffParentChildFilter());
        return SolrIterator.getSingleResult(solr, query)
            .flatMap(doc -> docToAnalysisMatchResult(doc));
    }

    private String getDiffParentChildFilter() {
        return "[child parentFilter=type_s:"+Types.ReqRespMatchResult.toString()
            +" childFilter=type_s:"+Types.Diff.toString()+" limit=-1]";
    }

    @Override
    public ReqRespResultsWithFacets
    getAnalysisMatchResults(AnalysisMatchResultQuery matchResQuery) {

        String queryString =
            "{!parent which=" + TYPEF + ":" + Types.ReqRespMatchResult.toString() + "}";

        String queryStringSansDiffFilter = queryString;

        if (matchResQuery.diffResolution.isPresent() || matchResQuery.diffJsonPath.isPresent() ||
            matchResQuery.diffType.isPresent()) {
            queryString = queryString.concat(" +(" + TYPEF + ":" + Types.Diff.toString() + ")");
        }

        queryString = queryString.concat(matchResQuery.diffResolution.map(res ->
            " +(" + DIFF_RESOLUTION_F + ":" + res + ")").orElse(""));
        queryString = queryString.concat(matchResQuery.diffJsonPath.map(res ->
            " +(" + DIFF_PATH_F + ":\"" + res + "\")").orElse(""));
        queryString = queryString.concat(matchResQuery.diffType.map(res ->
            " +(" + DIFF_TYPE_F + ":" + res + ")").orElse(""));

        SolrQuery query = new SolrQuery(queryString);
        query.setFields("*");
        // NOT NEEDED
        // addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
        addFilter(query, REPLAYIDF, matchResQuery.replayId);
        addFilter(query, SERVICEF, matchResQuery.service);
        addFilter(query, PATHF, matchResQuery.apiPaths);
        addFilter(query, REQMTF, matchResQuery.reqMatchType.map(Enum::toString));
        addFilter(query, RESP_COMP_RES_TYPE_F,
            matchResQuery.respCompResType.map(Enum::toString));
        addFilter(query, REQ_COMP_RES_TYPE_F,
            matchResQuery.reqCompResType.map(Enum::toString));
        matchResQuery.traceId.ifPresent(traceId ->
            query.addFilterQuery("(" + RECORDTRACEIDF + ":" + traceId + " OR "
                + REPLAYTRACEIDF + ":" + traceId + ")"));
        addFilter(query, RECORDREQIDF, matchResQuery.recordReqId);
        addFilter(query, REPLAYREQIDF, matchResQuery.replayReqId);
        addSort(query, IDF, true);
        query.addField(getDiffParentChildFilter());
        query.setFacetMinCount(1);


        Map domainBlockMap = new HashMap();
        domainBlockMap.put("blockChildren", "type_s: " + Types.ReqRespMatchResult.toString());
        Facet diffChildFacet = Facet.createTermFacetWithDomain(DIFF_RESOLUTION_F, Optional.of(domainBlockMap), Optional.empty());
        FacetQ facetq = new FacetQ();
        facetq.addFacet(DIFFRESOLUTIONFACET, diffChildFacet);

        String jsonFacets="";
        try {
            jsonFacets = config.jsonMapper.writeValueAsString(facetq);
            query.add(SOLRJSONFACETPARAM, jsonFacets);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting facets to json"), e);
        }

        Result<ReqRespMatchResult> result = SolrIterator.getResults(solr, query, matchResQuery.numMatches,
            this::docToAnalysisMatchResult, matchResQuery.start);
        ArrayList diffResolutionFacets = result.getFacets(FACETSFIELD, DIFFRESOLUTIONFACET, BUCKETFIELD);


        SolrQuery queryForServPathFacets = new SolrQuery(queryStringSansDiffFilter);
        queryForServPathFacets.setFields("*");
        addFilter(queryForServPathFacets, REPLAYIDF, matchResQuery.replayId);
        addFilter(queryForServPathFacets, SERVICEF, matchResQuery.service);

        Facet servicef = Facet.createTermFacet(SERVICEF, Optional.empty());
        Facet pathf = Facet.createTermFacet(PATHF, Optional.empty());

        Facet respMatchTypeFacets = Facet.createTermFacet(RESP_COMP_RES_TYPE_F, Optional.empty());
        Facet reqCompareTypeFacets = Facet.createTermFacet(REQ_COMP_RES_TYPE_F, Optional.empty());
        Facet reqMatchTypeFacets = Facet.createTermFacet(REQMTF, Optional.empty());


        FacetQ resolutionFacetsq = new FacetQ();
        resolutionFacetsq.addFacet(RESPMATCHTYPEFACET, respMatchTypeFacets);
        resolutionFacetsq.addFacet(REQCOMPAPARETYPEFACET, reqCompareTypeFacets);
        resolutionFacetsq.addFacet(REQMATCHTYPEFACET, reqMatchTypeFacets);
        pathf.addSubFacet(resolutionFacetsq);

        facetq.removeFacet(DIFFRESOLUTIONFACET);
        facetq.addFacet(SERVICEFACET, servicef);
        facetq.addFacet(PATHFACET, pathf);

        try {
            jsonFacets = config.jsonMapper.writeValueAsString(facetq);
            queryForServPathFacets.add(SOLRJSONFACETPARAM, jsonFacets);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting facets to json"), e);
        }

        Result<ReqRespMatchResult> resultsServPath = SolrIterator.getResults(solr, queryForServPathFacets, matchResQuery.numMatches,
                this::docToAnalysisMatchResult, matchResQuery.start);

        ArrayList serviceFacetResults = resultsServPath.getFacets(FACETSFIELD, SERVICEFACET, BUCKETFIELD);
        ArrayList pathFacetResults = resultsServPath.getFacets(FACETSFIELD, PATHFACET, BUCKETFIELD);
        pathFacetResults.forEach(pathFacetResult -> {
            HashMap respMatchTypeFacetMap = (HashMap) ((HashMap) pathFacetResult).get(RESPMATCHTYPEFACET);
            HashMap reqMatchTypeFacetMap = (HashMap) ((HashMap) pathFacetResult).get(REQMATCHTYPEFACET);
            HashMap reqCompareTypeFacetMap = (HashMap) ((HashMap) pathFacetResult).get(REQCOMPAPARETYPEFACET);
            ((HashMap)pathFacetResult).put(RESPMATCHTYPEFACET,
                resultsServPath.solrNamedPairToMap((ArrayList)respMatchTypeFacetMap.get(BUCKETFIELD)));
            ((HashMap)pathFacetResult).put(REQMATCHTYPEFACET,
                resultsServPath.solrNamedPairToMap((ArrayList)reqMatchTypeFacetMap.get(BUCKETFIELD)));
            ((HashMap)pathFacetResult).put(REQCOMPAPARETYPEFACET,
                resultsServPath.solrNamedPairToMap((ArrayList)reqCompareTypeFacetMap.get(BUCKETFIELD)));
        });

        return new ReqRespResultsWithFacets(result, diffResolutionFacets, serviceFacetResults, pathFacetResults);
    }

    @Override
    public ArrayList getServicePathHierarchicalFacets(String collectionId, RunType runType) {
        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, TYPEF, Types.Event.toString());
        addFilter(query, COLLECTIONF, collectionId);
        addFilter(query, RRTYPEF, runType.toString());

        FacetQ facetq = new FacetQ();

        Facet servicef = Facet.createTermFacet(SERVICEF, Optional.empty());

        FacetQ pathFacetsq = new FacetQ();
        Facet pathf = Facet.createTermFacet(PATHF, Optional.empty());
        pathFacetsq.addFacet(PATHFACET, pathf);
        servicef.addSubFacet(pathFacetsq);

        facetq.addFacet(SERVICEFACET, servicef);
        query.setFacetMinCount(1);

        String jsonFacets;
        try {
            jsonFacets = config.jsonMapper.writeValueAsString(facetq);
            query.add(SOLRJSONFACETPARAM, jsonFacets);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting facets to json"), e);
        }

        Result<Event> result = SolrIterator.getResults(solr, query, Optional.empty(),
            this::docToEvent, Optional.empty());

        ArrayList serviceFacetResults = result.getFacets(FACETSFIELD, SERVICEFACET, BUCKETFIELD);
        serviceFacetResults.forEach(serviceFacetResult -> {
            HashMap pathFacetMap = (HashMap) ((HashMap) serviceFacetResult).get(PATHFACET);
            ((HashMap)serviceFacetResult).put(PATHFACET,
                result.solrNamedPairToMap((ArrayList)pathFacetMap.get(BUCKETFIELD)));
        });

        return serviceFacetResults;


    }

    @Override
    public Result<ReqRespMatchResult> getAnalysisMatchResultOnlyNoMatch(String replayId) {
        SolrQuery query = new SolrQuery( REQMTF + ":" + Comparator.MatchType.NoMatch.toString()
            + " OR " + RESP_COMP_RES_TYPE_F + ":" + Comparator.MatchType.NoMatch.toString());
        query.setFields("*");
        addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
        addFilter(query, REPLAYIDF, replayId);

        return SolrIterator.getResults(solr, query, Optional.empty()
            , this::docToAnalysisMatchResult, Optional.empty());
    }

    @Override
    public boolean deleteReqResByTraceId(String traceId, String collectionName) {
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , TRACEIDF , traceId , false );
        addToQryStr(queryBuff , COLLECTIONF , collectionName ,false);
        addToQryStr(queryBuff , TYPEF , Types.Event.name() ,false);

        return deleteDocsByQuery(queryBuff.toString());
    }

    @Override
    public boolean deleteReqResByReqId(String reqId, String customerId, Optional<EventType> eventType) {

        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , REQIDF , reqId , false );
        addToQryStr(queryBuff , TYPEF , Types.Event.name() , false );
        addToQryStr(queryBuff , CUSTOMERIDF , customerId , false );


        if (eventType.isPresent()) {
            addToQryStr(queryBuff , EVENTTYPEF , eventType.get().name() , false);
        }

        return deleteDocsByQuery(queryBuff.toString());
    }

    @Override
    public boolean deleteAgentConfig(String customerId, String app, String service, String instanceId) {
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , TYPEF , Types.AgentConfig.name() , false );
        addToQryStr(queryBuff , CUSTOMERIDF , customerId , false );
        addToQryStr(queryBuff , APPF , app , false );
        addToQryStr(queryBuff , SERVICEF , service , false );
        addToQryStr(queryBuff , INSTANCEIDF , instanceId , false );
        return deleteDocsByQuery(queryBuff.toString());
    }


    private SolrInputDocument protoDescriptorDAOToSolrDoc(ProtoDescriptorDAO protoDescriptorDAO) {
        final SolrInputDocument doc = new SolrInputDocument();
        doc.setField(TYPEF, Types.ProtoDescriptor.toString());
        doc.setField(INT_VERSION_F, protoDescriptorDAO.version);
        doc.setField(CUSTOMERIDF, protoDescriptorDAO.customerId);
        doc.setField(APPF, protoDescriptorDAO.app);
        doc.setField(PROTO_DESCRIPTOR_FILE_F, protoDescriptorDAO.encodedFile);
        return doc;
    }

    private Optional<ProtoDescriptorDAO> docToProtoDescriptorDAO(SolrDocument doc) {
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<Integer> version = getIntField(doc, INT_VERSION_F);
        Optional<String> encodedFile = getStrFieldMVFirst(doc,PROTO_DESCRIPTOR_FILE_F);
        try {
            ProtoDescriptorDAO protoDescriptorDAO = new ProtoDescriptorDAO(customerId.orElse(null),
                app.orElse(null), encodedFile.orElse(null));
            protoDescriptorDAO.setVersion(version.orElse(0));
            ValidateProtoDescriptorDAO.validate(protoDescriptorDAO);
            return Optional.of(protoDescriptorDAO);
        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Data fields are null or empty in protoDescriptorDAO")), e);
        } catch (DescriptorValidationException | IOException e) {
            LOGGER.error(
                new ObjectMessage(Map.of(Constants.MESSAGE,
                    "Error decoding encoded file in Proto Descriptor or invalid Descriptor")), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean storeProtoDescriptorFile(ProtoDescriptorDAO protoDescriptorDAO) {
        Optional<ProtoDescriptorDAO> currentDoc = getLatestProtoDescriptorDAO(protoDescriptorDAO.customerId, protoDescriptorDAO.app);
        int maxVersion = currentDoc.map(cd -> cd.version).orElse(0);
        protoDescriptorDAO.setVersion(maxVersion+1);
        SolrInputDocument doc = protoDescriptorDAOToSolrDoc(protoDescriptorDAO);
        return saveDoc(doc) && softcommit();
    }

    @Override
    public Optional<ProtoDescriptorDAO> getLatestProtoDescriptorDAO(String customerId, String app) {

        SolrQuery maxVersionQuery = new SolrQuery("*:*");
        addFilter(maxVersionQuery, TYPEF, Types.ProtoDescriptor.toString());
        addFilter(maxVersionQuery, CUSTOMERIDF,  customerId);
        addFilter(maxVersionQuery, APPF, app);
        addSort(maxVersionQuery, INT_VERSION_F, false);

        return SolrIterator.getSingleResult(solr, maxVersionQuery).flatMap(this::docToProtoDescriptorDAO);

    }

    @Override
    public boolean deleteReqResByTraceId(String traceId , String customerId, String collectionName , Optional<EventType> eventType){
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , TRACEIDF , traceId , false);
        addToQryStr(queryBuff , TYPEF , Types.Event.name() , false);
        addToQryStr(queryBuff , CUSTOMERIDF , customerId , false);
        addToQryStr(queryBuff , COLLECTIONF , collectionName , false);

        if (eventType.isPresent()) {
            addToQryStr(queryBuff , EVENTTYPEF , eventType.get().name() , false);
        }

        return deleteDocsByQuery(queryBuff.toString());
    }

    /*
    @Override
    public boolean deleteReqResp(EventQuery query) {

        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , TYPEF , Types.Event.name() , false);
        if(query.getCustomerId()!=null){
            addToQryStr(queryBuff , CUSTOMERIDF , query.getCustomerId() , false);
        }
        addToQryStr(queryBuff , REQIDF , query.getReqIds() , false);
        addToQryStr(queryBuff , EVENTTYPEF , query.getEventTypes().stream().map(null).collect(Collectors.toList()) , false);

        if (eventType.isPresent()) {
            addToQryStr(queryBuff , EVENTTYPEF , eventType.get().name() , false);
        }

        return deleteDocsByQuery(queryBuff.toString());
    }
    */


    /**
     * Convert Solr document to corresponding ReqRespMatchResult object
     * @param doc
     * @return
     */
    private Optional<ReqRespMatchResult> docToAnalysisMatchResult(SolrDocument doc) {
        try {
            Optional<String> recordReqId = getStrField(doc, RECORDREQIDF);
            Optional<String> replayReqId = getStrField(doc, REPLAYREQIDF);
            String replayId = getStrField(doc, REPLAYIDF).orElse("");

            Comparator.MatchType reqMatchType = getStrField(doc, REQMTF)
                .map(Comparator.MatchType::valueOf).orElse(Comparator.MatchType.Default);

            Comparator.MatchType respMatchType = getStrField(doc, RESP_COMP_RES_TYPE_F)
                .map(Comparator.MatchType::valueOf).orElse(Comparator.MatchType.Default);
            String respMatchMetaData = getStrField(doc, RESP_COMP_RES_META_F).orElse("");

            List<Diff> respMatchDiffList =  getDiffFromChildDocs(doc, DiffType.Response);

            Optional<JsonNode> modifiedRecRespPayload =
                getStrField(doc, MODIFIED_REC_RESP_PAYLOAD_F).map(modifiedRespPayloadStr ->
                {
                    try {
                        return config.jsonMapper.readValue(modifiedRespPayloadStr
                            , JsonNode.class);
                    } catch (IOException e) {
                        LOGGER.error("Error while reading modified recorded resp "
                            + "payload from solr ", e);
                        return null;
                    }
                });

            Optional<JsonNode> modifiedReplayRespPayload =
                getStrField(doc, MODIFIED_REPLAY_RESP_PAYLOAD_F).map(modifiedRespPayloadStr ->
                {
                    try {
                        return config.jsonMapper.readValue(modifiedRespPayloadStr
                            , JsonNode.class);
                    } catch (IOException e) {
                        LOGGER.error("Error while reading modified recorded resp "
                            + "payload from solr ", e);
                        return null;
                    }
                });

            Match respMatch = new Match(respMatchType, respMatchMetaData, respMatchDiffList
                , modifiedRecRespPayload , modifiedReplayRespPayload);

            Optional<Match> reqCompResOptional = getStrField(doc, REQ_COMP_RES_TYPE_F)
                .map(Comparator.MatchType::valueOf).map(UtilException.rethrowFunction(
                    reqCompResType -> { String reqCompResMeta = getStrField(doc
                        , REQ_COMP_RES_META_F).orElse("");
                List<Diff> reqCompDiffList = getDiffFromChildDocs(doc, DiffType.Request);
                return new Match(reqCompResType, reqCompResMeta, reqCompDiffList);
            }));
            Integer numMatch = getIntField(doc, NUMMATCHF).orElse(-1);
            String service = getStrField(doc, SERVICEF).orElse("");
            String path = getStrField(doc, PATHF).orElse("");
            Optional<String> recordTraceId = getStrField(doc, RECORDTRACEIDF);
            Optional<String> replayTraceId = getStrField(doc, REPLAYTRACEIDF);
            Optional<String> recordSpanId = getStrField(doc, RECORD_SPANID_F);
            Optional<String> recordParentSpandId = getStrField(doc, RECORD_PARENT_SPANID_F);
            Optional<String> replaySpanId = getStrField(doc, REPLAY_SPANID_F);
            Optional<String> replayParentSpanId = getStrField(doc, REPLAY_PARENT_SPANID_F);

            return Optional.of(new ReqRespMatchResult(recordReqId, replayReqId, reqMatchType
                , numMatch, replayId, service, path, recordTraceId, replayTraceId, recordSpanId,
                recordParentSpandId, replaySpanId, replayParentSpanId, respMatch
                , reqCompResOptional.orElse(Match.DONT_CARE)));
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                "Unable to convert solr doc to diff")), e);
            return Optional.empty();
        }
    }

    private List<Diff> getDiffFromChildDocs(SolrDocument doc, DiffType diffType) throws Exception {
        if (doc.getChildDocuments() == null) return Collections.emptyList();
        return doc.getChildDocuments().stream().filter(childDoc ->
            diffType.name().equals(getStrField(childDoc, DIFF_TYPE_F).orElse("")))
            .map(UtilException.rethrowFunction(this::solrDocToDiff))
            .collect(Collectors.toList());
    }


    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getAnalysis(java.lang.String)
     */
    @Override
    public Optional<Analysis> getAnalysis(String replayId) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        //query.setRows(1);
        addFilter(query, TYPEF, Types.Analysis.toString());
        addFilter(query, REPLAYIDF, replayId);

        return SolrIterator.getSingleResult(solr, query).flatMap(doc -> docToAnalysis(doc, this));

    }

    /**
     * @param doc
     * @param rrstore
     * @return
     */
    private Optional<Analysis> docToAnalysis(SolrDocument doc, ReqRespStoreSolr rrstore) {

        Optional<String> json = getStrFieldMV(doc, OBJJSONF).stream().findFirst();
        Optional<Analysis> analysis = json.flatMap(j -> {
            try {
                return Optional.ofNullable(config.jsonMapper.readValue(j, Analysis.class));
            } catch (IOException e) {
                LOGGER.error(String.format("Not able to parse json into Analysis object: %s", j), e);
                return Optional.empty();
            }
        });
        return analysis;
    }

    private SolrQuery getEventQuery(ApiTraceFacetQuery apiTraceFacetQuery) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Event.toString());
        addFilter(query, CUSTOMERIDF, apiTraceFacetQuery.customerId);
        addFilter(query, APPF, apiTraceFacetQuery.appId);
        addFilter(query, INSTANCEIDF, apiTraceFacetQuery.instanceId);
        addRangeFilter(query, TIMESTAMPF, apiTraceFacetQuery.startDate,
            apiTraceFacetQuery.endDate, true, false);
        boolean includeEmpty = apiTraceFacetQuery.recordingType
                .map(v -> v.equals(RecordingType.Golden.toString()))
                .orElse(false);
        addFilter(query, RECORDING_TYPE_F, apiTraceFacetQuery.recordingType, true, includeEmpty);
        addFilter(query, COLLECTIONF,apiTraceFacetQuery.collections);
        //addFilter(query, PATHF, apiTraceFacetQuery.apiPath);
        addFilter(query, RUNIDF, apiTraceFacetQuery.runIds);
        return query;
    }

    @Override
    public ArrayList getApiFacets(ApiTraceFacetQuery apiTraceFacetQuery) {
        final SolrQuery query = getEventQuery(apiTraceFacetQuery);
        addFilter(query, SERVICEF,apiTraceFacetQuery.service);
        addFilter(query,PATHF, apiTraceFacetQuery.apiPath);
        addFilter(query, EVENTTYPEF, EventType.HTTPRequest.toString());
        FacetQ serviceFacetq = new FacetQ();
        Facet servicef = Facet.createTermFacet(SERVICEF, Optional.empty());

        FacetQ pathFacetq = new FacetQ();
        Facet pathf = Facet.createTermFacet(PATHF, Optional.empty());

        FacetQ instanceIdFacetq = new FacetQ();
        Facet instanceIdf = Facet.createTermFacet(INSTANCEIDF, Optional.empty());
        instanceIdFacetq.addFacet(INSTANCEFACET,instanceIdf);

        pathf.addSubFacet(instanceIdFacetq);
        pathFacetq.addFacet(PATHFACET,pathf);

        servicef.addSubFacet(pathFacetq);
        serviceFacetq.addFacet(SERVICEFACET, servicef);

        query.setFacetMinCount(1);
        String jsonFacets;
        try {
            jsonFacets = config.jsonMapper.writeValueAsString(serviceFacetq);
            query.add(SOLRJSONFACETPARAM, jsonFacets);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting facets to json"), e);
        }
        Result<Event> result = SolrIterator.getResults(solr, query, Optional.empty(),
            this::docToEvent, Optional.empty());
        ArrayList serviceFacetResults = result.getFacets(FACETSFIELD, SERVICEFACET, BUCKETFIELD);
        serviceFacetResults.forEach(serviceFacetResult -> {
            HashMap pathFacetMap = (HashMap) ((HashMap) serviceFacetResult).get(PATHFACET);
            ArrayList pathFacetResults = result.solrNamedPairToMap((ArrayList) pathFacetMap.get(BUCKETFIELD));

            pathFacetResults.forEach(pathFacetResult -> {
                HashMap instanceFacetMap = (HashMap) ((HashMap) pathFacetResult).get(INSTANCEFACET);
                ((HashMap)pathFacetResult).put(INSTANCEFACET,
                    result.solrNamedPairToMap((ArrayList)instanceFacetMap.get(BUCKETFIELD)));
            });

            ((HashMap)serviceFacetResult).put(PATHFACET,pathFacetResults);
        });
        return serviceFacetResults;
    }

    @Override
    public Result<Event> getApiTrace(ApiTraceFacetQuery apiTraceFacetQuery, Optional<Integer> start, Optional<Integer> numberOfResults, List<EventType> eventTypes, boolean addPathServiceFilter) {

        final SolrQuery query = getEventQuery(apiTraceFacetQuery);
        addFilter(query, EVENTTYPEF, eventTypes.stream().map(type -> type.toString()).collect(Collectors.toList()));
        addFilter(query, TRACEIDF, apiTraceFacetQuery.traceIds);
        if (addPathServiceFilter) {
            if(apiTraceFacetQuery.service.isEmpty() && apiTraceFacetQuery.apiPath.isEmpty()) {
                addToFilterOrQuery(query , new StringBuffer() , PARENT_SPAN_ID_F , Arrays.asList("NA", ""), true , Optional.empty());
            }
            addFilter(query, PATHF, apiTraceFacetQuery.apiPath);
            addFilter(query, SERVICEF, apiTraceFacetQuery.service);
        }
        addSort(query, TIMESTAMPF, false /* desc */);
        addSort(query, IDF, true);

        return SolrIterator.getResults(solr, query, numberOfResults,
            this::docToEvent, start);
    }


    private static final String RECORDINGSTATUSF = CPREFIX + Constants.STATUS + STRING_SUFFIX;
    private static final String RECORDING_TYPE_F = CPREFIX + Constants.RECORDING_TYPE_FIELD + STRING_SUFFIX;
    private static final String ROOT_RECORDING_IDF = CPREFIX + Constants.ROOT_RECORDING_FIELD + STRING_SUFFIX;
    private static final String PARENT_RECORDING_IDF = CPREFIX + Constants.PARENT_RECORDING_FIELD + STRING_SUFFIX;
    private static final String GOLDEN_NAMEF = CPREFIX + Constants.GOLDEN_NAME_FIELD + STRING_SUFFIX;
    private static final String RECORDING_IDF = CPREFIX + Constants.RECORDING_ID + STRING_SUFFIX;
    private static final String GOLDEN_LABELF = CPREFIX + Constants.GOLDEN_LABEL_FIELD + STRING_SUFFIX;
    private static final String CODE_VERSIONF = CPREFIX + Constants.CODE_VERSION_FIELD + STRING_SUFFIX;
    private static final String BRANCHF = CPREFIX + Constants.BRANCH_FIELD + STRING_SUFFIX;
    private static final String TAGSF = CPREFIX + Constants.TAGS_FIELD + STRINGSET_SUFFIX;
    private static final String ARCHIVEDF = CPREFIX + Constants.ARCHIVED_FIELD + BOOLEAN_SUFFIX;
    private static final String GIT_COMMIT_IDF = CPREFIX + Constants.GIT_COMMIT_ID_FIELD + STRING_SUFFIX;
    private static final String COLLECTION_UPD_OP_SET_IDF = CPREFIX + Constants.COLLECTION_UPD_OP_SET_ID_FIELD + STRING_SUFFIX;
    private static final String TEMPLATE_UPD_OP_SET_IDF = CPREFIX + Constants.TEMPLATE_UPD_OP_SET_ID_FIELD + STRING_SUFFIX;
    private static final String GOLDEN_COMMENTF = CPREFIX + Constants.GOLDEN_COMMENT_FIELD + TEXT_SUFFIX;
    private static final String GENERATED_CLASS_JAR_PATH = CPREFIX +  Constants.GENERATED_CLASS_JAR_PATH_FIELD + STRING_SUFFIX;

    private Optional<CustomerAppConfig> docToCustomerAppConfig(SolrDocument doc){
        final Optional<String> tracer = getStrField(doc, TRACERF);

        CustomerAppConfig.Builder builder = new CustomerAppConfig.Builder();
        tracer.ifPresent(builder::withTracer);

        return Optional.of(builder.build());
    }
    private Optional<Recording> docToRecording(SolrDocument doc) {

        Optional<String> id = getStrField(doc, IDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> instanceId = getStrField(doc, INSTANCEIDF);
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<RecordingStatus> status = getStrField(doc, RECORDINGSTATUSF)
            .flatMap(s -> Utils.valueOf(RecordingStatus.class, s));
        Optional<Recording> recording = Optional.empty();
        Optional<String> templateVersion = getStrField(doc, VERSIONF);
        Optional<String> parentRecordingId = getStrField(doc, PARENT_RECORDING_IDF);
        Optional<String> rootRecordingId = getStrField(doc, ROOT_RECORDING_IDF);
        Optional<String> name = getStrField(doc, GOLDEN_NAMEF);
        Optional<String> label = getStrField(doc, GOLDEN_LABELF);
        Optional<String> codeVersion = getStrField(doc, CODE_VERSIONF);
        Optional<String> branch = getStrField(doc, BRANCHF);
        List<String> tags = getStrFieldMV(doc, TAGSF);
        Optional<Boolean> archived = getBoolField(doc, ARCHIVEDF);
        Optional<String> gitCommitId = getStrField(doc, GIT_COMMIT_IDF);
        Optional<String> collectionUpdOpSetId = getStrField(doc, COLLECTION_UPD_OP_SET_IDF);
        Optional<String> templateUpdOpSetId = getStrField(doc, TEMPLATE_UPD_OP_SET_IDF);
        Optional<String> comment = getStrField(doc, GOLDEN_COMMENTF);
        Optional<String> userId = getStrField(doc, USERIDF);
        Optional<String> generatedClassJarPath = getStrField(doc, GENERATED_CLASS_JAR_PATH);
        Optional<RecordingType> recordingType = getStrField(doc, RECORDING_TYPE_F)
            .flatMap(r -> Utils.valueOf(RecordingType.class, r));
        Optional<String> dynamicInjectionConfigVersion = getStrField(doc , DYNAMIC_INJECTION_CONFIG_VERSIONF);
        Optional<String> runId = getStrField(doc , RUNIDF);

        if (id.isPresent() && customerId.isPresent() && app.isPresent() && instanceId.isPresent() && collection
            .isPresent() &&
            status.isPresent() && templateVersion.isPresent() && archived.isPresent() && name
            .isPresent() && userId.isPresent()) {
            RecordingBuilder recordingBuilder = new RecordingBuilder(
                customerId.get(), app.get(), instanceId.get(), collection.get())
                .withStatus(status.get()).withTemplateSetVersion(templateVersion.get())
                .withName(name.get()).withArchived(archived.get()).withUserId(userId.get())
                .withTags(tags)
                .withId(id.get()); // existing recording, so carry over id
            getTSField(doc, TIMESTAMPF).ifPresent(recordingBuilder::withUpdateTimestamp);
            parentRecordingId.ifPresent(recordingBuilder::withParentRecordingId);
            rootRecordingId.ifPresent(recordingBuilder::withRootRecordingId);
            codeVersion.ifPresent(recordingBuilder::withCodeVersion);
            branch.ifPresent(recordingBuilder::withBranch);
            gitCommitId.ifPresent(recordingBuilder::withGitCommitId);
            collectionUpdOpSetId.ifPresent(recordingBuilder::withCollectionUpdateOpSetId);
            templateUpdOpSetId.ifPresent(recordingBuilder::withTemplateUpdateOpSetId);
            comment.ifPresent(recordingBuilder::withComment);
            label.ifPresent(recordingBuilder::withLabel);
            recordingType.ifPresent(recordingBuilder::withRecordingType);
            dynamicInjectionConfigVersion.ifPresent(recordingBuilder::withDynamicInjectionConfigVersion);
            runId.ifPresent(recordingBuilder::withRunId);

            try {
                generatedClassJarPath.ifPresent(
                    UtilException.rethrowConsumer(recordingBuilder::withGeneratedClassJarPath));
                recording = Optional.of(recordingBuilder.build());
            } catch (Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Not able to convert Solr result to Recording object",
                    Constants.CUSTOMER_ID_FIELD, customerId.orElse(""),
                    Constants.APP_FIELD, app.orElse(""),
                    Constants.INSTANCE_ID_FIELD, instanceId.orElse("")
                )), e);
            }
        } else {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Not able to convert Solr result to Recording object",
                Constants.ERROR, "One of required fields missing",
                Constants.CUSTOMER_ID_FIELD, customerId.orElse(""),
                Constants.APP_FIELD, app.orElse(""),
                Constants.INSTANCE_ID_FIELD, instanceId.orElse("")
            )));
        }

        return recording;
    }

    private static SolrInputDocument recordingToSolrDoc(Recording recording) {
        final SolrInputDocument doc = new SolrInputDocument();

        String type = Types.Recording.toString();
        // the id field is to (cust app, collection) which is unique
        String id = recording.getId();
        doc.setField(TYPEF, type);
        doc.setField(IDF, id);
        doc.setField(CUSTOMERIDF, recording.customerId);
        doc.setField(APPF, recording.app);
        doc.setField(INSTANCEIDF, recording.instanceId);
        doc.setField(COLLECTIONF, recording.collection);
        doc.setField(RECORDINGSTATUSF, recording.status.toString());
        doc.setField(VERSIONF, recording.templateVersion);
        doc.setField(ROOT_RECORDING_IDF, recording.rootRecordingId);
        doc.setField(ARCHIVEDF, recording.archived);
        doc.setField(GOLDEN_NAMEF, recording.name);
        doc.setField(GOLDEN_LABELF, recording.label);
        doc.setField(USERIDF, recording.userId);
        doc.setField(RECORDING_TYPE_F, recording.recordingType.toString());
        doc.setField(RUNIDF, recording.runId);
        recording.parentRecordingId.ifPresent(parentRecId -> doc.setField(PARENT_RECORDING_IDF, parentRecId));
        recording.generatedClassJarPath.ifPresent(jarPath -> doc.setField(GENERATED_CLASS_JAR_PATH, jarPath));
        recording.updateTimestamp.ifPresent(timestamp -> doc.setField(TIMESTAMPF , timestamp.toString()));
        recording.codeVersion.ifPresent(cv -> doc.setField(CODE_VERSIONF, cv));
        recording.branch.ifPresent(branch -> doc.setField(BRANCHF, branch));
        recording.tags.forEach(tag -> doc.addField(TAGSF, tag));
        recording.gitCommitId.ifPresent(gitCommitId -> doc.setField(GIT_COMMIT_IDF,gitCommitId));
        recording.collectionUpdOpSetId.ifPresent(c -> doc.setField(COLLECTION_UPD_OP_SET_IDF, c));
        recording.templateUpdOpSetId.ifPresent(t -> doc.setField(TEMPLATE_UPD_OP_SET_IDF, t));
        recording.comment.ifPresent(comment -> doc.setField(GOLDEN_COMMENTF, comment));
        recording.dynamicInjectionConfigVersion.ifPresent(diCfgVer -> doc.setField(DYNAMIC_INJECTION_CONFIG_VERSIONF , diCfgVer));
        return doc;
    }


    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.dao.Replay)
     */
    public boolean saveRecording(Recording recording) {
        SolrInputDocument doc = recordingToSolrDoc(recording);
        return saveDoc(doc) && softcommit();
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRecording(java.util.Optional, java.util.Optional, java.util.Optional, com.cube.dao.Recording.RecordingStatus)
     */
    @Override
    public Result<Recording> getRecording(Optional<String> customerId, Optional<String> app, Optional<String> instanceId, Optional<RecordingStatus> status,
        Optional<String> collection, Optional<String> templateVersion, Optional<String> name, Optional<String> parentRecordingId, Optional<String> rootRecordingId,
        Optional<String> codeVersion, Optional<String> branch, List<String> tags, Optional<Boolean> archived, Optional<String> gitCommitId,
        Optional<String> collectionUpdOpSetId, Optional<String> templateUpdOpSetId, Optional<String> userId, Optional<String> label, Optional<String> recordingType,
        Optional<String> recordingId, Optional<Integer> numberOfResults, Optional<Integer> start) {

        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, INSTANCEIDF, instanceId);
        addFilter(query, RECORDINGSTATUSF, status.map(Enum::toString));
        addFilter(query, COLLECTIONF, collection);
        addFilter(query, VERSIONF, templateVersion);
        addFilter(query, PARENT_RECORDING_IDF, parentRecordingId);
        addFilter(query, ROOT_RECORDING_IDF, rootRecordingId);
        addFilter(query, GOLDEN_NAMEF, name);
        addFilter(query, GOLDEN_LABELF, label);
        addFilter(query, CODE_VERSIONF, codeVersion);
        addFilter(query, BRANCHF, branch);
        addFilter(query, ARCHIVEDF, archived.map(a -> a.toString()));
        addFilter(query, GIT_COMMIT_IDF, gitCommitId);
        addFilter(query, TAGSF, tags);
        addFilter(query, COLLECTION_UPD_OP_SET_IDF, collectionUpdOpSetId);
        addFilter(query, TEMPLATE_UPD_OP_SET_IDF, templateUpdOpSetId);
        addFilter(query, USERIDF, userId);
        addFilter(query, IDF, recordingId);
        boolean includeEmpty = recordingType.map(v -> v.equals(RecordingType.Golden.toString()))
                .orElse(false);
        addFilter(query, RECORDING_TYPE_F, recordingType, true, includeEmpty);
        addSort(query, TIMESTAMPF, false); // descending
        addSort(query, IDF, true);

        //Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getResults(solr, query, numberOfResults,
            this::docToRecording, start);
    }

    @Override
    public Optional<Recording> getRecording(String recordingId) {
        SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, IDF, recordingId);
        return SolrIterator.getSingleResult(solr, query).flatMap(doc -> docToRecording(doc));
    }

    @Override
    public boolean deleteAllRecordingData(Recording recording) {
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , COLLECTIONF , recording.collection ,false);
        addToQryStr(queryBuff , TYPEF , Types.Recording.name(), false);
        boolean deleteRecording =  deleteDocsByQuery(queryBuff.toString());
        if(deleteRecording) {
            return deleteEventsByCollection(List.of(recording.collection));
        }
        return deleteRecording;
    }

    public boolean deleteEventsByCollection(List<String> collections) {
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , COLLECTIONF , collections ,false, Optional.empty());
        addToQryStr(queryBuff , TYPEF , Types.Event.name(), false);
        return deleteDocsByQuery(queryBuff.toString());
    }

    @Override
    public boolean deleteAllReplayData(List<Replay> replays) {
        StringBuffer queryBuff = new StringBuffer();
        List<String> replayIds = replays.stream().map(replay -> replay.replayId).collect(Collectors.toList());
        addToQryStr(queryBuff , REPLAYIDF ,  replayIds ,false, Optional.empty());
        addToQryStr(queryBuff , TYPEF , Types.ReplayMeta.name(), false);

         boolean deleteReplay = deleteDocsByQuery(queryBuff.toString());
         if(deleteReplay) {
             deleteEventsByCollection(replayIds);
             return deleteAllAnalysisData(replayIds);
         }
         return deleteReplay;
    }

    @Override
    public boolean deleteAllAnalysisData(List<String> replayIds) {
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , REPLAYIDF ,  replayIds ,false, Optional.empty());
        addToQryStr(queryBuff , TYPEF ,
            List.of(Types.Analysis.name(), Types.MatchResultAggregate.name()),
            false, Optional.empty());

        boolean analysisDeleted = deleteDocsByQuery(queryBuff.toString());
        if(analysisDeleted) {
            final SolrQuery query = new SolrQuery("*:*");
            query.addField("*");
            addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
            addFilter(query, REPLAYIDF, replayIds);

            BatchingIterator.batchedStreamOf(SolrIterator.getStream(solr, query, Optional.empty()), 200)
                .forEach(docs -> {
                    List<String> ids = docs.stream().flatMap(doc -> getStrField(doc, IDF).stream()).collect(Collectors.toList());
                    deleteReqRespMatchResults(ids);
                });
        }
        return analysisDeleted;
    }

    @Override
    public boolean deleteAllData(String customerId) {
        StringBuffer queryBuff = new StringBuffer();
        addToQryStr(queryBuff , CUSTOMERIDF , customerId ,false);
        return deleteDocsByQuery(queryBuff.toString());
    }

    public boolean deleteReqRespMatchResults(List<String> ids) {
        try {
            solr.deleteById(ids);
            return softcommit();
        } catch(Exception e) {
            LOGGER.error("Error in deleting ReqRespMatchResults from solr for ids " +
                ids.toString(), e);
            return false;
        }
    }

    @Override
    public Optional<CustomerAppConfig> getAppConfiguration(String customerId , String app) {

        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.CustomerAppConfig.toString());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        return SolrIterator.getSingleResult(solr, query).flatMap(this::docToCustomerAppConfig);
    }


    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRecordingByCollection(java.lang.String, java.lang.String, java.lang.String)
     * (cust, app, collection) is a unique key, so only record will satisfy at most
     */
    @Override
    public Optional<Recording> getRecordingByCollectionAndTemplateVer(String customerId, String app,
                                                        String collection, Optional<String> templateSetVersion) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, COLLECTIONF, collection);
        addFilter(query, VERSIONF, templateSetVersion);
        return SolrIterator.getSingleResult(solr, query).flatMap(doc -> docToRecording(doc));
    }

    @Override
    public Optional<Recording> getRecordingByName(String customerId, String app, String name, Optional<String> label) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, GOLDEN_NAMEF, name);
        label.ifPresentOrElse( l -> addFilter(query, GOLDEN_LABELF, l), () -> {addSort(query, TIMESTAMPF, false); addSort(query, IDF, true);});
        return SolrIterator.getSingleResult(solr, query).flatMap(doc -> docToRecording(doc));
    }


    @Override
    public String saveDynamicInjectionConfig(DynamicInjectionConfig dynamicInjectionConfig)
        throws SolrStoreException {
        SolrInputDocument solrDoc = dynamicInjectionConfigToSolrDoc(dynamicInjectionConfig);
        boolean success = saveDoc(solrDoc) && softcommit();
        if(!success) {
            throw new SolrStoreException("Error saving Injection config in Solr");
        }
        return solrDoc.getFieldValue(IDF).toString();

    }

    public SolrInputDocument dynamicInjectionConfigToSolrDoc(
        DynamicInjectionConfig dynamicInjectionConfig) {
        String extractionMetas;
        String injectionMetas;

        final SolrInputDocument doc = new SolrInputDocument();
        try {
            extractionMetas = this.config.jsonMapper.writeValueAsString(dynamicInjectionConfig.extractionMetas);
            injectionMetas = this.config.jsonMapper.writeValueAsString(dynamicInjectionConfig.injectionMetas);
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE,
                "Unable to convert extraction/injection metas to string")), e);
            return doc;
        }

        String type = Types.DynamicInjectionConfig.name();
        String id = type.concat("-").concat(String.valueOf(Objects.hash(
            dynamicInjectionConfig.customerId, dynamicInjectionConfig.app , dynamicInjectionConfig.version)));
        doc.setField(IDF , id);
        doc.setField(EXTRACTION_METAS_JSON , extractionMetas);
        doc.setField(INJECTION_METAS_JSON , injectionMetas);
        doc.setField(APPF , dynamicInjectionConfig.app);
        doc.setField(CUSTOMERIDF , dynamicInjectionConfig.customerId);
        doc.setField(DYNAMIC_INJECTION_CONFIG_VERSIONF, dynamicInjectionConfig.version);
        doc.setField(TIMESTAMPF , dynamicInjectionConfig.timestamp.toString());
        doc.setField(TYPEF , type);
        return doc;
    }


    @Override
    public Optional<DynamicInjectionConfig> getDynamicInjectionConfig(String customerId,
                                                                      String app,
                                                                      String version) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.DynamicInjectionConfig.name());
        addFilter(query, CUSTOMERIDF, customerId);
        addFilter(query, APPF, app);
        addFilter(query, DYNAMIC_INJECTION_CONFIG_VERSIONF, version, true);
        return SolrIterator.getSingleResult(solr, query)
            .flatMap(this::docToDynamicInjectionConfig);
    }

    private Optional<DynamicInjectionConfig> docToDynamicInjectionConfig(SolrDocument doc) {
        Optional<List<ExtractionMeta>> extractionMetas = getStrField(doc, EXTRACTION_METAS_JSON).flatMap(em -> {
            try {
                return Optional.of(config.jsonMapper.readValue(em, new TypeReference<List<ExtractionMeta>>(){}));
            } catch (IOException e) {
                LOGGER.error("Error while reading ExtractionMeta object from json :: " + getIntField(doc , IDF).orElse(-1),e);
                return Optional.empty();
            }
            catch (Exception e) {
                LOGGER.error("Error while reading ExtractionMeta object from json :: " + getIntField(doc , IDF).orElse(-1),e);
                return Optional.empty();
            }
        });
        Optional<List<InjectionMeta>> injectionMetas = getStrField(doc, INJECTION_METAS_JSON).flatMap(im -> {
            try {
                return Optional.of(config.jsonMapper.readValue(im, new TypeReference<List<InjectionMeta>>(){}));
            } catch (IOException e) {
                LOGGER.error("Error while reading InjectionMeta object from json :: " + getIntField(doc , IDF).orElse(-1),e);
                return Optional.empty();
            }
            catch (Exception e) {
                LOGGER.error("Error while reading InjectionMeta object from json :: " + getIntField(doc , IDF).orElse(-1),e);
                return Optional.empty();
            }
        });

        Optional<DynamicInjectionConfig> dynamicInjectionConfig = Optional.empty();
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> version = getStrField(doc, DYNAMIC_INJECTION_CONFIG_VERSIONF);
        Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);

        if(app.isPresent() && customerId.isPresent() && version.isPresent() && extractionMetas.isPresent() && injectionMetas.isPresent()) {
            dynamicInjectionConfig = Optional
                .of(new DynamicInjectionConfig(version.get(), customerId.get(), app.get(), timestamp, extractionMetas.get(),
                    injectionMetas.get()));
        }
        else {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Not able to convert Solr result to DynamicInjectionConfig object",
                Constants.ERROR, "One of required fields missing",
                Constants.CUSTOMER_ID_FIELD, customerId.orElse(""),
                Constants.APP_FIELD, app.orElse(""),
                Constants.DYNACMIC_INJECTION_CONFIG_VERSION_FIELD, version.orElse("")
            )));
        }

        return dynamicInjectionConfig;
    }

    public static class SolrStoreException extends Exception {
        public SolrStoreException(String message) {
            super(message);
        }
    }

    private final static int FACETLIMIT = 500;
    private static final String REQMTFACET = "reqmt_facets";
    private static final String RESPMTFACET = "respmt_facets";
    private static final String PATHFACET = "path_facets";
    private static final String SERVICEFACET = "service_facets";
    private static final String TRACEIDFACET = "traceId_facets";
    private static final String INSTANCEFACET = "instance_facets";
    private static final String TAGFACET = "tag_facets";
    private static final String SOLRJSONFACETPARAM = "json.facet"; // solr facet query param
    private static final String BUCKETFIELD = "buckets"; // term in solr results indicating facet buckets
    private static final String MISSINGBUCKETFIELD = "missing"; // term in solr results indicating facet bucket for
    // missing value
    private static final String VALFIELD = "val"; // term in solr facet results indicating a distinct value of the field
    private static final String COUNTFIELD = "count"; // term in solr facet results indicating aggregate value computed
    private static final String FACETSFIELD = "facets"; // term in solr facet results indicating the facet results block
    private static final String DIFFRESOLUTIONFACET = "diff_resolution_facets";
    private static final String RESPMATCHTYPEFACET = "respMatchType_facets";
    private static final String REQMATCHTYPEFACET = "reqMatchType_facets";
    private static final String REQCOMPAPARETYPEFACET = "reqCmpResType_facets";
    private static final String SAMPLINGFACET = "sampling_facets";


    /**
     * Results needed in one instance of MatchResultAggregate come as part of different facets. These
     * are accumulated into one object by using a map whose key is of type FacetResKey
     *
     */
    static class FacetResKey {

        /**
         * @param service
         * @param path
         */
        private FacetResKey(Optional<String> service, Optional<String> path) {
            super();
            this.service = service;
            this.path = path;
        }



        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + ((service == null) ? 0 : service.hashCode());
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         * Code generated by Eclipse
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FacetResKey other = (FacetResKey) obj;
            if (path == null) {
                if (other.path != null)
                    return false;
            } else if (!path.equals(other.path))
                return false;
            if (service == null) {
                return other.service == null;
            } else return service.equals(other.service);
        }



        public Optional<String> service = Optional.empty();
        public Optional<String> path = Optional.empty();
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getResultAggregate(java.lang.String, java.util.Optional)
     */
    @Override
    public Collection<MatchResultAggregate> computeResultAggregate(String replayId,
                                                                   Optional<String> service, boolean facetpath) {

        Optional<Replay> replay = getReplay(replayId);
        Map<FacetResKey, MatchResultAggregate> resMap = new HashMap<>();

        replay.ifPresent(replayv -> {
            final SolrQuery query = new SolrQuery("*:*");
            query.addField("*");
            addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
            addFilter(query, REPLAYIDF, replayv.replayId);
            service.ifPresent(servicev -> addFilter(query, SERVICEF, servicev));

            // First generate the solr facet query with appropriate nested facets
            /* A nested facet query will look as follows
             * {
             *   "reqmt_facets": {
             *     "field":"reqmt_s","limit":100,"type":"terms","facet": {
             *       "service_facets":{"field":"service_s","limit":100,"type":"terms"}}
             *   },
             *   "respmt_facets": {
             *     "field":"respmt_s","limit":100,"type":"terms","facet": {
             *       "service_facets":{"field":"service_s","limit":100,"type":"terms"}}
             *   }
             * }
             * If bypath is true, there will one more level of nesting with path_s attribute
             */
            FacetQ facetq = new FacetQ();
            List<List<String>> facetFields = new ArrayList<List<String>>();
            Facet reqmatchf = Facet.createTermFacet(REQMTF, Optional.of(FACETLIMIT));
            Facet respmatchf = Facet.createTermFacet(RESP_COMP_RES_TYPE_F, Optional.of(FACETLIMIT));
            facetq.addFacet(REQMTFACET, reqmatchf);
            facetq.addFacet(RESPMTFACET, respmatchf);

            // facetFields keeps track of the facets at each level of nesting. This is used later while
            // parsing the faceted results
            facetFields.add(List.of(REQMTFACET, RESPMTFACET));

            List<FacetQ> otherfacets = new ArrayList<FacetQ>();


            if (service.isEmpty()) {
                // facet on service as well
                Facet servicef = Facet.createTermFacet(SERVICEF, Optional.of(FACETLIMIT));
                FacetQ servicefq = new FacetQ();
                servicefq.addFacet(SERVICEFACET, servicef);
                otherfacets.add(servicefq);
                facetFields.add(List.of(SERVICEFACET));
            }

            if (facetpath) {
                Facet pathf = Facet.createTermFacet(PATHF, Optional.of(FACETLIMIT));
                FacetQ pathfq = new FacetQ();
                pathfq.addFacet(PATHFACET, pathf);
                otherfacets.add(pathfq);
                facetFields.add(List.of(PATHFACET));
            }


            // create the nesting
            facetq.nest(otherfacets);

            String json="";
            try {
                json = config.jsonMapper.writeValueAsString(facetq);
                query.add(SOLRJSONFACETPARAM, json);
            } catch (JsonProcessingException e) {
                LOGGER.error(String.format("Error in converting facets to json"), e);
            }

            // we don't need any results, set to 1
            query.setRows(1);
            SolrIterator.runQuery(solr, query).ifPresent(response -> {
                getNLFromNL(response.getResponse(), FACETSFIELD).ifPresent(facets -> {

                    // facet results will be nested since we used nested facets
                    // first flatten the results into a tabular structure
                    flatten(facets, facetFields).forEach(fr -> {

                        // combine facet results into MatchResultAggregate objects by using FacetResKey
                        FacetResKey frkey = fr.toFacetResKey();
                        Optional.ofNullable(resMap.get(frkey)).ifPresentOrElse(mra -> updateMatchResult(mra, fr), () -> {
                            MatchResultAggregate mra = new MatchResultAggregate(replayv.app, replayv.replayId, frkey.service, frkey.path);
                            updateMatchResult(mra, fr);
                            resMap.put(frkey, mra);
                        });
                    });
                });
            });
        });

        // if service filter was present initially, add it to all the results, since it was not
        // included in the facet query (it was included in the filter query)
        service.ifPresent(servicev -> resMap.forEach((k, mra) -> mra.service = Optional.of(servicev)));

        return resMap.values();
    }

    /**
     * @param mra
     * @param fr
     */
    private void updateMatchResult(MatchResultAggregate mra, FacetR fr) {
        fr.keys.forEach(frkey -> {
            if (frkey.facetName.equals(REQMTFACET)) {
                Utils.valueOf(Comparator.MatchType.class, frkey.key).ifPresent(rmt -> {
                    switch (rmt) {
                        case ExactMatch: mra.reqmatched = fr.val; break;
                        case FuzzyMatch: mra.reqpartiallymatched = fr.val; break;
                        case NoMatch: mra.reqnotmatched = fr.val; break;
                        case RecReqNoMatch: mra.recReqNotMatched = fr.val; break;
                        case MockReqNoMatch: mra.mockReqNotMatched = fr.val; break;
                        case ReplayReqNoMatch: mra.replayReqNotMatched = fr.val; break;
                    }

                });
            }
            if (frkey.facetName.equals(RESPMTFACET)) {
                Utils.valueOf(Comparator.MatchType.class, frkey.key).ifPresent(rmt -> {
                    switch (rmt) {
                        case ExactMatch: mra.respmatched = fr.val; break;
                        case FuzzyMatch: mra.resppartiallymatched = fr.val; break;
                        case NoMatch: mra.respnotmatched = fr.val; break;
                        case Exception: mra.respmatchexception = fr.val; break;
                    }

                });
            }
        });

    }

    public class ReqRespResultsWithFacets {
        ReqRespResultsWithFacets(Result<ReqRespMatchResult> result, ArrayList diffResolFacets,
            ArrayList serviceFacets, ArrayList pathFacets) {
             this.result = result;
             this.diffResolFacets = diffResolFacets;
             this.serviceFacets = serviceFacets;
             this.pathFacets = pathFacets;
        }

        public final Result<ReqRespMatchResult> result;
        public final ArrayList diffResolFacets;
        public final ArrayList serviceFacets;
        public final ArrayList pathFacets;
    }

    static class Facet {

        private static final String TYPEK = "type";
        private static final String FIELDK = "field";
        private static final String DOMAINK = "domain";
        private static final String LIMITK = "limit";
        private static final String FACETK = "facet";
        private static final String MISSINGK = "missing";
        private static final String MINCOUNT = "mincount";


        /**
         * @param params
         */
        private Facet(Map<String, Object> params) {
            super();
            this.params = params;
        }

        // These annotations are used for Jackson to flatten params while serializing/deserializing
        @JsonAnySetter
        public void addSubFacet(FacetQ subfacet) {
            params.put(FACETK, subfacet);
        }

        // These annotations are used for Jackson to flatten params while serializing/deserializing
        @JsonAnyGetter
        public Map<String, Object> getParams() {
            return params;
        }

        final private Map<String, Object> params;

        static Facet createTermFacet(String fieldname, Optional<Integer> limit) {
            return createTermFacet(fieldname, limit, Optional.empty());
        }

        static Facet createTermFacet(String fieldname, Optional<Integer> limit, Optional<Integer> mincount) {
            return createTermFacetWithDomain(fieldname, Optional.empty(), limit, mincount);
        }

        static Facet createTermFacetWithDomain(String fieldname, Optional<Map> domainBlock, Optional<Integer> limit) {
            return createTermFacetWithDomain(fieldname, domainBlock, limit, Optional.empty());

        }

        static Facet createTermFacetWithDomain(String fieldname, Optional<Map> domainBlock, Optional<Integer> limit,
                                               Optional<Integer> mincount) {
            Map<String, Object> params = new HashMap<>();
            params.put(TYPEK, "terms");
            params.put(FIELDK, fieldname);
            domainBlock.ifPresent(d -> params.put(DOMAINK, d));
            limit.ifPresentOrElse(l -> params.put(LIMITK, l), () -> {
                params.put(LIMITK, FACETLIMIT);
            });
            // include missing value in facet
            params.put(MISSINGK, true);
            mincount.ifPresent(l -> params.put(MINCOUNT, l));

            return new Facet(params);
        }
    }

    static class FacetQ {



        /**
         *
         */
        private FacetQ() {
            super();
            facetqs = new HashMap<>();
        }

        // These annotations are used for Jackson to flatten params while serializing/deserializing
        @JsonAnySetter
        public void addFacet(String name, Facet facet) {
            facetqs.put(name, facet);
        }

        public void removeFacet(String name) {
            facetqs.remove(name);
        }

        // These annotations are used for Jackson to flatten params while serializing/deserializing
        @JsonAnyGetter
        public Map<String, Facet> getFacetQs() {
            return facetqs;
        }

        private void nestFacetQ(FacetQ nestedq) {
            facetqs.forEach((fn, fq) -> fq.addSubFacet(nestedq));
        }

        final Map<String, Facet> facetqs;

        // create a nested facet query
        void nest(List<FacetQ> rest) {
            if (rest.size() > 0) {
                FacetQ head = rest.get(0);
                head.nest(rest.subList(1, rest.size()));
                nestFacetQ(head);
            }
        }
    }

    static class FacetRKey {



        /**
         * @param key
         * @param facetName
         */
        private FacetRKey(String key, String facetName) {
            super();
            this.key = key;
            this.facetName = facetName;
        }

        final String key;
        final String facetName;
    }

    // multi-dimensional facet results
    // this represents one entry of the flattened form of solr nested facet results
    static class FacetR {


        /**
         * @param keys
         * @param val
         */
        private FacetR(List<FacetRKey> keys, int val) {
            super();
            this.keys = keys;
            this.val = val;
        }

        /**
         * @return
         */
        FacetResKey toFacetResKey() {
            Optional<String> service = keys.stream().filter(frkey -> frkey.facetName.equals(SERVICEFACET)).findFirst().map(frkey -> frkey.key);
            Optional<String> path = keys.stream().filter(frkey -> frkey.facetName.equals(PATHFACET)).findFirst().map(frkey -> frkey.key);

            return new FacetResKey(service, path);
        }

        List<FacetRKey> keys;
        final int val;
        /**
         * @param rkey
         */
        void addKey(FacetRKey rkey) {
            keys.add(rkey);
        }
    }

    /* Helper functions to parse solr nested facet results */
    private static Optional<NamedList<Object>> objToNL(Object namedlist) {
        if (namedlist instanceof NamedList<?>) {
            @SuppressWarnings("unchecked")
            NamedList<Object> nl = (NamedList<Object>) namedlist;
            return Optional.of(nl);
        }
        return Optional.empty();
    }


    private static Optional<Object> getObjFromNL(NamedList<Object> namedlist, String name) {
        return objToNL(namedlist).flatMap(nl -> Optional.ofNullable(nl.get(name)));
    }

    @SuppressWarnings("unchecked")
    private static Optional<NamedList<Object>> getNLFromNL(NamedList<Object> namedlist, String name) {
        return getObjFromNL(namedlist, name).flatMap(v -> {
            if (v instanceof NamedList<?>) {
                return Optional.of((NamedList<Object>) v);
            }
            return Optional.empty();
        });
    }

    @SuppressWarnings("unchecked")
    private static Optional<List<Object>> getListFromNL(NamedList<Object> namedlist, String name) {
        return getObjFromNL(namedlist, name).flatMap(v -> {
            if (v instanceof List<?>) {
                return Optional.of((List<Object>) v);
            }
            return Optional.empty();
        });
    }

    private static Optional<String> getStringFromNL(NamedList<Object> namedlist, String name) {
        return getObjFromNL(namedlist, name).flatMap(v -> {
            if (v instanceof String) {
                return Optional.of((String) v);
            }
            return Optional.empty();
        });
    }

    private static Optional<Integer> getIntFromNL(NamedList<Object> namedlist, String name) {
        return getObjFromNL(namedlist, name).flatMap(v -> {
            if (v instanceof Integer) {
                return Optional.of((Integer) v);
            } else if (v instanceof Long) {
                return Optional.of(((Long) v).intValue());
            }
            return Optional.empty();
        });
    }

    /* Parse and flatten solr nested facet results
     * Recursive function
     * Solr results will have facet count in a json element of this form:
     *
  "facets":{
    "count":6,
    "reqmt_facets":{
      "buckets":[{
          "val":"ExactMatch",
          "count":6,
          "service_facets":{
            "buckets":[{
                "val":"productpage",
                "count":6}]}}]},
    "respmt_facets":{
      "buckets":[{
          "val":"ExactMatch",
          "count":3,
          "service_facets":{
            "buckets":[{
                "val":"productpage",
                "count":3}]}},
        {
          "val":"NoMatch",
          "count":3,
          "service_facets":{
            "buckets":[{
                "val":"productpage",
                "count":3}]}}]}}
     * This function will flatten it and create tuples of the form
     * <{}, 6>
     * <{reqmt_facets.ExactMatch}, 6>
     * <{reqmt_facets.Exactmatch, service_facets.productpage}, 6>
     * <{respmt_facets.ExactMatch}, 3>
     * <{respmt_facets.ExactMatch, service_facets.productpage}, 3>
     * <{respmt_facets.NoMatch}, 3>
     * <{respmt_facets.NoMatch, service_facets.productpage}>, 3>
     *
     */
    private static List<FacetR> flatten(NamedList<Object> facetresult, List<List<String>> facetFields) {
        List<FacetR> results = new ArrayList<>();
        if (facetFields.size() > 0) {
            List<String> head = facetFields.get(0);
            List<List<String>> rest = facetFields.subList(1, facetFields.size());
            head.forEach(facetname -> {
                getNLFromNL(facetresult, facetname)
                    .ifPresent(bucketObj -> {
                        getListFromNL(bucketObj, BUCKETFIELD).ifPresent(bucketarrobj -> {
                            bucketarrobj.forEach(bucket -> {
                                processFacetBucket(results, rest, facetname, bucket);
                            });
                        });
                        getObjFromNL(bucketObj, MISSINGBUCKETFIELD).ifPresent(bucket ->
                            processFacetBucket(results, rest, facetname, bucket));
                    });
            });
        }
        return results;
    }

    private static void processFacetBucket(List<FacetR> results, List<List<String>> rest,
                                           String facetname, Object bucket) {
        objToNL(bucket).ifPresent(b -> {
            // VALFIELD is missing for bucket with missing values. Treat that as empty string ""
            String val = getStringFromNL(b, VALFIELD).orElse("");
            Optional<Integer> count = getIntFromNL(b, COUNTFIELD);

            List<FacetR> subfacetr = flatten(b, rest);
            FacetRKey rkey = new FacetRKey(val, facetname);
            subfacetr.forEach(subr -> {
                subr.addKey(rkey);
                results.add(subr);
            });
            count.ifPresent(c -> {
                if (c > 0) {
                    List<FacetRKey> frkeys = new ArrayList<>();
                    frkeys.add(rkey);
                    FacetR newr = new FacetR(frkeys, c);
                    results.add(newr);
                }
            });
        });
    }


	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#commit()
	 */
	@Override
	public boolean commit() {

		return softcommit();
	}

}
