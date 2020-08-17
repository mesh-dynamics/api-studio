package io.md.injection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	// Default constructor for Jackson
	private DynamicInjectionConfig() {
		version = "";
		customerId = "";
		app = "";
		timestamp = Instant.now();
		extractionMetas = new ArrayList<>();
		injectionMetas = new ArrayList<>();
	}

	public DynamicInjectionConfig(String version, String customerId, String app,
		Optional<Instant> timestamp,
		List<ExtractionMeta> extractionMetas, List<InjectionMeta> injectionMetas) {
		this.version = version;
		this.customerId = customerId;
		this.app = app;
		this.timestamp = timestamp.orElse(Instant.now());
		this.extractionMetas = extractionMetas;
		this.injectionMetas = injectionMetas;
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

		public ExtractionMeta() {
			apiPath = "";
			method = HTTPMethodType.POST;
			name = "";
			value = "";
			reset = true;
			valueObject = false;
		}

		public ExtractionMeta(String apiPath, HTTPMethodType method,
			String name, String value, boolean reset, boolean valueObject) {
			this.apiPath = apiPath;
			this.method = method;
			this.name = name;
			this.value = value;
			this.reset = reset;
			this.valueObject = valueObject;
		}

	}

	static public class InjectionMeta {

		private static final Logger LOGGER = LoggerFactory.getLogger(InjectionMeta.class);

		@JsonProperty("apiPath")
		public final List<String> apiPaths;

		@JsonProperty("jsonPath")
		public final String jsonPath;

		// Can go away once we have regex matching for apiPath(s).
		@JsonProperty("injectAllPaths")
		public final boolean injectAllPaths;

		@JsonProperty("name")
		public final String name;

		@JsonProperty("regex")
		public final Optional<String> regex;

		public InjectionMeta() {
			this.apiPaths = Collections.EMPTY_LIST;
			this.jsonPath = "";
			this.injectAllPaths = false;
			this.name = "";
			this.regex = Optional.empty();
		}


		public InjectionMeta(List<String> apiPaths, String jsonPath, boolean injectAllPaths
			, String name, Optional<String> regex) {
			this.apiPaths = apiPaths;
			this.jsonPath = jsonPath;
			this.injectAllPaths = injectAllPaths;
			this.name = name;
			this.regex = regex;
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
			}).orElse(original);
		}


		public DataObj map(String original, DataObj replacement, ObjectMapper jsonMapper)
			throws DataObjProcessingException, PathNotFoundException {

			String replacementStr = replacement.getValAsString("");
			String replaced = map(original, replacementStr);
			return new JsonDataObj(replaced, jsonMapper);

		}

		public enum HTTPMethodType {
			GET,
			POST
		}

	}
}