package com.cube.learning;

import com.cube.learning.InjectionExtractionMeta.ExtractionConfig;
import io.md.injection.DynamicInjectionConfig;
import com.cube.learning.InjectionExtractionMeta;
import java.util.*;

public class DynamicInjectionGeneratedToActualConvertor {

    HashSet<InjectionExtractionMeta.ExtractionConfig> generatedExtractionConfigHashSet = new HashSet<>();
    List<DynamicInjectionConfig.ExtractionMeta> actualExtractionConfigList = new ArrayList<>();
    List<DynamicInjectionConfig.InjectionMeta> actualInjectionConfigList = new ArrayList<>();

    private Random random = new Random();

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
            if (!generatedExtractionConfigHashSet.contains(injectionExtractionMeta.extractionConfig)){
                injectionExtractionMeta.extractionConfig.nameSuffix = getNameSuffix(
                    injectionExtractionMeta.extractionConfig.apiPath,
                    injectionExtractionMeta.extractionConfig.jsonPath);
                generatedExtractionConfigHashSet.add(injectionExtractionMeta.extractionConfig);
            }

            DynamicInjectionConfig.InjectionMeta actualInjectionConfig = new DynamicInjectionConfig.InjectionMeta(
                Collections.singletonList(injectionExtractionMeta.injectionConfig.apiPath),
                injectionExtractionMeta.injectionConfig.jsonPath,
                false,
                String.format("${Golden.Request: %s}" +
                        injectionExtractionMeta.extractionConfig.nameSuffix,
                    injectionExtractionMeta.injectionConfig.jsonPath),
                null,
                injectionExtractionMeta.injectionConfig.method,
                Optional.empty());

            actualInjectionConfigList.add(actualInjectionConfig);
        }

        for (ExtractionConfig extractionConfig : generatedExtractionConfigHashSet) {
            // Uniqueness in injection configs is assumed to be ensured in the imported csv
            DynamicInjectionConfig.ExtractionMeta actualExtractionConfig = new DynamicInjectionConfig.ExtractionMeta(
                extractionConfig.apiPath,
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
            actualExtractionConfigList, actualInjectionConfigList);

    }

}
