package com.cube.golden;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.dao.Analysis;
import com.cube.dao.Event;
import com.cube.dao.Recording;
import com.cube.dao.RecordingOperationSetMeta;
import com.cube.dao.RecordingOperationSetSP;
import com.cube.dao.Result;
import com.cube.utils.Constants;
import com.cube.ws.Config;

public class RecordingUpdate {

    private final Config config;
    private static final Logger LOGGER = LogManager.getLogger(RecordingUpdate.class);

    public RecordingUpdate(Config config) {
        this.config = config;
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
                Event recordRequest = res.recordReqId.flatMap(config.rrstore::getRequestEvent)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordReqId.get()));
                Event recordResponse = res.recordReqId.flatMap(config.rrstore::getResponseEvent)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordReqId.get()));

                Optional<Event> replayRequest = res.replayReqId.flatMap(config.rrstore::getRequestEvent);
                Optional<Event> replayResponse = res.replayReqId.flatMap(config.rrstore::getResponseEvent);

                Optional<RecordingOperationSetSP> updateOperationSet = Optional.ofNullable(
                    apiPathVsUpdateOperationSet.get(recordRequest.apiPath));
                List<ReqRespUpdateOperation> operationsList = updateOperationSet
                    .map(updateOpSet -> updateOpSet.operationsList)
                    .orElse(Collections.emptyList());


                String newReqId = generateReqId(recordResponse.reqId, newCollectionName);

                LOGGER.debug("applying transformations");
                Event transformedResponse = recordResponse.applyTransform(replayResponse, operationsList, config,
                        newCollectionName, newReqId, Optional.empty());


                TemplateKey key = new TemplateKey(originalRec.templateVersion, originalRec.customerId,
                    originalRec.app, recordRequest.service, recordRequest.apiPath,
                    TemplateKey.Type.Request);
                Comparator comparator = config.comparatorCache.getComparator(key , Event.EventType.HTTPRequest);

                // Currently request is not transformed, so send empty operation list and empty replayRequest
                Event transformedRequest = recordRequest.applyTransform(Optional.empty(), Collections.emptyList(), config,
                        newCollectionName, newReqId, Optional.of(comparator));


                LOGGER.debug(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Saving transformed request/response",
                    Constants.REQ_ID_FIELD, transformedResponse.reqId
                )));
                boolean saved =
                    config.rrstore.save(transformedRequest) && config.rrstore.save(transformedResponse);
                if(!saved) {
                    LOGGER.error(new ObjectMessage(Map.of(
                        Constants.MESSAGE, "Error in saving request/response to Solr",
                        Constants.REQ_ID_FIELD, recordRequest.reqId
                    )));
                }
            } catch (Exception e) {
                LOGGER.error(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Exception occured while transforming response",
                    Constants.ERROR, e.getMessage()
                )));
            }

        });

        /* This should no longer be needed. TODO: Remove once java function recording path is tested
        config.rrstore.saveFnReqRespNewCollec(originalRec.customerId, originalRec.app,
            originalRec.collection, newCollectionName);
        */

        return true; // todo: false?
    }

    public boolean createSanitizedCollection(String replayId, String newCollectionName, Recording originalRec) {

        Stream<Analysis.ReqRespMatchResult> results = getReqRespMatchResultStream(replayId);

        //1. Create a new collection with all the Req/Responses
        results.forEach(res -> {
            try {
                Event recordRequest = res.recordReqId.flatMap(config.rrstore::getRequestEvent)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordReqId.get()));
                Event recordResponse = res.recordReqId.flatMap(config.rrstore::getResponseEvent)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordReqId.get()));

                String newReqId = generateReqId(recordResponse.reqId, newCollectionName);

                Event transformedResponse = new Event.EventBuilder(recordResponse.customerId, recordResponse.app, recordResponse.service, recordResponse.instanceId, "", recordResponse.getTraceId(),
                    recordResponse.runType, recordResponse.timestamp, newReqId, recordResponse.apiPath, recordResponse.eventType)
                    .setRawPayloadBinary(recordResponse.rawPayloadBinary)
                    .setRawPayloadString(recordResponse.rawPayloadString)
                    .setPayloadKey(recordResponse.payloadKey)
                    .createEvent();

                transformedResponse.setCollection(newCollectionName);

                LOGGER.debug("Changing the reqid and collection name in the response for the sanitized collection");

                Event transformedRequest = new Event.EventBuilder(recordRequest.customerId, recordRequest.app, recordRequest.service, recordRequest.instanceId, "", recordRequest.getTraceId(),
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
