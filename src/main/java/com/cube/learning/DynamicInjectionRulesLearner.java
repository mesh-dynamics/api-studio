package com.cube.learning;

import com.cube.dao.Result;
import com.cube.learning.InjectionExtractionMeta.ExtractionConfig;
import com.cube.learning.InjectionExtractionMeta.InjectionConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.JsonDataObj;
import io.md.dao.ReqRespMatchResult;
import io.md.injection.DynamicInjectionConfig;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.solr.common.util.Hash;


public class DynamicInjectionRulesLearner {

    List<String> paths;

    DynamicInjectionConfigGenerator diGen = new DynamicInjectionConfigGenerator();
    static final String methodPath = "/method";

    public DynamicInjectionRulesLearner(Optional<List<String>> paths) {
        this.paths = paths.orElse(
            Arrays.asList("/pathSegments", "/queryParams", "/body", "/hdrs"));
    }

    public void processEvents(Result<Event> events) {
        events.getObjects().forEach(this::processEvent);
    }

    public void processEvent(Event event) {

        Optional<String> methodString;
        Optional<HTTPMethodType> method;

        Optional<String> requestId = Optional.of(event.reqId);

        try {
            methodString = Optional.ofNullable(event.payload.getValAsString(methodPath));
        } catch (PathNotFoundException e) {
            methodString = Optional.empty();
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

    public static List<InjectionExtractionMeta> filterRulesByReplayMatchResults(
        DynamicInjectionConfig dynamicInjectionConfig,
        Stream<ReqRespMatchResult> reqRespMatchResultStream) {

        HashSet<diffPaths> diffPathsHashSet = new HashSet<>();
        ArrayList<ExtractionMeta> unmatchedExtractionList = new ArrayList<>();
//        HashSet<String> finalExtractionIdList = new HashSet<>();
        List<InjectionMeta> unmatchedInjectionList = new ArrayList<>();
        HashMap<String, ExtractionMeta> extractionMetaHashMap = new HashMap();
        List<InjectionExtractionMeta> injectionExtractionMetas = new ArrayList<>();
        reqRespMatchResultStream.forEach(res ->
                res.respCompareRes.diffs.forEach(
                    diff    -> diffPathsHashSet.add(new diffPaths(res.path, diff.path))
                )
        );

        dynamicInjectionConfig.extractionMetas.forEach(extractionMeta -> {
            extractionMeta.metadata.extractionJsonPath.ifPresent(jsonPath -> {
                diffPaths extractionPath = new diffPaths(extractionMeta.apiPath, jsonPath);
                if (diffPathsHashSet.contains(extractionPath)) {
                    extractionMeta.metadata.extractionId
                        .ifPresent(id -> extractionMetaHashMap.put(id, extractionMeta));
                }
            });
        });

        dynamicInjectionConfig.injectionMetas.forEach(injectionMeta ->
            injectionMeta.metadata.extractionId.ifPresent(id ->
                Optional.ofNullable(extractionMetaHashMap.get(id)).ifPresent(
                    extConfig -> injectionMeta.apiPaths
                        // Separate out each inj api path into a separate line
                        .forEach(path -> injectionExtractionMetas.add(new InjectionExtractionMeta(
                            new InjectionExtractionMeta.ExtractionConfig(extConfig.apiPath,
                                extConfig.metadata.extractionJsonPath.get(), extConfig.method),
                            new InjectionConfig(path, injectionMeta.jsonPath,
                                injectionMeta.method)))))));

        return injectionExtractionMetas;
    }

    public List<InjectionExtractionMeta> generateRules(Optional<Boolean> discardSingleValues)
        throws JsonProcessingException {
        return diGen.generateConfigs(discardSingleValues.orElse(false));
    }

    private static class diffPaths{
        String apiPath, jsonPath;

        public diffPaths(String apiPath, String jsonPath) {
            this.apiPath = apiPath;
            this.jsonPath = jsonPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            diffPaths diffPaths = (diffPaths) o;
            return apiPath.equals(diffPaths.apiPath) &&
                jsonPath.equals(diffPaths.jsonPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, jsonPath);
        }
    }
}
