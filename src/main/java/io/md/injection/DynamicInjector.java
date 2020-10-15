package io.md.injection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.md.constants.Constants;
import io.md.dao.DataObj;
import io.md.dao.Event;
import io.md.dao.JsonDataObj;
import io.md.dao.Payload;
import io.md.services.DataStore;
import io.md.utils.ServerUtils;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class DynamicInjector {

    private static Logger LOGGER = LoggerFactory.getLogger(DynamicInjector.class);

    protected final Optional<DynamicInjectionConfig> dynamicInjectionConfig;
    protected final DataStore dataStore;
    protected final ObjectMapper jsonMapper;
    protected final Map<String, DataObj> extractionMap;

    public static Map<String , DataObj> convert(Map<String , String> extractionMap , ObjectMapper jsonMapper) {
        Map<String , DataObj> dataObjMap = new HashMap<>();
        for(Map.Entry<String , String> entry : extractionMap.entrySet()){
            JsonNode node = ServerUtils.convertStringToNode(entry.getValue(), jsonMapper);
            dataObjMap.put(entry.getKey() ,  new JsonDataObj(node , jsonMapper));
        }
        return dataObjMap;
    }
    public static Map<String , String> convertToStrMap(Map<String , DataObj> extractionMap) throws DataObj.DataObjProcessingException  {

        Map<String , String> strMap = new HashMap<>();
        for(Map.Entry<String , DataObj> entry : extractionMap.entrySet()){
            strMap.put(entry.getKey() , entry.getValue().serializeDataObj());
        }
        return strMap;
    }

    public DynamicInjector(Optional<DynamicInjectionConfig> diCfg , DataStore dataStore , ObjectMapper jsonMapper , Map<String, DataObj> extractionMap){
        this.dynamicInjectionConfig = diCfg;
        this.dataStore = dataStore;
        this.jsonMapper = jsonMapper;
        this.extractionMap = extractionMap;
    }

    public DynamicInjector(Optional<DynamicInjectionConfig> diCfg , DataStore dataStore , ObjectMapper jsonMapper){
        this.dynamicInjectionConfig = diCfg;
        this.dataStore = dataStore;
        this.jsonMapper = jsonMapper;
        this.extractionMap = new HashMap<>();
    }

    public void extract(Event goldenRequestEvent, Payload testResponsePayload) {
        dynamicInjectionConfig.ifPresent(dic-> {

            dic.extractionMetas.forEach(extractionMeta -> {
                // Test request is same as golden for replay extraction.
                InjectionVarResolver varResolver = new InjectionVarResolver(goldenRequestEvent,
                        testResponsePayload,
                        goldenRequestEvent.payload, dataStore);
                StringSubstitutor sub = new StringSubstitutor(varResolver);
                DataObj value;
                String requestHttpMethod = getHttpMethod(goldenRequestEvent);
                boolean apiPathMatch = apiPathMatch(Collections.singletonList(extractionMeta.apiPath), goldenRequestEvent.apiPath);
                if (apiPathMatch && extractionMeta.method.toString()
                        .equalsIgnoreCase(requestHttpMethod)) {
                    //  TODO ADD checks on reset field
                    String sourceString = extractionMeta.value;
                    // Boolean placeholder to specify if the value to be extracted
                    // is an Object and not a string.
                    // NOTE - if this is true value should be a single source & jsonPath
                    // (Only one placeholder of ${Source: JSONPath}
                    if (extractionMeta.valueObject) {
                        String lookupString = sourceString.trim()
                                .substring(sourceString.indexOf("{") + 1, sourceString.indexOf("}"));
                        value = varResolver.lookupObject(lookupString);
                    } else {
                        String valueString = sub.replace(sourceString);
                        value = new JsonDataObj(new TextNode(valueString), jsonMapper);
                    }
                    if (value != null) {
                        extractionMap
                                .put(sub.replace(extractionMeta.name), value);
                    }
                }
            });
        });
    }


    public void inject(Event request) {

        dynamicInjectionConfig.ifPresent(dic -> {
            dic.injectionMetas.forEach(injectionMeta -> {
                StringSubstitutor sub = new StringSubstitutor(
                        new InjectionVarResolver(request, null, request.payload, dataStore));

                String requestHttpMethod = getHttpMethod(request);
                boolean apiPathMatch = apiPathMatch(injectionMeta.apiPaths, request.apiPath);
                if ((injectionMeta.injectAllPaths || apiPathMatch) && injectionMeta.method
                        .toString().equalsIgnoreCase(requestHttpMethod)) {
                    String key = sub.replace(injectionMeta.name);
                    DataObj value = extractionMap.get(key);
                    try {
                        if (value != null) {
                            request.payload.put(injectionMeta.jsonPath,
                                    injectionMeta.map(request.payload
                                            .getValAsString(injectionMeta.jsonPath), value, jsonMapper));
                            LOGGER.info(String.format("Injecting value in request before replaying Key %s Value %s %s %s %s %s" , key , value , Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
                                    Constants.REQ_ID_FIELD, request.reqId));                        }
                        else {
                            LOGGER.info(String.format("Not injecting value as key not found in extraction map Key %s  %s %s %s %s" , key , Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
                                    Constants.REQ_ID_FIELD, request.reqId));
                        }

                    } catch (DataObj.PathNotFoundException e) {
                        LOGGER.error(String.format("Couldn't inject value as path not found in request Key %s  %s %s %s %s" , key , Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
                                Constants.REQ_ID_FIELD, request.reqId) , e);

                    } catch (Exception e) {
                        LOGGER.error(String.format("Exception occurred while injecting in request Key %s  %s %s %s %s" , key , Constants.JSON_PATH_FIELD, injectionMeta.jsonPath,
                                Constants.REQ_ID_FIELD, request.reqId) , e);
                    }

                }
            });
        });
    }

    static public String getHttpMethod(Event event) {
        String requestHttpMethod;
        try {
            requestHttpMethod= event.payload.getValAsString(Constants.METHOD_PATH);
        } catch (DataObj.PathNotFoundException e) {
            LOGGER
                    .error("Cannot find /method in request" + event.reqId + " No extraction", e);
            requestHttpMethod = "";
        }
        return requestHttpMethod;
    }

    static public boolean apiPathMatch(List<String> apiPathRegexes, String apiPathToMatch) {
        return apiPathRegexes.stream().anyMatch(regex -> {
            Pattern p = Pattern.compile(regex);
            return p.matcher(apiPathToMatch).matches();
        });
    }

    public Map<String, DataObj> getExtractionMap() {
        return extractionMap;
    }
}
