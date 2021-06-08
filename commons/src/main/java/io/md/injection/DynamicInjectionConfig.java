package io.md.injection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.DataObj;
import io.md.dao.DataObj.DataObjProcessingException;
import io.md.dao.DataObj.PathNotFoundException;
import io.md.dao.JsonDataObj;
import io.md.injection.DynamicInjectionConfig.InjectionMeta.HTTPMethodType;

public class DynamicInjectionConfig {

	@JsonProperty("version")
	public final String version;

	@JsonProperty("customerId")
	public final String customerId;

	@JsonProperty("app")
	public final String app;

	@JsonProperty("timestamp")
	public final Instant timestamp;

	@JsonProperty("extraction")
	public final List<ExtractionMeta> extractionMetas;

	@JsonProperty("injection")
	public final List<InjectionMeta> injectionMetas;

	@JsonProperty("static")
	public List<StaticValue> staticValues;

	@JsonIgnore
	public List<ExternalInjectionExtraction> externalInjectionExtractions;

	public static final String staticVersionSuffix = "::Static";

	public static String getAuthConfigVersion(String customer, String app){
		return customer + "::" + app;
	}
	// Default constructor for Jackson
	private DynamicInjectionConfig() {
		version = "";
		customerId = "";
		app = "";
		timestamp = Instant.now();
		extractionMetas = new ArrayList<>();
		injectionMetas = new ArrayList<>();
		staticValues = new ArrayList<>();
	}

	public DynamicInjectionConfig(String version, String customerId, String app,
		Optional<Instant> timestamp,
		List<ExtractionMeta> extractionMetas, List<InjectionMeta> injectionMetas,
		List<StaticValue> staticValues, List<ExternalInjectionExtraction> externalInjectionExtractions) {
		this.version = version;
		this.customerId = customerId;
		this.app = app;
		this.timestamp = timestamp.orElse(Instant.now());
		this.extractionMetas = extractionMetas;
		this.injectionMetas = injectionMetas;
		this.staticValues = staticValues;
		this.externalInjectionExtractions = externalInjectionExtractions;
	}

	static public class ExtractionMeta {

		@JsonProperty("apiPath")
		public final String apiPath;

		@JsonSetter

		@JsonProperty("method")
		public final HTTPMethodType method;

		@JsonProperty("name")
		public final String name;

		@JsonProperty("value")
		public final String value;

		@JsonProperty("reset")
		public final boolean reset;

		// Boolean placeholder to specify if the value to be extracted
		// is an Object and not a string. Defacto value to be false.
		// NOTE - if this is true value should be a single source & jsonPath
		// (Only one placeholder of ${Source: JSONPath}
		@JsonProperty("valueObject")
		public final boolean valueObject;

		@JsonProperty("forEach")
		public final Optional<ForEachStruct> forEach;

		@JsonProperty("metadata")
		public final Optional<Metadata> metadata;


		public ExtractionMeta() {
			metadata = Optional.empty();
			apiPath = "";
			method = HTTPMethodType.POST;
			name = "";
			value = "";
			reset = true;
			valueObject = false;
			forEach = Optional.empty();
		}

		public ExtractionMeta(String apiPath, String jsonPath, HTTPMethodType method,
			String name, String value, boolean reset, boolean valueObject,
			Optional<ForEachStruct> forEach) {
			this.metadata = Optional.of(new Metadata(getExtractionId(apiPath, jsonPath, method), jsonPath));
			this.apiPath = apiPath;
			this.method = method;
			this.name = name;
			this.value = value;
			this.reset = reset;
			this.valueObject = valueObject;
			this.forEach = forEach;
		}

		public static String getExtractionId(String apiPath, String jsonPath, HTTPMethodType method){
			return String.valueOf(Objects.hash(apiPath, jsonPath, method));
		}

	}

	static public class InjectionMeta {

		private static final Logger LOGGER = LogMgr.getLogger(InjectionMeta.class);

		@JsonProperty("apiPaths")
		public final List<String> apiPaths;

		@JsonProperty("jsonPath")
		public final String jsonPath;

		// Can go away once we have regex matching for apiPath(s).
		@JsonProperty("injectAllPaths")
		public final boolean injectAllPaths;

		@JsonProperty("name")
		public final String name;

		@JsonProperty("keyTransform")
		public final Optional<String> keyTransform;

		@JsonProperty("valueTransform")
		public final Optional<String> valueTransform;

		@JsonProperty("regex")
		public final Optional<String> regex;

		@JsonProperty("method")
		public final HTTPMethodType method;

		@JsonProperty("forEach")
		public final Optional<ForEachStruct> forEach;

		@JsonProperty("metadata")
		public final Optional<Metadata> metadata;

		@JsonIgnore
		public static final String keyTransformSeparator = "::";



		public InjectionMeta() {
			this(Collections.EMPTY_LIST, "", false, "", Optional.empty(), Optional.empty(),
				Optional.empty(),
				HTTPMethodType.POST, Optional.empty(), Optional.empty());
		}
		public InjectionMeta(List<String> apiPaths, String jsonPath, boolean injectAllPaths
			, String name, Optional<String> keyTransform, Optional<String> valueTransform, Optional<String> regex, HTTPMethodType method,
			Optional<ForEachStruct> forEach, Optional<Metadata> metadata) {
			this.apiPaths = apiPaths;
			this.jsonPath = jsonPath;
			this.injectAllPaths = injectAllPaths;
			this.name = name;
			this.keyTransform = keyTransform;
			this.valueTransform = valueTransform;
			this.regex = regex;
			this.method = method;
			this.forEach = forEach;
			this.metadata = metadata;
		}

		public InjectionMeta(List<String> apiPaths, String jsonPath, boolean injectAllPaths
			, String name, Optional<String> keyTransform, Optional<String> valueTransform, Optional<String> regex, HTTPMethodType method,
			Optional<ForEachStruct> forEach, String extractionApiPath, String extractionJsonPath,
			HTTPMethodType extractionMethod) {
			this(apiPaths, jsonPath, injectAllPaths, name, keyTransform, valueTransform, regex, method,
				forEach,
				Optional.of(new Metadata(ExtractionMeta
					.getExtractionId(extractionApiPath, extractionJsonPath, extractionMethod),
					extractionJsonPath)));
		}

		public String map(String original, String replacement) {
			return regex.map(rex -> {
				StringBuilder builder = new StringBuilder();
				Matcher matcher = Pattern.compile(rex).matcher(original);
				int lastIndex = 0;
				while (matcher.find()) {

					if (matcher.groupCount() == 0) {
						builder.append(original, lastIndex, matcher.start());
						builder.append(replacement);
						lastIndex = matcher.end();
					} else {
						for (int i = 1 ; i <= matcher.groupCount() ; i++) {
							builder.append(original, lastIndex, matcher.start(i));
							builder.append(replacement);
							lastIndex = matcher.end(i);
						}
					}

				}
				builder.append(original.substring(lastIndex));
				return builder.toString();
			}).orElse(replacement);
		}


		public DataObj map(String original, DataObj replacement, ObjectMapper jsonMapper)
			throws DataObjProcessingException, PathNotFoundException {

			String replacementStr = replacement.getValAsString("");
			String replaced = map(original, replacementStr);
			return new JsonDataObj(replaced, jsonMapper);

		}

		public enum HTTPMethodType {
			GET,
			POST,
			PUT,
			DELETE,
			PATCH
		}

	}

	static public class ForEachStruct {

		@JsonProperty("sourceForName")
		public final String sourceForName;

		@JsonProperty("sourceForValue")
		public final String sourceForValue;

		@JsonProperty("path")
		public final String path;

		// To be used later when matching has to be done on keys instead of path/indexes
		// in arrays
		@JsonProperty("keys")
		public final List<String> keys;

		public ForEachStruct() {
			sourceForName = "";
			sourceForValue = "";
			path = "";
			keys = Collections.EMPTY_LIST;
		}

		public ForEachStruct(String sourceForName, String sourceForValue, String path,
			List<String> keys) {
			this.sourceForName = sourceForName;
			this.sourceForValue = sourceForValue;
			this.path = path;
			this.keys = keys;
		}
	}

	static public class StaticValue {
		public final String name;

		public final String value;

		@JsonCreator
		public StaticValue(@JsonProperty("name") String name, @JsonProperty("value") String value) {
			this.name = name;
			this.value = value;
		}
	}

	static public class Metadata{
		@JsonProperty("extractionId")
		public String extractionId;

		@JsonProperty("extractionJsonPath")
		public String extractionJsonPath;

		public Metadata() {
			// Empty constructor for JSON deserialization
			this.extractionId = "";
			this.extractionJsonPath = "";
		}

		public Metadata(String extractionId,
			String extractionJsonPath) {
			this.extractionId = extractionId;
			this.extractionJsonPath = extractionJsonPath;
		}
	}
}