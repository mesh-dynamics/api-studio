package com.cube;

import io.md.injection.DynamicInjector;
import io.md.injection.InjectionVarResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import io.md.utils.Utils;
import org.apache.commons.text.StringSubstitutor;

import io.md.dao.CubeMetaInfo;
import io.md.dao.DataObj;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventType;
import io.md.dao.Event.RunType;
import io.md.dao.HTTPRequestPayload;
import io.md.dao.HTTPResponsePayload;
import io.md.dao.JsonDataObj;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording.RecordingType;
import io.md.injection.DynamicInjectionConfig;
import io.md.injection.DynamicInjectionConfig.ExtractionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;
import io.md.injection.DynamicInjector;
import io.md.injection.InjectionVarResolver;

import io.md.drivers.AbstractReplayDriver;
import com.cube.ws.Config;

public class DynamicInjectionTest {

	public static void main(String[] args) {
		try {

			List<ExtractionMeta> extractionMetaList = new ArrayList<>();
			List<InjectionMeta> injectionMetaList = new ArrayList<>();

			//String apiPath, HTTPMethodType method,
			//			String name, String value, boolean reset, boolean valueObject
			ExtractionMeta extractionMeta = new ExtractionMeta("minfo/health", HTTPMethodType.POST,
				"${Golden.Request: /hdrs/cookie/0 : cookie1 ([^;]+)}_value",
				"${TestSet.Response: /hdrs/cookie/0 : cookie1 ([^;]+)}", true, false, Optional.empty());

			ExtractionMeta extractionMeta2 = new ExtractionMeta("minfo/health", HTTPMethodType.POST,
				"${Golden.Request: /hdrs/cookie/0 : cookie2 ([^;\\]]+)}_value",
				"${TestSet.Response: /hdrs/cookie/0 : cookie2 ([^;\\]]+)}", true, false, Optional.empty());

			ExtractionMeta extractionMeta3 = new ExtractionMeta("minfo/health", HTTPMethodType.POST,
				"${Golden.Request}_value",
				"${TestSet.Response}", true, false, Optional.empty());

			extractionMetaList.add(extractionMeta);
			extractionMetaList.add(extractionMeta2);
			extractionMetaList.add(extractionMeta3);

			DynamicInjectionConfig dynamicInjectionConfig = new DynamicInjectionConfig("ver1",
				"ravivj", "RandomApp", Optional.empty(), extractionMetaList, injectionMetaList);

			CubeMetaInfo cubeMetaInfo = new CubeMetaInfo("random-user"
				, "test", "movie-info", "mi-rest");
			MDTraceInfo traceInfo = new MDTraceInfo("random-trace"
				, null, null);
			String apiPath = "/minfo/health";
			EventBuilder eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, apiPath, EventType.HTTPRequest
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden).withRunId(traceInfo.traceId);

			MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
			headers.add("content-type", MediaType.APPLICATION_JSON);
			headers.add("cookie", "cookie1 abc; cookie2 def");
			MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<>();
			queryParams.add("filmName", "Beverly Outlaw");
			eventBuilder.setPayload(new HTTPRequestPayload(headers, queryParams,
				null, "GET", null, apiPath));
			Event goldenRequestEvent = eventBuilder.createEvent();

			eventBuilder = new EventBuilder(cubeMetaInfo, traceInfo
				, RunType.Record, apiPath, EventType.HTTPResponse
				, Optional.empty(), "random-req-id", "random-collection", RecordingType.Golden).withRunId(traceInfo.traceId);

			headers = new MultivaluedHashMap<>();
			headers.add("content-type", MediaType.APPLICATION_JSON);
			headers.add("cookie", "cookie1 123; cookie2 456");
			eventBuilder.setPayload(new HTTPResponsePayload(headers,
				200, new byte[]{}));
			Event testResponseEvent = eventBuilder.createEvent();

			//Event goldenRequestEvent =

			Config config = new Config();
			Map<String, DataObj> extractionMap = new HashMap<>();
	/*		Optional<DynamicInjectionConfig> dynamicConfig =
				config.rrstore.getDynamicInjectionConfig("PaawanM" , "CourseApp" , "BodyInjectTest");
	*/
			dynamicInjectionConfig.extractionMetas.forEach(extMeta -> {
				InjectionVarResolver varResolver = new InjectionVarResolver(goldenRequestEvent,
					testResponseEvent.payload,
					goldenRequestEvent.payload, config.rrstore);
				StringSubstitutor sub = new StringSubstitutor(varResolver);
				DataObj value;
				String requestHttpMethod = Utils.getHttpMethod(goldenRequestEvent);
				if (extMeta.apiPath.equalsIgnoreCase(goldenRequestEvent.apiPath)
					&& extractionMeta.method.toString().equalsIgnoreCase(requestHttpMethod)) {
					//  TODO ADD checks for method type GET/POST & also on reset field
					String sourceString = extMeta.value;
					// Boolean placeholder to specify if the value to be extracted
					// is an Object and not a string.
					// NOTE - if this is true value should be a single source & jsonPath
					// (Only one placeholder of ${Source: JSONPath}
					if (extMeta.valueObject) {
						String lookupString = sourceString.trim()
							.substring(sourceString.indexOf("{") + 1, sourceString.indexOf("}"));
						value = varResolver.lookupObject(lookupString);
					} else {
						String valueString = sub.replace(sourceString);
						value = new JsonDataObj(valueString, config.jsonMapper);
					}
					extractionMap
						.put(sub.replace(extMeta.name), value);
				}
			});
			System.out.println(extractionMap);

			//public InjectionMeta(List<String> apiPaths,String jsonPath, boolean injectAllPaths, String name) {

			InjectionMeta injectionMeta1 = new InjectionMeta(Arrays.asList(""), "/hdrs/cookie/0",
				true
				, "${Golden.Request: /hdrs/cookie/0 : cookie1 ([^;]+)}_value"
				, Optional.of("cookie1 ([^;]+)"), HTTPMethodType.POST, Optional.empty());

			InjectionMeta injectionMeta2 = new InjectionMeta(Arrays.asList(""), "/hdrs/cookie/0",
				true
				, "${Golden.Request: /hdrs/cookie/0 : cookie2 ([^;\\]]+)}_value"
				, Optional.of("cookie2 ([^;\\]]+)"), HTTPMethodType.POST, Optional.empty());

			injectionMetaList.add(injectionMeta1);
			injectionMetaList.add(injectionMeta2);

			System.out.println(config.jsonMapper.writeValueAsString(goldenRequestEvent));

			injectionMetaList.forEach(injectionMeta -> {
				StringSubstitutor sub = new StringSubstitutor(
					new InjectionVarResolver(goldenRequestEvent, null
						, goldenRequestEvent.payload, config.rrstore));

				if (injectionMeta.injectAllPaths || injectionMeta.apiPaths
					.contains(goldenRequestEvent.apiPath)) {
					String key = sub.replace(injectionMeta.name);
					DataObj value = extractionMap.get(key);

					if (value != null) {
						try {
							goldenRequestEvent.payload.put(injectionMeta.jsonPath,
								injectionMeta.map(goldenRequestEvent.payload
										.getValAsString(injectionMeta.jsonPath), value,
									config.jsonMapper));
						} catch (Exception e) {
							e.printStackTrace();
						}


					}
				}
			});

			System.out.println(config.jsonMapper.writeValueAsString(goldenRequestEvent));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
