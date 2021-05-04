package com.cube.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.JsonDataObj;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjector;
import io.md.injection.ExternalInjectionExtraction;
import io.md.injection.ExternalInjectionExtraction.ExternalExtraction;
import io.md.injection.ExternalInjectionExtraction.ExternalInjection;
import io.md.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;


public class DynamicInjectionConfigGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicInjector.class);

    // These maps are to retrieve singleton objects for each unique inj/ext config and meta
    private final HashMap<ExternalInjection, ExternalInjection> injectionToObjectMap = new HashMap<>();
    private final HashMap<ExternalExtraction, ExternalExtraction> extractionToObjectMap = new HashMap<>();
    private final HashMap<ExternalInjectionExtraction, ExternalInjectionExtraction> injExtToObjectMap = new HashMap<>();

    // This map keeps track of which all values are already seen in requests, to avoid creating
    // extraction configs for them.
    private final Set<String> valuesAlreadySeenInRequestSet = new HashSet<>();

    // Insertion Order of configs is important, hence using a linked hash for some maps.

    // This map keeps track of which all extraction configs are applicable for a particular value.
    private final HashMap<String, LinkedHashSet<ExternalExtraction>> valueToExtractionMap = new HashMap<>();

    // These are 1-Many maps for injection to extraction and vice versa.
    private final HashMap<ExternalInjection, LinkedHashSet<ExternalExtraction>> injToExtractionsMap = new HashMap<>();

    // List of qualifying injection-extraction metas
    private final ArrayList<ExternalInjectionExtraction> finalInjExtList = new ArrayList<>();

    // Map to match responses to requests for method extraction
    private final HashMap<String, HTTPMethodType> requestMatchMap = new HashMap<>();

    // Strings not considered for config generation
    private static final Set<String> excludedStrings = new HashSet<>();
    private static final Set<String> restrictedHeadersAdditional = new HashSet<>();
    private static final Set<String> restictedFields = new HashSet<>();

    private static final String AUTH_HDR = "/hdrs/authorization";

    static{
        excludedStrings.addAll((Arrays
            .asList("true", "false", "0", "1", "", "\"\"", "null", "none", "en_us", "application/json")));
        restrictedHeadersAdditional.addAll(Arrays.asList("content-type"));
        restictedFields.addAll(Arrays.asList("status"));
    }

    private static final Map<String, String> regexPathsMap = new HashMap<>();


    private ExternalExtraction getExtractionInstance(String apiPath, String jsonPath,
        HTTPMethodType method) {

        ExternalExtraction newDIConfig = new ExternalExtraction(apiPath, jsonPath, method);
        return extractionToObjectMap.computeIfAbsent(newDIConfig, k -> newDIConfig);

    }

    private ExternalInjection getExternalInjectionInstance(String apiPath, String jsonPath,
        HTTPMethodType method, String xfm, Boolean injectAllPaths) {

        ExternalInjection newDIConfig = new ExternalInjection(apiPath, jsonPath, method, xfm, injectAllPaths);
        return injectionToObjectMap.computeIfAbsent(newDIConfig, k -> newDIConfig);

    }

    private ExternalInjectionExtraction getMetaInstance(ExternalExtraction extractionConfig,
        ExternalInjection externalInjection) {

        ExternalInjectionExtraction newMeta = new ExternalInjectionExtraction(extractionConfig,
            externalInjection);
        return injExtToObjectMap.computeIfAbsent(newMeta, k -> newMeta);

    }

    private Optional<LinkedHashSet<ExternalExtraction>> getExtractionSetForValue(String value) {
        return Optional.ofNullable(valueToExtractionMap.get(value));
    }

    private LinkedHashSet<ExternalExtraction> createExtractionSetForValue(String value) {

        return valueToExtractionMap.computeIfAbsent(value, k -> new LinkedHashSet<>());
    }

    private Optional<LinkedHashSet<ExternalExtraction>> getExtractionSetForInjection(
        ExternalInjection externalInjection) {
        return Optional.ofNullable(injToExtractionsMap.get(externalInjection));
    }

    private LinkedHashSet<ExternalExtraction> createExtractionSetForInjection(
        ExternalInjection externalInjection) {
        return injToExtractionsMap
            .computeIfAbsent(externalInjection, k -> new LinkedHashSet<>());
    }

    public void processJSONObject(JsonDataObj jsonDataObj, String apiPath, String baseJSONPath,
        EventType eventType, Optional<String> requestId, Optional<HTTPMethodType> method) {

        method.map(m -> {
            requestId.ifPresent(r -> requestMatchMap.put(r, m));
            return m;
        }).or(() -> {
            return requestId.flatMap(r -> Optional.ofNullable(requestMatchMap.get(r)));
        }).ifPresent(m -> {

            if (!jsonDataObj.isDataObjEmpty()) {
                JsonNode jsonNode = jsonDataObj.getRoot();
                handleJsonNode(apiPath, jsonNode, baseJSONPath, m, eventType);
            }
        });

    }

    private ExternalExtraction getFirstPostOrElseFirstGetExt(Set<ExternalExtraction> extractionConfigs){
        for (ExternalExtraction extractionConfig : extractionConfigs) {
            if (extractionConfig.method == HTTPMethodType.POST){
                return extractionConfig;
            }
        }
        // Assumes the list is non-empty
        return extractionConfigs.iterator().next();
    }

    private Boolean isRestrictedJsonPath(String jsonPath) {
        String[] subPaths = jsonPath.split("/");
        int len = subPaths.length;
        if (len > 1 && subPaths[len - 2].equals("hdrs")) {
            String headerField = subPaths[len - 1];
            if (!Utils.ALLOWED_HEADERS.test(headerField) || restrictedHeadersAdditional
                .contains(headerField) || headerField.startsWith(":")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns ref value to lookup, xfm required before insertion,
     * based on special handling of certain paths.
     * @param jsonPath
     * @param value
     * @return Pair (lookupValue, Xfrm)
     */
    private Pair<String, String> getLookupValAndXfm(String jsonPath,
        String value) {
        if (jsonPath.toLowerCase().startsWith(AUTH_HDR) && value.toLowerCase()
            .startsWith("bearer")) {
            return Pair.of(value.replaceFirst("^[Bb]earer", "").trim(),
                "Bearer " + InjectionMeta.valueMarker);
        } else {
            return Pair.of(value, InjectionMeta.valueMarker);
        }
    }

    private boolean shouldInjectAllPaths(String jsonPath){
        return jsonPath.toLowerCase().startsWith(AUTH_HDR);
    }

    private void handleJsonNode(String apiPath, JsonNode jsonNode, String jsonPath,
        HTTPMethodType method, EventType eventType) {
        if (restictedFields.contains(jsonPath) || isRestrictedJsonPath(jsonPath)){
            return;
        }
        if (jsonNode.isObject()) {
            handleJSONObject(apiPath, jsonNode, jsonPath, method, eventType);
        } else if (jsonNode.isArray()) {
            handleJSONArray(apiPath, jsonNode, jsonPath, method, eventType);
        } else if (jsonNode.isValueNode()) {
            String stringValue = jsonNode.asText();
            if (excludedStrings.contains(stringValue.toLowerCase())) {
                return;
            }

            if (eventType == Event.EventType.HTTPResponse) {
                if (!valuesAlreadySeenInRequestSet.contains(stringValue)) {
                    // Consider for extraction
                    ExternalExtraction extConfig = getExtractionInstance(apiPath, jsonPath,
                        method);
                    extConfig.instanceCount++;
                    extConfig.values.add(stringValue);  // Set, so duplication is taken care of

                    getExtractionSetForValue(stringValue)
                        .orElse(createExtractionSetForValue(stringValue))
                        .add(extConfig);
                }
            } else if (eventType == Event.EventType.HTTPRequest) {

                String lookupVal = stringValue;
                String xfm = InjectionMeta.valueMarker;
                Boolean injectAllPaths = shouldInjectAllPaths(jsonPath);
                // Add to map to keep track of values already spotted in requests
                valuesAlreadySeenInRequestSet.add(lookupVal);


                String modifiedApiPath = Optional.ofNullable(regexPathsMap.get(apiPath)).orElse(apiPath);

                if (jsonPath.contains("pathSegments")) {
                    // Replace exact subPath matches in the beg, middle, or end of string
                    modifiedApiPath = modifiedApiPath.replaceFirst("(^|/)" + lookupVal + "(/|$)", "$1.+$2");
                }

                final String finalApiPath = modifiedApiPath;

                Optional<LinkedHashSet<ExternalExtraction>> extractionsForPresentValue = getExtractionSetForValue(
                    lookupVal);

                if (extractionsForPresentValue.isEmpty()){
                    // Retry with modified value. injectAllPaths is retained as it is path-based.
                    Pair<String, String> lookupValAndXfm = getLookupValAndXfm(jsonPath, stringValue);
                    lookupVal = lookupValAndXfm.getLeft();
                    xfm = lookupValAndXfm.getRight();
                    valuesAlreadySeenInRequestSet.add(lookupVal);
                    extractionsForPresentValue = getExtractionSetForValue(lookupVal);
                }

                final String finalLookupVal = lookupVal;
                final String finalXfm = xfm;

                // If no extraction set exists for the present injection, create a new set with
                // the first extraction in the set for the present value (which is also the place of value's first appearance).
                // Else, check if the best extraction config already present in the existing set.
                // If this config already exists, then just add this new value in the values list of inj-ext meta.
                // Else, create a new inj-ext meta instance and initialize the equivalence set size based on
                // the present situation. *** This is the only time a new meta instance will get created ***
                // Post this filtering, we do eventually retain multiple extractions for an injection
                // because it is possible that 2 different instances for the same injection are fed from 2 different extractions.
                // The way to represent this as actual configs is to create 2 injection configs. Based on
                // value_{suffix} variable name matching, if the first returns a null, it will be ineffective,
                // in which case the second injection config will kick-in.

                extractionsForPresentValue.ifPresent(esForValue -> {

                    ExternalInjection injection = getExternalInjectionInstance(apiPath, jsonPath,
                        method, finalXfm, injectAllPaths);

                    if (injection.values.contains(finalLookupVal)) {
                        // The extraction set for a previously seen value is already processed
                        return;
                    }

                    injection.values.add(finalLookupVal);
                    injection.instanceCount++;

                    Optional<LinkedHashSet<ExternalExtraction>> existingSet = getExtractionSetForInjection(
                        injection);

                    // Use only the best extraction, keeping others as its equivalence set candidates
                    // and record their count
                    ExternalExtraction bestExtractionForValue = getFirstPostOrElseFirstGetExt(
                        esForValue);

                    existingSet
                        .or(() -> Optional.of(createExtractionSetForInjection(injection)))
                        .ifPresent(existingSetForInj -> {

                            ExternalInjectionExtraction injectionExtraction = getMetaInstance(
                                bestExtractionForValue,
                                injection);

                            if (!existingSetForInj.contains(bestExtractionForValue)) {
                                existingSetForInj.add(bestExtractionForValue);

                                // Only if this extraction config is seen for the first time for this
                                // injection, update the equivalence set size.

                                // All other extractions for the same value contribute to the equivalence set
                                injectionExtraction.extractionEquivalenceSetSize = esForValue
                                    .size();
                            }

                            // Count for this pair
                            injectionExtraction.instanceCount++;

                            // Add reference values for this specific pair
                            injectionExtraction.values.add(finalLookupVal);

                            if (!finalApiPath.equals(apiPath)){
                                // This is a regex-ed path. Overwrite existing instance
                                // as a new instance will be more regex-ed than prev.
                                // E.g.: api/x/y -> api/*/y -> api/*/*
                                regexPathsMap.put(apiPath, finalApiPath);
                            }
                        });

                    // add this injection to its set in the extraction -> injections map

                });

            } else {
                LOGGER.error("Found unhandled requestType: " + eventType.toString());
            }

        }
    }

    private void handleJSONObject(String apiPath, JsonNode jsonNode, String keyPath,
        HTTPMethodType method, EventType eventType) {
        jsonNode.fieldNames().forEachRemaining(key -> {
            JsonNode value = jsonNode.get(key);
            handleJsonNode(apiPath, value, keyPath + '/' + key.toString(), method, eventType);
        });
    }

    private void handleJSONArray(String apiPath, JsonNode jsonNode, String keyPath,
        HTTPMethodType method, EventType eventType) {
        Integer index = 0;
        Iterator<JsonNode> jsonNodesIterator = jsonNode.elements();
        while (jsonNodesIterator.hasNext()) {
            JsonNode value = jsonNodesIterator.next();
            handleJsonNode(apiPath, value, keyPath + '/' + index.toString(), method, eventType);
            index++;
        }
    }

    public List<ExternalInjectionExtraction> generateConfigs(Boolean discardSingleValues)
        throws JsonProcessingException {

        Map<ExternalInjectionExtraction, ExternalInjectionExtraction> regexedMetasMap = new HashMap<>();

        for (ExternalInjectionExtraction injectionExtraction : injExtToObjectMap.keySet()) {
            String injApiPath = injectionExtraction.externalInjection.apiPath;
            injectionExtraction.externalInjection.apiPath = Optional
                .ofNullable(regexPathsMap.get(injApiPath)).orElse(injApiPath);
            regexedMetasMap
                .computeIfAbsent(injectionExtraction, k -> injectionExtraction).values
                .addAll(injectionExtraction.values); // Add new new ref values
        }

        for (ExternalInjectionExtraction injectionExtraction : regexedMetasMap.keySet()) {
            if (!discardSingleValues || injectionExtraction.values.size() > 1) {
                injectionExtraction.calculateScores();
                finalInjExtList.add(injectionExtraction);
            }
        }

        Collections.sort(finalInjExtList);

        return finalInjExtList;
    }
}
