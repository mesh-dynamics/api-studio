package com.cube.learning;

import com.cube.dao.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.JsonDataObj;
import io.md.dao.ReqRespMatchResult;
import io.md.injection.DynamicInjectionConfig;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjector;
import io.md.injection.InjectionExtractionMeta;
import io.md.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.collections4.trie.PatriciaTrie;


public class DynamicInjectionRulesLearner {

    private final List<String> paths;

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

    private void processReplayMatchResults(Stream<ReqRespMatchResult> reqRespMatchResultStream, Set<DiffPath> diffPathHashSet) {
        // NOTE: We are being sloppy here by not considering the req/resp method. But since it is
        // just filtering rules based on diffs, an occasional false-positive is admissible.
        // Retrieving method from orig events is too much of data processing overhead for marginal ben.
        reqRespMatchResultStream.forEach(res ->
                res.respCompareRes.diffs.forEach(
                    // Use set to de-dup recurring api,json path combinations.
                    diff -> {
                        DiffPath diffPath = new DiffPath(res.path, diff.path);
                        diff.fromValue.ifPresent(fVal -> diffPath.refValues.add(fVal.asText()));
                        diff.value.ifPresent(val -> diffPath.refValues.add(val.asText()));
                        diffPathHashSet.add(diffPath);
                    }
                )
        );

    }

    public List<InjectionExtractionMeta> generateFilteredRules(DynamicInjectionConfig dynamicInjectionConfig, Stream<ReqRespMatchResult> reqRespMatchResultStream){
        final Set<InjectionExtractionMeta> selectedMetasSet = new HashSet<>();
        final PatriciaTrie<Set<InjectionExtractionMeta>> injectionExtractionMetaTrie = new PatriciaTrie<>();
        final Set<DiffPath> diffPathHashSet = new HashSet<>();

        processReplayMatchResults(reqRespMatchResultStream, diffPathHashSet);

        dynamicInjectionConfig.injectionExtractionMetas.forEach(meta ->
                // Add all metas to a trie with json path as key
                // Multiple APIPaths may have same json path, so add all apiPath Metas to the jsonPath key
                injectionExtractionMetaTrie
                    .computeIfAbsent(meta.extractionConfig.jsonPath, k -> new HashSet<>())
                    .add(meta));

        diffPathHashSet.forEach(diffPath ->
            // Cannot use a hash-table as a) extConfig api path may be regex; and
            // b) the diff jsonPath may be at a parent path of the ext config json path, e.g.
            // extJsonPath at /body/id but diff is at /body as body itself is missing.
            injectionExtractionMetaTrie.prefixMap(diffPath.jsonPath).values()
                .forEach(injectionExtractionMetas -> injectionExtractionMetas.forEach(meta -> {
                        if (DynamicInjector.apiPathMatch(Arrays.asList(meta.extractionConfig.apiPath),
                            diffPath.apiPath)) {
                            if (diffPath.jsonPath.equals(meta.extractionConfig.jsonPath)) {
                                // Take ref values only if json path is an exact match.
                                meta.values.addAll(diffPath.refValues);
                            }
                            selectedMetasSet.add(meta);
                        }
                    }
                )));

        final List<InjectionExtractionMeta> selectedMetasList = new ArrayList<>(selectedMetasSet);

        Collections.sort(selectedMetasList);
        return selectedMetasList;
    }

    public List<InjectionExtractionMeta> generateRules(Optional<Boolean> discardSingleValues)
        throws JsonProcessingException {
        return diGen.generateConfigs(discardSingleValues.orElse(false));
    }

    private static class DiffPath {
        String apiPath, jsonPath;
        Set<String> refValues = new HashSet<>();

        public DiffPath(String apiPath, String jsonPath) {
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
            DiffPath diffPath = (DiffPath) o;
            return apiPath.equals(diffPath.apiPath) &&
                jsonPath.equals(diffPath.jsonPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(apiPath, jsonPath);
        }
    }

    private static class ExtractionMetaWithValues{
        ExtractionMeta extractionMeta;
        Set<String> values;

        public ExtractionMetaWithValues(
            ExtractionMeta extractionMeta, Set<String> values) {
            this.extractionMeta = extractionMeta;
            this.values = values;
        }
    }
}
