package com.cube.golden;


import static io.md.core.TemplateKey.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

import io.md.core.BatchingIterator;
import io.md.core.Comparator;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.EventQuery;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording;
import io.md.dao.ReqRespMatchResult;
import io.md.dao.ReqRespUpdateOperation;
import io.md.dao.RecordingOperationSetSP;
import io.md.dao.RequestPayload;
import io.md.dao.ResponsePayload;
import io.md.utils.Constants;
import io.md.utils.UtilException;

import com.cube.dao.AnalysisMatchResultQuery;
import com.cube.dao.RecordingOperationSetMeta;

import com.cube.dao.Result;
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
        RecordingOperationSetMeta recordingOperationSetMeta = new RecordingOperationSetMeta(customer
            , app);
        boolean stored = config.rrstore.storeRecordingOperationSetMeta(recordingOperationSetMeta);
        if (!stored) {
            LOGGER.error("error storing recording operation set");
            return null; // todo: what to return if storing fails?
        }
        LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE
            , "Successfully Created New Recording " + "Update Operation Set"
            , Constants.CUSTOMER_ID_FIELD, customer, Constants.APP_FIELD
            , app, Constants.RECORDING_UPDATE_OPERATION_SET_ID, recordingOperationSetMeta.id)));
        // if successful return id
        return recordingOperationSetMeta.id;
    }

    /*
     * update the operation set
     * could be to partially or completely update existing operations or create a new
     * operation set if not present
     */
    public boolean updateRecordingOperationSet(RecordingOperationSetSP updateRequest) {
        // fetch operation set meta from Solr to verify the recordingOperationSetId

        Optional<RecordingOperationSetMeta> recordingOperationSetMeta = config.rrstore
            .getRecordingOperationSetMeta(updateRequest.operationSetId);
        if (recordingOperationSetMeta.isEmpty()) {
            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE, "Recording Update Operation"
                    + "Set Doesn't exist", Constants.RECORDING_UPDATE_OPERATION_SET_ID
                , updateRequest.operationSetId)));
            return false;
        }

        // fetch the operation set from Solr
        LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
            , "Trying to fetch RecordingOperationSetSP"
            , Constants.SERVICE_FIELD, updateRequest.service, Constants.API_PATH_FIELD
            , updateRequest.path, Constants.RECORDING_UPDATE_OPERATION_SET_ID
            , updateRequest.operationSetId)));
        Optional<RecordingOperationSetSP> storedOperationSet = config.rrstore
            .getRecordingOperationSetSP(updateRequest.operationSetId,
                updateRequest.service, updateRequest.path);
        return storedOperationSet
            // if present, update/insert the new operations
            .map(recordingOperationSet -> {
                // convert operation list to map
                //LOGGER.debug("recording operation set: " + recordingOperationSet);
                Map<String, ReqRespUpdateOperation> operationMap = createOperationsMap(
                    recordingOperationSet.operationsList);
                // upsert new operation for path
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
                    , "Updating RecordingOperationSetSP"
                    , Constants.SERVICE_FIELD, updateRequest.service, Constants.API_PATH_FIELD
                    , updateRequest.path, Constants.RECORDING_UPDATE_OPERATION_SET_ID
                    , updateRequest.operationSetId, Constants.RECORDING_UPDATE_API_OPERATION_SET_ID
                    , recordingOperationSet.operationSetId)));
                updateRequest.operationsList.forEach(
                    newOperation -> operationMap.put(newOperation.key(), newOperation)
                );
                recordingOperationSet.setOperationsList(new ArrayList<>(operationMap.values()));
                // store it back
                // if successful return true
                LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
                    , "Storing Updated RecordingOperationSetSP"
                    , Constants.SERVICE_FIELD, updateRequest.service, Constants.API_PATH_FIELD
                    , updateRequest.path, Constants.RECORDING_UPDATE_OPERATION_SET_ID
                    , updateRequest.operationSetId, Constants.RECORDING_UPDATE_API_OPERATION_SET_ID
                    , recordingOperationSet.operationSetId)));
                return config.rrstore.storeRecordingOperationSet(recordingOperationSet);
            })
            // if empty, create a new one (we have verified the existence of the
            // recordingOperationSetId)
            .orElseGet(() -> {
                LOGGER.info(new ObjectMessage(Map.of(Constants.MESSAGE
                    , "RecordingOperationSetSP not found, creating new one"
                    , Constants.SERVICE_FIELD, updateRequest.service, Constants.API_PATH_FIELD
                    , updateRequest.path, Constants.RECORDING_UPDATE_OPERATION_SET_ID
                    , updateRequest.operationSetId)));
//                RecordingOperationSetSP recordingOperationSet
//                    = new RecordingOperationSetSP(recordingOperationSetId,
//                    recordingOperationSetMeta.get().customer,
//                    recordingOperationSetMeta.get().app, Optional.of(service), Optional.of(path)
//                    , newOperationSet);
                return config.rrstore.storeRecordingOperationSet(updateRequest);
            });
    }

    // convert operations list to map (jsonpath->operation)
    private Map<String, ReqRespUpdateOperation> createOperationsMap(List<ReqRespUpdateOperation>
        operationsList) {
        LOGGER.debug("converting operation set list to map of (jsonpath -> operation)");
        return operationsList.stream()
            .collect(Collectors.toMap(ReqRespUpdateOperation::key, op -> op, (op1, op2) -> op2));
    }

    /*
    * fetch recording operation set given the id
    */
    public Optional<RecordingOperationSetSP> getRecordingOperationSet(String recordingOperationSetId
        , String service, String path){
        return config.rrstore.getRecordingOperationSetSP(recordingOperationSetId, service, path);
    }

    /*
     * apply the operations on a recording collection
     */
    // TODO: sort the match results by (service, apiPath) and process each apiPath at a
    //  time, so that comparator need not be lookup up repeatedly
    public boolean applyRecordingOperationSet(String replayId, String newCollectionName,
                                              String recordingOperationSetId
        , Recording originalRec, String newTemplateSetVersion) {

        // use recordingOperationSetId to fetch the list of operations
        // use replayid to fetch the analyze results (reqrespmatchresults)
        // apply the operations to the results
        // create a new collection
        // store it
        Map<String, RecordingOperationSetSP> apiPathVsUpdateOperationSet =
            config.rrstore.getRecordingOperationSetSPs(recordingOperationSetId)
                .collect(Collectors.toMap(set -> set.path
                    , Function.identity()));
        Stream<ReqRespMatchResult> results = getReqRespMatchResultStream(replayId);
        List<ReqRespMatchResult> resList = results.collect(Collectors.toList());
        Map<String, Event> reqMap = new HashMap<>();
        Map<String, Event> respMap = new HashMap<>();
        try {
            BatchingIterator.batchedStreamOf(resList.stream(), 100)
                .forEach(UtilException.rethrowConsumer(res -> {

                    List<String> reqIds = res.stream()
                        .flatMap(r -> Stream.of(r.recordReqId, r.replayReqId))
                        .flatMap(Optional::stream)
                        .collect(Collectors.toList());

                    if (!reqIds.isEmpty()) {
                        EventQuery.Builder reqBuilder = new EventQuery.Builder(
                            originalRec.customerId,
                            originalRec.app,
                            Collections.emptyList());
                        reqBuilder.withReqIds(reqIds);
                        reqBuilder.withCollections(List.of(originalRec.collection , replayId));
                        reqBuilder.withoutScoreOrder().withSeqIdAsc(true).withTimestampAsc(true);

                        Result<Event> reqRespEvents = config.rrstore.getEvents(reqBuilder.build());
                        reqRespEvents.getObjects().forEach(event -> {
                            if (event.payload instanceof RequestPayload) {
                                reqMap.put(event.reqId, event);
                            } else if (event.payload instanceof ResponsePayload) {
                                respMap.put(event.reqId, event);
                            }
                        });
                    }

                    Stream<Event> events = res.stream().flatMap(reqRespMatchResult -> {
                        try {
                            LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
                                , "Applying Recording Update", Constants.RECORD_REQ_ID_FIELD
                                , reqRespMatchResult.recordReqId.orElse(Constants.NOT_PRESENT)
                                , Constants.REPLAY_REQ_ID_FIELD,
                                reqRespMatchResult.replayReqId.orElse(Constants.NOT_PRESENT),
                                Constants.REPLAY_ID_FIELD, reqRespMatchResult.replayId, Constants
                                    .RECORDING_UPDATE_OPERATION_SET_ID, recordingOperationSetId)));

                            Event recordRequest = reqRespMatchResult.recordReqId.map(reqMap::get)
                                .orElseThrow(
                                    () -> new Exception("Unable to fetch recorded request :: "
                                        + reqRespMatchResult.recordReqId
                                        .orElse(Constants.NOT_PRESENT)));
                            Optional<Event> recordResponseOpt = reqRespMatchResult.recordReqId
                                .map(respMap::get);
                            Optional<Event> replayRequest = reqRespMatchResult.replayReqId
                                .map(reqMap::get);
                            Optional<Event> replayResponse = reqRespMatchResult.replayReqId
                                .map(respMap::get);

                            Optional<RecordingOperationSetSP> updateOperationSet = Optional
                                .ofNullable(
                                    apiPathVsUpdateOperationSet.get(recordRequest.apiPath));
                            List<ReqRespUpdateOperation> operationsList = updateOperationSet
                                .map(updateOpSet -> updateOpSet.operationsList)
                                .orElse(Collections.emptyList());

                            Map<Boolean, List<ReqRespUpdateOperation>> operationTypeVsList
                                = operationsList.stream().collect(Collectors.partitioningBy(
                                op -> op.eventType == ReqRespUpdateOperation.Type.Request));

                            List<ReqRespUpdateOperation> reqOperationList = operationTypeVsList
                                .get(true);
                            List<ReqRespUpdateOperation> responseOperationList = operationTypeVsList
                                .get(false);

                            String newReqId = generateReqId(recordRequest.reqId, newCollectionName);
                            Optional<Event> transformedResponseOpt = recordResponseOpt
                                .map(UtilException
                                    .rethrowFunction(
                                        recordResponse -> recordResponse
                                            .applyTransform(replayResponse
                                                , responseOperationList, newCollectionName,
                                                newReqId,
                                                Optional.empty())));

                            TemplateKey key = new TemplateKey(newTemplateSetVersion,
                                originalRec.customerId,
                                originalRec.app, recordRequest.service, recordRequest.apiPath,
                                Type.RequestMatch, io.md.utils.Utils.extractMethod(recordRequest),
                                originalRec.collection);
                            Comparator comparator = config.rrstore.getComparator(key
                                , Event.EventType.HTTPRequest);

                            // Transform request
                            Event transformedRequest = recordRequest.applyTransform(replayRequest
                                , reqOperationList, newCollectionName, newReqId
                                , Optional.of(comparator));

                            LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
                                , "Saving transformed request/response",
                                Constants.RECORD_REQ_ID_FIELD,
                                reqRespMatchResult.recordReqId.orElse(Constants.NOT_PRESENT)
                                , Constants.REPLAY_REQ_ID_FIELD,
                                reqRespMatchResult.replayReqId.orElse(Constants.NOT_PRESENT),
                                Constants.REPLAY_ID_FIELD, reqRespMatchResult.replayId,
                                Constants.REQ_ID_FIELD, Optional
                                    .ofNullable(newReqId).orElse(Constants.NOT_PRESENT),
                                Constants.RECORDING_UPDATE_OPERATION_SET_ID, recordingOperationSetId
                        /*, Constants.PAYLOAD, Optional.ofNullable(transformedResponse.rawPayloadString)
                            .orElse(Constants.NOT_PRESENT)*/)));

                            return transformedResponseOpt
                                .map(resp -> Stream.of(transformedRequest, resp))
                                .orElseGet(() -> Stream.of(transformedRequest));

                        } catch (Exception e) {
                            LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
                                , "Exception Occurred while transforming request/response",
                                Constants.RECORD_REQ_ID_FIELD,
                                reqRespMatchResult.recordReqId.orElse(Constants.NOT_PRESENT)
                                , Constants.REPLAY_REQ_ID_FIELD,
                                reqRespMatchResult.replayReqId.orElse(Constants.NOT_PRESENT),
                                Constants.REPLAY_ID_FIELD, reqRespMatchResult.replayId,
                                Constants.RECORDING_UPDATE_OPERATION_SET_ID,
                                recordingOperationSetId)), e);
                        }
                        return Stream.empty();
                    });

                    if (!config.rrstore.save(events)) {
                        LOGGER.error(new ObjectMessage(Map.of(Constants.MESSAGE
                            , "Error in saving transformed request/response"
                            , Constants.RECORDING_UPDATE_OPERATION_SET_ID,
                            recordingOperationSetId)));
                        throw new Exception("Error in saving transformed request/response");
                    }
                    reqMap.clear();
                    respMap.clear();
                }));
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(
                Map.of(Constants.MESSAGE, "Exception occured : " + e.getMessage())));
            return false;
        } finally {
            reqMap.clear();
            respMap.clear();
        }

        /* This should no longer be needed. TODO: Remove once java function recording path is tested
        config.rrstore.saveFnReqRespNewCollec(originalRec.customerId, originalRec.app,
            originalRec.collection, newCollectionName);
        */

        return true; // todo: false?
    }

    public boolean createSanitizedCollection(String replayId, String newCollectionName
        , Recording originalRec) {

        Stream<ReqRespMatchResult> results = getReqRespMatchResultStream(replayId);

        //1. Create a new collection with all the Req/Responses
        Stream<Event> events =  results.flatMap(res -> {
            try {
                Event recordRequest = res.recordReqId.flatMap(config.rrstore::getRequestEvent)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded request :: "
                        + res.recordReqId.orElse(Constants.NOT_PRESENT)));
                Event recordResponse = res.recordReqId.flatMap(config.rrstore::getResponseEvent)
                    .orElseThrow(() -> new Exception("Unable to fetch recorded response :: "
                        + res.recordReqId.orElse(Constants.NOT_PRESENT)));

                String newReqId = generateReqId(recordResponse.reqId, newCollectionName);

                Event transformedResponse = copyEvent(newCollectionName, recordResponse, newReqId,
                    recordResponse.getTraceId());

                LOGGER.debug("Changing the reqid and collection name in the response for "
                    + "the sanitized collection");

                Event transformedRequest = copyEvent(newCollectionName, recordRequest, newReqId,
                    recordResponse.getTraceId());

                LOGGER.debug("saving request/response with reqid: " + newReqId);

                return Stream.of(transformedRequest , transformedResponse );

            } catch (Exception e) {
                LOGGER.error("Error occurred creating new collection while sanitizing :: "
                    + e.getMessage(), e);
            }
            return Stream.empty();
        });

        if(!config.rrstore.save(events)) {
            LOGGER.error("request/response bulk saved failed");
        }

        config.rrstore.commit();

        //2. Get all the ReqResMatchResult with MatchType as NoMatch either for request or response
        // match
        Stream<ReqRespMatchResult> resultsOnlyNoMatch = config.rrstore
            .getAnalysisMatchResultOnlyNoMatch(replayId).getObjects();

        //3. Delete all the requests and responses in the new collection that has the trace id
        // in the above list
        resultsOnlyNoMatch.forEach( res -> {
            res.recordTraceId.ifPresentOrElse( recTraceId -> {
                config.rrstore.deleteReqResByTraceId(recTraceId, newCollectionName);
                res.replayTraceId.ifPresent(repTraceId -> {
                    if (!repTraceId.equalsIgnoreCase(recTraceId)) {
                        config.rrstore.deleteReqResByTraceId(repTraceId, newCollectionName);
                    }
                });
            }, () -> res.replayTraceId.ifPresent(repTraceId -> config.rrstore
                .deleteReqResByTraceId(repTraceId, newCollectionName)));
        });

        return true;
    }

    private Event copyEvent(String newCollectionName, Event recordEvent, String newReqId,
        String traceId) throws EventBuilder.InvalidEventException {
        Event eventCopy = new EventBuilder(recordEvent.customerId
            , recordEvent.app, recordEvent.service, recordEvent.instanceId
            , "", new MDTraceInfo(traceId, null
            , null), recordEvent.getRunType()
            , Optional.of(recordEvent.timestamp), newReqId, recordEvent.apiPath
            , recordEvent.eventType, recordEvent.recordingType).withRunId(recordEvent.runId)
            .setPayload(recordEvent.payload)
            /*.setRawPayloadString(recordResponse.rawPayloadString)*/
            .setPayloadKey(recordEvent.payloadKey)
            .createEvent();

        eventCopy.setCollection(newCollectionName);

        return eventCopy;
    }


    private Stream<ReqRespMatchResult> getReqRespMatchResultStream(String replayId
         /*, RecordingOperationSetSP recordingOperationSetSP*/) {
        Result<ReqRespMatchResult> matchResults = (Result<ReqRespMatchResult>) config.rrstore.getAnalysisMatchResults(
            new AnalysisMatchResultQuery(replayId)
        ).result;

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
