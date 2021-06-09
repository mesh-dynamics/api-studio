package com.cube.learning;

import io.md.injection.DynamicInjectionConfig;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.ExternalInjectionExtraction;
import io.md.injection.ExternalInjectionExtraction.ExternalExtraction;
import io.md.injection.ExternalInjectionExtraction.ExternalInjection;
import java.util.*;

public class DynamicInjectionGeneratedToActualConvertor {

    // Not using static vars as a single instance is expected to be created of this class.

    // This map holds Master extraction config object pointers to avoid duplicate extraction configs.
    HashMap<ExternalExtraction, ExternalExtraction> generatedExtractionConfigHashMap = new HashMap<>();
    List<ExtractionMeta> actualExtractionConfigList = new ArrayList<>();
    List<InjectionMeta> actualInjectionConfigList = new ArrayList<>();

    //Fixed seed to ensure reproducibility of config
    private final Random random = new Random(1);

    private String getNameSuffix(String apiPath, String jsonPath) {
        String[] apiPathList = apiPath.split("/");
        String[] keyPathList = jsonPath.split("/");
        // Add random number to further reduce chances of collision
        return "_" + apiPathList[apiPathList.length - 1] + "_" + keyPathList[keyPathList.length - 1]
            + "_" + (int) (random.nextFloat() * 1000);
    }

    public static String getVariableName(String eventId, String jsonPath, Optional<String> nameSuffix) {
        // ":-" symbol is a delimiter for specifying a default value specified by the library
        // - <empty> in this case.
        // Ref: https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html
        // This is to handle case for devtool where no Golden Response exists for the first
        // time a request is sent, yet we want ext/inj to happen. E.g. login.
        return String.format("${%s: %s:-}" + nameSuffix.orElse(""), eventId, jsonPath);
    }

    public static ExternalExtraction convertInternalExtractionToExternal(
        ExtractionMeta extractionMeta) {
        return new ExternalExtraction(extractionMeta.apiPath,
            extractionMeta.metadata.map(meta -> meta.extractionJsonPath).orElse(""),
            extractionMeta.method);
    }

    public static ExternalInjection convertInternalInjectiontoExternal(
        InjectionMeta injectionMeta) {
        return new ExternalInjection(
            injectionMeta.apiPaths.size() > 0 ? injectionMeta.apiPaths.get(0) : "",
            injectionMeta.jsonPath, injectionMeta.method, injectionMeta.keyTransform.orElse(""),
            injectionMeta.valueTransform.orElse(""), injectionMeta.injectAllPaths);
    }

    public static ExtractionMeta convertExternalExtractionToInternal(ExternalExtraction extConfig, String varName){
        return new ExtractionMeta(
            extConfig.apiPath,
            extConfig.jsonPath,
            extConfig.method,
            varName,
            getVariableName("TestSet.Response", extConfig.jsonPath,
                Optional.empty()),
            true,
            false,
            Optional.empty());
    }

    public static InjectionMeta convertExternalInjectionToInternal(
        ExternalInjection injConfig, String varName, String extApiPath, String extJsonPath,
        HTTPMethodType extMethod) {
        return new InjectionMeta(
            Optional.ofNullable(injConfig.apiPath)
                .map(Collections::singletonList).orElse(Collections.emptyList()),
            injConfig.jsonPath,
            injConfig.injectAllPaths,
            varName,

            // Include only if there is some change from the default
            injConfig.keyTransform == null || injConfig.keyTransform.equals("") ? Optional.empty()
                : Optional.of(injConfig.valueTransform),

            // Include only if there is some change from the default
            injConfig.valueTransform == null || injConfig.valueTransform.equals("") ? Optional.empty()
                : Optional.of(injConfig.valueTransform),

            null,
            injConfig.method,
            Optional.empty(),
            extApiPath,
            extJsonPath,
            extMethod);

    }

    public DynamicInjectionConfig convertExternalInjExtToInternal(String customerId, String app,
        String version,
        List<ExternalInjectionExtraction> externalInjectionExtractionList) {

        for (ExternalInjectionExtraction externalInjectionExtraction : externalInjectionExtractionList) {
            // Make sure only unique extraction configs survive

            ExternalExtraction externalExtraction = generatedExtractionConfigHashMap
                .get(externalInjectionExtraction.externalExtraction);

            if (externalExtraction == null) {
                // This is the first instance, and henceforth the Master instance
                externalExtraction = externalInjectionExtraction.externalExtraction;
                externalExtraction.nameSuffix = getNameSuffix(
                    externalInjectionExtraction.externalExtraction.apiPath,
                    externalInjectionExtraction.externalExtraction.jsonPath);
                // Put this extraction config as the reference for all subsequent injections using this
                generatedExtractionConfigHashMap.put(externalExtraction, externalExtraction);
            }

            ExternalInjection injConfig = externalInjectionExtraction.externalInjection;

            InjectionMeta actualInjectionConfig =
                convertExternalInjectionToInternal(injConfig, getVariableName("Golden.Request",
                    externalInjectionExtraction.externalInjection.jsonPath,
                    Optional.of(externalExtraction.nameSuffix)), externalExtraction.apiPath,
                    externalExtraction.jsonPath,
                    externalExtraction.method
                );

            actualInjectionConfigList.add(actualInjectionConfig);
        }

        for (ExternalExtraction externalExtraction : generatedExtractionConfigHashMap.values()) {
            // Uniqueness in injection configs is assumed to be ensured in the imported csv
            ExtractionMeta actualExtractionConfig = convertExternalExtractionToInternal(
                externalExtraction, getVariableName("Golden.Response", externalExtraction.jsonPath,
                    Optional.of(externalExtraction.nameSuffix))
            );

            actualExtractionConfigList.add(actualExtractionConfig);
        }

        return new DynamicInjectionConfig(version, customerId, app, Optional.empty(),
            actualExtractionConfigList, actualInjectionConfigList, Collections.emptyList(),
            externalInjectionExtractionList);

    }

}
