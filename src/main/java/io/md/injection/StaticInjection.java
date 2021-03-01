package io.md.injection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjectionConfig.StaticValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class StaticInjection{


    public final String customerId;
    public final String app;
    public final String version;

    private final static String staticValPrefix = "staticVal_";

    public StaticInjection(String customerId, String app, String version) {
        this.version = version;
        this.customerId = customerId;
        this.app = app;
    }

    public DynamicInjectionConfig convertStaticMetasToDynamicConfig(List<StaticInjectionMeta> staticConfigList){
        final List<StaticValue> staticValueList = new ArrayList<>();
        final List<InjectionMeta> injectionMetaList = new ArrayList<>();
        staticConfigList.forEach(config -> {
            final String varName = staticValPrefix + config.value;
            staticValueList.add(new StaticValue(varName, config.value));
            injectionMetaList.add(
                new InjectionMeta(config.apiPaths, config.jsonPath, false,
                    varName, Optional.empty(), config.method, Optional.empty()));
        });
        return new DynamicInjectionConfig(version, customerId, app, Optional.empty(),
            Collections.emptyList(), injectionMetaList, staticValueList);
    }

    public static List<StaticInjectionMeta> getStaticMetasFromDynamicConfig(
        DynamicInjectionConfig diConfig) {
        HashMap<String, String> varNameToValueMap = new HashMap<>();
        List<StaticInjectionMeta> staticInjectionMetas = new ArrayList<>();
        diConfig.staticValues.forEach(staticValue -> {
            varNameToValueMap.put(staticValue.name, staticValue.value);
        });
        diConfig.injectionMetas.forEach(injectionMeta -> {
            Optional<String> value = Optional.ofNullable(varNameToValueMap.get(injectionMeta.name));
            value.ifPresent(val -> {
                staticInjectionMetas.add(new StaticInjectionMeta(val,
                    injectionMeta.apiPaths, injectionMeta.jsonPath, injectionMeta.method));
            });

        });
        return staticInjectionMetas;
    }
    @JsonPropertyOrder({"Value", "APIPaths", "JSONPath", "Method"})
    public static class StaticInjectionMeta {
        @JsonProperty("Value")
        final String value;
        @JsonProperty("APIPaths")
        final List<String> apiPaths;
        @JsonProperty("JSONPath")
        final String jsonPath;
        @JsonProperty("Method")
        final HTTPMethodType method;

        @JsonCreator
        public StaticInjectionMeta(@JsonProperty("Value") String value,
            @JsonProperty("APIPaths") List<String> apiPaths, @JsonProperty("JSONPath") String jsonPath,
            @JsonProperty("Method") HTTPMethodType method) {
            this.value = value;
            this.apiPaths = apiPaths;
            this.jsonPath = jsonPath;
            this.method = method;
        }
    }

}



