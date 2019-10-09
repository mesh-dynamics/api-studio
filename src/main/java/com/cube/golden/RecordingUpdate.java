package com.cube.golden;


import com.cube.core.Comparator;
import com.cube.dao.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RecordingUpdate {

    private final ReqRespStoreSolr rrStore;
    private final ResponseTransformer responseTransformer;
    private static final Logger LOGGER = LogManager.getLogger(RecordingUpdate.class);

    public RecordingUpdate(ReqRespStoreSolr rrStore, ObjectMapper jsonmapper) {
        this.rrStore = rrStore;
        this.responseTransformer = new ResponseTransformer(jsonmapper);
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
            LOGGER.debug(String.format("get record and replay responses with recordreqid %s, replayreqid %s",
                res.recordreqid.get(), res.replayreqid.get()));
            Request recordRequest = res.recordreqid.flatMap(rrStore::getRequest)
                .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordreqid.get()));
            Response recordResponse = res.recordreqid.flatMap(rrStore::getResponse)
                .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordreqid.get()));
            Optional<Response> replayResponse = res.replayreqid.flatMap(rrStore::getResponse);

            Optional<RecordingOperationSetSP> updateOperationSet = Optional.ofNullable(
                apiPathVsUpdateOperationSet.get(recordRequest.path));


            Optional<String> newReqId = generateReqId(recordResponse.reqid, newCollectionName);
            Instant timeStamp = Instant.now();

            String transformedResponseBody = replayResponse.flatMap(repResponse ->
                updateOperationSet.flatMap(updateOpSet ->
                    responseTransformer.transformResponse(recordResponse.body , repResponse.body, updateOpSet.operationsList)))
                    .orElse(recordResponse.body);

            Response transformedResponse = new Response(newReqId, recordResponse.status,
                recordResponse.meta, recordResponse.hdrs, transformedResponseBody, Optional.of(newCollectionName),
                    Optional.of(timeStamp), recordResponse.rrtype, recordResponse.customerid, recordResponse.app);

            LOGGER.debug("applying transformations");
            recordRequest.reqid = transformedResponse.reqid;
            recordRequest.collection = Optional.of(newCollectionName);


            LOGGER.debug("saving request/response with reqid: " + transformedResponse.reqid);
            boolean saved = rrStore.save(recordRequest) && rrStore.save(transformedResponse);
            if(!saved) {
                LOGGER.debug("request/response not saved");
                // todo raise error?
            }
            } catch (Exception e) {
                LOGGER.error("Error occured while transforming response :: " + e.getMessage(), e);
            }

        });

        rrStore.saveFnReqRespNewCollec(originalRec.customerid, originalRec.app,
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

       Set<String> inCompletetraceIds = new HashSet<>();

        results.filter( res -> {
            if (res.reqmt.equals(Comparator.MatchType.NoMatch) || res.respmt.equals(Comparator.MatchType.NoMatch) ) {
                res.recordTraceId.ifPresentOrElse( inCompletetraceIds::add , () -> res.replayTraceId.ifPresent( inCompletetraceIds::add ) );
                LOGGER.info(String.format("Filtering out the ReqRespMatchResult as req/response mismatch found " +
                    "with record trace id: %s & replay trace id: %s, record req id: %s, replay req id: %s",
                    res.recordreqid, res.replayTraceId, res.recordreqid, res.replayreqid));
                return false;
            } else if (inCompletetraceIds.contains(res.recordTraceId) || inCompletetraceIds.contains(res.replayTraceId)) {
                LOGGER.info(String.format("Filtering out the ReqRespMatchResult as it found to be part of traceid " +
                    "that has req/resp Mismatch type as NoMatch with record trace id: %s & response trace id: %s " +
                    "and record reqid: %s, replay reqid: %s", res.recordTraceId, res.replayTraceId, res.recordreqid, res.replayreqid));
                return false;
            } else {
                return true;
            }
        }).forEach(res -> {
           try {
               Request recordRequest = res.recordreqid.flatMap(rrStore::getRequest)
                   .orElseThrow(() -> new Exception("Unable to fetch recorded request :: " + res.recordreqid.get()));
               Response recordResponse = res.recordreqid.flatMap(rrStore::getResponse)
                   .orElseThrow(() -> new Exception("Unable to fetch recorded response :: " + res.recordreqid.get()));

               Optional<String> newReqId = generateReqId(recordResponse.reqid, newCollectionName);
               Instant timeStamp = Instant.now();

               Response transformedResponse = new Response(newReqId, recordResponse.status,
                   recordResponse.meta, recordResponse.hdrs, recordResponse.body, Optional.of(newCollectionName),
                   Optional.of(timeStamp), recordResponse.rrtype, recordResponse.customerid, recordResponse.app);

               LOGGER.debug("Changing the reqid and collection name in the response for the sanitized collection");
               recordRequest.reqid = transformedResponse.reqid;
               recordRequest.collection = Optional.of(newCollectionName);

               LOGGER.debug("saving request/response with reqid: " + newReqId);
               boolean saved = rrStore.save(recordRequest) && rrStore.save(transformedResponse);

               if(!saved) {
                   LOGGER.debug("request/response not saved");
                   // todo raise error?
               }
           } catch (Exception e) {
               LOGGER.error("Error occurred while sanitizing :: " + e.getMessage(), e);
           }
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
            LOGGER.debug(String.format("get record and replay responses with recordreqid %s, replayreqid %s",
                res.recordreqid.get(), res.replayreqid.get()));
            Optional<Response> recordResponse = res.recordreqid.flatMap(rrStore::getResponse);
            Optional<Response> replayResponse = res.replayreqid.flatMap(rrStore::getResponse);
            Optional<Request> recordRequest = res.recordreqid.flatMap(rrStore::getRequest);

            // apply the transformation operations
            LOGGER.debug("applying transformations");
            Response transformedResponse = transform(newCollectionName, recordingOperationSetSP, recordResponse,
                replayResponse);

            // store the new request/response
            recordRequest = recordRequest.map(request -> {
                request.reqid = transformedResponse.reqid;
                request.collection = Optional.of(newCollectionName);
                return request;
            });

            LOGGER.debug("saving request/response with reqid: " + transformedResponse.reqid);
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
            reqid -> "gu-" + Objects.hash(reqid, collectionName));
    }
}
