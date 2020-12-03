package com.cube.learning;

import com.cube.dao.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.JsonDataObj;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjectionConfigGenerator;
import io.md.injection.InjectionExtractionMeta;
import io.md.utils.Utils;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class DynamicInjectionRulesLearner {

    List<String> paths;

    DynamicInjectionConfigGenerator diGen = new DynamicInjectionConfigGenerator();
    static final String methodPath = "/method";

    public DynamicInjectionRulesLearner(Optional<List<String>> paths) {
        this.paths = paths.orElse(
            Arrays.asList("/pathSegments", "/queryParams", "/body", "/hdrs"));
    }

    public void processEvents(Result<Event> events) {
        events.getObjects().forEach(event -> processEvent(event));
    }

    public void processEvent(Event event) {

        Optional<String> requestId = Optional.empty();
        Optional<String> methodString = Optional.empty();
        Optional<HTTPMethodType> method;

        requestId = Optional.of(event.reqId);
        {
            try {
                methodString = Optional.ofNullable(event.payload.getValAsString(methodPath));
            } catch (PathNotFoundException e) {
                methodString = Optional.empty();
            }
        }
            method = methodString.flatMap(v -> Utils.valueOf(HTTPMethodType.class,
                v));

        for (String path : paths) {
            diGen.processJSONObject((JsonDataObj) event.payload.getVal(path),
                event.apiPath,
                path,
                event.eventType,
                requestId,
                method);
        }
    }

    public List<InjectionExtractionMeta> generateRules(Optional<Boolean> discardSingleValues)
        throws JsonProcessingException {
        return diGen.generateConfigs(discardSingleValues.orElse(false));
    }
}
