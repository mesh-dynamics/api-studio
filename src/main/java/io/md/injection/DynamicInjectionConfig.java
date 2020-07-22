package io.md.injection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

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

	public DynamicInjectionConfig(String version, String customerId, String app, Optional<Instant> timestamp,
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

		private ExtractionMeta() {
			apiPath = "";
			method = HTTPMethodType.POST;
			name = "";
			value = "";
			reset = true;
			valueObject = false;
		}

	}

	static public class InjectionMeta {

		@JsonProperty("apiPath")
		public final List<String> apiPaths;

		@JsonProperty("jsonPath")
		public final String jsonPath;

		// Can go away once we have regex matching for apiPath(s).
		@JsonProperty("injectAllPaths")
		public final boolean injectAllPaths;

		@JsonProperty("name")
		public final String name;

		public InjectionMeta() {
			this.apiPaths = Collections.EMPTY_LIST;
			this.jsonPath = "";
			this.injectAllPaths = false;
			this.name = "";
		}
	}

	public enum HTTPMethodType {
		GET,
		POST
	}


}