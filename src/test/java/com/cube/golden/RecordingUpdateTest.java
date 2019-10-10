package com.cube.golden;

import com.cube.dao.*;
import com.cube.ws.Config;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class RecordingUpdateTest {

    @Test
    void testCreateRecordingUpdate() {
        Config config = null;
        try {
            config = new Config();
        } catch (Exception e) {
            e.printStackTrace();
        }
        ReqRespStoreSolr rrStore = (ReqRespStoreSolr) config.rrstore;
        RecordingUpdate recordingUpdate = new RecordingUpdate(rrStore, config.jsonMapper);

        var customerid = "Prasad";
        var app = "MovieInfo";
        var service = "movieinfo";
        var path = "minfo/rentmovie";

        // create new operation set
        String recordingOperationSetId = recordingUpdate.createRecordingOperationSet(customerid, app);
        System.out.println("created recording operation set: " + recordingOperationSetId);
        Assertions.assertNotNull(recordingOperationSetId);

        // add operations to the operation set (for the [service, path])
        List<ReqRespUpdateOperation> updateOperationList = Arrays.asList(
            new ReqRespUpdateOperation(OperationType.REPLACE, "/inventory_id"),
            new ReqRespUpdateOperation(OperationType.REMOVE, "/rent"),
            new ReqRespUpdateOperation(OperationType.REPLACE, "/num_updates")
        );

        System.out.println("updating operations: " + updateOperationList);
        boolean update =
            recordingUpdate.updateRecordingOperationSet(
                new RecordingOperationSetSP(recordingOperationSetId, customerid, app, service, path,
                    updateOperationList));
        Assertions.assertTrue(update);

        // get operation set
        Optional<RecordingOperationSetSP> storedRecordingOperationSetOpt = recordingUpdate.getRecordingOperationSet(
            recordingOperationSetId, service, path);
        Assertions.assertNotEquals(Optional.empty(), storedRecordingOperationSetOpt,"stored operation set empty");

        var storedRecordingOperationSet = storedRecordingOperationSetOpt.get();

        // verify the stored operation set
        for (ReqRespUpdateOperation updateOperation : updateOperationList) {
            boolean found = false;
            for (ReqRespUpdateOperation operation : storedRecordingOperationSet.operationsList) {
                if (updateOperation.jsonpath.equals(operation.jsonpath)) {
                    Assertions.assertEquals(updateOperation.operationType, operation.operationType);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Assertions.fail("operation lists do not match: " + updateOperation.jsonpath);
            }
        }

        // apply the operations to a recording
        String replayId = "prasad-testc2-d01f96bd-0b8f-4f75-bfc4-43ee91046a2f";
        System.out.println("applying recording operation set to replay: " + replayId);
        String newCollectionName = "gu-test-coll-3";
        boolean applied = true;//recordingUpdate.applyRecordingOperationSet(replayId, newCollectionName, recordingOperationSetId);
        Assertions.assertTrue(applied);
    }

    @Test
    void testUpdateCollection() {
        String replayId = "prasad-testc2-d01f96bd-0b8f-4f75-bfc4-43ee91046a2f";
        var customerid = "Prasad";
        var app = "MovieInfo";
        var service = "movieinfo";
        var path = "minfo/rentmovie";

        String newCollectionName = "test-gu-update";
        List<ReqRespUpdateOperation> updateOperationList = Arrays.asList(
            new ReqRespUpdateOperation(OperationType.REPLACE, "/inventory_id"),
            new ReqRespUpdateOperation(OperationType.REMOVE, "/rent"),
            new ReqRespUpdateOperation(OperationType.REPLACE, "/num_updates")
        );

        RecordingOperationSetSP operationSet = new RecordingOperationSetSP(newCollectionName + "-id",
            newCollectionName + "-osid", customerid, app, service, path, updateOperationList);

       /* Config config = null;
        try {
            config = new Config();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ReqRespStoreSolr rrStore = (ReqRespStoreSolr) config.rrstore;
        RecordingUpdate recordingUpdate = new RecordingUpdate(rrStore, config.jsonMapper);

        Stream<Analysis.ReqRespMatchResult> reqRespMatchResultStream = recordingUpdate.getReqRespMatchResultStream(
            replayId);

        reqRespMatchResultStream.forEach(res -> {

            // get record and replay responses using IDs
            Optional<Response> recordResponse = res.recordreqid.flatMap(rrStore::getResponse);
            Optional<Response> replayResponse = res.replayreqid.flatMap(rrStore::getResponse);

            // apply the transformation operations
            Response transformedResponse = recordingUpdate.transform(newCollectionName, operationSet,
                recordResponse,
                replayResponse);

            System.out.println(transformedResponse);
        });*/
    }

}
