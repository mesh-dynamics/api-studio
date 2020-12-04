package com.cube.learning;

import static org.junit.jupiter.api.Assertions.*;

import com.cube.dao.Result;
import com.cube.ws.Config;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;
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
            .withIndexOrderAsc(true).withCollections(collections).build();

        DynamicInjectionRulesLearner diLearner = new DynamicInjectionRulesLearner(paths);

        Result<Event> events = config.rrstore.getEvents(eventQuery);

        diLearner.processEvents(events);

        List<InjectionExtractionMeta> finalMetaList = diLearner.generateRules(discardSingleValues);

        System.out.print(config.jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(finalMetaList));

//        config.rrstore.saveDynamicInjectionConfigFromCsv(customerId, app, version, finalMetaList);
    }
}
