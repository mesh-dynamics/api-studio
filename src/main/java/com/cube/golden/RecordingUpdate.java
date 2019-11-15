package com.cube.golden;


import com.cube.cache.TemplateKey;
import com.cube.core.RequestComparator;
import com.cube.dao.*;
import com.cube.ws.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecordingUpdate {

    private final Config config;
    private final ResponseTransformer responseTransformer;
    private static final Logger LOGGER = LogManager.getLogger(RecordingUpdate.class);

    public RecordingUpdate(Config config) {
        this.config = config;
        this.responseTransformer = new ResponseTransformer(config.jsonMapper);
    }

    /*
    * create operation set and return the id
    */
    public String createRecordingOperationSet(String customer, String app){
        RecordingOperationSetMeta recordingOperationSetMeta = new RecordingOperationSetMeta(customer, app);
        LOGGER.info("Creating new recording operation set with id: " + recordingOperationSetMeta.id);
        boolean stored = config.rrstore.storeRecordingOperationSetMeta(recordingOperationSetMeta);
        if (!stored) {
            LOGGER.error("error storing recording operation set");
            return null; // todo: what to return if storing fails?
        }
        // if successful return id
        return recordingOperationSetMeta.id;
    }

    /*
    * update the operation set
    * could be to partially or completely update existing operations or create a new operation set if not present
    */
    public boolean updateRecordingOperationSet(RecordingOperationSetSP updateRequest){
        // fetch operation set meta from Solr to verify the recordingOperationSetId
        LOGGER.info("Fetching and verifying recordingOperationSetId");

        Optional<RecordingOperationSetMeta> recordingOperationSetMeta = config.rrstore.getRecordingOperationSetMeta(
            updateRequest.operationSetId);
        if(recordingOperationSetMeta.isEmpty()) {
            LOGGER.error(String.format("recording operation set with id %s does not exist", updateRequest.operationSetId));
            // todo raise error?
            return false;
        }

        // fetch the operation set from Solr
        LOGGER.info(String.format("Fetching RecordingOperationSetSP for service %s, path %s with " +
                "recordingOperationSetId %s", updateRequest.service,
            updateRequest.path, updateRequest.operationSetId));
        Optional<RecordingOperationSetSP> storedOperationSet = config.rrstore.getRecordingOperationSetSP(updateRequest.operationSetId,
            updateRequest.service, updateRequest.path);
        return storedOperationSet
            // if present, update/insert the new operations
            .map(recordingOperationSet -> {
                // convert operation list to map
                LOGGER.debug("recording operation set: " + recordingOperationSet);
                Map<String, ReqRespUpdateOperation> operationMap = createOperationsMap(recordingOperationSet.operationsList);
                // upsert new operation for path
                LOGGER.debug("updating operations");
                updateRequest.operationsList.forEach(
                    newOperation -> operationMap.put(newOperation.jsonpath, newOperation)
                );
                recordingOperationSet.setOperationsList(new ArrayList<>(operationMap.values()));
                // store it back
                // if successful return true
                LOGGER.info("Storing updated operation set");
                return config.rrstore.storeRecordingOperationSet(recordingOperationSet);
            })
            // if empty, create a new one (we have verified the existence of the recordingOperationSetId)
            .orElseGet(() -> {
                LOGGER.info("RecordingOperationSetSP not found, creating new one");
//                RecordingOperationSetSP recordingOperationSet
//                    = new RecordingOperationSetSP(recordingOperationSetId, recordingOperationSetMeta.get().customer,
//                    recordingOperationSetMeta.get().app, Optional.of(service), Optional.of(path), newOperationSet);
                return config.rrstore.storeRecordingOperationSet(updateRequest);
            });
    }

    // convert operations list to map (jsonpath->operation)
    private Map<String, ReqRespUpdateOperation> createOperationsMap(List<ReqRespUpdateOperation> operationsList) {
        LOGGER.debug("converting operation set list to map of (jsonpath -> operation)");
        return operationsList.stream()
            .collect(Collectors.toMap(
                op -> op.jsonpath, op -> op));
    }

    /*
    * fetch recording operation set given the id
    */
    public Optional<RecordingOperationSetSP> getRecordingOperationSet(String recordingOperationSetId,
                                                                      String service, String path){
        return config.rrstore.getRecordingOperationSetSP(recordingOperationSetId, service, path);
    }

    /*
    * apply the operations on a recording collection
     */
    // TODO: Event redesign: This needs to be rewritten to get as event
    // TODO: sort the match results by (service, apiPath) and process each apiPath at a time, so that comparator need
    //  not be lookup up repeatedly
    public boolean applyRecordingOperationSet(String replayId, String newCollectionName,
                                                String recordingOperationSetId, Recording originalRec) {


        // use recordingOperationSetId to fetch the list of operations
        // use replayid to fetch the analyze results (reqrespmatchresults)
        // apply the operations to the results
        // create a new collection
        // store it
        Map<String, RecordingOperationSetSP> apiPathVsUpdateOperationSet =
            config.rrstore.getRecordingOperationSetSPs(recordingOperationSetId).collect(Collectors.toMap( set -> set.path
            , Function.identity()));
        Stream<Analysis.ReqRespMatchResult> results = getReqRespMatchResultStream(replayId/*, recordingOperationSetSP*/);
        results.forEach(res -> {
            try {
            LOGGER.debug(String.format("get record and replay responses with recordReqId %s, replayReqId %s",
                res.recordReqId.get(), res.replayReqId.get()));
            Request recordRequest = res.recordReqId.flatMap(config.rrstore::getRequest)
                .flatMap(event -> Request.fromEvent(event, config.jsonMapper))
                .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordReqId.get()));
            Response recordResponse = res.recordReqId.flatMap(config.rrstore::getResponse)
                .flatMap(event -> Response.fromEvent(event, config.jsonMapper))
                .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordReqId.get()));
            Optional<Response> replayResponse = res.replayReqId.flatMap(config.rrstore::getResponse)
                .flatMap(event -> Response.fromEvent(event, config.jsonMapper));

            Optional<RecordingOperationSetSP> updateOperationSet = Optional.ofNullable(
                apiPathVsUpdateOperationSet.get(recordRequest.apiPath));


            Optional<String> newReqId = generateReqIdOld(recordResponse.reqId, newCollectionName);

            String transformedResponseBody = replayResponse.flatMap(repResponse ->
                updateOperationSet.flatMap(updateOpSet ->
                    responseTransformer.transformResponse(recordResponse.body , repResponse.body, updateOpSet.operationsList)))
                    .orElse(recordResponse.body);

            Response transformedResponse = new Response(newReqId, recordResponse.status,
                recordResponse.meta, recordResponse.hdrs, transformedResponseBody, Optional.of(newCollectionName),
                    recordResponse.timestamp, recordResponse.runType, recordResponse.customerId, recordResponse.app,
                recordResponse.apiPath);

            LOGGER.debug("applying transformations");
            recordRequest.reqId = transformedResponse.reqId;
            recordRequest.collection = Optional.of(newCollectionName);

            TemplateKey key = new TemplateKey(originalRec.templateVersion, originalRec.customerId,
                originalRec.app, recordRequest.getService().orElse("NA"), recordRequest.apiPath,
                TemplateKey.Type.Request);
            RequestComparator comparator = config.requestComparatorCache.getRequestComparator(key , true);


            LOGGER.debug("saving request/response with reqId: " + transformedResponse.reqId);
            boolean saved =
                config.rrstore.save(recordRequest.toEvent(comparator, config)) && config.rrstore.save(transformedResponse.toEvent(config, recordRequest.apiPath));
            if(!saved) {
                LOGGER.debug("request/response not saved");
                // todo raise error?
            }
            } catch (Exception e) {
                LOGGER.error("Error occured while transforming response :: " + e.getMessage(), e);
            }

        });

        config.rrstore.saveFnReqRespNewCollec(originalRec.customerId, originalRec.app,
            originalRec.collection, newCollectionName);


        // get the operation sets using operation set id
        // for each operation set, perform the transformation and store the new collection
        /*LOGGER.debug("fetching RecordingOperationSets for " + recordingOperationSetId);
        config.rrstore.getRecordingOperationSetSPs(recordingOperationSetId)
            .forEach(recordingOperationSet ->
                fetchTransformAndStore(replayId, newCollectionName, recordingOperationSet)
            );*/

        return true; // todo: false?
    }

    public boolean createSanitizedCollection(String replayId, String newCollectionName, Recording originalRec) {

        Stream<Analysis.ReqRespMatchResult> results = getReqRespMatchResultStream(replayId);

        //1. Create a new collection with all the Req/Responses
        results.forEach(res -> {
            try {
                Event recordRequest = res.recordReqId.flatMap(config.rrstore::getRequest)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordReqId.get()));
                Event recordResponse = res.recordReqId.flatMap(config.rrstore::getResponse)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordReqId.get()));

                String newReqId = generateReqId(recordResponse.reqId, newCollectionName);

                Event transformedResponse = new Event.EventBuilder(recordResponse.customerId, recordResponse.app, recordResponse.service, recordResponse.instanceId, "", recordResponse.traceId,
                    recordResponse.runType, recordResponse.timestamp, newReqId, recordResponse.apiPath, recordResponse.eventType)
                    .setRawPayloadBinary(recordResponse.rawPayloadBinary)
                    .setRawPayloadString(recordResponse.rawPayloadString)
                    .setPayloadKey(recordResponse.payloadKey)
                    .createEvent();

                transformedResponse.setCollection(newCollectionName);

                LOGGER.debug("Changing the reqid and collection name in the response for the sanitized collection");

                Event transformedRequest = new Event.EventBuilder(recordRequest.customerId, recordRequest.app, recordRequest.service, recordRequest.instanceId, "", recordRequest.traceId,
                    recordRequest.runType, recordRequest.timestamp, newReqId, recordRequest.apiPath, recordRequest.eventType)
                    .setRawPayloadBinary(recordRequest.rawPayloadBinary)
                    .setRawPayloadString(recordRequest.rawPayloadString)
                    .setPayloadKey(recordRequest.payloadKey)
                    .createEvent();

                transformedRequest.setCollection(newCollectionName);

                LOGGER.debug("saving request/response with reqid: " + newReqId);
                boolean saved = config.rrstore.save(transformedRequest) && config.rrstore.save(transformedResponse);

                if(!saved) {
                    LOGGER.debug("request/response not saved");
                    throw new Exception ("Unable to persist new sanitized collection");
                }
            } catch (Exception e) {
                LOGGER.error("Error occurred creating new collection while sanitizing :: " + e.getMessage(), e);
            }
        });

        config.rrstore.commit();

        //2. Get all the ReqResMatchResult with MatchType as NoMatch either for request or response match
        Stream<Analysis.ReqRespMatchResult> resultsOnlyNoMatch = config.rrstore.getAnalysisMatchResultOnlyNoMatch(replayId).getObjects();

        //3. Delete all the requests and responses in the new collection that has the trace id in the above list
        resultsOnlyNoMatch.forEach( res -> {
            res.recordTraceId.ifPresentOrElse( recTraceId -> {
                config.rrstore.deleteReqResByTraceId(recTraceId, newCollectionName);
                res.replayTraceId.ifPresent(repTraceId -> {
                    if (!repTraceId.equalsIgnoreCase(recTraceId)) {
                        config.rrstore.deleteReqResByTraceId(repTraceId, newCollectionName);
                    }
                });
            }, () -> res.replayTraceId.ifPresent(repTraceId -> config.rrstore.deleteReqResByTraceId(repTraceId, newCollectionName)));
        });

        return true;
    }

    // fetch the request/response from the replay, apply the transformation operations and store to Solr
    private void fetchTransformAndStore(String replayId, String newCollectionName,
                                        RecordingOperationSetSP recordingOperationSetSP) {
       /* LOGGER.debug(String.format("fetch ReqRespMatchResults for replay %s, service %s, path %s", replayId,
            recordingOperationSetSP.service, recordingOperationSetSP.path));
        Stream<Analysis.ReqRespMatchResult> results = getReqRespMatchResultStream(replayId*//*, recordingOperationSetSP*//*);
        results.forEach(res -> {
            // get record and replay responses using IDs
            LOGGER.debug(String.format("get record and replay responses with recordReqId %s, replayReqId %s",
                res.recordReqId.get(), res.replayReqId.get()));
            Optional<Response> recordResponse = res.recordReqId.flatMap(config.rrstore::getResponse);
            Optional<Response> replayResponse = res.replayReqId.flatMap(config.rrstore::getResponse);
            Optional<Request> recordRequest = res.recordReqId.flatMap(config.rrstore::getRequestOld);

            // apply the transformation operations
            LOGGER.debug("applying transformations");
            Response transformedResponse = transform(newCollectionName, recordingOperationSetSP, recordResponse,
                replayResponse);

            // store the new request/response
            recordRequest = recordRequest.map(request -> {
                request.reqId = transformedResponse.reqId;
                request.collection = Optional.of(newCollectionName);
                return request;
            });

            LOGGER.debug("saving request/response with reqId: " + transformedResponse.reqId);
            boolean saved = config.rrstore.save(recordRequest.get()) && config.rrstore.save(transformedResponse);
            if(!saved) {
                LOGGER.debug("request/response not saved");
                // todo raise error?
            }
        });*/
    }

     Stream<Analysis.ReqRespMatchResult> getReqRespMatchResultStream(String replayId/*, RecordingOperationSetSP recordingOperationSetSP*/) {
        Result<Analysis.ReqRespMatchResult> matchResults = config.rrstore.getAnalysisMatchResults(
            replayId,
            Optional.empty(),//Optional.of(recordingOperationSetSP.service),
            Optional.empty(),//Optional.of(recordingOperationSetSP.path),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );

        return matchResults.getObjects();
    }

    private Optional<String> generateReqIdOld(Optional<String> recReqId, String collectionName) {
        return recReqId.map(
            reqId -> "gu-" + Objects.hash(reqId, collectionName));
    }

    private String generateReqId(String reqId, String collectionName) {
        return "gu-" + Objects.hash(reqId, collectionName);
    }
}
