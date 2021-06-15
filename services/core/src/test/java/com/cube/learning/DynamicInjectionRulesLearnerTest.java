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

import static org.junit.jupiter.api.Assertions.*;

import com.cube.dao.Result;
import com.cube.ws.Config;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;
import io.md.injection.ExternalInjectionExtraction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DynamicInjectionRulesLearnerTest {

//    @Test
    void generateRules() throws Exception {
        final Config config = new Config();

        String customerId = "Pronto", app = "ProntoApp", version = "111";
        Optional<Boolean> discardSingleValues = Optional.empty();
        Optional<List<String>> paths = Optional.empty();
        List<EventType> eventTypes = Collections.emptyList();
        List<String> collections = Arrays.asList("a7dc081c-830d-4f67-ab77-db6cbc8855d5");

        EventQuery eventQuery = new EventQuery.Builder(customerId, app, eventTypes)
            .withTimestampAsc(true)
            .withCollections(collections).build();

        DynamicInjectionRulesLearner diLearner = new DynamicInjectionRulesLearner(paths);

        Result<Event> events = config.rrstore.getEvents(eventQuery);

        diLearner.processEvents(events);

        List<ExternalInjectionExtraction> finalInjExtList = diLearner.generateRules(discardSingleValues);

        System.out.print(config.jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(finalInjExtList));

//        config.rrstore.saveDynamicInjectionConfigFromCsv(customerId, app, version, finalInjExtList);
    }
}
