package com.cube.dao;

import com.cube.learning.DynamicInjectionRulesLearner;
import com.cube.learning.InjectionExtractionMeta;
import io.md.dao.Event;
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
        List<ReqRespUpdateOperation> operationsList = Arrays.asList(
            new ReqRespUpdateOperation(OperationType.ADD, "/path/1"),
            new ReqRespUpdateOperation(OperationType.REMOVE, "/path/2"),
            new ReqRespUpdateOperation(OperationType.REPLACE, "/path/3")
        );
        RecordingOperationSetSP recordingOperationSetSP = new RecordingOperationSetSP(operationSetId, customer, app,
            service, path, operationsList);
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

//    @Test
    void getPotentialDynamicInjectionConfigs() throws Exception {
        final Config config = new Config();

        String customerId = "Pronto", app = "ProntoApp", version = "111";
        Optional<Boolean> discardSingleValues = Optional.empty();
        Optional<List<String>> paths = Optional.empty();
        Optional<List<String>> recordingsList = Optional.of(Arrays.asList(("Recording-965809473")));
        Optional<String> instanceId = Optional.empty();

        DynamicInjectionRulesLearner diLearner = new DynamicInjectionRulesLearner(paths);

        recordingsList.ifPresentOrElse(recIdList -> recIdList.forEach(recId -> {
            Result<Event> events = config.rrstore
                .getReqRespEventsInTimestampOrder(customerId, app, instanceId,
                    Optional.ofNullable(recId));
            diLearner.processEvents(events);

        }), () -> {
            Result<Event> events = config.rrstore
                .getReqRespEventsInTimestampOrder(customerId, app, instanceId, Optional.empty());
            diLearner.processEvents(events);
        });

        List<InjectionExtractionMeta> finalMetaList = diLearner.generateRules(discardSingleValues);

        System.out.print(config.jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(finalMetaList));

//        config.rrstore.saveDynamicInjectionConfigFromCsv(customerId, app, version, finalMetaList);
    }
}
