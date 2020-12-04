package com.cube.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.JsonDataObj;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjector;
import com.cube.learning.InjectionExtractionMeta.ExtractionConfig;
import com.cube.learning.InjectionExtractionMeta.InjectionConfig;
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
    List<String> excludedStrings = Arrays
        .asList("true", "false", "0", "1", "", "\"\"", "null", "none", "en_us");

    private ExtractionConfig getExtractionConfigInstance(String apiPath, String jsonPath,
        HTTPMethodType method) {

        ExtractionConfig newDIConfig = new ExtractionConfig(apiPath, jsonPath, method);
        return extractionConfigToObjectMap.computeIfAbsent(newDIConfig, k -> newDIConfig);

    }

    private InjectionConfig getInjectionConfigInstance(String apiPath, String jsonPath,
        HTTPMethodType method) {

        InjectionConfig newDIConfig = new InjectionConfig(apiPath, jsonPath, method);
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

    private void handleJsonNode(String apiPath, JsonNode jsonNode, String keyPath,
        HTTPMethodType method, EventType eventType) {
        if (jsonNode.isObject()) {
            handleJSONObject(apiPath, jsonNode, keyPath, method, eventType);
        } else if (jsonNode.isArray()) {
            handleJSONArray(apiPath, jsonNode, keyPath, method, eventType);
        } else if (jsonNode.isValueNode()) {
            String stringValue = jsonNode.asText();
            if (excludedStrings.contains(stringValue.toLowerCase())) {
                return;
            }

            if (eventType == Event.EventType.HTTPResponse) {
                // Consider for extraction
                Optional<LinkedHashSet<ExtractionConfig>> extractionConfigListForThisValue = getExtractionSetForValue(
                    stringValue);

                extractionConfigListForThisValue.or(() -> {
                    if (!valuesAlreadySeenInRequestSet.contains(stringValue)) {
                        return Optional.of(createExtractionSetForValue(stringValue));
                    } else {
                        return Optional.empty();
                    }

                }).ifPresent(extConfigList -> {
                    ExtractionConfig extConfig = getExtractionConfigInstance(
                        apiPath,
                        keyPath,
                        method);
                    extConfig.instanceCount++;
                    extConfig.values.add(stringValue);  // Set, so duplication is taken care of
                    extConfigList.add(extConfig);
                });
            } else if (eventType == Event.EventType.HTTPRequest) {
                // Add to map to keep track of values
                // already spotted in requests
                String modifiedApiPath = apiPath;
                valuesAlreadySeenInRequestSet.add(stringValue);
                if (keyPath.contains("pathSegments")) {
                    modifiedApiPath = apiPath.replace(stringValue, ".+");
                }

                final String finalApiPath = modifiedApiPath;

                Optional<LinkedHashSet<ExtractionConfig>> extractionConfigsForPresentValue = getExtractionSetForValue(
                    stringValue);

                // If no extraction set exists for the present injection, create a new set with
                // the first extraction in the set for the present value (which is also the place of value's first appearance).
                // Else, check if the first extraction config already present in the existing set.
                // If this config already exists, then just add this new value in the values list of inj-ext meta.
                // Else, create a new inj-ext meta instance and initialize the equivalence set size based on
                // the present situation. *** This is the only time a new meta instance will get created ***
                // Post this filtering, we do eventually retain multiple extractions for an injection
                // because it is possible that 2 different instances for the same injection are fed from 2 different extractions.
                // The way to represent this as actual configs is to create 2 injection configs. Based on
                // value_{suffix} variable name matching, if the first returns a null, it will be ineffective,
                // in which case the second injection config will kick-in.

                extractionConfigsForPresentValue.ifPresent(esForValue -> {

                    InjectionConfig injectionConfig = getInjectionConfigInstance(
                        finalApiPath, keyPath,
                        method);

                    if (injectionConfig.values.contains(stringValue)) {
                        // The extraction set for a previously seen value is already processed
                        return;
                    }

                    injectionConfig.values.add(stringValue);
                    injectionConfig.instanceCount++;

                    Optional<LinkedHashSet<ExtractionConfig>> existingSet = getExtractionSetForInjection(
                        injectionConfig);

                    // Use only the first extraction, keeping others as its equivalence set candidates
                    // and record their count
                    ExtractionConfig firstExtractionConfig = esForValue.iterator().next();

                    existingSet
                        .or(() -> Optional.of(createExtractionSetForInjection(injectionConfig)))
                        .ifPresent(existingSetForInj -> {

                            InjectionExtractionMeta injectionExtractionMeta = getMetaInstance(
                                firstExtractionConfig,
                                injectionConfig);

                            if (!existingSetForInj.contains(firstExtractionConfig)) {
                                existingSetForInj.add(firstExtractionConfig);

                                // Only if this extraction config is seen for the first time for this
                                // injection, update the equivalence set size.

                                // All other extractions for the same value contribute to the equivalence set
                                injectionExtractionMeta.extractionEquivalenceSetSize = esForValue
                                    .size();
                            }

                            // Count for this pair
                            injectionExtractionMeta.instanceCount++;

                            // Add reference values for this specific pair
                            injectionExtractionMeta.values.add(stringValue);
                        });

                    // add this injection to its set in the extraction -> injections map

                });

                valuesAlreadySeenInRequestSet.add(stringValue);

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

        for (InjectionExtractionMeta injectionExtractionMeta : metaToObjectMap.keySet()) {
            if (!discardSingleValues || injectionExtractionMeta.values.size() > 1) {
                injectionExtractionMeta.calculateScores();
                finalMetaList.add(injectionExtractionMeta);

            }
        }

        Collections.sort(finalMetaList);

        return finalMetaList;
    }
}
