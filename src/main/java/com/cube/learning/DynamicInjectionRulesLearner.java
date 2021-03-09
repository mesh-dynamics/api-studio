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
import io.md.injection.DynamicInjector;
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

    private final List<String> paths;

    DynamicInjectionConfigGenerator diGen = new DynamicInjectionConfigGenerator();
    static final String methodPath = "/method";

    private final HashSet<diffPaths> diffPathsHashSet = new HashSet<>();


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

    public void processReplayMatchResults(Stream<ReqRespMatchResult> reqRespMatchResultStream) {
        // NOTE: We are being sloppy here by not considering the req/resp method. But since it is
        // just filtering rules based on diffs, an occasional false-positive is admissible.
        // Retrieving method from orig events is too much of data processing overhead for marginal ben.
        reqRespMatchResultStream.forEach(res ->
                res.respCompareRes.diffs.forEach(
                    // Use set to de-dup recurring api,json path combinations.
                    diff -> diffPathsHashSet.add(new diffPaths(res.path, diff.path))
                )
        );

    }

    public List<InjectionExtractionMeta> generateFilteredRules(DynamicInjectionConfig dynamicInjectionConfig){
        final HashMap<String, ExtractionMeta> extractionMetaHashMap = new HashMap();

        dynamicInjectionConfig.extractionMetas.forEach(extractionMeta -> {

            extractionMeta.metadata.ifPresent(metadata -> {
                diffPathsHashSet.forEach(diffPath -> {
                    // Cannot use a hash-table as a) extConfig api path may be regex; and
                    // b) the diff jsonPath may be at a parent path of the ext config json path, e.g.
                    // extJsonPath at /body/id but diff is at /body as body itself is missing.
                    if (DynamicInjector
                        .apiPathMatch(Arrays.asList(extractionMeta.apiPath), diffPath.apiPath)
                        && metadata.extractionJsonPath.contains(diffPath.jsonPath)) {
                        extractionMetaHashMap.put(metadata.extractionId, extractionMeta);
                    }
                });

            });
        });

        final List<InjectionExtractionMeta> injectionExtractionMetas = new ArrayList<>();

        dynamicInjectionConfig.injectionMetas.forEach(injectionMeta ->
            injectionMeta.metadata.ifPresent(metadata ->
                Optional.ofNullable(extractionMetaHashMap.get(metadata.extractionId)).ifPresent(
                    extConfig -> injectionMeta.apiPaths
                        // Separate out each inj api path into a separate line in csv file
                        .forEach(path -> injectionExtractionMetas.add(new InjectionExtractionMeta(
                            new InjectionExtractionMeta.ExtractionConfig(extConfig.apiPath,
                                metadata.extractionJsonPath, extConfig.method),
                            new InjectionConfig(path, injectionMeta.jsonPath,
                                injectionMeta.method)))))));
        Collections.sort(injectionExtractionMetas);
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
