/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.cube.core.Utils;
import io.cube.agent.CommonUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.FnKey;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import redis.clients.jedis.Jedis;

import com.cube.agent.FnReqResponse;
import com.cube.agent.FnResponse;
import com.cube.cache.ReplayResultCache.ReplayPathStatistic;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplateVersioned;
import com.cube.core.RequestComparator;
import com.cube.core.RequestComparator.PathCT;
import com.cube.dao.Analysis.ReqRespMatchResult;
import com.cube.dao.RRBase.RR;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.Replay.ReplayStatus;
import com.cube.golden.ReqRespUpdateOperation;
import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateSet;
import com.cube.golden.TemplateUpdateOperationSet;
import com.cube.ws.Config;

/**
 * @author prasad
 *
 */
public class ReqRespStoreSolr extends ReqRespStoreImplBase implements ReqRespStore {

    private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreSolr.class);

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#save(com.cube.dao.ReqRespStore.Request)
     */
    @Override
    public boolean save(Request req) {

        SolrInputDocument doc = reqToSolrDoc(req);
        return saveDoc(doc);
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#save(com.cube.dao.ReqRespStore.Response)
     */
    @Override
    public boolean save(Response resp) {

        SolrInputDocument doc = respToSolrDoc(resp);
        return saveDoc(doc);
    }

    @Override
    public boolean save(Event event) {

        SolrInputDocument doc = eventToSolrDoc(event);
        return saveDoc(doc);
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRequest()
     * qr - query request
     */
    @Override
    public Stream<Request> getRequests(Request qr, RequestComparator mspec, Optional<Integer> nummatches,
                                       Optional<Integer> start) {

        final SolrQuery query = reqMatchSpecToSolrQuery(qr, mspec);

        return SolrIterator.getStream(solr, query, nummatches, start).flatMap(doc -> docToRequest(doc).stream());

    }

    @Override
    void removeCollectionKey(ReqRespStoreImplBase.CollectionKey collectionKey) {
        if (config.intentResolver.isIntentToMock()) return;
        try (Jedis jedis = config.jedisPool.getResource()) {
            //jedis.del(collectionKey.toString());
            Long result = jedis.expire(collectionKey.toString(), Config.REDIS_DELETE_TTL);
            LOGGER.info(
                String.format("Expiring redis key \"%s\" in %d seconds", collectionKey.toString(),
                    Config.REDIS_DELETE_TTL));
        } catch (Exception e) {
            LOGGER.error("Unable to remove key from redis cache :: "+ e.getMessage());
        }
    }

    private FnKey recordReplayRetrieveKey;
    private FnKey recordReplayStoreKey;

    @Override
    Optional<RecordOrReplay> retrieveFromCache(CollectionKey key) {
        Optional<RecordOrReplay> toReturn = Optional.empty();
        if (recordReplayRetrieveKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            recordReplayRetrieveKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(recordReplayRetrieveKey,  CommonUtils.getCurrentTraceId(),
                CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.empty(), key);
            if (ret.retStatus == io.cube.agent.FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            return (Optional<RecordOrReplay>) ret.retVal;
        }

        try (Jedis jedis = config.jedisPool.getResource()) {
            String fromCache = jedis.get(key.toString());
            if (fromCache != null) {
                LOGGER.info("Successfully retrieved from redis, key :: " + key.toString());
                toReturn = Optional.of(config.jsonmapper.readValue(fromCache, RecordOrReplay.class));
            }
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(recordReplayRetrieveKey,  CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    io.cube.agent.FnReqResponse.RetStatus.Success, Optional.empty(), key);
            }
        } catch (Exception e) {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(recordReplayRetrieveKey, CommonUtils.getCurrentTraceId(), CommonUtils.getCurrentSpanId(),
                    CommonUtils.getParentSpanId(), e, io.cube.agent.FnReqResponse.RetStatus.Exception,
                    Optional.of(e.getClass().getName()), key);
            }
            LOGGER.error("Error while retrieving RecordOrReplay from cache :: " + e.getMessage());
        }
        return toReturn;
    }

    @Override
    void populateCache(CollectionKey collectionKey, RecordOrReplay rr) {
        if (recordReplayStoreKey == null) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            recordReplayStoreKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(recordReplayStoreKey,  CommonUtils.getCurrentTraceId(),
                CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.empty(), collectionKey , rr);
            if (ret != null && ret.retStatus == io.cube.agent.FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable)ret.retVal);
            }
            // do nothing -- return type is void
            return;
        }

        try (Jedis jedis = config.jedisPool.getResource()) {
            String toString = config.jsonmapper.writeValueAsString(rr);
            LOGGER.info("Before storing in cache :: " + toString);
            jedis.set(collectionKey.toString() , toString);
            LOGGER.info("Successfully stored in redis, key :: " + collectionKey.toString());
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while population RecordOrReplay in cache :: "  + e.getMessage());
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(recordReplayStoreKey, CommonUtils.getCurrentTraceId(), CommonUtils.getCurrentSpanId(),
                    CommonUtils.getParentSpanId(), e, io.cube.agent.FnReqResponse.RetStatus.Exception,
                    Optional.of(e.getClass().getName()), collectionKey , rr);
            }
        }
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRequests(java.lang.String, java.lang.String, java.lang.String, java.lang.Iterable, com.cube.dao.ReqRespStore.RR, com.cube.dao.ReqRespStore.Types)
     */
    @Override
    public Result<Request> getRequests(String customerid, String app, String collection,
                                       List<String> reqids, List<String> paths, RRBase.RR rrtype) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Request.toString());
        addFilter(query, CUSTOMERIDF, customerid);
        addFilter(query, APPF, app);
        addFilter(query, COLLECTIONF, collection);
        String reqfilter = reqids.stream().collect(Collectors.joining(" OR ", "(", ")"));
        if (reqids.size() > 0)
            addFilter(query, REQIDF, reqfilter, false);

        String pathfilter = paths.stream().collect(Collectors.joining(" OR ", "(", ")"));
        if (paths.size() > 0)
            addFilter(query, PATHF, pathfilter, false);


        query.addFilterQuery(String.format("%s:%s", RRTYPEF, rrtype.toString()));

        return SolrIterator.getResults(solr, query, Optional.empty(), ReqRespStoreSolr::docToRequest);

    }

    @Override
    public Result<Event> getEvents(EventQuery eventQuery) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Event.toString());
        addFilter(query, CUSTOMERIDF, Optional.ofNullable(eventQuery.getCustomerId()));
        addFilter(query, APPF, Optional.ofNullable(eventQuery.getApp()));
        addFilter(query, SERVICEF, Optional.ofNullable(eventQuery.getService()));
        addFilter(query, COLLECTIONF, Optional.ofNullable(eventQuery.getCollection()));
        addFilter(query, TRACEIDF, Optional.ofNullable(eventQuery.getTraceId()));
        addFilter(query, REQIDF, Optional.ofNullable(eventQuery.getReqids()).orElse(Collections.emptyList()));
        addFilter(query, PATHF, Optional.ofNullable(eventQuery.getPaths()).orElse(Collections.emptyList()));
        addFilter(query, EVENTTYPEF, Optional.ofNullable(eventQuery.getEventType()).map(type -> type.toString()));
        addFilterInt(query, PAYLOADKEYF, Optional.ofNullable(eventQuery.getPayloadKey()));
        addSort(query, TIMESTAMPF, eventQuery.isSortOrderAsc());

        return SolrIterator.getResults(solr, query, Optional.ofNullable(eventQuery.getLimit()),
            this::docToEvent, Optional.of(Optional.ofNullable(eventQuery.getOffset()).orElse(20)));
    }

    private void addFilterInt(SolrQuery query, String payloadkeyf, Optional<Integer> payloadKey) {
    }

    public Stream<Request> expandOnTraceId(List<Request> originalList, List<String> intermediateServices,
                                           String collectionId) {
        List<String> traceIds = originalList.stream().map(request-> CommonUtils.getCaseInsensitiveMatches(request.hdrs,
                Config.DEFAULT_TRACE_FIELD)).flatMap(List::stream).collect(Collectors.toList());
        if (traceIds.isEmpty() || intermediateServices.isEmpty()) return originalList.stream() ;
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query , METASERVICEF , intermediateServices.stream().collect(Collectors.joining(" OR "
                , "(" , ")")) , false);
        addFilter(query , TYPEF , Types.Request.toString());
        addFilter(query , COLLECTIONF , collectionId);
        addFilter(query , HDRTRACEF , traceIds.stream().collect(Collectors.joining("\" OR \"" , "(\""
                , "\")")) , false);
        return Stream.concat(originalList.stream() , SolrIterator.getStream(solr, query, Optional.empty())
                .flatMap(doc -> docToRequest(doc).stream()));
    }

    @Override
    public Stream<ReqRespMatchResult> expandOnTrace(ReqRespMatchResult reqRespMatchResult, boolean recordOrReplay) {
        String replayId = reqRespMatchResult.replayid;
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
        FnReqResponse.RetStatus retStatus =
            getStrField(doc, FUNC_RET_STATUSF).flatMap(rs -> Utils.valueOf(FnReqResponse.RetStatus.class,
            rs)).orElse(FnReqResponse.RetStatus.Success);
        Optional<String> exceptionType = getStrField(doc, FUNC_EXCEPTION_TYPEF);
        return getStrFieldMVFirst(doc,FUNC_RET_VAL).map(retVal -> new FnResponse(retVal ,getTSField(doc,TIMESTAMPF),
            retStatus, exceptionType, multipleResults));
    }

    @Override
    public Optional<FnResponse> getFunctionReturnValue(FnReqResponse funcReqResponse, String collection) {
        StringBuilder argsQuery = new StringBuilder();
        argsQuery.append("*:*");
        var counter = new Object(){int x =0;};
        funcReqResponse.traceId.ifPresent(trace ->
            argsQuery.append(" OR ").append("(").append(HDRTRACEF).append(":").append(trace).append(")^2"));
        funcReqResponse.respTS.ifPresent(timestamp -> argsQuery.
            append(" OR ").append(TIMESTAMPF).append(":{").append(timestamp.toString()).append(" TO *]"));
        SolrQuery query = new SolrQuery(argsQuery.toString());
        query.setFields("*");
        addFilter(query, TYPEF, Types.FuncReqResp.toString());
        addFilter(query, FUNC_SIG_HASH, funcReqResponse.fnSignatureHash);
        addFilter(query, COLLECTIONF, collection);
        addFilter(query, SERVICEF, funcReqResponse.service);
        addSort(query, TIMESTAMPF, true);
        Arrays.asList(funcReqResponse.argsHash).forEach(argHashVal ->
            addFilter(query, FUNC_ARG_HASH_PREFIX + ++counter.x + INT_SUFFIX, argHashVal));
        Optional<Integer> maxResults = Optional.of(1);
        Result<SolrDocument> solrDocumentResult = SolrIterator.getResults(solr, query, maxResults, x-> Optional.of(x));
        return solrDocumentResult.getObjects().findFirst().flatMap(doc -> solrDocToFnResponse(doc,solrDocumentResult.numFound>1));
    }

    @Override
    public boolean saveFnReqRespNewCollec(String customer, String app, String collection
        , String newCollection) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        addFilter(solrQuery, TYPEF , Types.FuncReqResp.toString());
        addFilter(solrQuery, CUSTOMERIDF , customer);
        addFilter(solrQuery, APPF , app);
        addFilter(solrQuery, COLLECTIONF , collection);
        SolrIterator.getStream(solr, solrQuery, Optional.empty()).forEach(doc -> {
            SolrInputDocument inputDoc = new SolrInputDocument();
            doc.entrySet().stream().forEach(v -> {
                inputDoc.addField(v.getKey(), v.getValue());
            });
            inputDoc.setField(COLLECTIONF , newCollection);
            inputDoc.removeField("_version_");
            inputDoc.removeField("id");
            try {
                solr.add(inputDoc);
            } catch (SolrServerException e) {
                LOGGER.error("Error while saving fnreqresp object in new collection :: " + e.getMessage());
            } catch (IOException e) {
                LOGGER.error("Error while saving fnreqresp object in new collection :: " + e.getMessage());
            }
        });
        softcommit();
        return true;
    }

    @Override
    public String createTemplateUpdateOperationSet(String customer, String app, String sourceTemplateSetVersion) throws Exception {
        String templateUpdateOperationSetId = Types.TemplateUpdateOperationSet.toString().concat("-").concat(
            UUID.randomUUID().toString());
        TemplateUpdateOperationSet templateSetUpdate = new TemplateUpdateOperationSet(templateUpdateOperationSetId,
            new HashMap<>());
        SolrInputDocument inputDoc = templateUpdateOperationSetToSolrDoc(templateSetUpdate);
        saveDoc(inputDoc);
        softcommit();
        return templateUpdateOperationSetId;
    }

    private static final String OPERATION = CPREFIX + "operation" + STRING_SUFFIX;
    private static final String TEMPLATE_KEY = CPREFIX + "template_key" + STRINGSET_SUFFIX;

    private SolrInputDocument templateUpdateOperationSetToSolrDoc(TemplateUpdateOperationSet operationSet) throws Exception {
        SolrInputDocument inputDoc = new SolrInputDocument();
        inputDoc.setField(TYPEF, Types.TemplateUpdateOperationSet.toString());
        inputDoc.setField(IDF,  operationSet.getTemplateUpdateOperationSetId());
        if (operationSet.getTemplateUpdates() != null && !operationSet.getTemplateUpdates().isEmpty()) {
            inputDoc.setField(OPERATION, config.jsonmapper.writeValueAsString(operationSet.getTemplateUpdates()));
        }
        return inputDoc;
    }

    @Override
    public boolean saveTemplateUpdateOperationSet(TemplateUpdateOperationSet templateUpdateOperationSet) throws Exception {
        return saveDoc(templateUpdateOperationSetToSolrDoc(templateUpdateOperationSet)) && softcommit();
    }

    private TypeReference<HashMap<TemplateKey, SingleTemplateUpdateOperation>> typeReference =
        new TypeReference<>() {};

    private Optional<TemplateUpdateOperationSet> solrDocToTemplateUpdateOperationSet(SolrDocument solrDocument) {
        Optional<String> id = getStrField(solrDocument , IDF);
        Optional<String> operationsAsString = getStrField(solrDocument, OPERATION);
        Map<TemplateKey, SingleTemplateUpdateOperation> updateMap  = (Map<TemplateKey, SingleTemplateUpdateOperation>)
            operationsAsString.flatMap(operations -> {
            try {
                return Optional.of(config.jsonmapper.readValue(operations , typeReference));
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

    public boolean storeRecordingOperationSetMeta(RecordingOperationSetMeta recordingOperationSetMeta){
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.setField(IDF, recordingOperationSetMeta.id);
        solrDoc.setField(TYPEF, Types.RecordingOperationSetMeta.toString());
        solrDoc.setField(CUSTOMERIDF,recordingOperationSetMeta.customer);
        solrDoc.setField(APPF, recordingOperationSetMeta.app);
        return saveDoc(solrDoc) && softcommit();
    }

    // get recordingOperationSet for a given operationset id, service and path
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
                opStr = config.jsonmapper.writeValueAsString(op);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            solrDoc.addField(OPERATIONLIST, opStr);
        });
        return saveDoc(solrDoc) && softcommit();
    }

    // get recordingOperationSet for a given operationset id, service and path
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
                    reqRespUpdateOperation = config.jsonmapper.readValue(op,
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
            TemplateKey templateKey = new TemplateKey(Optional.of(templateSet.version), templateSet.customer,
                templateSet.app,
                template.service , template.requestPath, template.type);
                templateIds.add(saveCompareTemplate(templateKey, config.jsonmapper.writeValueAsString(template)));
        }));
        return storeTemplateSetMetadata(templateSet, templateIds);
    }

    private static final String TEMPLATE_ID = "template_id" + STRINGSET_SUFFIX;
    private static final String TEMPLATE_VERSION = "version" + STRING_SUFFIX;
    private static final String TEMPLATE_SET = "template_set_id" + STRING_SUFFIX;
    private static final String ROOT_GOLDEN_SET = "root_golden_set_id" + STRING_SUFFIX;
    private static final String PARENT_GOLDEN_SET = "parent_golden_set_id" + STRING_SUFFIX;

    private String storeTemplateSetMetadata(TemplateSet templateSet, List<String> templateIds) {
        SolrInputDocument solrDoc = new SolrInputDocument();
        String id = Types.TemplateSet.toString().concat("-").concat(String.valueOf(Objects.hash(
            templateSet.customer, templateSet.app, templateSet.version)));

        solrDoc.setField(IDF, id);
        solrDoc.setField(TYPEF, Types.TemplateSet.toString());
        solrDoc.setField(TEMPLATE_VERSION, templateSet.version);
        solrDoc.setField(CUSTOMERIDF , templateSet.customer);
        solrDoc.setField(APPF, templateSet.app);
        solrDoc.setField(TIMESTAMPF , templateSet.timestamp.toString());
        templateIds.forEach(templateId -> solrDoc.addField(TEMPLATE_ID, templateId));
        saveDoc(solrDoc);
        softcommit();
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
    public Optional<TemplateSet> getTemplateSet(String customerid, String app, String version) {
        try {
            SolrQuery query = new SolrQuery("*:*");
            addFilter(query, TYPEF, Types.TemplateSet.toString());
            addFilter(query, CUSTOMERIDF, customerid);
            addFilter(query, APPF, app);
            addFilter(query, TEMPLATE_VERSION, version);
            Optional<Integer> maxResults = Optional.of(1);

            return SolrIterator.getStream(solr, query, maxResults).findFirst().flatMap(this::solrDocToTemplateSet);
        } catch (Exception e) {
            LOGGER.error("Error occured while fetching template set for customer/app/verion :: " + customerid + "::"
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
            Optional<Integer> maxResults = Optional.of(1);

            return SolrIterator.getStream(solr, query, maxResults).findFirst().flatMap(this::solrDocToTemplateSet);
        } catch (Exception e) {
            LOGGER.error("Error occured while fetching template set for customer :: " + customer + " :: app :: " + app + " :: " + e.getMessage());
        }
        return Optional.empty();
    }

/*    @Override
    public String createGoldenSet(String collection, String templateSetId, Optional<String> parentGoldenSetIdOpt, Optional<String> rootGoldenSetIdOpt) {
        SolrInputDocument inputDoc = new SolrInputDocument();
        String id = Types.GoldenSet.toString().concat("-")
            .concat(String.valueOf(Objects.hash(collection, templateSetId)));
        inputDoc.setField(TYPEF, Types.GoldenSet.toString());
        inputDoc.setField(IDF, id);
        inputDoc.setField(COLLECTIONF, collection);
        inputDoc.setField(TEMPLATE_SET, templateSetId);
        parentGoldenSetIdOpt.ifPresent(parentGoldenSetId -> inputDoc.setField(PARENT_GOLDEN_SET, parentGoldenSetId));
        inputDoc.setField(ROOT_GOLDEN_SET, rootGoldenSetIdOpt.orElse(id));
        inputDoc.setField(TIMESTAMPF , Instant.now());
        saveDoc(inputDoc);
        softcommit();
        return id;
    }

    private GoldenSet solrDocToGoldenSet(SolrDocument doc) throws Exception {
        Optional<String> id = getStrField(doc, IDF);
        Optional<String> collectionOpt = getStrField(doc, COLLECTIONF);
        Optional<String> templateSetOpt = getStrField(doc, TEMPLATE_SET);
        Optional<Instant> creationTimestamp = getTSField(doc, TIMESTAMPF);
        Optional<String> parentGoldenSetId = getStrField(doc, PARENT_GOLDEN_SET);
        Optional<String> rootGoldenSetId = getStrField(doc, ROOT_GOLDEN_SET);
        Optional<String> customerOpt = getStrField(doc, CUSTOMERIDF);
        Optional<String> appOpt = getStrField(doc, APPF);
        Optional<String> instance = getStrField(doc, INSTANCEIDF);

        String customer = customerOpt.orElseThrow(() -> new Exception("Customer field empty"));
        String app = appOpt.orElseThrow(() -> new Exception("App field empty"));
        String collection = collectionOpt.orElseThrow(() -> new Exception("Collection field empty"));
        String templateSetVersion = templateSetOpt.orElseThrow(() -> new Exception("Template Set Version not specified"));
        return new GoldenSet(customer, app, instance, collection, templateSetVersion,
            rootGoldenSetId, parentGoldenSetId, creationTimestamp);

    }

    @Override
    public Optional<GoldenSet> getGoldenSet(String goldenSetId) throws Exception {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, IDF, goldenSetId);
        addFilter(query, TYPEF, Types.GoldenSet.toString());
        return SolrIterator.getStream(solr , query, Optional.of(1)).findFirst().map(com.cube.core.UtilException
                .rethrowFunction(this::solrDocToGoldenSet));
    }

    public Stream<GoldenSet> getGoldenSetStream(SolrQuery query) {
        return SolrIterator.getStream(solr, query, Optional.of(20))
            .flatMap(doc -> {
                try {
                    return Stream.of(solrDocToGoldenSet(doc));
                } catch (Exception e) {
                    LOGGER.error("Error while converting solr doc to golden set object :: " + e.getMessage());
                    return Stream.empty();
                }
            });
    }


    @Override
    public Stream<GoldenSet> getGoldenSetStream(Optional<String> customer,
                                                Optional<String> app, Optional<String> instanceid) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, CUSTOMERIDF, customer);
        addFilter(query, APPF, app);
        addFilter(query, INSTANCEIDF, instanceid);
        addFilter(query, TYPEF, Types.GoldenSet.toString());
        return getGoldenSetStream(query);
    }

    @Override
    public Stream<GoldenSet> getAllDerivedGoldenSets(String rootGoldentSetId) {
        SolrQuery query = new SolrQuery("*:*");
        addFilter(query, TYPEF, Types.GoldenSet.toString());
        addFilter(query, ROOT_GOLDEN_SET, rootGoldentSetId);
        return getGoldenSetStream(query);
    }*/

    private Optional<TemplateSet> solrDocToTemplateSet(SolrDocument doc) {
        Optional<String> version = getStrField(doc, TEMPLATE_VERSION);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<Instant> creationTimestamp = getTSField(doc, TIMESTAMPF);
        List<String> templateIds = getStrFieldMV(doc, TEMPLATE_ID);
        if (version.isEmpty() || customerId.isEmpty() || app.isEmpty() || creationTimestamp.isEmpty()) {
            LOGGER.error("Improper template set stored in solr for template set id :: " + getStrField(doc, IDF).get());
            return Optional.empty();
        }

        TemplateSet templateSet = new TemplateSet(version.get(), customerId.get(), app.get(),
            creationTimestamp.get(), getVersionedTemplatesFromSolr(templateIds));
        return Optional.of(templateSet);
    }

    private Stream<CompareTemplateVersioned> solrDocToCompareTemplate(SolrDocument doc) {
        Optional<String> version = getStrField(doc, TEMPLATE_VERSION);
        Optional<String> customerId = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, SERVICEF);
        String templateId = getStrField(doc, IDF).get();
        Optional<String> type = getStrField(doc, TYPEF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> compareTemplate = getStrField(doc, COMPARETEMPLATEJSON);
        Optional<String> requestPath = getStrField(doc, PATHF);
        if (type.isEmpty() || service.isEmpty() || compareTemplate.isEmpty() || requestPath.isEmpty()) {
            LOGGER.error("Improper compare-template stored in solr :: " + templateId);
            return Stream.empty();
        }

        try {
            CompareTemplate compareTemplateObj = config.jsonmapper.readValue(compareTemplate.get() , CompareTemplate.class);
            TemplateKey.Type templateType = type.get().equals(Types.RequestCompareTemplate.toString()) ?
                TemplateKey.Type.Request : TemplateKey.Type.Response;
            CompareTemplateVersioned compareTemplateVersioned = new CompareTemplateVersioned(service , requestPath,
                templateType, compareTemplateObj);
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

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getResponse(java.lang.String)
     */
    @Override
    public Optional<Response> getResponse(String reqid) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        //query.setRows(1);

        addFilter(query, TYPEF, Types.Response.toString());
        addFilter(query, REQIDF, reqid);

        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> docToResponse(doc));

    }

    @Override
    public Map<String, Response> getResponses(List<Request> requests) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        // adding filter for type response
        addFilter(query , TYPEF , Types.Response.toString());
        // adding filter for request id's against which we want to find repsonses
        addFilter(query, REQIDF ,
                requests.stream().map(request -> request.reqid).filter(Optional::isPresent).map(Optional::get)
                        .collect(Collectors.joining(" OR " , "(" , ")")), false );
        Optional<Integer> maxResults = Optional.of(requests.size());
        Map<String, Response> result = new HashMap<>();
        SolrIterator.getStream(solr , query , maxResults).forEach(doc -> {
            docToResponse(doc).ifPresent(response ->
                    response.reqid.ifPresent(reqid -> result.put(reqid , response)));
        });
        return result;
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRequest(java.lang.String)
     */
    @Override
    public Optional<Request> getRequest(String reqid) {

        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        //query.setRows(1);

        addFilter(query, TYPEF, Types.Request.toString());
        addFilter(query, REQIDF, reqid);

        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> docToRequest(doc));

    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRespForReq(com.cube.dao.ReqRespStore.Request)
     */
    @Override
    public Optional<Response> getRespForReq(Request qr, RequestComparator mspec) {
        // Find request, without considering request id
        Optional<Request> req = getRequests(qr, mspec, Optional.of(1)).findFirst();
        return req.flatMap(reqv -> reqv.reqid).flatMap(this::getResponse);
    }

    /**
     * @param solr
     * @param config
     */
    public ReqRespStoreSolr(SolrClient solr, Config config) {
        super();
        this.solr = solr;
        this.config = config;
        SolrIterator.setConfig(config);
    }

    private final SolrClient solr;
    private final Config config;

    private static final String TYPEF = CPREFIX + "type" + STRING_SUFFIX;

    // field names in Solr
    private static final String PATHF = CPREFIX + "path" + STRING_SUFFIX;
    private static final String REQIDF = CPREFIX + "reqid" + STRING_SUFFIX;
    private static final String METHODF = CPREFIX + "method" + STRING_SUFFIX;
    private static final String BODYF = CPREFIX + "body" + NOTINDEXED_SUFFIX;
    private static final String OLDBODYF = CPREFIX + "body" + TEXT_SUFFIX;
    private static final String COLLECTIONF = CPREFIX + "collection" + STRING_SUFFIX;
    private static final String TIMESTAMPF = CPREFIX + "timestamp" + DATE_SUFFIX;
    private static final String RRTYPEF = CPREFIX + "rrtype" + STRING_SUFFIX;
    private static final String CUSTOMERIDF = CPREFIX + "customerid" + STRING_SUFFIX;
    private static final String APPF = CPREFIX + "app" + STRING_SUFFIX;
    private static final String INSTANCEIDF = CPREFIX + "instanceid" + STRING_SUFFIX;
    private static final String STATUSF = CPREFIX + "status" + INT_SUFFIX;
    private static final String CONTENTTYPEF = CPREFIX + "contenttype" + STRING_SUFFIX;
    private static final String OPERATIONSETIDF = CPREFIX + "operationsetid" + STRING_SUFFIX;
    private static final String OPERATIONLIST = CPREFIX + "operationlist" + STRINGSET_SUFFIX;
    private static final String TRACEIDF = CPREFIX + "traceid" + STRING_SUFFIX;
    private static final String PAYLOADBINF = CPREFIX + "payloadBin" + BIN_SUFFIX;
    private static final String PAYLOADSTRF = CPREFIX + "payloadStr" + NOTINDEXED_SUFFIX;
    private static final String PAYLOADKEYF = CPREFIX + "payloadKey" + INT_SUFFIX;
    private static final String EVENTTYPEF = CPREFIX + "eventType" + STRING_SUFFIX;


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
    private static final String HDRTRACEF = HDR + "_"  + Config.DEFAULT_TRACE_FIELD + STRINGSET_SUFFIX;
    private static final String HDRSPANF = HDR + "_"  + Config.DEFAULT_SPAN_FIELD + STRINGSET_SUFFIX;
    private static final String HDRPARENTSPANF = HDR + "_"  + Config.DEFAULT_PARENT_SPAN_FIELD + STRINGSET_SUFFIX;
    private static final String METASERVICEF = META + "_service" + STRINGSET_SUFFIX;

    private static void addFilter(SolrQuery query, String fieldname, String fval, boolean quote) {
        //String newfval = quote ? String.format("\"%s\"", StringEscapeUtils.escapeJava(fval)) : fval ;
        String newfval = quote ? SolrIterator.escapeQueryChars(fval) : fval;
        query.addFilterQuery(String.format("%s:%s", fieldname, newfval));
    }


    private static void addFilter(SolrQuery query, String fieldname, String fval) {
        // add quotes by default in case the strings have spaces in them
        addFilter(query, fieldname, fval, true);
    }

    private static void addFilter(SolrQuery query, String fieldname, Optional<String> fval) {
        addFilter(query, fieldname, fval, false);
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

    private static void addFilter(SolrQuery query, String fieldname, Integer fval) {
        addFilter(query, fieldname, String.valueOf(fval));
    }

    private static void addFilter(SolrQuery query, String fieldname, List<String> orValues) {
        if(orValues.isEmpty()) return;
        String value = orValues.stream().map(SolrIterator::escapeQueryChars)
            .collect(Collectors.joining(" OR " , "(" , ")"));
        addFilter(query , fieldname, value, false);
    }

    private static void addWeightedPathFilter(SolrQuery query , String fieldName , String originalPath) {
        String[] pathElements = originalPath.split("/");
        StringBuffer pathBuffer = new StringBuffer();
        StringBuffer queryBuffer = new StringBuffer();
        var countWrapper = new Object() {int count = 0;};
        Arrays.asList(pathElements).stream().forEachOrdered(elem ->
        {
            pathBuffer.append(((countWrapper.count != 0)? "/" : "") + elem);
            String escapedPath =  SolrIterator.escapeQueryChars(pathBuffer.toString())
                    .concat((countWrapper.count != pathElements.length -1)? SolrIterator.escapeQueryChars("*") : "") ;
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


    // for predicates in the solr q param. Assumes *:* is already there in the buffer
    private static void addToQryStr(StringBuffer qstr, String fieldname, String fval, boolean quote) {
        // String newfval = quote ? String.format("\"%s\"", StringEscapeUtils.escapeJava(fval)) : fval;
        String newfval = quote ? SolrIterator.escapeQueryChars(fval) : fval;
        qstr.append(String.format(" OR %s:%s", fieldname, newfval));
    }


    private static void addToQryStr(StringBuffer qstr, String fieldname, String fval) {
        // add quotes to field vals by default
        addToQryStr(qstr, fieldname, fval, true);
    }

    private static void addToQryStr(StringBuffer qstr, String fieldname, Optional<String> fval) {
        fval.ifPresent(val -> addToQryStr(qstr, fieldname, val));
    }

    private static void addToQryStr(StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, String key) {
        String f = getSolrFieldName(fieldname, key);
        Optional.ofNullable(fvalmap.get(key)).ifPresent(vals -> vals.forEach(v -> {
            addToQryStr(qstr, f, v);
        }));
    }

    private static void addToQryStr(StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, List<String> keys) {
        // Empty list of selected keys is treated as if all keys are to be added
        Collection<String> ftoadd = (keys.isEmpty()) ? fvalmap.keySet() : keys;
        ftoadd.forEach(k -> {
            addToQryStr(qstr, fieldname, fvalmap, k);
        });
    }

    private static void addMatch(ComparisonType mt, SolrQuery query, StringBuffer qparam, String fieldname, String fval) {
        switch (mt) {
            case Equal: addFilter(query, fieldname, fval); break;
            case EqualOptional: addToQryStr(qparam, fieldname, fval); break;
            default:
        }
    }

    private static void addMatch(ComparisonType mt, SolrQuery query, StringBuffer qstr, String fieldname, Optional<String> fval) {
        switch (mt) {
            case Equal: addFilter(query, fieldname, fval); break;
            case EqualOptional: addToQryStr(qstr, fieldname, fval); break;
            default:
        }
    }

    private static void addToQuery(SolrQuery query, StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap, ComparisonType ct, String path) {
        if (ct == ComparisonType.Equal) {
            addFilter(query, fieldname, fvalmap, path);
        } else if (ct == ComparisonType.EqualOptional) {
            addToQryStr(qstr, fieldname, fvalmap, path);
        }
    }

    private static void addMatch(SolrQuery query, StringBuffer qstr, String fieldname, MultivaluedMap<String, String> fvalmap,
                                 List<PathCT> pathCTS) {
        Optional<PathCT> rootpct = Optional.empty();

        for (PathCT pct : pathCTS) {
            if (pct.path.equals('/') || pct.path.isBlank()) {
                // if path is empty, it means the rule is at rule level, so should be applied to it descendents
                // this is the rule at the root level
                rootpct = Optional.of(pct);
            } else {
                addToQuery(query, qstr, fieldname, fvalmap, pct.ct, StringUtils.removeStart(pct.path, "/"));
            }
        }
        // check for inheritance of paths not covered in pathCTs
        rootpct.ifPresent(rootpctv -> fvalmap.keySet().forEach(k -> {
            // check that no rule is already defined on path /k
            if (pathCTS.stream().filter(pct -> pct.path.equals("/" + k)).findFirst().isEmpty()) {
               addToQuery(query, qstr, fieldname, fvalmap, rootpctv.ct, k);
            }
        }));
    }

    private static void setRRFields(Types type, RRBase rr, SolrInputDocument doc) {

        rr.reqid.ifPresent(id -> {
        	doc.setField(REQIDF, id);
        	doc.setField(IDF, type.toString() + "-" + id);
        });
        doc.setField(BODYF, rr.body);
        addFieldsToDoc(doc, META, rr.meta);
        addFieldsToDoc(doc, HDR, rr.hdrs);
        rr.collection.ifPresent(c -> doc.setField(COLLECTIONF, c));
        rr.timestamp.ifPresent(t -> doc.setField(TIMESTAMPF, t.toString()));
        rr.rrtype.ifPresent(c -> doc.setField(RRTYPEF, c.toString()));
        rr.customerid.ifPresent(c -> doc.setField(CUSTOMERIDF, c));
        rr.app.ifPresent(c -> doc.setField(APPF, c));

    }



    private static SolrInputDocument reqToSolrDoc(Request req) {
        final SolrInputDocument doc = new SolrInputDocument();

        setRRFields(Types.Request, req, doc);
        doc.setField(TYPEF, Types.Request.toString());
        doc.setField(PATHF, req.path);
        doc.setField(METHODF, req.method);
        addFieldsToDoc(doc, QPARAMS, req.qparams);
        addFieldsToDoc(doc, FPARAMS, req.fparams);

        return doc;
    }

    private static SolrInputDocument eventToSolrDoc(Event event) {
        final SolrInputDocument doc = new SolrInputDocument();

        doc.setField(TYPEF, Types.Event.toString());
        doc.setField(CUSTOMERIDF, event.customerid);
        doc.setField(APPF, event.app);
        doc.setField(SERVICEF, event.service);
        doc.setField(INSTANCEIDF, event.instanceid);
        doc.setField(COLLECTIONF, event.getCollection());
        doc.setField(TRACEIDF, event.traceid);
        doc.setField(TIMESTAMPF, event.timestamp.toString());
        doc.setField(REQIDF, event.reqid);
        doc.setField(PATHF, event.apiPath);
        doc.setField(EVENTTYPEF, event.type.toString());
        doc.setField(PAYLOADBINF, event.rawPayloadBinary);
        doc.setField(PAYLOADSTRF, event.rawPayloadString);
        doc.setField(PAYLOADKEYF, event.payloadKey);

        return doc;
    }

    private Optional<Event> docToEvent(SolrDocument doc) {

        Optional<String> docid = getStrField(doc, IDF);
        Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);
        Optional<String> service = getStrField(doc, SERVICEF);
        Optional<String> instanceid = getStrField(doc, INSTANCEIDF);
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<String> traceid = getStrField(doc, TRACEIDF);
        Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);
        Optional<String> reqid = getStrField(doc, REQIDF);
        Optional<String> path = getStrField(doc, PATHF);
        Optional<String> type = getStrField(doc, TYPEF);
        Optional<byte[]> payloadBin = getBinField(doc, PAYLOADBINF);
        Optional<String> payloadStr = getStrField(doc, PAYLOADSTRF);
        Optional<Integer> payloadKey = getIntField(doc, PAYLOADKEYF);

        return Event.createEvent(docid.orElse("NA"), customerid, app, service, instanceid, collection,
            traceid, timestamp, reqid, path, type, payloadBin, payloadStr, payloadKey, config);
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

    private static Optional<String> getStrField(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof String)
                return Optional.of((String) v);
            return Optional.empty();
        });
    }

    private static List<String> getStrFieldMV(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            @SuppressWarnings("unchecked")
            Optional<List<String>> vals = (v instanceof List<?>) ? Optional.of((List<String>)v) : Optional.empty();
            return vals;
        }).orElse(new ArrayList<>());
    }

    // get first value of a multi-valued field
    private static Optional<String> getStrFieldMVFirst(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof List<?>)
                return ((List<String>) v).stream().findFirst();
            return Optional.empty();
        });
    }

    private static Optional<Integer> getIntField(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof Integer)
                return Optional.of((Integer) v);
            return Optional.empty();
        });
    }

    private static Optional<Double> getDblField(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof Double) {
                return Optional.of((Double) v);
            } else if (v instanceof Float) {
                return Optional.of(((Float)v).doubleValue());
            } else if (v instanceof Integer) {
                return Optional.of(((Integer)v).doubleValue());
            }
            return Optional.empty();
        });
    }

    private static Optional<Instant> getTSField(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof Date)
                return Optional.of(((Date) v).toInstant());
            return Optional.empty();
        });
    }

    private static Optional<Boolean> getBoolField(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof Boolean)
                return Optional.of((Boolean) v);
            return Optional.empty();
        });
    }

    // get binary field
    private static Optional<byte[]> getBinField(SolrDocument doc, String fname) {
        return Optional.ofNullable(doc.get(fname)).flatMap(v -> {
            if (v instanceof byte[])
                return Optional.of((byte[]) v);
            return Optional.empty();
        });
    }

    private static Optional<Request> docToRequest(SolrDocument doc) {

        Optional<String> type = getStrField(doc, TYPEF);
        Optional<String> path = getStrField(doc, PATHF);
        Optional<String> reqid = getStrField(doc, REQIDF);
        MultivaluedMap<String, String> qparams = new MultivaluedHashMap<>(); // query params
        MultivaluedMap<String, String> fparams = new MultivaluedHashMap<>(); // form params
        MultivaluedMap<String, String> meta = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<>();
        Optional<String> method = getStrField(doc, METHODF);
        Optional<String> body = getStrFieldMVFirst(doc, BODYF).or(() -> getStrField(doc, OLDBODYF)); // TODO - remove
        // OLDBODYF
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);
        Optional<RR> rrtype = getStrField(doc, RRTYPEF).flatMap(rrt -> Utils.valueOf(RR.class, rrt));
        Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);

        for (Entry<String, Object> kv : doc) {
            String k = kv.getKey();
            Object v = kv.getValue();
            Matcher m = pattern.matcher(k);
            if (m.find()) {
                switch (m.group(1)) {
                case QPARAMS:
                    checkAndAddValues(qparams, m.group(2), v);
                    break;
                case FPARAMS:
                    checkAndAddValues(fparams, m.group(2), v);
                    break;
                case META:
                    checkAndAddValues(meta, m.group(2), v);
                    break;
                case HDR:
                    checkAndAddValues(hdrs, m.group(2), v);
                    break;
                default:
                }
            }
        }

        final String p = path.orElse("");
        final String m = method.orElse("");
        final String b = body.orElse("");
        return type.map(t -> {
            if (t.equals(Types.Request.toString())) {
                return new Request(p, reqid, qparams, fparams, meta, hdrs, m, b, collection, timestamp, rrtype, customerid, app);
            } else {
                return null;
            }
        });
    }



    private static SolrInputDocument respToSolrDoc(Response resp) {
        final SolrInputDocument doc = new SolrInputDocument();

        setRRFields(Types.Response, resp, doc);
        doc.setField(TYPEF, Types.Response.toString());
        doc.setField(STATUSF, resp.status);

        return doc;
    }

    private static SolrInputDocument replayStatisticsToSolrDoc(String service, String replayId,
                                                               List<ReplayPathStatistic> pathStatistics,
                                                               ObjectMapper jsonMapper) {

        final SolrInputDocument doc = new SolrInputDocument();
        ReplayPathStatistic first = pathStatistics.get(0);
        doc.setField(CUSTOMERIDF, first.customer);
        doc.setField(APPF , first.app);
        doc.setField(SERVICEF , service);
        doc.setField(TYPEF , Types.ReplayStats.toString());
        doc.setField(REPLAYIDF , replayId);
        doc.setField(IDF , Types.ReplayStats.toString()+ "-"
                + Objects.hash(replayId , first.customer, first.app , first.service));
        pathStatistics.forEach(pathStatistic -> {
            try {
                doc.addField(REPLAYPATHSTATF, jsonMapper.writeValueAsString(pathStatistic));
            } catch (JsonProcessingException e) {
                LOGGER.error("Unable to write to solr path statistic for path ::" + pathStatistic.path + " :: service :: "
                        + service +  " :: replayid :: " + replayId);
            }
        });

        return doc;
    }



    private static Optional<Response> docToResponse(SolrDocument doc) {

        Optional<String> type = getStrField(doc, TYPEF);
        Optional<Integer> status = getIntField(doc, STATUSF);
        Optional<String> reqid = getStrField(doc, REQIDF);
        MultivaluedMap<String, String> meta = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> hdrs = new MultivaluedHashMap<>();
        Optional<String> body = getStrFieldMVFirst(doc, BODYF).or(() -> getStrField(doc, OLDBODYF)); // TODO - remove
        // OLDBODYF
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<Instant> timestamp = getTSField(doc, TIMESTAMPF);
        Optional<RR> rrtype = getStrField(doc, RRTYPEF).flatMap(rrt -> Utils.valueOf(RR.class, rrt));
        Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
        Optional<String> app = getStrField(doc, APPF);


        for (Entry<String, Object> kv : doc) {
            String k = kv.getKey();
            Object v = kv.getValue();
            Matcher m = pattern.matcher(k);
            if (m.find()) {
                switch (m.group(1)) {
                case META:
                    checkAndAddValues(meta, m.group(2), v);
                    break;
                case HDR:
                    checkAndAddValues(hdrs, m.group(2), v);
                    break;
                default:
                }
            }
        }

        final String b = body.orElse("");
        return type.flatMap(t -> {
            if (t.equals(Types.Response.toString())) {
                return status.map(sv -> new Response(reqid, sv, meta, hdrs, b, collection, timestamp,rrtype, customerid, app));
            } else {
                return Optional.empty();
            }
        });
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
            FnResponseObj ret = config.mocker.mock(saveFuncKey , CommonUtils.getCurrentTraceId(),
                CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.empty(), doc);
            if (ret.retStatus == io.cube.agent.FnReqResponse.RetStatus.Exception) {
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
                config.recorder.record(saveFuncKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), fromSolr,
                    io.cube.agent.FnReqResponse.RetStatus.Success, Optional.empty(), doc);
            }
            return toReturn;
        } catch (Throwable e) {
            if (config.intentResolver.isIntentToRecord()) {
                config.recorder.record(saveFuncKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(),
                    CommonUtils.getParentSpanId(),
                    e, io.cube.agent.FnReqResponse.RetStatus.Exception, Optional.of(e.getClass().getName()), doc);
            }
            throw e;
        }
    }

    // field names in Solr for Replay object
    private static final String IDF = "id";
    private static final String ENDPOINTF = CPREFIX + "endpoint" + STRING_SUFFIX;
    private static final String REQIDSF = CPREFIX + "reqid" + STRINGSET_SUFFIX;
    private static final String REPLAYIDF = CPREFIX + "replayid" + STRING_SUFFIX;
    private static final String ASYNCF = CPREFIX + "async" + BOOLEAN_SUFFIX;
    private static final String REPLAYSTATUSF = CPREFIX + "status" + STRING_SUFFIX;
    private static final String PATHSF = CPREFIX + "path" + STRINGSET_SUFFIX;
    private static final String REQCNTF = CPREFIX + "reqcnt" + INT_SUFFIX;
    private static final String REQSENTF = CPREFIX + "reqsent" + INT_SUFFIX;
    private static final String REQFAILEDF = CPREFIX + "reqfailed" + INT_SUFFIX;
    private static final String CREATIONTIMESTAMPF = CPREFIX + "creationtimestamp" + STRING_SUFFIX;
    private static final String SAMPLERATEF = CPREFIX + "samplerate" + DOUBLE_SUFFIX;
    private static final String REPLAYPATHSTATF = CPREFIX + "pathstat" + STRINGSET_SUFFIX;
    private static final String INTERMEDIATESERVF = CPREFIX + "intermediateserv" + STRINGSET_SUFFIX;


    // field names in Solr for compare template (stored as json)
    private static final String COMPARETEMPLATEJSON = CPREFIX + "comparetemplate" + STRING_SUFFIX;
    private static final String PARTIALMATCH = CPREFIX + "partialmatch" + STRING_SUFFIX;

    private static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    private static SolrInputDocument replayToSolrDoc(Replay replay) {
        final SolrInputDocument doc = new SolrInputDocument();

        String type = Types.ReplayMeta.toString();
        String id = type + '-' + replay.replayid;
        // the id field is set to replay id so that the document can be updated based on id
        doc.setField(IDF, id);
        doc.setField(APPF, replay.app);
        doc.setField(ASYNCF, replay.async);
        doc.setField(COLLECTIONF, replay.collection);
        doc.setField(CUSTOMERIDF, replay.customerid);
        doc.setField(INSTANCEIDF, replay.instanceid);
        doc.setField(ENDPOINTF, replay.endpoint);
        doc.setField(REPLAYIDF, replay.replayid);
        replay.reqids.forEach(reqid -> doc.addField(REQIDSF, reqid));
        doc.setField(REPLAYSTATUSF, replay.status.toString());
        doc.setField(TYPEF, type);
        replay.paths.forEach(path -> doc.addField(PATHSF, path));
        doc.setField(REQCNTF, replay.reqcnt);
        doc.setField(REQSENTF, replay.reqsent);
        doc.setField(REQFAILEDF, replay.reqfailed);
        doc.setField(CREATIONTIMESTAMPF, replay.creationTimeStamp);
        replay.intermediateServices.forEach(service -> doc.addField(INTERMEDIATESERVF , service));
        replay.samplerate.ifPresent(sr -> doc.setField(SAMPLERATEF, sr));
        replay.templateVersion.ifPresent(templateVer -> doc.setField(TEMPLATE_VERSION, templateVer));

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
        String type = getTemplateType(key);
        // Sample key in solr ResponseCompareTemplate-1234-bookinfo-getAllBooks--2013106077
        String id = type.concat("-").concat(String.valueOf(Objects.hash(
                key.getCustomerId() , key.getAppId() , key.getServiceId() , key.getPath()
                , key.getReqOrResp().toString() , key.getVersion())));
        doc.setField(IDF , id);
        doc.setField(COMPARETEMPLATEJSON, jsonCompareTemplate);
        String path = key.getPath();
        doc.setField(PATHF , path);
        doc.setField(APPF , key.getAppId());
        doc.setField(CUSTOMERIDF , key.getCustomerId());
        doc.setField(SERVICEF , key.getServiceId());
        doc.setField(TYPEF , type);
        doc.setField(TEMPLATE_VERSION, key.getVersion());
        return doc;
    }



    private static Optional<Replay> docToReplay(SolrDocument doc, ReqRespStore rrstore) {

        Optional<String> app = getStrField(doc, APPF);
        Optional<String> instanceid = getStrField(doc, INSTANCEIDF);
        Optional<Boolean> async = getBoolField(doc, ASYNCF);
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
        Optional<String> endpoint = getStrField(doc, ENDPOINTF);
        Optional<String> replayid = getStrField(doc, REPLAYIDF);
        List<String> reqids = getStrFieldMV(doc, REQIDSF);
        Optional<ReplayStatus> status = getStrField(doc, REPLAYSTATUSF).flatMap(s -> Utils.valueOf(ReplayStatus.class, s));
        List<String> paths = getStrFieldMV(doc, PATHSF);
        int reqcnt = getIntField(doc, REQCNTF).orElse(0);
        int reqsent = getIntField(doc, REQSENTF).orElse(0);
        int reqfailed = getIntField(doc, REQFAILEDF).orElse(0);
        Optional<String> creationTimestamp = getStrField(doc, CREATIONTIMESTAMPF);
        Optional<Double> samplerate = getDblField(doc, SAMPLERATEF);
        List<String> intermediateService = getStrFieldMV(doc , INTERMEDIATESERVF);
        Optional<String> templateVersion = getStrField(doc, TEMPLATE_VERSION);

        Optional<Replay> replay = Optional.empty();
        if (endpoint.isPresent() && customerid.isPresent() && app.isPresent() &&
                instanceid.isPresent() && collection.isPresent()
                && replayid.isPresent() && async.isPresent() && status.isPresent() /*&& templateVersion.isPresent()*/) {
            try {
				replay = Optional.of(new Replay(endpoint.get(), customerid.get(), app.get(), instanceid.get(), collection.get(),
				        reqids, replayid.get(), async.get(), templateVersion, status.get(), paths, reqcnt, reqsent, reqfailed,
                        creationTimestamp.isEmpty() ? format.parse("2010-01-01 00:00:00.000").toString() : creationTimestamp.get(),
                        samplerate , intermediateService));
			} catch (ParseException e) {
				LOGGER.error(String.format("Not able to convert Solr result to Replay object for replay id %s", replayid.orElse("")));
			}
        } else {
            LOGGER.error(String.format("Not able to convert Solr result to Replay object for replay id %s", replayid.orElse("")));
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
        super.saveReplay(replay);
        SolrInputDocument doc = replayToSolrDoc(replay);
        return saveDoc(doc) && softcommit();
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getReplay(java.lang.String)
     */
    @Override
    public Optional<Replay> getReplay(String replayid) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        //query.setRows(1);
        addFilter(query, TYPEF, Types.ReplayMeta.toString());
        addFilter(query, REPLAYIDF, replayid);

        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> docToReplay(doc, this));

    }

    /**
     * Extract ResponseCompareTemplate from query result
     * @param doc Retrieve Result
     * @return
     */
    private  Optional<CompareTemplate> docToCompareTemplate(SolrDocument doc) {
        return getStrField(doc, COMPARETEMPLATEJSON).flatMap(templateJson -> {
            try {
                return Optional.of(config.jsonmapper.readValue(templateJson, CompareTemplate.class));
            } catch (IOException e) {
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
    public String saveCompareTemplate(TemplateKey key, String templateAsJson) {
        SolrInputDocument solrDoc = compareTemplateToSolrDoc(key ,templateAsJson);
        boolean success =  saveDoc(solrDoc) && softcommit();
        return solrDoc.getFieldValue(IDF).toString();
    }

    public static String getTemplateType(TemplateKey key) {
        return (key.getReqOrResp() == TemplateKey.Type.Request) ?
                Types.RequestCompareTemplate.toString() : Types.ResponseCompareTemplate.toString();
    }


    /**
     * Get compare template from solr for the given key parameters
     * @param key
     * @return
     */
    @Override
    public Optional<CompareTemplate> getCompareTemplate(TemplateKey key) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, getTemplateType(key));
        addFilter(query, CUSTOMERIDF, key.getCustomerId());
        addFilter(query, APPF, key.getAppId());
        addFilter(query , SERVICEF , key.getServiceId());
        addWeightedPathFilter(query , PATHF , key.getPath());
        addFilter(query, TEMPLATE_VERSION, key.getVersion(), true);
        //addFilter(query, PATHF , key.getPath());
        Optional<Integer> maxResults = Optional.of(1);
        return SolrIterator.getStream(solr , query , maxResults).findFirst().flatMap(this::docToCompareTemplate);
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getReplay(java.util.Optional, java.util.Optional, java.util.Optional, com.cube.dao.Replay.ReplayStatus)
     */
    @Override
    public Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app, Optional<String> instanceid,
                                    ReplayStatus status) {
        return getReplay(customerid,app,instanceid,List.of(status),Optional.of(1),Optional.empty());
    }

    @Override
    public Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app, List<String> instanceid,
                             List<ReplayStatus> status, Optional<Integer> numofResults, Optional<String> collection) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.ReplayMeta.toString());
        addFilter(query, CUSTOMERIDF, customerid);
        addFilter(query, APPF, app);
        addFilter(query, INSTANCEIDF, instanceid);
        addFilter(query, REPLAYSTATUSF, status.stream().map(ReplayStatus::toString).collect(Collectors.toList()));
        addFilter(query, COLLECTIONF , collection);
        // Heuristic: getting the latest replayid if there are multiple.
        // TODO: what happens if there are multiple replays running for the
        // same triple (customer, app, instance)
        addSort(query, CREATIONTIMESTAMPF, false /* desc */);

        //Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, numofResults).flatMap(doc -> docToReplay(doc, this).stream());
    }

    @Override
    public Stream<Replay> getReplay(Optional<String> customerid, Optional<String> app, Optional<String> instanceid,
            List<ReplayStatus> status, Optional<Integer> numofResults, Optional<String> collection) {
        //Reference - https://stackoverflow.com/a/31688505/3918349
        List<String> instanceidList = instanceid.stream().collect(Collectors.toList());
        return getReplay(customerid, app, instanceidList, status, numofResults, collection);
    }

    // Some useful functions
    private static SolrQuery reqMatchSpecToSolrQuery(Request qr, RequestComparator spec) {
        final SolrQuery query = new SolrQuery("*:*");
        final StringBuffer qstr = new StringBuffer("*:*");
        query.addField("*");

        addMatch(spec.getCTreqid(), query, qstr, REQIDF, qr.reqid);
        addMatch(query, qstr, META, qr.meta, spec.getCTMeta());
        addMatch(query, qstr, HDR, qr.hdrs, spec.getCTHdrs());
        addMatch(spec.getCTbody(), query, qstr, BODYF, qr.body);
        addMatch(spec.getCTcollection(), query, qstr, COLLECTIONF, qr.collection);
        addMatch(spec.getCTtimestamp(), query, qstr, TIMESTAMPF, qr.timestamp.toString());
        addMatch(spec.getCTrrtype(), query, qstr, RRTYPEF, qr.rrtype.map(Enum::toString));
        addMatch(spec.getCTcustomerid(), query, qstr, CUSTOMERIDF, qr.customerid);
        addMatch(spec.getCTapp(), query, qstr, APPF, qr.app);
        //addMatch(spec.getCTcontenttype, query, qstr, CONTENTTYPEF, qr.hdrs.getHeaderString(HttpHeaders.CONTENT_TYPE));

        addMatch(spec.getCTpath(), query, qstr, PATHF, qr.path);
        addMatch(query, qstr, QPARAMS, qr.qparams, spec.getCTQparams());
        addMatch(query, qstr, FPARAMS, qr.fparams, spec.getCTFparams());
        addMatch(spec.getCTmethod(), query, qstr, METHODF, qr.method);

        addFilter(query, TYPEF, Types.Request.toString());

        query.setQuery(qstr.toString());
        return query;
    }

    private static final String OBJJSONF = CPREFIX + "json" + NOTINDEXED_SUFFIX;



    public boolean saveMatchResultAggregate(MatchResultAggregate resultAggregate) {
        SolrInputDocument doc = matchResultAggregateToSolrDoc(resultAggregate);
        return saveDoc(doc) && softcommit();
    }

    /**
     * @param resultAggregate
     * @return
     */
    private SolrInputDocument matchResultAggregateToSolrDoc(MatchResultAggregate resultAggregate) {
        final SolrInputDocument doc = new SolrInputDocument();

        String resultAggregateJson="";
        try {
            resultAggregateJson = config.jsonmapper.writeValueAsString(resultAggregate);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting MatchResultAggregate object into string for replay id %s", resultAggregate.replayid), e);
        }

        String type = Types.MatchResultAggregate.toString();
        // the id field is set using (replayid, service, path) which is unique
        String id = type + "-" + Objects.hash(resultAggregate.replayid, resultAggregate.service.orElse(""), resultAggregate.path.orElse(""));

        doc.setField(TYPEF, type);
        doc.setField(IDF, id);
        doc.setField(APPF, resultAggregate.app);
        doc.setField(REPLAYIDF, resultAggregate.replayid);
        doc.setField(OBJJSONF, resultAggregateJson);

        // Set the fields if present else put the default value
        doc.setField(SERVICEF, resultAggregate.service.orElse(DEFAULT_EMPTY_FIELD_VALUE));
        doc.setField(PATHF, resultAggregate.path.orElse(DEFAULT_EMPTY_FIELD_VALUE));

        return doc;
    }


    public Stream<MatchResultAggregate> getResultAggregate(String replayid, Optional<String> service,
                                                        boolean bypath) {

        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, TYPEF, Types.MatchResultAggregate.toString());
        addFilter(query, REPLAYIDF, replayid);
//        addFilter(query, SERVICEF, service.orElse(DEFAULT_EMPTY_FIELD_VALUE));
        service.ifPresent(servicev -> addFilter(query, SERVICEF, servicev));

        if(!bypath) {
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
                return Optional.ofNullable(config.jsonmapper.readValue(j, MatchResultAggregate.class));
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
    public boolean saveAnalysis(Analysis analysis) {
        SolrInputDocument doc = analysisToSolrDoc(analysis);
        return saveDoc(doc) && softcommit();
    }

    /**
     * @param analysis
     * @return
     */
    private SolrInputDocument analysisToSolrDoc(Analysis analysis) {
        final SolrInputDocument doc = new SolrInputDocument();

        String json="";
        try {
            json = config.jsonmapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            LOGGER.error(String.format("Error in converting Analysis object into string for replay id %s", analysis.replayid), e);
        }

        String type = Types.Analysis.toString();
        String id = type + '-' + analysis.replayid;
        // the id field is set to replay id so that the document can be updated based on id
        doc.setField(IDF, id);
        doc.setField(REPLAYIDF, analysis.replayid);
        doc.setField(OBJJSONF, json);
        doc.setField(TYPEF, type);
        analysis.templateVersion.ifPresent(v -> doc.setField(TEMPLATE_VERSION, v));

        return doc;
    }

    private static final String RECORDREQIDF = CPREFIX + "recordreqid" + STRING_SUFFIX;
    private static final String REPLAYREQIDF = CPREFIX + "replayreqid" + STRING_SUFFIX;
    private static final String REQMTF = CPREFIX + "reqmt" + STRING_SUFFIX;
    private static final String NUMMATCHF = CPREFIX + "nummatch" + INT_SUFFIX;
    private static final String RESPMTF = CPREFIX + "respmt" + STRING_SUFFIX; // match type
    private static final String RESPMATCHMETADATAF = CPREFIX + "respmatchmetadata" + STRING_SUFFIX;
    private static final String DIFFF = CPREFIX + "diff" + NOTINDEXED_SUFFIX;
    private static final String SERVICEF = CPREFIX + "service" + STRING_SUFFIX;
    private static final String RECORDTRACEIDF = CPREFIX + "recordtraceid" + STRING_SUFFIX;
    private static final String REPLAYTRACEIDF = CPREFIX + "replaytraceid" + STRING_SUFFIX;

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#saveResult(com.cube.dao.Analysis.Result)
     */
    @Override
    public boolean saveResult(ReqRespMatchResult res) {
        SolrInputDocument doc = resultToSolrDoc(res);
        return saveDoc(doc);
    }

    /**
     * @param res
     * @return
     */
    private SolrInputDocument resultToSolrDoc(ReqRespMatchResult res) {
        final SolrInputDocument doc = new SolrInputDocument();

        // usually result will never be updated. But we set id field uniquely anyway

        String type = Types.ReqRespMatchResult.toString();
        // the id field is to (recordreqid, replayreqid) which is unique
        String id = type + '-' + res.recordreqid.orElse("None") + '-' + res.replayreqid.orElse("None");

        doc.setField(TYPEF, type);
        doc.setField(IDF, id);
        res.recordreqid.ifPresent(recordReqId ->  doc.setField(RECORDREQIDF, recordReqId));
        res.replayreqid.ifPresent(replayReqId ->  doc.setField(REPLAYREQIDF, replayReqId));
        doc.setField(REQMTF, res.reqmt.toString());
        doc.setField(NUMMATCHF, res.nummatch);
        doc.setField(RESPMTF, res.respmt.toString());
        doc.setField(RESPMATCHMETADATAF, res.respmatchmetadata);
        doc.setField(DIFFF, res.diff);
        doc.setField(CUSTOMERIDF, res.customerid);
        doc.setField(APPF, res.app);
        doc.setField(SERVICEF, res.service);
        doc.setField(PATHF, res.path);
        doc.setField(REPLAYIDF, res.replayid);
        res.recordTraceId.ifPresent(traceId  -> doc.setField(RECORDTRACEIDF, traceId));
        res.replayTraceId.ifPresent(traceId  -> doc.setField(REPLAYTRACEIDF, traceId));
        return doc;
    }

    /**
     * Get request/response match result (as computed by analysis) for a given recorded request and
     * replay Id combination. The assumption is there will be only one such result  in solr per request/replay.
     * Ideally the analysis should overwrite the result, if we perform analysis for the same replay.
     * @param recordReqId
     * @param replayId
     * @return
     */
    @Override
    public Optional<ReqRespMatchResult> getAnalysisMatchResult(String recordReqId , String replayId) {
            SolrQuery query = new SolrQuery("*:*");
            query.setFields("*");
            addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
            addFilter(query, RECORDREQIDF, recordReqId);
            addFilter(query, REPLAYIDF, replayId);

            Optional<Integer> maxresults = Optional.of(1);
            return SolrIterator.getStream(solr, query, maxresults).findFirst()
                    .flatMap(doc -> docToAnalysisMatchResult(doc));
    }

    @Override
    public Optional<ReqRespMatchResult> getAnalysisMatchResult(Optional<String> recordReqId, Optional<String> replayReqId,
                                                        String replayId) {
        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
        addFilter(query, RECORDREQIDF, recordReqId, true);
        addFilter(query, REPLAYREQIDF, replayReqId, true);
        addFilter(query, REPLAYIDF, replayId);

        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst()
            .flatMap(doc -> docToAnalysisMatchResult(doc));
    }


    @Override
    public Result<ReqRespMatchResult>
    getAnalysisMatchResults(String replayId, Optional<String> service, Optional<String> path, Optional<Comparator.MatchType> reqmt,
                            Optional<Comparator.MatchType> respmt, Optional<Integer> start, Optional<Integer> nummatches) {

        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
        addFilter(query, REPLAYIDF, replayId);
        addFilter(query, SERVICEF, service);
        addFilter(query, PATHF, path);
        addFilter(query, REQMTF, reqmt.map(Enum::toString));
        addFilter(query, RESPMTF, respmt.map(Enum::toString));

        return SolrIterator.getResults(solr, query, nummatches, ReqRespStoreSolr::docToAnalysisMatchResult, start);
    }

    /**
     * Save Replay Stats for a Virtual(Mock) Service. The stats (request match/not match counts)
     * are stored path wise as a json string in the same solr document.
     * @param pathStatistics
     * @param replayId
     */
    @Override
    public void saveReplayResult(Map<String, List<ReplayPathStatistic>> pathStatistics
            , String replayId) {
            pathStatistics.entrySet().forEach(entry-> {
                SolrInputDocument inputDocument = replayStatisticsToSolrDoc(entry.getKey() , replayId
                        , entry.getValue() ,config.jsonmapper);
                saveDoc(inputDocument);
            });
    }

    /**
     * Get Request Match / Not Match Count for a given virtual(mock) service during replay.
     * Return the statistics for each path in the service as a separate json string
     * @param customer
     * @param app
     * @param service
     * @param replayId
     * @return
     */
    @Override
    public List<String> getReplayRequestCounts(String customer, String app, String service, String replayId) {
        SolrQuery query = new SolrQuery("*:*");
        query.setFields("*");
        addFilter(query, CUSTOMERIDF, customer);
        addFilter(query, APPF, app);
        addFilter(query, SERVICEF, service);
        addFilter(query, REPLAYIDF, replayId);
        addFilter(query, TYPEF, Types.ReplayStats.toString());
        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr , query , maxresults)
                .findFirst().map(doc -> getReplayStats(doc)).orElse(Collections.EMPTY_LIST);
    }

    private List<String> getReplayStats(SolrDocument document) {
        return getStrFieldMV(document ,  REPLAYPATHSTATF);
    }

    /**
     * Convert Solr document to corresponding ReqRespMatchResult object
     * @param doc
     * @return
     */
    private static Optional<ReqRespMatchResult> docToAnalysisMatchResult(SolrDocument doc) {
        Optional<String> recordReqId = getStrField(doc , RECORDREQIDF);
        Optional<String> replayReqId = getStrField(doc, REPLAYREQIDF);
        String replayId = getStrField(doc, REPLAYIDF).orElse("");

        Comparator.MatchType reqMatchType = getStrField(doc , REQMTF)
                .map(Comparator.MatchType::valueOf).orElse(Comparator.MatchType.Default);
        Comparator.MatchType respMatchType = getStrField(doc, RESPMTF)
                .map(Comparator.MatchType::valueOf).orElse(Comparator.MatchType.Default);
        Integer nummatch = getIntField(doc , NUMMATCHF).orElse(-1);
        String respMatchMetaData = getStrField(doc , RESPMATCHMETADATAF).orElse("");
        String diff = getStrFieldMV(doc , DIFFF).stream().findFirst().orElse("");
        String customerId = getStrField(doc , CUSTOMERIDF).orElse("");
        String app = getStrField(doc , APPF).orElse("");
        String service = getStrField(doc, SERVICEF).orElse("");
        String path = getStrField(doc, PATHF).orElse("");
        Optional<String> recordTraceId = getStrField(doc, RECORDTRACEIDF);
        Optional<String> replayTraceId = getStrField(doc, REPLAYTRACEIDF);
        return Optional.of(new ReqRespMatchResult(
                recordReqId , replayReqId , reqMatchType , nummatch , respMatchType, respMatchMetaData,
                diff, customerId, app, service, path, replayId, recordTraceId, replayTraceId));
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getAnalysis(java.lang.String)
     */
    @Override
    public Optional<Analysis> getAnalysis(String replayid) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        //query.setRows(1);
        addFilter(query, TYPEF, Types.Analysis.toString());
        addFilter(query, REPLAYIDF, replayid);

        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> docToAnalysis(doc, this));

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
                return Optional.ofNullable(config.jsonmapper.readValue(j, Analysis.class));
            } catch (IOException e) {
                LOGGER.error(String.format("Not able to parse json into Analysis object: %s", j), e);
                return Optional.empty();
            }
        });
        return analysis;
    }

    private static final String RECORDINGSTATUSF = CPREFIX + "status" + STRING_SUFFIX;
    private static final String ROOT_RECORDING_ID = "root_recording_id" + STRING_SUFFIX;
    private static final String PARENT_RECORDING_ID = "parent_recording_id" + STRING_SUFFIX;

    private static Optional<Recording> docToRecording(SolrDocument doc) {

        Optional<String> app = getStrField(doc, APPF);
        Optional<String> instanceid = getStrField(doc, INSTANCEIDF);
        Optional<String> collection = getStrField(doc, COLLECTIONF);
        Optional<String> customerid = getStrField(doc, CUSTOMERIDF);
        Optional<RecordingStatus> status = getStrField(doc, RECORDINGSTATUSF).flatMap(s -> Utils.valueOf(RecordingStatus.class, s));
        Optional<Recording> recording = Optional.empty();
        Optional<String> templateVersion = getStrField(doc, TEMPLATE_VERSION);
        Optional<String> parentRecordingId = getStrField(doc, PARENT_RECORDING_ID);
        Optional<String> rootRecordingId = getStrField(doc, ROOT_RECORDING_ID);
        if (customerid.isPresent() && app.isPresent()
                && instanceid.isPresent() && collection.isPresent() && status.isPresent()) {
            recording = Optional.of(new Recording(customerid.get(), app.get(), instanceid.get(), collection.get(),
                status.get() ,  getTSField(doc, TIMESTAMPF), templateVersion, parentRecordingId, rootRecordingId));
        } else {
            LOGGER.error(String.format("Not able to convert Solr result to Recording object for customerid %s, app id %s, instance id %s",
                    customerid.orElse(""), app.orElse(""), instanceid.orElse("")));
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
        doc.setField(CUSTOMERIDF, recording.customerid);
        doc.setField(APPF, recording.app);
        doc.setField(INSTANCEIDF, recording.instanceid);
        doc.setField(COLLECTIONF, recording.collection);
        doc.setField(RECORDINGSTATUSF, recording.status.toString());
        recording.templateVersion.ifPresent(templateVer -> doc.setField(TEMPLATE_VERSION, templateVer));
        recording.parentRecordingId.ifPresent(parentRecId -> doc.setField(PARENT_RECORDING_ID, parentRecId));
        recording.rootRecordingId.ifPresent(rootRecId -> doc.setField(ROOT_RECORDING_ID, rootRecId));
        recording.updateTimestamp.ifPresent(timestamp -> doc.setField(TIMESTAMPF , timestamp.toString()));

        return doc;
    }


    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.dao.Replay)
     */
    @Override
    public boolean saveRecording(Recording recording) {
        super.saveRecording(recording);
        SolrInputDocument doc = recordingToSolrDoc(recording);
        return saveDoc(doc) && softcommit();
    }

    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRecording(java.util.Optional, java.util.Optional, java.util.Optional, com.cube.dao.Recording.RecordingStatus)
     */
    @Override
    public Stream<Recording> getRecording(Optional<String> customerid, Optional<String> app,
            Optional<String> instanceid, Optional<RecordingStatus> status) {

        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, CUSTOMERIDF, customerid);
        addFilter(query, APPF, app);
        addFilter(query, INSTANCEIDF, instanceid);
        addFilter(query, RECORDINGSTATUSF, status.map(Enum::toString));
        addSort(query, TIMESTAMPF, false); // descending

        //Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, Optional.empty()).flatMap(doc -> docToRecording(doc).stream());
    }

    @Override
    public Optional<Recording> getRecording(String recordingId) {
        SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, IDF, recordingId);
        return SolrIterator.getStream(solr, query, Optional.of(1)).findFirst().flatMap(doc -> docToRecording(doc));
    }


    /* (non-Javadoc)
     * @see com.cube.dao.ReqRespStore#getRecordingByCollection(java.lang.String, java.lang.String, java.lang.String)
     * (cust, app, collection) is a unique key, so only record will satisfy at most
     */
    @Override
    public Optional<Recording> getRecordingByCollectionAndTemplateVer(String customerid, String app,
                                                        String collection, Optional<String> templateSetVersion) {
        final SolrQuery query = new SolrQuery("*:*");
        query.addField("*");
        addFilter(query, TYPEF, Types.Recording.toString());
        addFilter(query, CUSTOMERIDF, customerid);
        addFilter(query, APPF, app);
        addFilter(query, COLLECTIONF, collection);
        addFilter(query, TEMPLATE_VERSION, templateSetVersion);
        Optional<Integer> maxresults = Optional.of(1);
        return SolrIterator.getStream(solr, query, maxresults).findFirst().flatMap(doc -> docToRecording(doc));
    }

    private final static int FACETLIMIT = 100;
    private static final String REQMTFACET = "reqmt_facets";
    private static final String RESPMTFACET = "respmt_facets";
    private static final String PATHFACET = "path_facets";
    private static final String SERVICEFACET = "service_facets";
    private static final String SOLRJSONFACETPARAM = "json.facet"; // solr facet query param
    private static final String BUCKETFIELD = "buckets"; // term in solr results indicating facet buckets
    private static final String VALFIELD = "val"; // term in solr facet results indicating a distinct value of the field
    private static final String COUNTFIELD = "count"; // term in solr facet results indicating aggregate value computed
    private static final String FACETSFIELD = "facets"; // term in solr facet results indicating the facet results block


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
    public Collection<MatchResultAggregate> computeResultAggregate(String replayid,
                                                                   Optional<String> service, boolean facetpath) {

        Optional<Replay> replay = getReplay(replayid);
        Map<FacetResKey, MatchResultAggregate> resMap = new HashMap<>();

        replay.ifPresent(replayv -> {
            final SolrQuery query = new SolrQuery("*:*");
            query.addField("*");
            addFilter(query, TYPEF, Types.ReqRespMatchResult.toString());
            addFilter(query, REPLAYIDF, replayv.replayid);
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
            Facet respmatchf = Facet.createTermFacet(RESPMTF, Optional.of(FACETLIMIT));
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
                json = config.jsonmapper.writeValueAsString(facetq);
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
                            MatchResultAggregate mra = new MatchResultAggregate(replayv.app, replayv.replayid, frkey.service, frkey.path);
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

    static class Facet {

        private static final String TYPEK = "type";
        private static final String FIELDK = "field";
        private static final String LIMITK = "limit";
        private static final String FACETK = "facet";

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
            Map<String, Object> params = new HashMap<>();
            params.put(TYPEK, "terms");
            params.put(FIELDK, fieldname);
            limit.ifPresent(l -> params.put(LIMITK, l));

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
                    .flatMap(bucketobj -> getListFromNL(bucketobj, BUCKETFIELD))
                    .ifPresent(bucketarrobj -> {
                        bucketarrobj.forEach(bucket -> {
                            objToNL(bucket).ifPresent(b -> {
                                Optional<String> val = getStringFromNL(b, VALFIELD);
                                Optional<Integer> count = getIntFromNL(b, COUNTFIELD);
                                val.ifPresent(v -> {
                                    List<FacetR> subfacetr = flatten(b, rest);
                                    FacetRKey rkey = new FacetRKey(v, facetname);
                                    subfacetr.forEach(subr -> {
                                        subr.addKey(rkey);
                                        results.add(subr);
                                    });
                                    count.ifPresent(c -> {
                                        List<FacetRKey> frkeys = new ArrayList<>();
                                        frkeys.add(rkey);
                                        FacetR newr = new FacetR(frkeys, c);
                                        results.add(newr);
                                    });
                                });
                            });
                        });
                    });
            });
        }
        return results;
    }

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#commit()
	 */
	@Override
	public boolean commit() {

		return softcommit();
	}

}
