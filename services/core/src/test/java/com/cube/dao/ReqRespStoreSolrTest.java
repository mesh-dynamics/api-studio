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

package com.cube.dao;

import io.md.dao.RecordingOperationSetSP;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.md.dao.ReqRespUpdateOperation;
import io.md.dao.ReqRespUpdateOperation.OperationType;

import com.cube.ws.Config;

class ReqRespStoreSolrTest {


    @Test
    void testRecordingOperationSetSolrMethods() {
        Config config = null;
        try {
            config = new Config();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ReqRespStoreSolr rrStore = (ReqRespStoreSolr) config.rrstore;
        String id;//  = "RecordingOperationSetSP-testid1";
        String operationSetId = "test-" + UUID.randomUUID().toString(); //"test-recording-operation-set-1";
        String customer = "test-customer";
        String app = "test-app";
        String service = "test-service-1";
        String path = "test/api/path/1";
        Optional<String> method = Optional.empty();
        List<ReqRespUpdateOperation> operationsList = Arrays.asList(
            new ReqRespUpdateOperation(OperationType.ADD, "/path/1"),
            new ReqRespUpdateOperation(OperationType.REMOVE, "/path/2"),
            new ReqRespUpdateOperation(OperationType.REPLACE, "/path/3")
        );
        RecordingOperationSetSP recordingOperationSetSP = new RecordingOperationSetSP(operationSetId, customer, app,
            service, path, method ,  operationsList);
        id = recordingOperationSetSP.id;
        boolean stored = rrStore.storeRecordingOperationSet(recordingOperationSetSP);
        Assertions.assertTrue(stored, "storing operationSet failed");

        Optional<RecordingOperationSetSP> recordingOperationSetStoredOptional =
            rrStore.getRecordingOperationSetSP(operationSetId, service, path);
        Assertions.assertFalse(recordingOperationSetStoredOptional.isEmpty(), "fetching stored operationSet failed");

        RecordingOperationSetSP recordingOperationSetSPStored = recordingOperationSetStoredOptional.get();
        Assertions.assertEquals(id, recordingOperationSetSPStored.id);
        Assertions.assertEquals(operationSetId, recordingOperationSetSPStored.operationSetId);
        Assertions.assertEquals(customer, recordingOperationSetSPStored.customer);
        Assertions.assertEquals(app, recordingOperationSetSPStored.app);
        Assertions.assertEquals(service, recordingOperationSetSPStored.service);
        Assertions.assertEquals(path, recordingOperationSetSPStored.path);
        Assertions.assertEquals(operationsList.size(), recordingOperationSetSPStored.operationsList.size());
    }


}
