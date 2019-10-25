package com.cube.golden;


import com.cube.dao.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecordingUpdate {

    private final ReqRespStoreSolr rrStore;
    private final ResponseTransformer responseTransformer;
    private static final Logger LOGGER = LogManager.getLogger(RecordingUpdate.class);

    public RecordingUpdate(ReqRespStoreSolr rrStore, ObjectMapper jsonMapper) {
        this.rrStore = rrStore;
        this.responseTransformer = new ResponseTransformer(jsonMapper);
    }

    /*
    * create operation set and return the id
    */
    public String createRecordingOperationSet(String customer, String app){
        RecordingOperationSetMeta recordingOperationSetMeta = new RecordingOperationSetMeta(customer, app);
        LOGGER.info("Creating new recording operation set with id: " + recordingOperationSetMeta.id);
        boolean stored = rrStore.storeRecordingOperationSetMeta(recordingOperationSetMeta);
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

        Optional<RecordingOperationSetMeta> recordingOperationSetMeta = rrStore.getRecordingOperationSetMeta(
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
        Optional<RecordingOperationSetSP> storedOperationSet =
            rrStore.getRecordingOperationSetSP(updateRequest.operationSetId,
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
                return rrStore.storeRecordingOperationSet(recordingOperationSet);
            })
            // if empty, create a new one (we have verified the existence of the recordingOperationSetId)
            .orElseGet(() -> {
                LOGGER.info("RecordingOperationSetSP not found, creating new one");
//                RecordingOperationSetSP recordingOperationSet
//                    = new RecordingOperationSetSP(recordingOperationSetId, recordingOperationSetMeta.get().customer,
//                    recordingOperationSetMeta.get().app, Optional.of(service), Optional.of(path), newOperationSet);
                return rrStore.storeRecordingOperationSet(updateRequest);
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
        return rrStore.getRecordingOperationSetSP(recordingOperationSetId, service, path);
    }

    /*
    * apply the operations on a recording collection
     */
    // TODO: Event redesign: This needs to be rewritten to get as event
    public boolean applyRecordingOperationSet(String replayId, String newCollectionName,
                                                String recordingOperationSetId, Recording originalRec) {


        // use recordingOperationSetId to fetch the list of operations
        // use replayid to fetch the analyze results (reqrespmatchresults)
        // apply the operations to the results
        // create a new collection
        // store it
        Map<String, RecordingOperationSetSP> apiPathVsUpdateOperationSet =
            rrStore.getRecordingOperationSetSPs(recordingOperationSetId).collect(Collectors.toMap( set -> set.path
            , Function.identity()));
        Stream<Analysis.ReqRespMatchResult> results = getReqRespMatchResultStream(replayId/*, recordingOperationSetSP*/);
        results.forEach(res -> {
            try {
            LOGGER.debug(String.format("get record and replay responses with recordReqId %s, replayReqId %s",
                res.recordReqId.get(), res.replayReqId.get()));
            Request recordRequest = res.recordReqId.flatMap(rrStore::getRequest)
                .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordReqId.get()));
            Response recordResponse = res.recordReqId.flatMap(rrStore::getResponse)
                .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordReqId.get()));
            Optional<Response> replayResponse = res.replayReqId.flatMap(rrStore::getResponse);

            Optional<RecordingOperationSetSP> updateOperationSet = Optional.ofNullable(
                apiPathVsUpdateOperationSet.get(recordRequest.apiPath));


            Optional<String> newReqId = generateReqId(recordResponse.reqId, newCollectionName);
            Instant timeStamp = Instant.now();

            String transformedResponseBody = replayResponse.flatMap(repResponse ->
                updateOperationSet.flatMap(updateOpSet ->
                    responseTransformer.transformResponse(recordResponse.body , repResponse.body, updateOpSet.operationsList)))
                    .orElse(recordResponse.body);

            Response transformedResponse = new Response(newReqId, recordResponse.status,
                recordResponse.meta, recordResponse.hdrs, transformedResponseBody, Optional.of(newCollectionName),
                    Optional.of(timeStamp), recordResponse.runType, recordResponse.customerId, recordResponse.app,
                recordResponse.apiPath);

            LOGGER.debug("applying transformations");
            recordRequest.reqId = transformedResponse.reqId;
            recordRequest.collection = Optional.of(newCollectionName);


            LOGGER.debug("saving request/response with reqId: " + transformedResponse.reqId);
            boolean saved = rrStore.save(recordRequest) && rrStore.save(transformedResponse);
            if(!saved) {
                LOGGER.debug("request/response not saved");
                // todo raise error?
            }
            } catch (Exception e) {
                LOGGER.error("Error occured while transforming response :: " + e.getMessage(), e);
            }

        });

        rrStore.saveFnReqRespNewCollec(originalRec.customerId, originalRec.app,
            originalRec.collection, newCollectionName);


        // get the operation sets using operation set id
        // for each operation set, perform the transformation and store the new collection
        /*LOGGER.debug("fetching RecordingOperationSets for " + recordingOperationSetId);
        rrStore.getRecordingOperationSetSPs(recordingOperationSetId)
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
               Request recordRequest = res.recordReqId.flatMap(rrStore::getRequest)
                   .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordReqId.get()));
               Response recordResponse = res.recordReqId.flatMap(rrStore::getResponse)
                   .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordReqId.get()));

               Optional<String> newReqId = generateReqId(recordResponse.reqId, newCollectionName);
               Instant timeStamp = Instant.now();

               Response transformedResponse = new Response(newReqId, recordResponse.status,
                   recordResponse.meta, recordResponse.hdrs, recordResponse.body, Optional.of(newCollectionName),
                   Optional.of(timeStamp), recordResponse.runType, recordResponse.customerId, recordResponse.app,
                   recordResponse.apiPath);

               LOGGER.debug("Changing the reqid and collection name in the response for the sanitized collection");
               recordRequest.reqId = transformedResponse.reqId;
               recordRequest.collection = Optional.of(newCollectionName);

               LOGGER.debug("saving request/response with reqid: " + newReqId);
               boolean saved = rrStore.save(recordRequest) && rrStore.save(transformedResponse);

               if(!saved) {
                   LOGGER.debug("request/response not saved");
                   throw new Exception ("Unable to persist new sanitized collection");
               }
           } catch (Exception e) {
               LOGGER.error("Error occurred creating new collection while sanitizing :: " + e.getMessage(), e);
           }
        });

        rrStore.commit();

        //2. Get all the ReqResMatchResult with MatchType as NoMatch either for request or response match
        Stream<Analysis.ReqRespMatchResult> resultsOnlyNoMatch = rrStore.getAnalysisMatchResultOnlyNoMatch(replayId).getObjects();

        //3. Delete all the requests and responses in the new collection that has the trace id in the above list
        resultsOnlyNoMatch.forEach( res -> {
            res.recordTraceId.ifPresentOrElse( recTraceId -> {
                rrStore.deleteReqResByTraceId(recTraceId, newCollectionName);
                res.replayTraceId.ifPresent(repTraceId -> {
                    if (!repTraceId.equalsIgnoreCase(recTraceId)) {
                        rrStore.deleteReqResByTraceId(repTraceId, newCollectionName);
                    }
                });
            }, () -> res.replayTraceId.ifPresent(repTraceId -> rrStore.deleteReqResByTraceId(repTraceId, newCollectionName)));
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
            Optional<Response> recordResponse = res.recordReqId.flatMap(rrStore::getResponse);
            Optional<Response> replayResponse = res.replayReqId.flatMap(rrStore::getResponse);
            Optional<Request> recordRequest = res.recordReqId.flatMap(rrStore::getRequest);

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
            boolean saved = rrStore.save(recordRequest.get()) && rrStore.save(transformedResponse);
            if(!saved) {
                LOGGER.debug("request/response not saved");
                // todo raise error?
            }
        });*/
    }

     Stream<Analysis.ReqRespMatchResult> getReqRespMatchResultStream(String replayId/*, RecordingOperationSetSP recordingOperationSetSP*/) {
        Result<Analysis.ReqRespMatchResult> matchResults = rrStore.getAnalysisMatchResults(
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

    private Optional<String> generateReqId(Optional<String> recReqId, String collectionName) {
        return recReqId.map(
            reqId -> "gu-" + Objects.hash(reqId, collectionName));
    }
}
