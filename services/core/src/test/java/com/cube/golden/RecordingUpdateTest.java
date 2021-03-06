/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.golden;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.md.dao.ReqRespUpdateOperation;
import io.md.dao.ReqRespUpdateOperation.OperationType;

import io.md.dao.RecordingOperationSetSP;
import com.cube.ws.Config;

class RecordingUpdateTest {

    @Test
    void testCreateRecordingUpdate() {
        Config config = null;
        try {
            config = new Config();
        } catch (Exception e) {
            e.printStackTrace();
        }
        RecordingUpdate recordingUpdate = new RecordingUpdate(config);

        var customerid = "Prasad";
        var app = "MovieInfo";
        var service = "movieinfo";
        var path = "minfo/rentmovie";
        Optional<String> method = Optional.empty();

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
                new RecordingOperationSetSP(recordingOperationSetId, customerid, app, service, path, method,
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
        Optional<String> method = Optional.empty();

        String newCollectionName = "test-gu-update";
        List<ReqRespUpdateOperation> updateOperationList = Arrays.asList(
            new ReqRespUpdateOperation(OperationType.REPLACE, "/inventory_id"),
            new ReqRespUpdateOperation(OperationType.REMOVE, "/rent"),
            new ReqRespUpdateOperation(OperationType.REPLACE, "/num_updates")
        );

        RecordingOperationSetSP operationSet = new RecordingOperationSetSP(newCollectionName + "-id",
            newCollectionName + "-osid", customerid, app, service, path, method , updateOperationList);

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
            Optional<Response> recordResponse = res.recordreqid.flatMap(rrStore::getResponseEvent);
            Optional<Response> replayResponse = res.replayreqid.flatMap(rrStore::getResponseEvent);

            // apply the transformation operations
            Response transformedResponse = recordingUpdate.transform(newCollectionName, operationSet,
                recordResponse,
                replayResponse);

            System.out.println(transformedResponse);
        });*/
    }

}
