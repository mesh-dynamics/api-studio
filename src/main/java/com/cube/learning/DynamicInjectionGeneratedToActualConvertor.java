package com.cube.learning;

import com.cube.learning.InjectionExtractionMeta.ExtractionConfig;
import io.md.injection.DynamicInjectionConfig;
import java.util.*;

public class DynamicInjectionGeneratedToActualConvertor {

    // Not using static vars as a single instance is expected to be created of this class.

    // This map holds Master extraction config object pointers to avoid duplicate extraction configs.
    HashMap<InjectionExtractionMeta.ExtractionConfig, InjectionExtractionMeta.ExtractionConfig> generatedExtractionConfigHashMap = new HashMap<>();
    List<DynamicInjectionConfig.ExtractionMeta> actualExtractionConfigList = new ArrayList<>();
    List<DynamicInjectionConfig.InjectionMeta> actualInjectionConfigList = new ArrayList<>();

    //Fixed seed to ensure reproducibility of config
    private Random random = new Random(1);

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

            ExtractionConfig extractionConfig = generatedExtractionConfigHashMap.get(injectionExtractionMeta.extractionConfig);

            if (extractionConfig == null) {
                // This is the first instance, and henceforth the Master instance
                extractionConfig = injectionExtractionMeta.extractionConfig;
                extractionConfig.nameSuffix = getNameSuffix(
                    injectionExtractionMeta.extractionConfig.apiPath,
                    injectionExtractionMeta.extractionConfig.jsonPath);
                // Put this extraction config as the reference for all subsequent injections using this
                generatedExtractionConfigHashMap.put(extractionConfig, extractionConfig);
            }

            DynamicInjectionConfig.InjectionMeta actualInjectionConfig = new DynamicInjectionConfig.InjectionMeta(
                Collections.singletonList(injectionExtractionMeta.injectionConfig.apiPath),
                injectionExtractionMeta.injectionConfig.jsonPath,
                false,
                String.format("${Golden.Request: %s}" +
                        extractionConfig.nameSuffix,
                    injectionExtractionMeta.injectionConfig.jsonPath),
                null,
                injectionExtractionMeta.injectionConfig.method,
                Optional.empty(),
                injectionExtractionMeta.extractionConfig.apiPath,
                injectionExtractionMeta.extractionConfig.jsonPath,
                injectionExtractionMeta.extractionConfig.method);

            actualInjectionConfigList.add(actualInjectionConfig);
        }

        for (ExtractionConfig extractionConfig : generatedExtractionConfigHashMap.values()) {
            // Uniqueness in injection configs is assumed to be ensured in the imported csv
            DynamicInjectionConfig.ExtractionMeta actualExtractionConfig = new DynamicInjectionConfig.ExtractionMeta(
                extractionConfig.apiPath,
                extractionConfig.jsonPath,
                extractionConfig.method,
                String.format("${Golden.Response: %s}" +
                        extractionConfig.nameSuffix,
                    extractionConfig.jsonPath),
                String.format("${TestSet.Response: %s}", extractionConfig.jsonPath),
                true,
                false,
                Optional.empty());

            actualExtractionConfigList.add(actualExtractionConfig);
        }

        return new DynamicInjectionConfig(version, customerId, app, Optional.empty(),
            actualExtractionConfigList, actualInjectionConfigList, Collections.emptyList());

    }

}
