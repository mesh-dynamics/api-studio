package com.cube.learning;

import io.md.injection.DynamicInjectionConfig;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.InjectionExtractionMeta;
import io.md.injection.InjectionExtractionMeta.ExtractionConfig;
import io.md.injection.InjectionExtractionMeta.InjectionConfig;
import java.util.*;

public class DynamicInjectionGeneratedToActualConvertor {

    // Not using static vars as a single instance is expected to be created of this class.

    // This map holds Master extraction config object pointers to avoid duplicate extraction configs.
    HashMap<ExtractionConfig, ExtractionConfig> generatedExtractionConfigHashMap = new HashMap<>();
    List<DynamicInjectionConfig.ExtractionMeta> actualExtractionConfigList = new ArrayList<>();
    List<DynamicInjectionConfig.InjectionMeta> actualInjectionConfigList = new ArrayList<>();

    //Fixed seed to ensure reproducibility of config
    private final Random random = new Random(1);

    private String getNameSuffix(String apiPath, String jsonPath) {
        String[] apiPathList = apiPath.split("/");
        String[] keyPathList = jsonPath.split("/");
        // Add random number to further reduce chances of collision
        return "_" + apiPathList[apiPathList.length - 1] + "_" + keyPathList[keyPathList.length - 1]
            + "_" + (int) (random.nextFloat() * 1000);
    }

    public DynamicInjectionConfig convertGeneratedToActualConfigs(String customerId, String app,
        String version,
        List<InjectionExtractionMeta> injectionExtractionMetaList) {

        for (InjectionExtractionMeta injectionExtractionMeta : injectionExtractionMetaList) {
            // Make sure only unique extraction configs survive

            ExtractionConfig extractionConfig = generatedExtractionConfigHashMap
                .get(injectionExtractionMeta.extractionConfig);

            if (extractionConfig == null) {
                // This is the first instance, and henceforth the Master instance
                extractionConfig = injectionExtractionMeta.extractionConfig;
                extractionConfig.nameSuffix = getNameSuffix(
                    injectionExtractionMeta.extractionConfig.apiPath,
                    injectionExtractionMeta.extractionConfig.jsonPath);
                // Put this extraction config as the reference for all subsequent injections using this
                generatedExtractionConfigHashMap.put(extractionConfig, extractionConfig);
            }

            InjectionConfig injConfig = injectionExtractionMeta.injectionConfig;

            DynamicInjectionConfig.InjectionMeta actualInjectionConfig =
                new DynamicInjectionConfig.InjectionMeta(
                    Collections.singletonList(injConfig.apiPath),
                    injConfig.jsonPath,
                    injConfig.injectAllPaths,
                    // ":-" symbol is a delimiter for specifying a default value specified by the library
                    // - <empty> in this case.
                    // Ref: https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html
                    String.format("${Golden.Request: %s:-}" +
                            extractionConfig.nameSuffix,
                        injectionExtractionMeta.injectionConfig.jsonPath),
                    // Include only if there is some change from the default
                    injConfig.xfm.equals(InjectionMeta.valueMarker) ? Optional.empty()
                        : Optional.of(injConfig.xfm),
                    null,
                    injConfig.method,
                    Optional.empty(),
                    extractionConfig.apiPath,
                    extractionConfig.jsonPath,
                    extractionConfig.method);

            actualInjectionConfigList.add(actualInjectionConfig);
        }

        for (ExtractionConfig extractionConfig : generatedExtractionConfigHashMap.values()) {
            // Uniqueness in injection configs is assumed to be ensured in the imported csv
            DynamicInjectionConfig.ExtractionMeta actualExtractionConfig =
                new DynamicInjectionConfig.ExtractionMeta(
                    extractionConfig.apiPath,
                    extractionConfig.jsonPath,
                    extractionConfig.method,
                    // ":-" symbol is a delimiter for specifying a default value specified by the library
                    // - <empty> in this case.
                    // Ref: https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html
                    // This is to handle case for devtool where no Golden Response exists for the first
                    // time a request is sent, yet we want ext/inj to happen. E.g. login.
                    String.format("${Golden.Response: %s:-}" +
                            extractionConfig.nameSuffix,
                        extractionConfig.jsonPath),
                    String.format("${TestSet.Response: %s}", extractionConfig.jsonPath),
                    true,
                    false,
                    Optional.empty());

            actualExtractionConfigList.add(actualExtractionConfig);
        }

        return new DynamicInjectionConfig(version, customerId, app, Optional.empty(),
            actualExtractionConfigList, actualInjectionConfigList, Collections.emptyList(),
            injectionExtractionMetaList);

    }

}
