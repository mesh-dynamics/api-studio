/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.injection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.md.constants.Constants;
import io.md.dao.DataObj;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.Event;
import io.md.dao.JsonDataObj;
import io.md.dao.Payload;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.logger.LogMgr;
import io.md.services.DataStore;
import io.md.utils.Utils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

public class DynamicInjector {

	private static final Logger LOGGER = LogMgr.getLogger(DynamicInjector.class);
	private static final StringSubstitutor emptySubstitutor = new StringSubstitutor(
		new EmptyResolver());


	protected final Optional<DynamicInjectionConfig> dynamicInjectionConfig;
	protected final DataStore dataStore;
	protected final ObjectMapper jsonMapper;
	protected final Map<String, DataObj> extractionMap;

	public static Map<String, DataObj> convert(Map<String, String> extractionMap,
		ObjectMapper jsonMapper) {
		Map<String, DataObj> dataObjMap = new HashMap<>();
		for (Map.Entry<String, String> entry : extractionMap.entrySet()) {
			JsonNode node = Utils.convertStringToNode(entry.getValue(), jsonMapper);
			dataObjMap.put(entry.getKey(), new JsonDataObj(node, jsonMapper));
		}
		return dataObjMap;
	}

	public static Map<String, String> convertToStrMap(Map<String, DataObj> extractionMap)
		throws DataObj.DataObjProcessingException {

		Map<String, String> strMap = new HashMap<>();
		for (Map.Entry<String, DataObj> entry : extractionMap.entrySet()) {
			strMap.put(entry.getKey(), entry.getValue().serializeDataObj());
		}
		return strMap;
	}

	private void populateStaticValues(){
		this.dynamicInjectionConfig.ifPresent(config -> {
			config.staticValues.forEach(pair -> extractionMap
				.put(pair.name, new JsonDataObj(new TextNode(pair.value), jsonMapper)));
		});
	}

	public DynamicInjector(Optional<DynamicInjectionConfig> diCfg, DataStore dataStore,
		ObjectMapper jsonMapper, Map<String, DataObj> extractionMap) {
		this.dynamicInjectionConfig = diCfg;
		this.dataStore = dataStore;
		this.jsonMapper = jsonMapper;
		this.extractionMap = extractionMap;
		populateStaticValues();
	}

	public DynamicInjector(Optional<DynamicInjectionConfig> diCfg, DataStore dataStore,
		ObjectMapper jsonMapper) {
		this.dynamicInjectionConfig = diCfg;
		this.dataStore = dataStore;
		this.jsonMapper = jsonMapper;
		this.extractionMap = new HashMap<>();
		populateStaticValues();
	}

	public void extract(Event goldenRequestEvent, Payload testResponsePayload) {
		dynamicInjectionConfig.ifPresent(dic -> {

			dic.extractionMetas.forEach(extractionMeta -> {
				// Test request is same as golden for replay extraction.
				InjectionVarResolver varResolver = new InjectionVarResolver(goldenRequestEvent,
					testResponsePayload, goldenRequestEvent.payload, dataStore);
				StringSubstitutor sub = new StringSubstitutor(varResolver);

				// Detect if key not found in substitution map
				sub.setEnableUndefinedVariableException(true);

				String requestHttpMethod = Utils.getHttpMethod(goldenRequestEvent);
				boolean apiPathMatch = apiPathMatch(
					Collections.singletonList(extractionMeta.apiPath), goldenRequestEvent.apiPath);
				if (apiPathMatch && extractionMeta.method.toString()
					.equalsIgnoreCase(requestHttpMethod)) {

					Utils.ifPresentOrElse(extractionMeta.forEach ,  forEachStruct -> {
						Payload sourceForNamePayload = varResolver
							.getPayload(forEachStruct.sourceForName);
						Payload sourceForValuePayload = varResolver
							.getPayload(forEachStruct.sourceForValue);
						Map<String, String> keyValMap = new HashMap<>();
						sourceForNamePayload.collectKeyVals(
							path -> apiPathMatch(Collections.singletonList(forEachStruct.path),
								path), keyValMap);
						for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
							String resolvedPath = entry.getKey();
							varResolver.set_pathResolved(resolvedPath);
							varResolver.set_valResolved(entry.getValue());
							try {
								varResolver.set_matchedValResolved(
									sourceForValuePayload.getValAsString(resolvedPath));
							} catch (PathNotFoundException e) {
								LOGGER.error("Cannot find JSONPath" + resolvedPath
									+ " in rhs/valueSource. Setting _MatchedVal as null", e);
							}
							addEntryToExtractionMap(extractionMeta.name, extractionMeta.value,
								extractionMeta, varResolver, sub, goldenRequestEvent);
						}

					}, () ->
						// if foreach struct not present take it as a regular singular extraction
						addEntryToExtractionMap(extractionMeta.name, extractionMeta.value,
							extractionMeta, varResolver, sub, goldenRequestEvent));
				}
			});
		});
	}

	private void addEntryToExtractionMap(String name, String valueString,
		ExtractionMeta extractionMeta,
		InjectionVarResolver varResolver, StringSubstitutor sub, Event goldenRequestEvent) {

		//  TODO ADD checks on reset field
		DataObj value = null;
		String nameResolved = null;
		try {
			nameResolved = sub.replace(name);
		} catch (Exception e) {
			// Exception indicates Key couldn't be resolved at api path
			LOGGER.error(
				String.format("Extraction variable in Key not found at API path. Reusing golden. " +
						"Key %s %s %s %s %s",
					extractionMeta.name, Constants.API_PATH_FIELD, extractionMeta.apiPath,
					Constants.REQ_ID_FIELD, goldenRequestEvent.reqId));
			nameResolved = null;
		}

		// Boolean placeholder to specify if the value to be extracted
		// is an Object and not a string.
		// NOTE - if this is true value should be a single source & jsonPath
		// (Only one placeholder of ${Source: JSONPath}
		if (extractionMeta.valueObject) {
			String lookupString = valueString.trim()
				.substring(valueString.indexOf("{") + 1, valueString.indexOf("}"));
			value = varResolver.lookupObject(lookupString);
		} else {
			try {
				String valueStringResolved = sub.replace(valueString);
				value = new JsonDataObj(new TextNode(valueStringResolved), jsonMapper);
			} catch (Exception e) {
				// Exception indicates Value couldn't be resolved at api path
				LOGGER.error(String.format("Extraction variable in Value not found at API path. " +
						"Reusing golden. Value %s %s %s %s %s",
					extractionMeta.value, Constants.API_PATH_FIELD, extractionMeta.apiPath,
					Constants.REQ_ID_FIELD, goldenRequestEvent.reqId));
				value = null;
			}
		}

		if (nameResolved != null && value != null) {
			extractionMap.put(nameResolved, value);
		}
	}


	public void inject(Event request) {

		dynamicInjectionConfig.ifPresent(dic -> {
			dic.injectionMetas.forEach(injectionMeta -> {
				InjectionVarResolver varResolver = new InjectionVarResolver(request, null,
					request.payload, dataStore);
				StringSubstitutor sub = new StringSubstitutor(varResolver);

				boolean isResponse = !Event.isReqType(request.eventType);
				String requestHttpMethod = isResponse ? "" : Utils.getHttpMethod(request);
				boolean apiPathMatch = apiPathMatch(injectionMeta.apiPaths, request.apiPath);
				if (injectionMeta.injectAllPaths || (apiPathMatch && (isResponse || injectionMeta.method
					.toString().equalsIgnoreCase(requestHttpMethod)))) {
					Utils.ifPresentOrElse(injectionMeta.forEach , forEachStruct -> {
						Payload sourceForNamePayload = varResolver
							.getPayload(forEachStruct.sourceForName);
						Map<String, String> keyValMap = new HashMap<>();
						sourceForNamePayload.collectKeyVals(
							path -> apiPathMatch(Collections.singletonList(forEachStruct.path),
								path), keyValMap);
						for (Map.Entry<String, String> entry : keyValMap.entrySet()) {
							String resolvedPath = entry.getKey();
							varResolver.set_pathResolved(resolvedPath);
							varResolver.set_valResolved(entry.getValue());
							injectAtPath(injectionMeta.name, resolvedPath, injectionMeta, sub,
								request);
						}
					}, () -> injectAtPath(injectionMeta.name, injectionMeta.jsonPath, injectionMeta,
						sub, request));

				}
			});
		});
	}

	private String getTransformedValue(String original, String transformExpr){
		String[] fromAndTo = transformExpr.split(InjectionMeta.keyTransformSeparator);
		if (fromAndTo.length ==2 ){
			return original.replaceFirst(fromAndTo[0], fromAndTo[1]);
		}else{
			return original;
		}

	}

	private void injectAtPath(String name, String path, InjectionMeta injectionMeta,
		StringSubstitutor sub, Event request) {
		String key = sub.replace(name);

		if (injectionMeta.keyTransform.isPresent()){
			key = getTransformedValue(key, injectionMeta.keyTransform.get());
		}

		DataObj value = extractionMap.get(key);

		// Try non-inj-path-val-specific key search. Would only find this key if corresponding
		// extr doesn't have a golden resp.
		if (value == null) {
			key = emptySubstitutor.replace(name);
			value = extractionMap.get(key);
		}

		try {
			if (value != null) {

				DataObj xfmdValue;

				if (injectionMeta.valueTransform.isPresent() && value.isLeaf()) {
					// Transform is applicable only for leaf nodes
					String xfmdString = getTransformedValue(value.serializeDataObj(), injectionMeta.valueTransform.get());

					xfmdValue = new JsonDataObj(new TextNode(xfmdString), jsonMapper);

				} else {
					xfmdValue = value;
				}

				String orig;
				try {
					 orig = request.payload
						.getValAsString(path);
				} catch (PathNotFoundException e) {
					// Trying to create a new path
					orig = null;
				}
				if (orig == null) {
					request.payload.put(path,xfmdValue);
				} else {
					request.payload.put(path,
						injectionMeta.map(orig, xfmdValue, jsonMapper));
				}

				LOGGER.info(String.format(
					"Injecting value in request before replaying Key %s Value %s %s %s %s %s",
					key, xfmdValue, Constants.JSON_PATH_FIELD, path,
					Constants.REQ_ID_FIELD, request.reqId));
			} else {
				LOGGER.info(String.format(
					"Not injecting value as key not found in extraction map Key %s  %s %s %s %s",
					key, Constants.JSON_PATH_FIELD, path,
					Constants.REQ_ID_FIELD, request.reqId));
			}

		} catch (PathNotFoundException e) {
			LOGGER.error(String.format(
				"Couldn't inject value as path not found in request Key %s  %s %s %s %s",
				key, Constants.JSON_PATH_FIELD, path,
				Constants.REQ_ID_FIELD, request.reqId), e);

		} catch (Exception e) {
			LOGGER.error(String.format(
				"Exception occurred while injecting in request Key %s  %s %s %s %s",
				key, Constants.JSON_PATH_FIELD, path,
				Constants.REQ_ID_FIELD, request.reqId), e);
		}
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

	private static class EmptyResolver implements StringLookup {

		@Override
		public String lookup(String lookupString) {
			return "";
		}
	}

}


