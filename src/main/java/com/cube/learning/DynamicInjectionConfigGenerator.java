package com.cube.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.JsonDataObj;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjector;
import io.md.injection.InjectionExtractionMeta;
import io.md.injection.InjectionExtractionMeta.ExtractionConfig;
import io.md.injection.InjectionExtractionMeta.InjectionConfig;
import io.md.utils.Utils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;


public class DynamicInjectionConfigGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicInjector.class);

    // These maps are to retrieve singleton objects for each unique inj/ext config and meta
    private final HashMap<InjectionConfig, InjectionConfig> injectionConfigToObjectMap = new HashMap<>();
    private final HashMap<ExtractionConfig, ExtractionConfig> extractionConfigToObjectMap = new HashMap<>();
    private final HashMap<InjectionExtractionMeta, InjectionExtractionMeta> metaToObjectMap = new HashMap<>();

    // This map keeps track of which all values are already seen in requests, to avoid creating
    // extraction configs for them.
    private final Set<String> valuesAlreadySeenInRequestSet = new HashSet<>();

    // Insertion Order of configs is important, hence using a linked hash for some maps.

    // This map keeps track of which all extraction configs are applicable for a particular value.
    private final HashMap<String, LinkedHashSet<ExtractionConfig>> valueToExtractionConfigMap = new HashMap<>();

    // These are 1-Many maps for injection to extraction and vice versa.
    private final HashMap<InjectionConfig, LinkedHashSet<ExtractionConfig>> injectionToExtractionsMap = new HashMap<>();
    private final HashMap<ExtractionConfig, LinkedHashSet<InjectionConfig>> extractionToInjectionsMap = new HashMap<>();

    // List of qualifying injection-extraction metas
    private final ArrayList<InjectionExtractionMeta> finalMetaList = new ArrayList<>();

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


    private ExtractionConfig getExtractionConfigInstance(String apiPath, String jsonPath,
        HTTPMethodType method) {

        ExtractionConfig newDIConfig = new ExtractionConfig(apiPath, jsonPath, method);
        return extractionConfigToObjectMap.computeIfAbsent(newDIConfig, k -> newDIConfig);

    }

    private InjectionConfig getInjectionConfigInstance(String apiPath, String jsonPath,
        HTTPMethodType method, String xfm, Boolean injectAllPaths) {

        InjectionConfig newDIConfig = new InjectionConfig(apiPath, jsonPath, method, xfm, injectAllPaths);
        return injectionConfigToObjectMap.computeIfAbsent(newDIConfig, k -> newDIConfig);

    }

    private InjectionExtractionMeta getMetaInstance(ExtractionConfig extractionConfig,
        InjectionConfig injectionConfig) {

        InjectionExtractionMeta newMeta = new InjectionExtractionMeta(extractionConfig,
            injectionConfig);
        return metaToObjectMap.computeIfAbsent(newMeta, k -> newMeta);

    }

    private Optional<LinkedHashSet<ExtractionConfig>> getExtractionSetForValue(String value) {
        return Optional.ofNullable(valueToExtractionConfigMap.get(value));
    }

    private LinkedHashSet<ExtractionConfig> createExtractionSetForValue(String value) {

        return valueToExtractionConfigMap.computeIfAbsent(value, k -> new LinkedHashSet<>());
    }

    private Optional<LinkedHashSet<ExtractionConfig>> getExtractionSetForInjection(
        InjectionConfig injectionConfig) {
        return Optional.ofNullable(injectionToExtractionsMap.get(injectionConfig));
    }

    private LinkedHashSet<ExtractionConfig> createExtractionSetForInjection(
        InjectionConfig injectionConfig) {
        return injectionToExtractionsMap
            .computeIfAbsent(injectionConfig, k -> new LinkedHashSet<>());
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

    private ExtractionConfig getFirstPostOrElseFirstGetExtConfig(Set<ExtractionConfig> extractionConfigs){
        for (ExtractionConfig extractionConfig : extractionConfigs) {
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
     * Returns ref value to lookup, xfm required before insertion, and if value should be
     * injected at all paths, based on special handling of certain paths.
     * @param jsonPath
     * @param value
     * @return Triple (lookupValue, Xfrm, InjectAllPaths)
     */
    private Triple<String, String, Boolean> getLookupValAndXfmAndInjectAllPaths(String jsonPath,
        String value) {
        if (jsonPath.toLowerCase().startsWith(AUTH_HDR) && value.toLowerCase()
            .startsWith("bearer")) {
            return Triple.of(value.replaceFirst("^[Bb]earer", "").trim(),
                "Bearer " + InjectionMeta.valueMarker, true);
        } else {
            return Triple.of(value, InjectionMeta.valueMarker, false);
        }
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
                    ExtractionConfig extConfig = getExtractionConfigInstance(apiPath, jsonPath,
                        method);
                    extConfig.instanceCount++;
                    extConfig.values.add(stringValue);  // Set, so duplication is taken care of

                    getExtractionSetForValue(stringValue)
                        .orElse(createExtractionSetForValue(stringValue))
                        .add(extConfig);
                }
            } else if (eventType == Event.EventType.HTTPRequest) {
                Triple<String, String, Boolean> getLookupValAndXfmAndInjectAllPaths =
                    getLookupValAndXfmAndInjectAllPaths(jsonPath, stringValue);
                final String lookupVal = getLookupValAndXfmAndInjectAllPaths.getLeft();
                final String xfm = getLookupValAndXfmAndInjectAllPaths.getMiddle();
                final Boolean injectAllPaths = getLookupValAndXfmAndInjectAllPaths.getRight();;
                // Add to map to keep track of values already spotted in requests
                valuesAlreadySeenInRequestSet.add(stringValue);

                String modifiedApiPath = Optional.ofNullable(regexPathsMap.get(apiPath)).orElse(apiPath);

                if (jsonPath.contains("pathSegments")) {
                    // Replace exact subPath matches in the beg, middle, or end of string
                    modifiedApiPath = modifiedApiPath.replaceFirst("(^|/)" + lookupVal + "(/|$)", "$1.+$2");
                }

                final String finalApiPath = modifiedApiPath;

                Optional<LinkedHashSet<ExtractionConfig>> extractionConfigsForPresentValue = getExtractionSetForValue(
                    lookupVal);

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

                extractionConfigsForPresentValue.ifPresent(esForValue -> {

                    InjectionConfig injectionConfig = getInjectionConfigInstance(apiPath, jsonPath,
                        method, xfm, injectAllPaths);

                    if (injectionConfig.values.contains(lookupVal)) {
                        // The extraction set for a previously seen value is already processed
                        return;
                    }

                    injectionConfig.values.add(lookupVal);
                    injectionConfig.instanceCount++;

                    Optional<LinkedHashSet<ExtractionConfig>> existingSet = getExtractionSetForInjection(
                        injectionConfig);

                    // Use only the best extraction, keeping others as its equivalence set candidates
                    // and record their count
                    ExtractionConfig bestExtractionConfigForValue = getFirstPostOrElseFirstGetExtConfig(
                        esForValue);

                    existingSet
                        .or(() -> Optional.of(createExtractionSetForInjection(injectionConfig)))
                        .ifPresent(existingSetForInj -> {

                            InjectionExtractionMeta injectionExtractionMeta = getMetaInstance(
                                bestExtractionConfigForValue,
                                injectionConfig);

                            if (!existingSetForInj.contains(bestExtractionConfigForValue)) {
                                existingSetForInj.add(bestExtractionConfigForValue);

                                // Only if this extraction config is seen for the first time for this
                                // injection, update the equivalence set size.

                                // All other extractions for the same value contribute to the equivalence set
                                injectionExtractionMeta.extractionEquivalenceSetSize = esForValue
                                    .size();
                            }

                            // Count for this pair
                            injectionExtractionMeta.instanceCount++;

                            // Add reference values for this specific pair
                            injectionExtractionMeta.values.add(lookupVal);

                            if (!finalApiPath.equals(apiPath)){
                                // This is a regex-ed path. Overwrite existing instance
                                // as a new instance will be more regex-ed than prev.
                                // E.g.: api/x/y -> api/*/y -> api/*/*
                                regexPathsMap.put(apiPath, finalApiPath);
                            }
                        });

                    // add this injection to its set in the extraction -> injections map

                });

                valuesAlreadySeenInRequestSet.add(lookupVal);

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

    public List<InjectionExtractionMeta> generateConfigs(Boolean discardSingleValues)
        throws JsonProcessingException {

        Map<InjectionExtractionMeta, InjectionExtractionMeta> regexedMetasMap = new HashMap<>();

        for (InjectionExtractionMeta injectionExtractionMeta : metaToObjectMap.keySet()) {
            String injApiPath = injectionExtractionMeta.injectionConfig.apiPath;
            injectionExtractionMeta.injectionConfig.apiPath = Optional
                .ofNullable(regexPathsMap.get(injApiPath)).orElse(injApiPath);
            regexedMetasMap
                .computeIfAbsent(injectionExtractionMeta, k -> injectionExtractionMeta).values
                .addAll(injectionExtractionMeta.values); // Add new new ref values
        }

        for (InjectionExtractionMeta injectionExtractionMeta : regexedMetasMap.keySet()) {
            if (!discardSingleValues || injectionExtractionMeta.values.size() > 1) {
                injectionExtractionMeta.calculateScores();
                finalMetaList.add(injectionExtractionMeta);
            }
        }

        Collections.sort(finalMetaList);

        return finalMetaList;
    }
}
