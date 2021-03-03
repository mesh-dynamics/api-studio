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
import io.md.utils.Utils;
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
    private static final HashSet<String> excludedStrings = new HashSet<>();
    private static final HashSet<String> restrictedHeadersAdditional = new HashSet<>();
    private static final HashSet<String> restrictedBodyFields = new HashSet<>();

    static{
        excludedStrings.addAll((Arrays
            .asList("true", "false", "0", "1", "", "\"\"", "null", "none", "en_us", "application/json")));
        restrictedHeadersAdditional.addAll(Arrays.asList("content-type"));
        restrictedBodyFields.addAll(Arrays.asList("status"));
    }


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

    private void deleteMetaInstance(ExtractionConfig extractionConfig,
        InjectionConfig injectionConfig){
        InjectionExtractionMeta toDeleteMeta = new InjectionExtractionMeta(extractionConfig,
            injectionConfig);
        metaToObjectMap.remove(toDeleteMeta);
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

    private ExtractionConfig getFirstPostOrElseFirstGetExtConfig(LinkedHashSet<ExtractionConfig> extractionConfigs){
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
        if (len > 1) {
            if (subPaths[len - 2].equals("hdrs")) {
                String headerField = subPaths[len - 1];
                if (!Utils.ALLOWED_HEADERS.test(headerField) || restrictedHeadersAdditional
                    .contains(headerField) || headerField.startsWith(":")) {
                    return true;
                }
            }
            else if(subPaths[len - 2].equals("body")){
                String bodyField = subPaths[len - 1];

                if (restrictedBodyFields.contains(bodyField) || bodyField.startsWith(":")){
                    return true;
                }
            }

        }
        return false;
    }

    private void handleJsonNode(String apiPath, JsonNode jsonNode, String jsonPath,
        HTTPMethodType method, EventType eventType) {
        if (isRestrictedJsonPath(jsonPath)){
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
                        jsonPath,
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
                if (jsonPath.contains("pathSegments")) {
                    modifiedApiPath = apiPath.replace(stringValue, ".+");
                }

                final String finalApiPath = modifiedApiPath;

                Optional<LinkedHashSet<ExtractionConfig>> extractionConfigsForPresentValue = getExtractionSetForValue(
                    stringValue);

                // If no extraction set exists for the present injection, create a new set with
                // the all extractions for the present value. Else take overlap of existing and new set.
                // If overlap is non-empty, check if previous best ext candidate and new candidates are
                // different. If they are different, remove injExt meta of prev ext. candidate.
                // Finally, create a new inj-ext meta instance and initialize the equivalence set size based on
                // the present situation. *** This is the only time a new meta instance will get created ***
                // Post this filtering, we do eventually retain multiple extractions for an injection
                // because it is possible that 2 different instances for the same injection are fed from 2 different extractions.
                // The way to represent this as actual configs is to create 2 injection configs. Based on
                // value_{suffix} variable name matching, if the first returns a null, it will be ineffective,
                // in which case the second injection config will kick-in.

                extractionConfigsForPresentValue.ifPresent(esForValue -> {

                    InjectionConfig injectionConfig = getInjectionConfigInstance(
                        finalApiPath, jsonPath,
                        method);

                    if (injectionConfig.values.contains(stringValue)) {
                        // The extraction set for a previously seen value is already processed
                        return;
                    }

                    injectionConfig.values.add(stringValue);
                    injectionConfig.instanceCount++;

                    Optional<LinkedHashSet<ExtractionConfig>> existingSet = getExtractionSetForInjection(
                        injectionConfig);

                    existingSet
                        .or(() -> Optional.of(createExtractionSetForInjection(injectionConfig)))
                        .ifPresent(existingSetForInj -> {

                            ExtractionConfig bestExtractionConfigForValue = getFirstPostOrElseFirstGetExtConfig(
                                esForValue);
                            Integer equivalenceSetSize = esForValue.size();

                            // Look for overlap between existing configs and configs for value;
                            LinkedHashSet<ExtractionConfig> overlappingExtractionConfigsForInj = new LinkedHashSet<>();
                            overlappingExtractionConfigsForInj.addAll(existingSetForInj);
                            overlappingExtractionConfigsForInj.retainAll(esForValue);


                            if (overlappingExtractionConfigsForInj.size() != 0) {
                                // Possible that the some of the earlier chosen extractions were a fluke match
                                // Check if the previous and new candidates from filtered list are same. If different
                                // remove the injectionExtractionMeta belonging to previous candidate.
                                // Addition for the new one will anyway happen in subsequent steps
                                ExtractionConfig previousBestExtraction = getFirstPostOrElseFirstGetExtConfig(
                                    existingSetForInj);
                                bestExtractionConfigForValue = getFirstPostOrElseFirstGetExtConfig(
                                    overlappingExtractionConfigsForInj);
                                if (previousBestExtraction != bestExtractionConfigForValue) {
                                    deleteMetaInstance(previousBestExtraction, injectionConfig);
                                }
                                existingSetForInj = overlappingExtractionConfigsForInj;
                                equivalenceSetSize = existingSetForInj.size();

                            } else {
                                // This case may be hit in 2 cases:
                                // a. This inj path is seen for first time
                                // b. The value being injected in this path comes from a different
                                //    source altogether in this instance. In this case, just add
                                //    new ones to the existing set so that we don't lose out on
                                //    the info in future of which ext config is more likely when another
                                //    new value is found in the injection path.
                                existingSetForInj.addAll(esForValue);
                            }

                            InjectionExtractionMeta injectionExtractionMeta = getMetaInstance(
                                bestExtractionConfigForValue,
                                injectionConfig);

                            injectionExtractionMeta.extractionEquivalenceSetSize = equivalenceSetSize;

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
