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

package com.cube.learning;

import com.cube.dao.AnalysisMatchResultQuery;
import com.cube.dao.ReqRespStoreSolr.ReqRespResultsWithFacets;
import com.cube.dao.Result;
import com.cube.ws.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.md.dao.Event;
import io.md.dao.ReqRespMatchResult;
import io.md.dao.RequestPayload;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

class CompareTemplatesLearnerTest {

    @Test
    void learnCompareTemplates() throws Exception {
        final Config config = new Config();

        // This replayId is for MovieInfo and only available in dev cluster.
        String replayId = "71672422-fc1a-4b71-acf1-de776841eee9-4ac32854-e7bc-4d49-9278-3bcc285b57a2";

        config.rrstore.getReplay(replayId).ifPresent(replay -> {

            AnalysisMatchResultQuery analysisMatchResultQuery = new AnalysisMatchResultQuery(
                replayId,
                new MultivaluedHashMap<>());

            ReqRespResultsWithFacets resultWithFacets = config.rrstore
                .getAnalysisMatchResults(analysisMatchResultQuery);

            CompareTemplatesLearner ctLearner = new CompareTemplatesLearner(replay.customerId,
                replay.app, replay.templateVersion, config.rrstore);

            List<ReqRespMatchResult> reqRespMatchResultList = resultWithFacets.result
                .getObjects().collect(Collectors.toList());

            List<String> reqIds = reqRespMatchResultList.stream().map(r -> r.recordReqId).flatMap(
                Optional::stream)
                .collect(Collectors.toList());

            Map<String, String> reqIdToMethodMap = new HashMap();


            if (!reqIds.isEmpty()) {
                // empty reqId list would lead to returning of all requests, so check for it
                Result<Event> requestResult = config.rrstore
                    .getRequests(replay.customerId, replay.app, replay.collection,
                        reqIds, Collections.emptyList(), Collections.emptyList(), Optional.empty());
                requestResult.getObjects().forEach(req -> reqIdToMethodMap
                    .put(req.reqId, ((RequestPayload) req.payload).getMethod()));
            }

            List<TemplateEntryMeta> finalMetaList = ctLearner.learnComparisonRules(reqIdToMethodMap,
                reqRespMatchResultList,
                config.rrstore
                    .getTemplateSet(replay.customerId, replay.app, replay.templateVersion));

            try {
                System.out.print(config.jsonMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(finalMetaList));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }
}
